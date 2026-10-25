package id.gustyxganteng.agent.domain.rules

import id.gustyxganteng.agent.core.database.dao.UserRuleDao
import id.gustyxganteng.agent.domain.rules.model.AgentRule
import id.gustyxganteng.agent.domain.rules.model.RuleSource
import java.io.File

class AgentRulesManager(
    private val userRuleDao: UserRuleDao
) {

    suspend fun getActiveRules(
        workspacePath: String?,
        yoloMode: Boolean,
        antiAiSlopMode: Boolean
    ): List<AgentRule> {
        val rules = mutableListOf<AgentRule>()

        // 1. System Invariants (Highest Priority)
        rules.add(
            AgentRule(
                id = "sys_sandbox",
                title = "Workspace Sandboxing & Boundary Isolation",
                content = """
                - Platform: Android OS with Toybox/Bionic environment.
                - All file writes, patches, and read operations must stay strictly within the active workspace root.
                - Path traversal outside workspace (e.g., using '../../' to target root or system private directories) is strictly prohibited.
                - ${if (yoloMode) "YOLO Mode Active: Automatic execution enabled, but irreversible destruction (e.g. rm -rf /, format storage) is strictly forbidden." else "Approval Mode Active: Destructive operations, file modifications, and shell executions require explicit user approval via Diff Approval Dialog."}
                """.trimIndent(),
                source = RuleSource.SYSTEM
            )
        )

        if (antiAiSlopMode) {
            rules.add(
                AgentRule(
                    id = "sys_anti_slop",
                    title = "Anti-AI-Slop & Direct Communication Standard",
                    content = """
                    - Zero sycophancy or conversational filler: Never start with 'Tentu!', 'Siap!', 'Pertanyaan bagus!', or 'Certainly!'. Start directly with the answer or action.
                    - Banned buzzwords: delve, foster, leverage, utilize, seamless, cutting-edge, inovasi terdepan, solusi komprehensif, di era digital.
                    - Zero narrator comments in code (e.g. '// Initialize state', '// Handle click'). Write comments only for non-obvious WHY reasons.
                    - Zero lazy placeholders (e.g. '// ... rest of code', '// TODO'). Provide full, operational implementations.
                    """.trimIndent(),
                    source = RuleSource.SYSTEM
                )
            )
        }

        // 2. Workspace Project Rules (AGENTS.md and .agents/rules/*.md)
        if (!workspacePath.isNullOrBlank()) {
            val workspaceDir = File(workspacePath)
            if (workspaceDir.exists() && workspaceDir.isDirectory) {
                // Check root AGENTS.md
                val agentsFile = File(workspaceDir, "AGENTS.md")
                if (agentsFile.exists() && agentsFile.isFile) {
                    val content = agentsFile.readText().trim()
                    if (content.isNotBlank()) {
                        rules.add(
                            AgentRule(
                                id = "workspace_agents_md",
                                title = "Project Invariants (AGENTS.md)",
                                content = content,
                                source = RuleSource.WORKSPACE
                            )
                        )
                    }
                }

                // Check .agents/rules/*.md
                val rulesDir = File(workspaceDir, ".agents/rules")
                if (rulesDir.exists() && rulesDir.isDirectory) {
                    rulesDir.listFiles { file -> file.isFile && file.extension.equals("md", ignoreCase = true) }
                        ?.forEach { ruleFile ->
                            val content = ruleFile.readText().trim()
                            if (content.isNotBlank()) {
                                rules.add(
                                    AgentRule(
                                        id = "workspace_rule_${ruleFile.nameWithoutExtension}",
                                        title = "Workspace Rule: ${ruleFile.nameWithoutExtension}",
                                        content = content,
                                        source = RuleSource.WORKSPACE
                                    )
                                )
                            }
                        }
                }
            }
        }

        // 3. User Custom Rules (Room DB)
        try {
            val userRules = userRuleDao.getEnabledRulesSync()
            userRules.forEach { entity ->
                rules.add(
                    AgentRule(
                        id = "user_${entity.id}",
                        title = entity.title,
                        content = entity.instruction,
                        source = RuleSource.USER
                    )
                )
            }
        } catch (_: Exception) {
            // DB fallback
        }

        return rules
    }

    fun formatRulesPrompt(rules: List<AgentRule>): String {
        if (rules.isEmpty()) return ""

        val sb = StringBuilder()
        sb.appendLine("### ACTIVE AGENT RULES & CONSTRAINTS:")

        val systemRules = rules.filter { it.source == RuleSource.SYSTEM }
        if (systemRules.isNotEmpty()) {
            sb.appendLine("<system_rules>")
            systemRules.forEach { rule ->
                sb.appendLine("#### ${rule.title}")
                sb.appendLine(rule.content)
            }
            sb.appendLine("</system_rules>")
        }

        val workspaceRules = rules.filter { it.source == RuleSource.WORKSPACE }
        if (workspaceRules.isNotEmpty()) {
            sb.appendLine("<workspace_rules>")
            workspaceRules.forEach { rule ->
                sb.appendLine("#### ${rule.title}")
                sb.appendLine(rule.content)
            }
            sb.appendLine("</workspace_rules>")
        }

        val userRules = rules.filter { it.source == RuleSource.USER }
        if (userRules.isNotEmpty()) {
            sb.appendLine("<user_rules>")
            userRules.forEach { rule ->
                sb.appendLine("#### ${rule.title}")
                sb.appendLine(rule.content)
            }
            sb.appendLine("</user_rules>")
        }

        return sb.toString().trim()
    }
}
