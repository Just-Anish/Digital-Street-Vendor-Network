package com.example.digitalstreetvendornetwork.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.digitalstreetvendornetwork.R

/**
 * Visual state of a single step within [OrderProgressTracker].
 */
private enum class StepState {
    DONE,
    CURRENT,
    FUTURE,
    DECLINED
}

/**
 * A visual step-by-step progress tracker for orders:
 * Placed (PENDING) -> Preparing (PREPARING) -> Completed (COMPLETED).
 *
 * Handles DECLINED status with a red indicator and renders nothing for unknown statuses.
 */
@Composable
fun OrderProgressTracker(
    status: String,
    modifier: Modifier = Modifier
) {
    val normalized = status.uppercase().trim()
    if (normalized !in setOf("PENDING", "PREPARING", "COMPLETED", "DECLINED")) {
        return
    }

    val isDeclined = normalized == "DECLINED"

    // Derive state for Step 0 (Placed)
    val step0State = when (normalized) {
        "PENDING" -> StepState.CURRENT
        else -> StepState.DONE
    }

    // Derive state for Step 1 (Preparing / Declined)
    val step1State = when (normalized) {
        "PENDING" -> StepState.FUTURE
        "PREPARING" -> StepState.CURRENT
        "COMPLETED" -> StepState.DONE
        "DECLINED" -> StepState.DECLINED
        else -> StepState.FUTURE
    }

    // Derive state for Step 2 (Completed)
    val step2State = when (normalized) {
        "COMPLETED" -> StepState.DONE
        else -> StepState.FUTURE
    }

    // Connector 0 (Placed -> Preparing/Declined) fill and color
    val connector0TargetFill = if (normalized != "PENDING") 1f else 0f
    val connector0Progress by animateFloatAsState(
        targetValue = connector0TargetFill,
        animationSpec = tween(500),
        label = "connector0Progress"
    )
    val connector0Color by animateColorAsState(
        targetValue = if (isDeclined) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        animationSpec = tween(400),
        label = "connector0Color"
    )

    // Connector 1 (Preparing -> Completed) fill and color
    val connector1TargetFill = if (normalized == "COMPLETED") 1f else 0f
    val connector1Progress by animateFloatAsState(
        targetValue = connector1TargetFill,
        animationSpec = tween(500),
        label = "connector1Progress"
    )
    val connector1Color by animateColorAsState(
        targetValue = MaterialTheme.colorScheme.primary,
        animationSpec = tween(400),
        label = "connector1Color"
    )

    // Infinite transition for pulsing current step ring
    val infiniteTransition = rememberInfiniteTransition(label = "pulseRingTransition")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )

    val trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Step 0: Placed
        ProgressStepItem(
            label = stringResource(R.string.step_placed),
            state = step0State,
            pulseScale = pulseScale,
            pulseAlpha = pulseAlpha
        )

        // Connector 0
        ProgressConnector(
            progress = connector0Progress,
            activeColor = connector0Color,
            trackColor = trackColor,
            modifier = Modifier.weight(1f)
        )

        // Step 1: Preparing or Declined
        ProgressStepItem(
            label = if (isDeclined) stringResource(R.string.step_declined) else stringResource(R.string.step_preparing),
            state = step1State,
            pulseScale = pulseScale,
            pulseAlpha = pulseAlpha
        )

        // Connector 1
        ProgressConnector(
            progress = connector1Progress,
            activeColor = connector1Color,
            trackColor = trackColor,
            modifier = Modifier.weight(1f)
        )

        // Step 2: Completed
        ProgressStepItem(
            label = stringResource(R.string.step_completed),
            state = step2State,
            pulseScale = pulseScale,
            pulseAlpha = pulseAlpha
        )
    }
}

@Composable
private fun ProgressStepItem(
    label: String,
    state: StepState,
    pulseScale: Float,
    pulseAlpha: Float,
    modifier: Modifier = Modifier
) {
    val targetCircleColor = when (state) {
        StepState.DONE, StepState.CURRENT -> MaterialTheme.colorScheme.primary
        StepState.DECLINED -> MaterialTheme.colorScheme.error
        StepState.FUTURE -> Color.Transparent
    }
    val circleColor by animateColorAsState(
        targetValue = targetCircleColor,
        animationSpec = tween(400),
        label = "circleColor_$label"
    )

    val targetBorderColor = when (state) {
        StepState.FUTURE -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        else -> Color.Transparent
    }
    val borderColor by animateColorAsState(
        targetValue = targetBorderColor,
        animationSpec = tween(400),
        label = "borderColor_$label"
    )

    val targetLabelColor = when (state) {
        StepState.DONE -> MaterialTheme.colorScheme.onSurface
        StepState.CURRENT -> MaterialTheme.colorScheme.primary
        StepState.DECLINED -> MaterialTheme.colorScheme.error
        StepState.FUTURE -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    }
    val labelColor by animateColorAsState(
        targetValue = targetLabelColor,
        animationSpec = tween(400),
        label = "labelColor_$label"
    )

    Column(
        modifier = modifier.width(68.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(28.dp),
            contentAlignment = Alignment.Center
        ) {
            // Soft pulsing ring for current step
            if (state == StepState.CURRENT) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .graphicsLayer {
                            scaleX = pulseScale
                            scaleY = pulseScale
                            alpha = pulseAlpha
                        }
                        .background(circleColor, CircleShape)
                )
            }

            // Core step circle
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(circleColor, CircleShape)
                    .then(
                        if (state == StepState.FUTURE) {
                            Modifier.border(1.5.dp, borderColor, CircleShape)
                        } else {
                            Modifier
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                when (state) {
                    StepState.DONE -> {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = stringResource(R.string.cd_completed),
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    StepState.DECLINED -> {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.cd_declined),
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    StepState.CURRENT -> {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .background(Color.White, CircleShape)
                        )
                    }
                    StepState.FUTURE -> Unit
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (state == StepState.CURRENT || state == StepState.DONE || state == StepState.DECLINED) {
                FontWeight.SemiBold
            } else {
                FontWeight.Normal
            },
            color = labelColor,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Composable
private fun ProgressConnector(
    progress: Float,
    activeColor: Color,
    trackColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .padding(top = 12.5.dp)
            .height(3.dp)
            .clip(RoundedCornerShape(1.5.dp))
            .background(trackColor)
    ) {
        if (progress > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .background(activeColor)
            )
        }
    }
}
