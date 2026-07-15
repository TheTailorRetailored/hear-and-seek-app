package com.bridgesdigital.hearandseek.ui

import android.os.SystemClock
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bridgesdigital.hearandseek.audio.FrequencySweep
import com.bridgesdigital.hearandseek.audio.TonePlayer
import com.bridgesdigital.hearandseek.ui.theme.Aqua
import com.bridgesdigital.hearandseek.ui.theme.DeepSurface
import com.bridgesdigital.hearandseek.ui.theme.Lime
import com.bridgesdigital.hearandseek.ui.theme.MutedText
import com.bridgesdigital.hearandseek.ui.theme.Night
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private const val HIDE_SECONDS = 15
private const val GAME_DURATION_MILLIS = 90_000L
private const val START_FREQUENCY_HZ = 20_000.0
private const val END_FREQUENCY_HZ = 800.0

private enum class GamePhase {
    READY,
    HIDING,
    HUNTING,
    FOUND,
}

@Composable
fun HearAndSeekApp() {
    val tonePlayer = remember { TonePlayer() }
    val sweep = remember {
        FrequencySweep(
            startHz = START_FREQUENCY_HZ,
            endHz = END_FREQUENCY_HZ,
            durationMillis = GAME_DURATION_MILLIS,
        )
    }

    var phase by remember { mutableStateOf(GamePhase.READY) }
    var countdown by remember { mutableIntStateOf(HIDE_SECONDS) }
    var startedAt by remember { mutableLongStateOf(0L) }
    var elapsedMillis by remember { mutableLongStateOf(0L) }
    var currentFrequency by remember { mutableDoubleStateOf(START_FREQUENCY_HZ) }

    DisposableEffect(Unit) {
        onDispose { tonePlayer.stop() }
    }

    LaunchedEffect(phase) {
        when (phase) {
            GamePhase.HIDING -> {
                countdown = HIDE_SECONDS
                repeat(HIDE_SECONDS) {
                    delay(1_000L)
                    countdown -= 1
                }
                phase = GamePhase.HUNTING
            }

            GamePhase.HUNTING -> {
                elapsedMillis = 0L
                currentFrequency = START_FREQUENCY_HZ
                startedAt = SystemClock.elapsedRealtime()
                tonePlayer.start(sweep)

                while (phase == GamePhase.HUNTING) {
                    elapsedMillis = SystemClock.elapsedRealtime() - startedAt
                    currentFrequency = sweep.frequencyAt(elapsedMillis)
                    if (elapsedMillis >= GAME_DURATION_MILLIS) {
                        tonePlayer.stop()
                        phase = GamePhase.FOUND
                        break
                    }
                    delay(50L)
                }
            }

            else -> Unit
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Night,
    ) {
        AnimatedContent(
            targetState = phase,
            label = "game phase",
        ) { currentPhase ->
            when (currentPhase) {
                GamePhase.READY -> ReadyScreen(
                    onStart = { phase = GamePhase.HIDING },
                )

                GamePhase.HIDING -> HidingScreen(
                    secondsRemaining = countdown,
                    onCancel = { phase = GamePhase.READY },
                )

                GamePhase.HUNTING -> HuntingScreen(
                    progress = sweep.progressAt(elapsedMillis).toFloat(),
                    frequencyHz = currentFrequency,
                    onFound = {
                        tonePlayer.stop()
                        phase = GamePhase.FOUND
                    },
                )

                GamePhase.FOUND -> FoundScreen(
                    onPlayAgain = { phase = GamePhase.HIDING },
                    onHome = { phase = GamePhase.READY },
                )
            }
        }
    }
}

@Composable
private fun ReadyScreen(onStart: () -> Unit) {
    ScreenColumn {
        BrandMark()
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "HEAR & SEEK",
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 38.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.5.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Hide the phone. Find it as the pitch falls.",
            color = MutedText,
            fontSize = 18.sp,
            lineHeight = 25.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(36.dp))
        InfoCard()
        Spacer(modifier = Modifier.weight(1f))
        PrimaryButton(
            text = "START GAME",
            onClick = onStart,
        )
    }
}

@Composable
private fun HidingScreen(
    secondsRemaining: Int,
    onCancel: () -> Unit,
) {
    ScreenColumn {
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = "HIDE THE PHONE",
            color = Aqua,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = secondsRemaining.coerceAtLeast(0).toString(),
            color = Lime,
            fontSize = 112.sp,
            fontWeight = FontWeight.Black,
        )
        Text(
            text = "The sound starts automatically",
            color = MutedText,
            fontSize = 17.sp,
        )
        Spacer(modifier = Modifier.weight(1f))
        SecondaryButton(
            text = "CANCEL",
            onClick = onCancel,
        )
    }
}

@Composable
private fun HuntingScreen(
    progress: Float,
    frequencyHz: Double,
    onFound: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Night)
            .padding(horizontal = 24.dp, vertical = 36.dp),
    ) {
        RadarPulse(
            modifier = Modifier
                .size(300.dp)
                .align(Alignment.Center),
        )

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "THE HUNT IS ON",
                color = Aqua,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = formatFrequency(frequencyHz),
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = Lime,
                trackColor = DeepSurface,
                strokeCap = StrokeCap.Round,
            )
            Spacer(modifier = Modifier.weight(1f))
            PrimaryButton(
                text = "FOUND IT!",
                onClick = onFound,
            )
        }
    }
}

@Composable
private fun FoundScreen(
    onPlayAgain: () -> Unit,
    onHome: () -> Unit,
) {
    ScreenColumn {
        Spacer(modifier = Modifier.weight(1f))
        BrandMark()
        Spacer(modifier = Modifier.height(28.dp))
        Text(
            text = "FOUND!",
            color = Lime,
            fontSize = 56.sp,
            fontWeight = FontWeight.Black,
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Pass the phone to the next hider.",
            color = MutedText,
            fontSize = 18.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.weight(1f))
        PrimaryButton(
            text = "PLAY AGAIN",
            onClick = onPlayAgain,
        )
        Spacer(modifier = Modifier.height(12.dp))
        SecondaryButton(
            text = "BACK TO START",
            onClick = onHome,
        )
    }
}

@Composable
private fun ScreenColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
        content = content,
    )
}

@Composable
private fun InfoCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = DeepSurface,
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            InfoLine("15 seconds", "to hide the phone")
            InfoLine("90 seconds", "for the pitch to fall")
            InfoLine("20 kHz → 800 Hz", "younger ears may start first")
        }
    }
}

@Composable
private fun InfoLine(value: String, description: String) {
    Column {
        Text(
            text = value,
            color = Lime,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = description,
            color = MutedText,
            fontSize = 15.sp,
        )
    }
}

@Composable
private fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(62.dp),
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Lime,
            contentColor = Night,
        ),
    ) {
        Text(
            text = text,
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp,
        )
    }
}

@Composable
private fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp),
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = DeepSurface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun BrandMark(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(122.dp)) {
        val centre = center
        drawCircle(
            color = Lime,
            radius = size.minDimension * 0.12f,
            center = centre,
        )
        drawArc(
            color = Aqua,
            startAngle = 120f,
            sweepAngle = 120f,
            useCenter = false,
            topLeft = Offset(size.width * 0.22f, size.height * 0.22f),
            size = androidx.compose.ui.geometry.Size(size.width * 0.56f, size.height * 0.56f),
            style = Stroke(width = 10f, cap = StrokeCap.Round),
        )
        drawArc(
            color = Lime,
            startAngle = 120f,
            sweepAngle = 120f,
            useCenter = false,
            topLeft = Offset(size.width * 0.05f, size.height * 0.05f),
            size = androidx.compose.ui.geometry.Size(size.width * 0.9f, size.height * 0.9f),
            style = Stroke(width = 9f, cap = StrokeCap.Round),
        )
        drawArc(
            color = Aqua,
            startAngle = -60f,
            sweepAngle = 120f,
            useCenter = false,
            topLeft = Offset(size.width * 0.22f, size.height * 0.22f),
            size = androidx.compose.ui.geometry.Size(size.width * 0.56f, size.height * 0.56f),
            style = Stroke(width = 10f, cap = StrokeCap.Round),
        )
        drawArc(
            color = Lime,
            startAngle = -60f,
            sweepAngle = 120f,
            useCenter = false,
            topLeft = Offset(size.width * 0.05f, size.height * 0.05f),
            size = androidx.compose.ui.geometry.Size(size.width * 0.9f, size.height * 0.9f),
            style = Stroke(width = 9f, cap = StrokeCap.Round),
        )
    }
}

@Composable
private fun RadarPulse(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "radar pulse")
    val pulse by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1_600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "pulse size",
    )

    Canvas(modifier = modifier) {
        val radius = size.minDimension / 2f
        drawCircle(
            color = Lime.copy(alpha = (1f - pulse) * 0.28f),
            radius = radius * pulse,
            style = Stroke(width = 5f),
        )
        drawCircle(
            color = Aqua.copy(alpha = 0.16f),
            radius = radius * 0.58f,
            style = Stroke(width = 4f),
        )
        drawCircle(
            color = Lime,
            radius = radius * 0.08f,
        )
    }
}

private fun formatFrequency(frequencyHz: Double): String {
    return if (frequencyHz >= 1_000.0) {
        "${(frequencyHz / 100.0).roundToInt() / 10.0} kHz"
    } else {
        "${frequencyHz.roundToInt()} Hz"
    }
}
