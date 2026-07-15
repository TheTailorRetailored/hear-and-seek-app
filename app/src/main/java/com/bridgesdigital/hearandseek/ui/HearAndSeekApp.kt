package com.bridgesdigital.hearandseek.ui

import android.os.SystemClock
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
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
import com.bridgesdigital.hearandseek.game.GameMode
import com.bridgesdigital.hearandseek.ui.theme.Aqua
import com.bridgesdigital.hearandseek.ui.theme.DeepSurface
import com.bridgesdigital.hearandseek.ui.theme.Lime
import com.bridgesdigital.hearandseek.ui.theme.MutedText
import com.bridgesdigital.hearandseek.ui.theme.Night
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private const val START_FREQUENCY_HZ = 20_000.0
private const val END_FREQUENCY_HZ = 800.0

private enum class GamePhase {
    READY,
    HIDING,
    HUNTING,
    RESULT,
}

private enum class RoundResult {
    FOUND,
    NOT_FOUND,
}

@Composable
fun HearAndSeekApp() {
    val tonePlayer = remember { TonePlayer() }

    var selectedMode by remember { mutableStateOf(GameMode.CLASSIC) }
    var phase by remember { mutableStateOf(GamePhase.READY) }
    var roundResult by remember { mutableStateOf<RoundResult?>(null) }
    var countdown by remember { mutableIntStateOf(selectedMode.hideSeconds) }
    var elapsedMillis by remember { mutableLongStateOf(0L) }
    var currentFrequency by remember { mutableDoubleStateOf(START_FREQUENCY_HZ) }

    val sweep = remember(selectedMode) {
        FrequencySweep(
            startHz = START_FREQUENCY_HZ,
            endHz = END_FREQUENCY_HZ,
            durationMillis = selectedMode.durationMillis,
        )
    }

    DisposableEffect(Unit) {
        onDispose { tonePlayer.stop() }
    }

    LaunchedEffect(phase, selectedMode) {
        when (phase) {
            GamePhase.HIDING -> {
                countdown = selectedMode.hideSeconds
                repeat(selectedMode.hideSeconds) {
                    delay(1_000L)
                    countdown -= 1
                }
                phase = GamePhase.HUNTING
            }

            GamePhase.HUNTING -> {
                elapsedMillis = 0L
                currentFrequency = START_FREQUENCY_HZ
                val startedAt = SystemClock.elapsedRealtime()
                tonePlayer.start(sweep)

                while (phase == GamePhase.HUNTING) {
                    elapsedMillis = SystemClock.elapsedRealtime() - startedAt
                    currentFrequency = sweep.frequencyAt(elapsedMillis)
                    if (elapsedMillis >= selectedMode.durationMillis) {
                        tonePlayer.stop()
                        roundResult = null
                        phase = GamePhase.RESULT
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
                    selectedMode = selectedMode,
                    onSelectMode = { selectedMode = it },
                    onStart = {
                        roundResult = null
                        phase = GamePhase.HIDING
                    },
                )

                GamePhase.HIDING -> HidingScreen(
                    mode = selectedMode,
                    secondsRemaining = countdown,
                    onCancel = { phase = GamePhase.READY },
                )

                GamePhase.HUNTING -> HuntingScreen(
                    mode = selectedMode,
                    progress = sweep.progressAt(elapsedMillis).toFloat(),
                    elapsedMillis = elapsedMillis,
                    frequencyHz = currentFrequency,
                    onFound = {
                        tonePlayer.stop()
                        roundResult = RoundResult.FOUND
                        phase = GamePhase.RESULT
                    },
                )

                GamePhase.RESULT -> ResultScreen(
                    result = roundResult,
                    onFound = { roundResult = RoundResult.FOUND },
                    onNotFound = { roundResult = RoundResult.NOT_FOUND },
                    onPlayAgain = {
                        roundResult = null
                        phase = GamePhase.HIDING
                    },
                    onHome = {
                        roundResult = null
                        phase = GamePhase.READY
                    },
                )
            }
        }
    }
}

@Composable
private fun ReadyScreen(
    selectedMode: GameMode,
    onSelectMode: (GameMode) -> Unit,
    onStart: () -> Unit,
) {
    ScreenColumn {
        BrandMark(modifier = Modifier.size(92.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "HEAR & SEEK",
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 34.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.5.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Choose how slowly the pitch falls.",
            color = MutedText,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(22.dp))
        ModePicker(
            selectedMode = selectedMode,
            onSelectMode = onSelectMode,
        )
        Spacer(modifier = Modifier.height(18.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = DeepSurface,
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "${selectedMode.title}: ${selectedMode.durationLabel}",
                    color = Lime,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${selectedMode.hideSeconds}-second hiding countdown • 20 kHz → 800 Hz",
                    color = MutedText,
                    fontSize = 14.sp,
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        PrimaryButton(
            text = "START ${selectedMode.title.uppercase()}",
            onClick = onStart,
        )
    }
}

@Composable
private fun ModePicker(
    selectedMode: GameMode,
    onSelectMode: (GameMode) -> Unit,
) {
    val modes = GameMode.entries
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ModeCard(
                mode = modes[0],
                selected = selectedMode == modes[0],
                onClick = { onSelectMode(modes[0]) },
                modifier = Modifier.weight(1f),
            )
            ModeCard(
                mode = modes[1],
                selected = selectedMode == modes[1],
                onClick = { onSelectMode(modes[1]) },
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ModeCard(
                mode = modes[2],
                selected = selectedMode == modes[2],
                onClick = { onSelectMode(modes[2]) },
                modifier = Modifier.weight(1f),
            )
            ModeCard(
                mode = modes[3],
                selected = selectedMode == modes[3],
                onClick = { onSelectMode(modes[3]) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ModeCard(
    mode: GameMode,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .height(82.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = if (selected) Lime.copy(alpha = 0.13f) else DeepSurface,
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) Lime else DeepSurface,
        ),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = mode.title,
                color = if (selected) Lime else MaterialTheme.colorScheme.onSurface,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = mode.durationLabel,
                color = MutedText,
                fontSize = 14.sp,
            )
        }
    }
}

@Composable
private fun HidingScreen(
    mode: GameMode,
    secondsRemaining: Int,
    onCancel: () -> Unit,
) {
    ScreenColumn {
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = "${mode.title.uppercase()} MODE",
            color = Aqua,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "HIDE THE PHONE",
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
        )
        Spacer(modifier = Modifier.height(18.dp))
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
    mode: GameMode,
    progress: Float,
    elapsedMillis: Long,
    frequencyHz: Double,
    onFound: () -> Unit,
) {
    val remainingSeconds = ((mode.durationMillis - elapsedMillis).coerceAtLeast(0L) + 999L) / 1_000L

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
                text = "${mode.title.uppercase()} HUNT",
                color = Aqua,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "${formatTime(remainingSeconds)} remaining",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = formatFrequency(frequencyHz),
                color = MutedText,
                fontSize = 16.sp,
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
private fun ResultScreen(
    result: RoundResult?,
    onFound: () -> Unit,
    onNotFound: () -> Unit,
    onPlayAgain: () -> Unit,
    onHome: () -> Unit,
) {
    ScreenColumn {
        Spacer(modifier = Modifier.weight(1f))
        BrandMark()
        Spacer(modifier = Modifier.height(24.dp))

        when (result) {
            null -> {
                Text(
                    text = "TIME'S UP",
                    color = Aqua,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Was the phone found?",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.weight(1f))
                PrimaryButton(
                    text = "YES, FOUND IT",
                    onClick = onFound,
                )
                Spacer(modifier = Modifier.height(12.dp))
                SecondaryButton(
                    text = "NO, NOT FOUND",
                    onClick = onNotFound,
                )
            }

            RoundResult.FOUND -> {
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

            RoundResult.NOT_FOUND -> {
                Text(
                    text = "NOT FOUND",
                    color = Aqua,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Black,
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Try again—or choose a longer mode.",
                    color = MutedText,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.weight(1f))
                PrimaryButton(
                    text = "TRY AGAIN",
                    onClick = onPlayAgain,
                )
                Spacer(modifier = Modifier.height(12.dp))
                SecondaryButton(
                    text = "CHOOSE ANOTHER MODE",
                    onClick = onHome,
                )
            }
        }
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

private fun formatTime(totalSeconds: Long): String {
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}
