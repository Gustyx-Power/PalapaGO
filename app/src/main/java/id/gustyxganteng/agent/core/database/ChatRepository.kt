package id.gustyxganteng.agent.core.database

import id.gustyxganteng.agent.core.database.dao.ChatSessionDao
import id.gustyxganteng.agent.core.database.dao.MessageDao
import id.gustyxganteng.agent.core.database.entity.ChatSessionEntity
import id.gustyxganteng.agent.core.database.entity.MessageEntity
import id.gustyxganteng.agent.core.model.ChatSession
import id.gustyxganteng.agent.core.model.Message
import id.gustyxganteng.agent.core.model.MessageRole
import id.gustyxganteng.agent.core.model.TokenUsage
import id.gustyxganteng.agent.core.model.ToolCall
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ChatRepository(
    private val sessionDao: ChatSessionDao,
    private val messageDao: MessageDao,
    private val json: Json = Json { ignoreUnknownKeys = true }
) {
    fun getAllSessions(): Flow<List<ChatSession>> {
        return sessionDao.getAllSessions().map { list ->
            list.map { entity ->
                ChatSession(
                    id = entity.id,
                    title = entity.title,
                    model = entity.model,
                    workspacePath = entity.workspacePath,
                    createdAt = entity.createdAt,
                    updatedAt = entity.updatedAt
                )
            }
        }
    }

    suspend fun getSessionById(sessionId: String): ChatSession? {
        val entity = sessionDao.getSessionById(sessionId) ?: return null
        return ChatSession(
            id = entity.id,
            title = entity.title,
            model = entity.model,
            workspacePath = entity.workspacePath,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    suspend fun saveSession(session: ChatSession) {
        sessionDao.insertOrUpdate(
            ChatSessionEntity(
                id = session.id,
                title = session.title,
                model = session.model,
                workspacePath = session.workspacePath,
                createdAt = session.createdAt,
                updatedAt = session.updatedAt
            )
        )
    }

    suspend fun deleteSession(sessionId: String) {
        messageDao.deleteMessagesForSession(sessionId)
        sessionDao.deleteSession(sessionId)
    }

    fun getMessagesForSession(sessionId: String): Flow<List<Message>> {
        return messageDao.getMessagesForSession(sessionId).map { list ->
            list.map { entityToMessage(it) }
        }
    }

    suspend fun getMessagesForSessionSync(sessionId: String): List<Message> {
        return messageDao.getMessagesForSessionSync(sessionId).map { entityToMessage(it) }
    }

    suspend fun saveMessage(message: Message) {
        messageDao.insertMessage(messageToEntity(message))
        val now = System.currentTimeMillis()
        val session = sessionDao.getSessionById(message.sessionId)
        if (session != null) {
            val isDefaultTitle = session.title.equals("New Chat", ignoreCase = true) ||
                session.title.equals("Obrolan Baru", ignoreCase = true) ||
                session.title.isBlank()
            if (isDefaultTitle && message.role == MessageRole.USER && message.content.isNotBlank()) {
                val titleCandidate = message.content.lines()
                    .firstOrNull { it.isNotBlank() }
                    ?.take(36)
                    ?.trim()
                if (!titleCandidate.isNullOrBlank()) {
                    sessionDao.updateSessionTitle(message.sessionId, titleCandidate, now)
                } else {
                    sessionDao.touchSession(message.sessionId, now)
                }
            } else {
                sessionDao.touchSession(message.sessionId, now)
            }
        }
    }

    suspend fun touchSession(sessionId: String) {
        sessionDao.touchSession(sessionId, System.currentTimeMillis())
    }

    suspend fun saveMessages(messages: List<Message>) {
        messageDao.insertMessages(messages.map { messageToEntity(it) })
        if (messages.isNotEmpty()) {
            sessionDao.touchSession(messages.first().sessionId, System.currentTimeMillis())
        }
    }

    suspend fun clearSessionMessages(sessionId: String) {
        messageDao.deleteMessagesForSession(sessionId)
    }

    private fun messageToEntity(msg: Message): MessageEntity {
        val roleStr = when (msg.role) {
            MessageRole.SYSTEM -> "system"
            MessageRole.USER -> "user"
            MessageRole.ASSISTANT -> "assistant"
            MessageRole.TOOL -> "tool"
        }
        val toolCallsJson = if (msg.toolCalls.isNotEmpty()) json.encodeToString(msg.toolCalls) else null
        val imagesJson = if (msg.imagesBase64.isNotEmpty()) json.encodeToString(msg.imagesBase64) else null
        val tokenJson = msg.tokenUsage?.let { json.encodeToString(it) }

        return MessageEntity(
            id = msg.id,
            sessionId = msg.sessionId,
            role = roleStr,
            content = msg.content,
            reasoningContent = msg.reasoningContent,
            thinkingDurationMs = msg.thinkingDurationMs,
            toolCallsJson = toolCallsJson,
            toolCallId = msg.toolCallId,
            imagesBase64Json = imagesJson,
            tokenUsageJson = tokenJson,
            timestamp = msg.timestamp
        )
    }

    private fun entityToMessage(entity: MessageEntity): Message {
        val role = when (entity.role.lowercase()) {
            "system" -> MessageRole.SYSTEM
            "user" -> MessageRole.USER
            "assistant" -> MessageRole.ASSISTANT
            "tool" -> MessageRole.TOOL
            else -> MessageRole.USER
        }

        val toolCalls: List<ToolCall> = entity.toolCallsJson?.let {
            try { json.decodeFromString(it) } catch (_: Exception) { emptyList() }
        } ?: emptyList()

        val images: List<String> = entity.imagesBase64Json?.let {
            try { json.decodeFromString(it) } catch (_: Exception) { emptyList() }
        } ?: emptyList()

        val tokenUsage: TokenUsage? = entity.tokenUsageJson?.let {
            try { json.decodeFromString(it) } catch (_: Exception) { null }
        }

        return Message(
            id = entity.id,
            sessionId = entity.sessionId,
            role = role,
            content = entity.content,
            reasoningContent = entity.reasoningContent,
            thinkingDurationMs = entity.thinkingDurationMs,
            toolCalls = toolCalls,
            toolCallId = entity.toolCallId,
            imagesBase64 = images,
            tokenUsage = tokenUsage,
            timestamp = entity.timestamp
        )
    }
}
