package id.gustyxganteng.agent.core.designsystem

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val M3ExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

val M3ExpressiveUserBubbleShape = RoundedCornerShape(
    topStart = 24.dp,
    topEnd = 24.dp,
    bottomEnd = 6.dp,
    bottomStart = 24.dp
)

val M3ExpressiveAssistantBubbleShape = RoundedCornerShape(
    topStart = 6.dp,
    topEnd = 24.dp,
    bottomEnd = 24.dp,
    bottomStart = 24.dp
)

val M3ExpressivePillShape = CircleShape

val M3ExpressiveBottomSheetShape = RoundedCornerShape(
    topStart = 32.dp,
    topEnd = 32.dp,
    bottomStart = 0.dp,
    bottomEnd = 0.dp
)

val M3ExpressiveCardShape = RoundedCornerShape(20.dp)
val M3ExpressiveSmallCardShape = RoundedCornerShape(14.dp)
val M3ExpressiveChipShape = RoundedCornerShape(12.dp)
