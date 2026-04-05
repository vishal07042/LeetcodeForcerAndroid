package com.leetcode.leetcodeEnforcer

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.leetcode.leetcodeEnforcer.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

enum class CheckSolvesPhase { Idle, Loading, SuccessNew, SuccessNoChange }

@Composable
fun CollapsedToolbarOverlay(
    scrollY: Int,
    serviceEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val collapse = (scrollY / 220f).coerceIn(0f, 1f)
    if (collapse < 0.02f) return
    val alpha = ((collapse - 0.35f) / 0.65f).coerceIn(0f, 1f)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .graphicsLayer { this.alpha = alpha },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(if (serviceEnabled) PrimaryOrange else ErrorRed)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            "LeetCode Forcer",
            style = Typography.titleLarge.copy(fontSize = 15.sp),
            modifier = Modifier.weight(1f)
        )
        Text(
            if (serviceEnabled) "Live" else "OFF",
            style = Typography.labelSmall.copy(
                color = if (serviceEnabled) SuccessGreen else ErrorRed,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
        )
    }
}

@Composable
fun ServiceStatusCardMicro(
    enabled: Boolean,
    onEnable: () -> Unit,
    modifier: Modifier = Modifier
) {
    var prevEnabled by remember { mutableStateOf<Boolean?>(null) }
    val morphT = remember { Animatable(0f) }
    val orbScale = remember { Animatable(1f) }
    val pingProgress = remember { Animatable(0f) }
    val rippleProgress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        if (enabled) {
            morphT.animateTo(1f, tween(280, easing = FastOutSlowInEasing))
            orbScale.snapTo(0.5f)
            orbScale.animateTo(
                1.1f,
                spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
            )
            orbScale.animateTo(
                1f,
                spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
            )
            pingProgress.snapTo(0f)
            pingProgress.animateTo(1f, tween(700, easing = FastOutSlowInEasing))
            pingProgress.snapTo(0f)
        }
    }

    LaunchedEffect(enabled) {
        val p = prevEnabled
        prevEnabled = enabled
        if (p == null) return@LaunchedEffect
        if (!enabled) {
            morphT.animateTo(0f, tween(200))
            return@LaunchedEffect
        }
        if (p == false) {
            morphT.snapTo(0f)
            morphT.animateTo(1f, tween(650, easing = FastOutSlowInEasing))
            rippleProgress.snapTo(0f)
            rippleProgress.animateTo(1f, tween(550, easing = FastOutSlowInEasing))
            rippleProgress.snapTo(0f)
        }
    }

    val orange = PrimaryOrange
    val red = ErrorRed
    val orbColor = lerp(red, orange, morphT.value)
    val dimColor = lerp(RedDim, OrangeDim, morphT.value)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceCard)
            .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
            .drawBehind {
                val color = orbColor.copy(0.08f)
                drawRect(
                    Brush.radialGradient(
                        listOf(color, Color.Transparent),
                        center = Offset(0f, 0f),
                        radius = size.width
                    )
                )
            }
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(64.dp)) {
                if (enabled) {
                    val infiniteTransition = rememberInfiniteTransition(label = "ring")
                    val ringScale by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 1.6f,
                        animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Restart),
                        label = "rs"
                    )
                    val ringAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.4f,
                        targetValue = 0f,
                        animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Restart),
                        label = "ra"
                    )
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val baseR = size.minDimension / 2.5f
                        if (pingProgress.value > 0f) {
                            val pr = pingProgress.value
                            drawCircle(
                                color = orange.copy(alpha = 0.45f * (1f - pr)),
                                radius = baseR * (1f + pr * 1.4f),
                                style = Stroke(2.dp.toPx())
                            )
                        }
                        if (rippleProgress.value > 0f) {
                            val r = rippleProgress.value
                            drawCircle(
                                color = orange.copy(alpha = 0.35f * (1f - r)),
                                radius = baseR * (1f + r * 2.2f),
                                style = Stroke(3.dp.toPx())
                            )
                        }
                        drawCircle(
                            color = orange,
                            radius = baseR * ringScale,
                            style = Stroke(2.dp.toPx()),
                            alpha = ringAlpha
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .scale(orbScale.value)
                        .clip(CircleShape)
                        .background(dimColor)
                        .border(1.dp, orbColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = orbColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("ACCESSIBILITY", style = Typography.labelSmall)
                Text(
                    if (enabled) "ACTIVE" else "INACTIVE",
                    style = Typography.titleMedium.copy(color = orbColor)
                )
            }
            if (enabled) StatusBadge("\u2713 Live", SuccessGreen, GreenDim)
            else EnforcerButton(text = "ENABLE", isSmall = true, onClick = onEnable)
        }
    }
}

@Composable
fun DailyProgressCardMicro(
    streak: Int,
    solvedToday: Int,
    uniqueSolved: Int,
    heatmapData: List<Int>,
    checkPhase: CheckSolvesPhase,
    fillTodayTrigger: Int,
    waveStreakTrigger: Int,
    streakShakeTrigger: Int,
    streakBreakOld: Int?,
    onStreakBreakConsumed: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceCard)
            .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(SurfaceInner)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(9.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.List, contentDescription = null, tint = BodyWhite, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "DAILY PROGRESS",
                    style = Typography.labelSmall.copy(fontSize = 12.sp, color = BodyWhite)
                )
                Spacer(modifier = Modifier.weight(1f))
                Text("Today", style = Typography.labelSmall)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .weight(1.2f)
                        .height(120.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceInner)
                        .padding(16.dp)
                ) {
                    StreakTileMicro(
                        streak = streak,
                        breakOldValue = streakBreakOld,
                        onBreakConsumed = onStreakBreakConsumed,
                        shakeKey = streakShakeTrigger
                    )
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("$solvedToday", "Solved today", SuccessGreen)
                    StatTile("$uniqueSolved", "Unique Solved", BodyWhite)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            LabelTextMicro("LAST 7 DAYS")
            HeatmapRowMicro(
                heatmapData = heatmapData,
                fillTodayTrigger = fillTodayTrigger,
                waveTrigger = waveStreakTrigger
            )
            Spacer(modifier = Modifier.height(20.dp))
            CheckSolvesButtonMicro(phase = checkPhase, onClick = onRefresh)
        }
    }
}

@Composable
private fun StreakTileMicro(
    streak: Int,
    breakOldValue: Int?,
    onBreakConsumed: () -> Unit,
    shakeKey: Int
) {
    val density = LocalDensity.current
    val crackFlash = remember { Animatable(0f) }
    val dropY = remember { Animatable(0f) }
    val shakeX = remember { Animatable(0f) }
    var breakVisual by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(breakOldValue) {
        if (breakOldValue != null && breakOldValue > 0) {
            breakVisual = breakOldValue
            delay(380)
            crackFlash.snapTo(0f)
            crackFlash.animateTo(1f, tween(100))
            crackFlash.animateTo(0f, tween(260))
            dropY.snapTo(0f)
            dropY.animateTo(with(density) { 28.dp.toPx() }, tween(200, easing = FastOutSlowInEasing))
            dropY.animateTo(
                0f,
                spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
            )
            breakVisual = null
            onBreakConsumed()
        }
    }

    LaunchedEffect(shakeKey) {
        if (shakeKey == 0) return@LaunchedEffect
        var v = with(density) { 6.dp.toPx() }
        repeat(5) {
            shakeX.snapTo(v)
            delay(42)
            v *= -0.7f
        }
        shakeX.animateTo(0f, spring(stiffness = Spring.StiffnessMedium))
    }

    val shownStreak = breakVisual ?: streak

    Column {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.graphicsLayer {
                    translationY = dropY.value
                    translationX = shakeX.value
                }
            ) {
                AnimatedContent(
                    targetState = shownStreak,
                    transitionSpec = {
                        (slideInVertically { h -> h } + fadeIn(tween(240)))
                            .togetherWith(slideOutVertically { h -> -h } + fadeOut(tween(200)))
                    },
                    label = "odometer"
                ) { s ->
                    Text("$s", style = Typography.headlineLarge)
                }
            }
            if (crackFlash.value > 0f) {
                Box(
                    Modifier
                        .matchParentSize()
                        .graphicsLayer { alpha = crackFlash.value * 0.9f }
                        .background(ErrorRed.copy(0.28f))
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("\ud83d\udd25", fontSize = 14.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Text("Day streak", style = Typography.labelSmall)
        }
    }
}

@Composable
private fun HeatmapRowMicro(heatmapData: List<Int>, fillTodayTrigger: Int, waveTrigger: Int) {
    val todayIndex = (heatmapData.size - 1).coerceAtLeast(0)
    val todayFill = remember { Animatable(1f) }
    var processedFillKey by remember { mutableIntStateOf(-1) }

    LaunchedEffect(fillTodayTrigger) {
        if (fillTodayTrigger <= 0 || fillTodayTrigger == processedFillKey) return@LaunchedEffect
        processedFillKey = fillTodayTrigger
        todayFill.snapTo(0f)
        todayFill.animateTo(1f, spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow))
    }

    var waveGen by remember { mutableIntStateOf(0) }
    LaunchedEffect(waveTrigger) {
        if (waveTrigger > 0) waveGen++
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        heatmapData.forEachIndexed { index, count ->
            key(index) {
                val baseColor = when {
                    count == 0 -> SurfaceInner
                    count >= 5 -> PrimaryOrange
                    else -> PrimaryOrange.copy(0.35f)
                }
                val isToday = index == todayIndex
                val fillH = if (isToday) {
                    if (count == 0) 0f else todayFill.value
                } else {
                    if (count > 0) 1f else 0f
                }
                val bounce = remember { Animatable(0f) }
                LaunchedEffect(waveGen) {
                    if (waveGen <= 0) return@LaunchedEffect
                    delay(index * 72L)
                    bounce.animateTo(1f, tween(200))
                    bounce.animateTo(
                        0f,
                        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .scale(1f + bounce.value * 0.11f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(SurfaceInner)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(fillH.coerceIn(0f, 1f))
                            .align(Alignment.BottomCenter)
                            .background(baseColor)
                    )
                }
            }
        }
    }
}

@Composable
fun CheckSolvesButtonMicro(phase: CheckSolvesPhase, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "press"
    )
    var prevCheckPhase by remember { mutableStateOf(phase) }
    val spin = rememberInfiniteTransition(label = "spin")
    val spinAngle by spin.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "a"
    )
    val shakeX = remember { Animatable(0f) }
    val density = LocalDensity.current
    LaunchedEffect(phase) {
        if (phase == CheckSolvesPhase.SuccessNoChange && prevCheckPhase != CheckSolvesPhase.SuccessNoChange) {
            val px2 = with(density) { 2.dp.toPx() }
            shakeX.snapTo(0f)
            shakeX.animateTo(px2, tween(40))
            shakeX.animateTo(-px2, tween(40))
            shakeX.animateTo(px2, tween(40))
            shakeX.animateTo(-px2, tween(40))
            shakeX.animateTo(0f, tween(60))
        }
        prevCheckPhase = phase
    }
    val bgColor by animateColorAsState(
        targetValue = when (phase) {
            CheckSolvesPhase.SuccessNew -> SuccessGreen.copy(0.35f)
            else -> Color.Transparent
        },
        animationSpec = tween(200),
        label = "bg"
    )
    val borderColor = when (phase) {
        CheckSolvesPhase.SuccessNew -> SuccessGreen
        CheckSolvesPhase.Loading -> PrimaryOrange.copy(0.5f)
        else -> PrimaryOrange
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(42.dp)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
                translationX = shakeX.value
            }
    ) {
        Button(
            onClick = onClick,
            enabled = phase != CheckSolvesPhase.Loading,
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, borderColor),
            colors = ButtonDefaults.buttonColors(
                containerColor = bgColor,
                contentColor = PrimaryOrange,
                disabledContainerColor = Color.Transparent,
                disabledContentColor = PrimaryOrange
            ),
            interactionSource = interaction,
            contentPadding = PaddingValues()
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (phase == CheckSolvesPhase.Loading) {
                    Canvas(modifier = Modifier.fillMaxSize().padding(2.dp)) {
                        val sw = 2.dp.toPx()
                        val pad = sw / 2f
                        drawArc(
                            color = PrimaryOrange,
                            startAngle = spinAngle,
                            sweepAngle = 100f,
                            useCenter = false,
                            topLeft = Offset(pad, pad),
                            size = Size(size.width - sw, size.height - sw),
                            style = Stroke(sw)
                        )
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "CHECK FOR NEW SOLVES",
                            style = Typography.bodyLarge.copy(fontSize = 11.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LabelTextMicro(text: String) =
    Text(text, style = Typography.labelSmall.copy(letterSpacing = 0.08.sp), modifier = Modifier.padding(vertical = 4.dp))

@Composable
fun EnforcerInputMicro(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Start,
    saveSuccessKey: Int = 0,
    errorShakeKey: Int = 0
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val scanT by animateFloatAsState(
        targetValue = if (focused) 1f else 0f,
        animationSpec = tween(420, easing = FastOutSlowInEasing),
        label = "scan"
    )
    var saveFlash by remember { mutableStateOf(false) }
    LaunchedEffect(saveSuccessKey) {
        if (saveSuccessKey > 0) {
            saveFlash = true
            delay(1200)
            saveFlash = false
        }
    }
    val innerBg by animateColorAsState(
        targetValue = if (saveFlash) SuccessGreen.copy(0.12f) else SurfaceInner,
        animationSpec = tween(300),
        label = "ibg"
    )
    val shakeX = remember { Animatable(0f) }
    val density = LocalDensity.current
    LaunchedEffect(errorShakeKey) {
        if (errorShakeKey == 0) return@LaunchedEffect
        var v = with(density) { 6.dp.toPx() }
        repeat(6) {
            shakeX.snapTo(v)
            delay(28)
            v *= -0.62f
        }
        shakeX.animateTo(0f, spring(stiffness = Spring.StiffnessMedium))
    }
    var showCheck by remember { mutableStateOf(false) }
    LaunchedEffect(saveSuccessKey) {
        if (saveSuccessKey > 0) {
            showCheck = true
            delay(1500)
            showCheck = false
        }
    }
    val borderCol by animateColorAsState(
        targetValue = when {
            saveFlash -> SuccessGreen
            focused -> PrimaryOrange.copy(0.85f)
            else -> BorderSubtle
        },
        animationSpec = tween(280),
        label = "ib"
    )

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .graphicsLayer { translationX = shakeX.value }
                .clip(RoundedCornerShape(10.dp))
                .background(innerBg)
                .border(1.dp, borderCol, RoundedCornerShape(10.dp))
                .drawWithContent {
                    drawContent()
                    val w = size.width
                    val h = size.height
                    val edge = 2.dp.toPx()
                    val travel = (w - edge * 2) * scanT
                    val orange = PrimaryOrange
                    val dim = BorderSubtle
                    val t0 = (travel / w).coerceIn(0f, 1f)
                    val t1 = ((travel + 24.dp.toPx()) / w).coerceIn(0f, 1f)
                    val scanBrush = Brush.horizontalGradient(
                        colorStops = arrayOf(
                            0f to dim,
                            t0 to orange,
                            t1 to dim,
                            1f to dim
                        )
                    )
                    drawRoundRect(
                        brush = scanBrush,
                        topLeft = Offset(0f, 0f),
                        size = Size(w, edge * 2),
                        cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
                    )
                    drawRoundRect(
                        brush = scanBrush,
                        topLeft = Offset(0f, h - edge * 2),
                        size = Size(w, edge * 2),
                        cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
                    )
                }
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (value.isEmpty()) {
                Text(
                    placeholder,
                    style = Typography.bodyLarge.copy(color = PlaceholderText),
                    textAlign = textAlign,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = Typography.bodyLarge.copy(textAlign = textAlign),
                cursorBrush = SolidColor(PrimaryOrange),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                interactionSource = interaction
            )
        }
        AnimatedVisibility(visible = showCheck, modifier = Modifier.padding(start = 8.dp)) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = SuccessGreen,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun AnimatedRuleItemMicro(
    pkg: String,
    badgeText: String,
    badgeColor: Color,
    badgeBg: Color,
    listIndex: Int,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(pkg) {
        delay(listIndex * 20L)
        visible = true
    }
    var removing by remember { mutableStateOf(false) }
    var swipeOffset by remember { mutableFloatStateOf(0f) }
    val pressScale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    AnimatedVisibility(
        visible = visible && !removing,
        enter = fadeIn(tween(280)) + slideInHorizontally { it },
        exit = fadeOut(tween(220)) + slideOutHorizontally { it },
        modifier = modifier.animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            val maxW = with(density) { 200.dp.toPx() }
            val frac = (-swipeOffset / maxW).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(10.dp))
                    .background(ErrorRed.copy(alpha = 0.25f + 0.55f * frac))
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(swipeOffset.roundToInt(), 0) }
                    .scale(pressScale.value)
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                if (swipeOffset < -with(density) { 96.dp.toPx() }) {
                                    scope.launch {
                                        pressScale.animateTo(0.94f, tween(80))
                                        swipeOffset = -with(density) { 400.dp.toPx() }
                                        delay(200)
                                        removing = true
                                        onRemove()
                                    }
                                } else {
                                    swipeOffset = 0f
                                }
                            },
                            onHorizontalDrag = { _, dx ->
                                swipeOffset = (swipeOffset + dx).coerceIn(-maxW, 0f)
                            }
                        )
                    }
                    .background(SurfaceInner, RoundedCornerShape(10.dp))
                    .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    pkg,
                    style = Typography.labelSmall.copy(color = BodyWhite, fontSize = 11.sp),
                    modifier = Modifier.weight(1f)
                )
                StatusBadge(badgeText, badgeColor, badgeBg)
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    Icons.Default.Close,
                    contentDescription = null,
                    tint = MutedText,
                    modifier = Modifier
                        .size(16.dp)
                        .clickable {
                            scope.launch {
                                pressScale.animateTo(0.92f, tween(90))
                                pressScale.animateTo(1f, tween(60))
                                swipeOffset = -with(density) { 420.dp.toPx() }
                                delay(220)
                                removing = true
                                onRemove()
                            }
                        }
                )
            }
        }
    }
}

@Composable
fun SessionItemComponentMicro(session: FocusSession, playSlideIn: Boolean, onRemove: () -> Unit) {
    var visible by remember(session.id) { mutableStateOf(!playSlideIn) }
    LaunchedEffect(session.id, playSlideIn) {
        if (playSlideIn) {
            delay(40)
            visible = true
        } else {
            visible = true
        }
    }
    var collapsing by remember { mutableStateOf(false) }
    val removeGrow = remember { Animatable(1f) }
    val redDeep = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    AnimatedVisibility(
        visible = visible && !collapsing,
        enter = fadeIn(tween(240)) + slideInVertically { it / 3 },
        exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(tween(200)),
        modifier = Modifier.animateContentSize(animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceInner)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${formatMinuteUi(session.startMinute)} - ${formatMinuteUi(session.endMinute)}",
                    style = Typography.bodyLarge.copy(fontSize = 14.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                )
                Text("${session.whitelist.size} apps whitelisted", style = Typography.labelSmall)
            }
            val pillBg = ErrorRed.copy(alpha = 0.12f + redDeep.value * 0.5f)
            Box(
                modifier = Modifier
                    .scale(removeGrow.value)
                    .clip(RoundedCornerShape(8.dp))
                    .background(pillBg)
                    .clickable {
                        scope.launch {
                            removeGrow.animateTo(1.12f, tween(100))
                            redDeep.animateTo(1f, tween(140))
                            collapsing = true
                            delay(260)
                            onRemove()
                        }
                    }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text("Remove", style = Typography.labelSmall.copy(color = ErrorRed, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold))
            }
        }
    }
}

@Composable
fun AddScheduleMorphButton(onClick: () -> Unit, morphToCheckKey: Int, modifier: Modifier = Modifier) {
    var showCheck by remember { mutableStateOf(false) }
    LaunchedEffect(morphToCheckKey) {
        if (morphToCheckKey > 0) {
            showCheck = true
            delay(600)
            showCheck = false
        }
    }
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val s by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "bp"
    )
    Button(
        onClick = onClick,
        modifier = modifier.height(32.dp).scale(s),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange, contentColor = Background),
        interactionSource = interaction,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
    ) {
        AnimatedContent(
            targetState = showCheck,
            transitionSpec = { fadeIn(tween(120)) togetherWith fadeOut(tween(120)) },
            label = "morph"
        ) { check ->
            if (check) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp), tint = Background)
            } else {
                Text("+ ADD NEW SCHEDULE", style = Typography.bodyLarge.copy(fontSize = 10.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold))
            }
        }
    }
}

private fun formatMinuteUi(totalMinutes: Int): String {
    val h24 = totalMinutes / 60
    val m = totalMinutes % 60
    val amPm = if (h24 < 12) "AM" else "PM"
    val h12 = when {
        h24 == 0 -> 12
        h24 > 12 -> h24 - 12
        else -> h24
    }
    return "%d:%02d %s".format(h12, m, amPm)
}
