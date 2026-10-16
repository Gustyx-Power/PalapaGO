package id.gustyxganteng.agent.core.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun GeminiSparkleIcon(
    modifier: Modifier = Modifier,
    size: Dp = 18.dp,
    tint: Color = GeminiPrimary
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val cx = w / 2f
        val cy = h / 2f

        val path = Path().apply {
            moveTo(cx, 0f)
            cubicTo(cx, cy * 0.55f, cx + w * 0.25f, cy, w, cy)
            cubicTo(cx + w * 0.25f, cy, cx, cy + h * 0.25f, cx, h)
            cubicTo(cx, cy + h * 0.25f, cx - w * 0.25f, cy, 0f, cy)
            cubicTo(cx - w * 0.25f, cy, cx, cy * 0.55f, cx, 0f)
            close()
        }

        drawPath(
            path = path,
            color = tint
        )
    }
}
