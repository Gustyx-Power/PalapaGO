package id.gustyxganteng.agent.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ToolCall(
    val id: String,
    val type: String = "function",
    val function: FunctionCall
)

@Serializable
data class FunctionCall(
    val name: String,
    val arguments: String
)

@Serializable
data class ToolDefinition(
    val type: String = "function",
    val function: FunctionSpec
)

@Serializable
data class FunctionSpec(
    val name: String,
    val description: String,
    val parameters: ToolParametersSchema
)

@Serializable
data class ToolParametersSchema(
    val type: String = "object",
    val properties: Map<String, PropertySchema> = emptyMap(),
    val required: List<String> = emptyList()
)

@Serializable
data class PropertySchema(
    val type: String,
    val description: String? = null,
    val enum: List<String>? = null,
    val default: String? = null
)

@Serializable
data class ToolExecutionResult(
    val toolCallId: String,
    val toolName: String,
    val stdout: String = "",
    val stderr: String = "",
    val exitCode: Int = 0,
    val isSuccess: Boolean = true
)
