package id.gustyxganteng.agent.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "chat_sessions")
data class ChatSessionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val model: String,
    val workspacePath: String?,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "messages",
    indices = [Index(value = ["sessionId"])]
)
data class MessageEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val role: String,
    val content: String,
    val reasoningContent: String?,
    val thinkingDurationMs: Long?,
    val toolCallsJson: String?,
    val toolCallId: String?,
    val imagesBase64Json: String?,
    val tokenUsageJson: String?,
    val isInterrupted: Boolean = false,
    val timestamp: Long
)

@Entity(tableName = "workspaces")
data class WorkspaceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val rootPath: String,
    val lastUsedAt: Long
)

@Entity(tableName = "user_custom_rules")
data class UserRuleEntity(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val instruction: String,
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
