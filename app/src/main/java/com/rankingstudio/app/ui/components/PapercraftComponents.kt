package com.rankingstudio.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rankingstudio.app.ui.theme.*

/**
 * Tactile Papercraft Card Container
 */
@Composable
fun PapercraftCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = PaperWhite,
    borderColor: Color = OutlineVariant,
    elevation: Dp = 4.dp,
    shape: Shape = RoundedCornerShape(8.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .shadow(elevation, shape)
            .background(backgroundColor, shape)
            .border(1.dp, borderColor, shape)
            .padding(16.dp),
        content = content
    )
}

/**
 * Tactile Cardstock Button with physical compression press state
 */
@Composable
fun PapercraftButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = PrimarySandishBrown,
    contentColor: Color = OnPrimaryWhite,
    shape: Shape = RoundedCornerShape(8.dp),
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val elevation = if (isPressed) 1.dp else 4.dp
    val offsetY = if (isPressed) 2.dp else 0.dp

    Surface(
        modifier = modifier
            .offset(y = offsetY)
            .shadow(elevation, shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        shape = shape,
        color = if (enabled) backgroundColor else backgroundColor.copy(alpha = 0.5f),
        contentColor = contentColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

/**
 * Perforated / Dashed Papercraft Divider Line
 */
@Composable
fun PapercraftDashedDivider(
    modifier: Modifier = Modifier,
    color: Color = OutlineBrown.copy(alpha = 0.4f),
    thickness: Dp = 2.dp,
    dashLength: Dp = 6.dp,
    gapLength: Dp = 4.dp
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(thickness)
    ) {
        val pathEffect = PathEffect.dashPathEffect(
            floatArrayOf(dashLength.toPx(), gapLength.toPx()), 0f
        )
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(0f, size.height / 2),
            end = androidx.compose.ui.geometry.Offset(size.width, size.height / 2),
            strokeWidth = thickness.toPx(),
            pathEffect = pathEffect
        )
    }
}
