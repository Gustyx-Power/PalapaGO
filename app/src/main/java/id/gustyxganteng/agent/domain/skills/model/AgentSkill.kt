package id.gustyxganteng.agent.domain.skills.model

data class AgentSkill(
    val id: String,
    val name: String,
    val description: String,
    val triggers: List<String>,
    val instructions: String,
    val isBuiltIn: Boolean = false
)
