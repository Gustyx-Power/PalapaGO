package id.gustyxganteng.agent.domain.skills

import id.gustyxganteng.agent.core.model.PromptMode
import id.gustyxganteng.agent.domain.skills.model.AgentSkill
import java.io.File

class AgentSkillsManager(
    private val skillMatcher: SkillMatcher = SkillMatcher()
) {

    private val builtInSkills: List<AgentSkill> = listOf(
        AgentSkill(
            id = "image-synthesis-mastery",
            name = "Image Synthesis & Visual Composition",
            description = "Expert prompt engineering and composition director for AI image synthesis.",
            triggers = listOf("gambar", "lukis", "poster", "ilustrasi", "rasio", "image", "aspect ratio", "art style", "visual design"),
            instructions = """
            - Prompt Structuring: Define subject, environment, lighting, composition, and artistic medium explicitly.
            - Aspect Ratio Optimization: Recommend aspect ratios based on use-case (1:1 for social avatars/thumbnails, 16:9 for landscape banners/presentations, 9:16 for mobile wallpapers/stories).
            - Typography & Text: Enclose explicit text for banners or posters in double quotes.
            - Visual Layering: Specify foreground, midground, and background depth with cohesive cinematic color palettes.
            """.trimIndent(),
            isBuiltIn = true
        ),
        AgentSkill(
            id = "academic-researcher",
            name = "Academic Literature & DOI Verification",
            description = "Multi-database academic literature search, DOI validation, and formal bibliography styling.",
            triggers = listOf("jurnal", "paper", "doi", "sitasi", "pustaka", "rujukan", "crossref", "openalex", "semantic scholar", "mendeley", "tesis", "skripsi", "citation"),
            instructions = """
            - Evidence Grounding: Never hallucinate paper titles, author names, publication years, or DOI numbers.
            - Tool Orchestration: Query `search_academic_papers` via Semantic Scholar and OpenAlex. Validate citation authenticity with `verify_doi_or_citation` via Crossref.
            - Citation Standards: When requested, output exact citations formatted in APA 7th, IEEE, or BibTeX.
            - Open Access Delivery: Surface direct PDF download links when papers are marked Open Access.
            """.trimIndent(),
            isBuiltIn = true
        ),
        AgentSkill(
            id = "android-compose-expert",
            name = "Jetpack Compose & Clean Architecture",
            description = "Idiomatic Android Kotlin 2.0 and Compose Material 3 implementation patterns.",
            triggers = listOf("compose", "composable", "recomposition", "modifier", "scaffold", "viewmodel", "uistate", "stateflow", "side-effect", "launchedeffect"),
            instructions = """
            - State Hoisting & UDF: Expose read-only StateFlow<UiState> from ViewModel; pass events up via lambda callbacks.
            - Recomposition Protection: Use @Immutable / @Stable data structures; avoid passing unstable lambdas or allocations inside composable bodies.
            - Material 3 Design: Use surface color layering (Canvas -> Card -> Elevated) with 1.dp hairline borders; avoid cheesy text gradients or heavy neon glows.
            - Function Sizing: Break UI components into modular functions under 150 lines.
            """.trimIndent(),
            isBuiltIn = true
        ),
        AgentSkill(
            id = "shell-automation-guard",
            name = "Android Toybox & Shell Scripting",
            description = "Safe shell execution in Android Toybox and Termux environments.",
            triggers = listOf("bash", "terminal", "shell", "script", "command", "process", "toybox", "termux"),
            instructions = """
            - Mobile Shell Grounding: Remember that standard Android uses Toybox/Bionic. Prefer portable POSIX constructs over non-standard Linux binaries.
            - Working Directory: Always execute commands relative to the active workspace folder.
            - Context Safety: Limit shell outputs to essential logs to prevent token exhaustion.
            - Destructive Guard: Refuse or request explicit approval before running any recursive removal or disk-modifying commands.
            """.trimIndent(),
            isBuiltIn = true
        )
    )

    fun getAllAvailableSkills(workspacePath: String?): List<AgentSkill> {
        val allSkills = builtInSkills.toMutableList()

        if (!workspacePath.isNullOrBlank()) {
            val workspaceSkillsDir = File(workspacePath, ".agents/skills")
            if (workspaceSkillsDir.exists() && workspaceSkillsDir.isDirectory) {
                workspaceSkillsDir.listFiles { file -> file.isDirectory }?.forEach { skillDir ->
                    val skillFile = File(skillDir, "SKILL.md")
                    if (skillFile.exists() && skillFile.isFile) {
                        val content = skillFile.readText().trim()
                        if (content.isNotBlank()) {
                            val skillName = skillDir.name
                            allSkills.add(
                                AgentSkill(
                                    id = "workspace_$skillName",
                                    name = skillName,
                                    description = "Custom workspace skill from ${skillDir.name}",
                                    triggers = listOf(skillName.replace("-", " "), skillName),
                                    instructions = content,
                                    isBuiltIn = false
                                )
                            )
                        }
                    }
                }
            }
        }

        return allSkills
    }

    fun selectSkillsForTurn(
        userPrompt: String,
        promptMode: PromptMode,
        workspacePath: String?
    ): List<AgentSkill> {
        val available = getAllAvailableSkills(workspacePath)
        return skillMatcher.matchSkills(
            userPrompt = userPrompt,
            promptMode = promptMode,
            availableSkills = available,
            maxSkillsLimit = 2
        )
    }

    fun formatSkillsPrompt(skills: List<AgentSkill>): String {
        if (skills.isEmpty()) return ""

        val sb = StringBuilder()
        sb.appendLine("### DYNAMICALLY ACTIVATED AGENT SKILLS:")
        skills.forEach { skill ->
            sb.appendLine("<agent_skill name=\"${skill.id}\" title=\"${skill.name}\">")
            sb.appendLine(skill.instructions)
            sb.appendLine("</agent_skill>")
        }
        return sb.toString().trim()
    }
}
