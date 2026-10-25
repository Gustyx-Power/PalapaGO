package id.gustyxganteng.agent.domain.rules.model

enum class RuleSource {
    SYSTEM,
    WORKSPACE,
    USER
}

data class AgentRule(
    val id: String,
    val title: String,
    val content: String,
    val source: RuleSource,
    val isEnabled: Boolean = true
)
