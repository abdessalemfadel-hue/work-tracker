package com.abdessalem.worktracker.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Coffee
import androidx.compose.material.icons.rounded.StopCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.hypot

@Suppress("DataClassPrivateConstructor")
data class ActionFeedback private constructor(
    val type: Type,
    val title: String,
    val subtitle: String,
    val originX: Float,
    val originY: Float
) {
    enum class Type { CLOCK_IN, CLOCK_OUT, BREAK, RESUME }

    companion object {
        fun clockIn(x: Float, y: Float, time: String) = ActionFeedback(Type.CLOCK_IN, "Clocked In", time, x, y)
        fun clockOut(x: Float, y: Float, summary: String) = ActionFeedback(Type.CLOCK_OUT, "Shift Complete", summary, x, y)
        fun breakStarted(x: Float, y: Float) = ActionFeedback(Type.BREAK, "Break Started", "Your work timer is paused", x, y)
        fun resumed(x: Float, y: Float) = ActionFeedback(Type.RESUME, "Back to Work", "The active timer is running", x, y)
    }
}

@Composable
fun ActionFeedbackOverlay(
    feedback: ActionFeedback?,
    modifier: Modifier = Modifier,
    onFinished: () -> Unit
) {
    if (feedback == null) return
    val progress = remember(feedback) { Animatable(0f) }
    val alpha = remember(feedback) { Animatable(1f) }
    var showContent by remember(feedback) { mutableStateOf(false) }
    val color = when (feedback.type) {
        ActionFeedback.Type.CLOCK_IN, ActionFeedback.Type.RESUME -> Color(0xFF39E878)
        ActionFeedback.Type.CLOCK_OUT -> Color(0xFFFF625F)
        ActionFeedback.Type.BREAK -> Color(0xFFFFB02E)
    }
    val icon: ImageVector = when (feedback.type) {
        ActionFeedback.Type.CLOCK_OUT -> Icons.Rounded.StopCircle
        ActionFeedback.Type.BREAK -> Icons.Rounded.Coffee
        else -> Icons.Rounded.CheckCircle
    }

    LaunchedEffect(feedback) {
        progress.animateTo(1f, tween(620, easing = FastOutSlowInEasing))
        showContent = true
        delay(if (feedback.type == ActionFeedback.Type.CLOCK_OUT) 650 else 450)
        alpha.animateTo(0f, tween(320))
        onFinished()
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val origin = Offset(
                feedback.originX.coerceIn(0f, size.width),
                feedback.originY.coerceIn(0f, size.height)
            )
            val maxRadius = maxDistanceToCorner(origin)
            drawCircle(
                color = color.copy(alpha = 0.96f * alpha.value),
                radius = maxRadius * progress.value,
                center = origin
            )
        }
        AnimatedVisibility(
            visible = showContent,
            enter = fadeIn(tween(220)) + scaleIn(initialScale = 0.6f, animationSpec = tween(280))
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(88.dp))
                Text(feedback.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 30.sp)
                Text(feedback.subtitle, color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

private fun DrawScope.maxDistanceToCorner(origin: Offset): Float = maxOf(
    hypot(origin.x, origin.y),
    hypot(size.width - origin.x, origin.y),
    hypot(origin.x, size.height - origin.y),
    hypot(size.width - origin.x, size.height - origin.y)
)
