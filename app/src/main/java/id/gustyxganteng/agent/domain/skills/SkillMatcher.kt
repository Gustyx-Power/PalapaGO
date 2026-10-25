package id.gustyxganteng.agent.domain.skills

import id.gustyxganteng.agent.core.model.PromptMode
import id.gustyxganteng.agent.domain.skills.model.AgentSkill

class SkillMatcher {

    fun matchSkills(
        userPrompt: String,
        promptMode: PromptMode,
        availableSkills: List<AgentSkill>,
        maxSkillsLimit: Int = 2
    ): List<AgentSkill> {
        val matched = mutableListOf<AgentSkill>()
        val promptLower = userPrompt.lowercase()

        // 1. Explicit Mode-driven matches
        when (promptMode) {
            PromptMode.IMAGE_GEN -> {
                availableSkills.find { it.id == "image-synthesis-mastery" }?.let { matched.add(it) }
            }
            PromptMode.ACADEMIC -> {
                availableSkills.find { it.id == "academic-researcher" }?.let { matched.add(it) }
            }
            else -> {}
        }

        // 2. Keyword/Heuristic-driven matches for remaining capacity
        for (skill in availableSkills) {
            if (matched.any { it.id == skill.id }) continue
            if (matched.size >= maxSkillsLimit) break

            val isTriggered = skill.triggers.any { trigger ->
                val trigLower = trigger.lowercase()
                promptLower.contains(trigLower)
            }

            if (isTriggered) {
                matched.add(skill)
            }
        }

        return matched.take(maxSkillsLimit)
    }
}
