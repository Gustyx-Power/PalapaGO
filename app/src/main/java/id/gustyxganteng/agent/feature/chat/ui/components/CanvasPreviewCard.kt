package id.gustyxganteng.agent.feature.chat.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Web
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.gustyxganteng.agent.R
import id.gustyxganteng.agent.core.designsystem.GeminiActiveChipContainer
import id.gustyxganteng.agent.core.designsystem.GeminiActiveChipContent
import id.gustyxganteng.agent.core.designsystem.GeminiPrimary
import id.gustyxganteng.agent.core.designsystem.GeminiSurface
import id.gustyxganteng.agent.core.designsystem.GeminiTextPrimary
import id.gustyxganteng.agent.core.designsystem.GeminiTextSecondary

@Composable
fun CanvasPreviewCard(
    title: String,
    language: String,
    onOpenCanvas: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isWeb = language.lowercase() in listOf("html", "js", "css", "web")

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = GeminiSurface,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Icon(
                imageVector = if (isWeb) Icons.Rounded.Web else Icons.Rounded.Code,
                contentDescription = null,
                tint = GeminiPrimary,
                modifier = Modifier.size(28.dp)
            )

            Spacer(Modifier.width(12.dp))

            // Title & Language
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = GeminiTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.canvas_language, language.uppercase()),
                    style = MaterialTheme.typography.labelSmall,
                    color = GeminiTextSecondary
                )
            }

            Spacer(Modifier.width(8.dp))

            // Action Pill Button
            Button(
                onClick = onOpenCanvas,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GeminiActiveChipContainer,
                    contentColor = GeminiActiveChipContent
                )
            ) {
                Text(
                    text = stringResource(R.string.canvas_open_button),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
