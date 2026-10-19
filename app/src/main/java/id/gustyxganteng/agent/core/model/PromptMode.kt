package id.gustyxganteng.agent.core.model

import androidx.annotation.StringRes
import id.gustyxganteng.agent.R

enum class PromptMode(
    val id: String,
    @StringRes val titleRes: Int,
    @StringRes val descRes: Int
) {
    CHAT(
        id = "chat",
        titleRes = R.string.prompt_mode_chat,
        descRes = R.string.prompt_mode_chat_desc
    ),
    AGENT(
        id = "agent",
        titleRes = R.string.prompt_mode_agent,
        descRes = R.string.prompt_mode_agent_desc
    ),
    ACADEMIC(
        id = "academic",
        titleRes = R.string.prompt_mode_academic,
        descRes = R.string.prompt_mode_academic_desc
    ),
    CANVAS(
        id = "canvas",
        titleRes = R.string.prompt_mode_canvas,
        descRes = R.string.prompt_mode_canvas_desc
    ),
    IMAGE_GEN(
        id = "image_gen",
        titleRes = R.string.prompt_mode_image_gen,
        descRes = R.string.prompt_mode_image_gen_desc
    );

    companion object {
        fun fromId(id: String?): PromptMode {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: CHAT
        }
    }
}
