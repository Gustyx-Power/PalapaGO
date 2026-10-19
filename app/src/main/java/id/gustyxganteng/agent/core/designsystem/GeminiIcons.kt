package id.gustyxganteng.agent.core.designsystem

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import id.gustyxganteng.agent.R

@Composable
fun GeminiSparkleIcon(
    modifier: Modifier = Modifier,
    size: Dp = 18.dp,
    tint: Color = GeminiPrimary
) {
    val alpha = tint.alpha
    Image(
        painter = painterResource(id = R.drawable.ic_logo),
        contentDescription = "PalapaGO Logo",
        alpha = alpha,
        modifier = modifier.size(size)
    )
}
