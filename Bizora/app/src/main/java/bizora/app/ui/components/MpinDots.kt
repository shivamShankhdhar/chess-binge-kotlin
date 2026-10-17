package bizora.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import bizora.app.ui.theme.LocalBizoraColors

/**
 * 4-Digit MPIN visual dots component matching React Native's MpinDots.tsx:
 * - 62x62dp circular slots with smooth states (empty, active with cursor pip, filled bullet, error)
 * - Horizontal shake animation sequence on error (-14dp -> +14dp -> -10dp -> +10dp -> -5dp -> 0dp)
 * - Brand primary / danger highlight borders and backgrounds
 */
@Composable
fun MpinDots(
    filledCount: Int,
    total: Int = 4,
    isError: Boolean = false,
    modifier: Modifier = Modifier
) {
    val colors = LocalBizoraColors.current
    val shakeOffset = remember { Animatable(0f) }

    LaunchedEffect(isError) {
        if (isError) {
            shakeOffset.snapTo(0f)
            shakeOffset.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 360
                    -14f at 60 using LinearEasing
                    14f at 120 using LinearEasing
                    -10f at 180 using LinearEasing
                    10f at 240 using LinearEasing
                    -5f at 300 using LinearEasing
                    0f at 360 using LinearEasing
                }
            )
        } else {
            shakeOffset.snapTo(0f)
        }
    }

    Row(
        modifier = modifier
            .offset(x = shakeOffset.value.dp)
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(total) { index ->
            val isFilled = index < filledCount
            val isActive = index == filledCount && !isError

            val slotBg = when {
                isError && isFilled -> colors.danger
                isFilled -> colors.brandPrimary
                isActive -> colors.brandTint
                else -> colors.surface
            }

            val slotBorderColor = when {
                isError && isFilled -> colors.danger
                isFilled -> colors.brandPrimary
                isActive -> colors.brandPrimary
                else -> colors.border
            }

            val slotBorderWidth = when {
                isError && isFilled -> 2.dp
                isFilled -> 2.dp
                isActive -> 2.dp
                else -> 1.5.dp
            }

            val slotScale = if (isActive) 1.03f else 1.0f
            val slotElevation = if (isFilled) 3.dp else if (isActive) 3.dp else 2.dp

            Box(
                modifier = Modifier
                    .size(62.dp)
                    .scale(slotScale)
                    .shadow(
                        elevation = slotElevation,
                        shape = CircleShape,
                        clip = false,
                        ambientColor = if (isFilled) colors.brandPrimary else Color(0x221B2210),
                        spotColor = if (isFilled) colors.brandPrimary else Color(0x221B2210)
                    )
                    .clip(CircleShape)
                    .background(slotBg)
                    .border(width = slotBorderWidth, color = slotBorderColor, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isError && isFilled -> {
                        // Error bullet (18dp white circle)
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                    }
                    isFilled -> {
                        // Filled bullet (18dp white circle with subtle shadow)
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .shadow(elevation = 1.dp, shape = CircleShape)
                                .clip(CircleShape)
                                .background(colors.textInverse)
                        )
                    }
                    isActive -> {
                        // Active cursor pip (9dp circle in brand primary)
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(colors.brandPrimary.copy(alpha = 0.85f))
                        )
                    }
                    else -> {
                        // Empty pip (9dp circle in border tone)
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFCAD5B8))
                        )
                    }
                }
            }
        }
    }
}
