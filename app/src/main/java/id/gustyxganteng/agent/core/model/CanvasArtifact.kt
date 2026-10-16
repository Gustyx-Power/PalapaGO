package id.gustyxganteng.agent.core.model

import kotlinx.serialization.Serializable

@Serializable
data class CanvasArtifact(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val language: String,
    val content: String,
    val associatedFilePath: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)

@Serializable
data class WorkspaceConfig(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val rootPath: String,
    val ignoredPatterns: List<String> = listOf(".git", "node_modules", "build", ".gradle", ".idea"),
    val lastUsedAt: Long = System.currentTimeMillis()
)
