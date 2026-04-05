package com.leetcode.leetcodeEnforcer

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.leetcode.leetcodeEnforcer.ui.theme.*
import kotlinx.coroutines.launch
import java.util.Calendar

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val permission = android.Manifest.permission.POST_NOTIFICATIONS
            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(permission), 101)
            }
        }
        enableEdgeToEdge()
        setContent {
            LeetCodeForcerTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Background
                ) { innerPadding ->
                    MainScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun MainScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    var isServiceEnabled by remember { mutableStateOf(isAccessibilityServiceEnabled(context)) }
    var username by remember { mutableStateOf(LeetCodeManager.getUsername(context)) }
    var usernameDraft by remember { mutableStateOf(username) }

    var streak by remember { mutableStateOf(LeetCodeManager.getStreak(context)) }
    var solvedToday by remember { mutableStateOf(LeetCodeManager.getSolvedToday(context)) }
    var totalSolved by remember { mutableStateOf(LeetCodeManager.getTotalSolved(context)) }
    var uniqueSolved by remember { mutableStateOf(LeetCodeManager.getUniqueSolved(context)) }
    var heatmapData by remember { mutableStateOf(LeetCodeManager.getHeatmapData(context)) }
    
    var showUsernameDialog by remember { mutableStateOf(false) }

    var whitelist by remember { mutableStateOf(FocusSettingsManager.getWhitelist(context).toList().sorted()) }
    var sessions by remember { mutableStateOf(FocusSettingsManager.getSessions(context)) }

    var showAppSelectionFor: String? by remember { mutableStateOf(null) }
    var installedApps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var startTimeInput by remember { mutableStateOf("09:00") }
    var endTimeInput by remember { mutableStateOf("11:00") }
    val selectedDays = remember { mutableStateListOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY) }

    var checkStatus by remember { mutableStateOf(LeetCodeManager.getDetailedStatus(context)) }
    var isRefreshing by remember { mutableStateOf(false) }

    val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    val componentName = ComponentName(context, LeetCodeDeviceAdminReceiver::class.java)
    var isAdminActive by remember { mutableStateOf(dpm.isAdminActive(componentName)) }
    var showAdminDialog by remember { mutableStateOf(false) }
    var showFocusInfoDialog by remember { mutableStateOf(false) }

    fun refreshRules() {
        whitelist = FocusSettingsManager.getWhitelist(context).toList().sorted()
        sessions = FocusSettingsManager.getSessions(context)
        isAdminActive = dpm.isAdminActive(componentName)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isServiceEnabled = isAccessibilityServiceEnabled(context)
                username = LeetCodeManager.getUsername(context)
                usernameDraft = username
                refreshRules()
                checkStatus = LeetCodeManager.getDetailedStatus(context)
                streak = LeetCodeManager.getStreak(context)
                solvedToday = LeetCodeManager.getSolvedToday(context)
                totalSolved = LeetCodeManager.getTotalSolved(context)
                heatmapData = LeetCodeManager.getHeatmapData(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        isServiceEnabled = isAccessibilityServiceEnabled(context)
        installedApps = getInstalledApps(context)
        
        if (username.isBlank()) {
            showUsernameDialog = true
        } else {
            isRefreshing = true
            coroutineScope.launch {
                LeetCodeManager.checkAndSaveStatus(context)
                checkStatus = LeetCodeManager.getDetailedStatus(context)
                streak = LeetCodeManager.getStreak(context)
                solvedToday = LeetCodeManager.getSolvedToday(context)
                totalSolved = LeetCodeManager.getTotalSolved(context)
                uniqueSolved = LeetCodeManager.getUniqueSolved(context)
                heatmapData = LeetCodeManager.getHeatmapData(context)
                isRefreshing = false
            }
        }
    }

    if (showUsernameDialog) {
        AlertDialog(
            onDismissRequest = { /* Must enter username */ },
            containerColor = SurfaceCard,
            title = { Text("Welcome", style = Typography.titleLarge) },
            text = {
                Column {
                    Text("Enter your LeetCode username to begin enforcements.", style = Typography.bodyLarge)
                    Spacer(modifier = Modifier.height(16.dp))
                    EnforcerInput(
                        value = usernameDraft,
                        onValueChange = { usernameDraft = it },
                        placeholder = "USERNAME"
                    )
                }
            },
            confirmButton = {
                EnforcerButton(text = "START", onClick = {
                    if (usernameDraft.isNotBlank()) {
                        LeetCodeManager.setUsername(context, usernameDraft.trim())
                        username = usernameDraft.trim()
                        showUsernameDialog = false
                        
                        isRefreshing = true
                        coroutineScope.launch {
                            LeetCodeManager.checkAndSaveStatus(context)
                            checkStatus = LeetCodeManager.getDetailedStatus(context)
                            streak = LeetCodeManager.getStreak(context)
                            solvedToday = LeetCodeManager.getSolvedToday(context)
                            totalSolved = LeetCodeManager.getTotalSolved(context)
                            uniqueSolved = LeetCodeManager.getUniqueSolved(context)
                            heatmapData = LeetCodeManager.getHeatmapData(context)
                            isRefreshing = false
                        }
                    }
                })
            }
        )
    }

    if (showAppSelectionFor != null) {
        AppSelectionDialog(
            installedApps = installedApps,
            onDismissRequest = { showAppSelectionFor = null },
            onAppSelected = { appInfo ->
                val resultMsg = FocusSettingsManager.addToWhitelist(context, appInfo.packageName)
                Toast.makeText(context, resultMsg, Toast.LENGTH_SHORT).show()
                showAppSelectionFor = null
                refreshRules()
            }
        )
    }

    if (showAdminDialog) {
        AlertDialog(
            onDismissRequest = { showAdminDialog = false },
            title = { Text("Prevent Uninstall", style = Typography.titleLarge) },
            text = { Text("This locks Settings during focus hours to prevent bypass.", style = Typography.bodyLarge) },
            confirmButton = {
                Button(onClick = {
                    showAdminDialog = false
                    val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                        putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName)
                        putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Enabling this will prevent the app from being uninstalled, enforcing focus.")
                    }
                    context.startActivity(intent)
                }) { Text("Confirm", style = Typography.bodyLarge.copy(fontWeight = FontWeight.Bold)) }
            },
            dismissButton = { TextButton(onClick = { showAdminDialog = false }) { Text("Cancel", style = Typography.bodyLarge) } }
        )
    }

    if (showFocusInfoDialog) {
        AlertDialog(
            onDismissRequest = { showFocusInfoDialog = false },
            title = { Text("Focus Engine", style = Typography.titleLarge) },
            text = {
                Text(
                    "• Active: Rules enforced.\n" +
                    "• Free: Blocker sleeps.\n" +
                    "• Empty: 24/7 Hard Enforcement.",
                    style = Typography.bodyLarge
                )
            },
            confirmButton = { TextButton(onClick = { showFocusInfoDialog = false }) { Text("OK", style = Typography.bodyLarge) } }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        HeaderSection()

        ServiceStatusCard(isServiceEnabled) {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            context.startActivity(intent)
        }

        DailyProgressCard(
            statusText = checkStatus,
            streak = streak,
            solvedToday = solvedToday,
            uniqueSolved = uniqueSolved,
            heatmapData = heatmapData,
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                coroutineScope.launch {
                    LeetCodeManager.checkAndSaveStatus(context)
                    checkStatus = LeetCodeManager.getDetailedStatus(context)
                    streak = LeetCodeManager.getStreak(context)
                    solvedToday = LeetCodeManager.getSolvedToday(context)
                    totalSolved = LeetCodeManager.getTotalSolved(context)
                    uniqueSolved = LeetCodeManager.getUniqueSolved(context)
                    heatmapData = LeetCodeManager.getHeatmapData(context)
                    isRefreshing = false
                }
            }
        )

        SectionCard(
            title = "LeetCode Account",
            subtitle = if (username.isBlank()) "@not_set" else "@$username",
            icon = Icons.Default.Person,
            iconColor = PrimaryOrange,
            iconBg = OrangeDim
        ) {
            EnforcerInput(
                value = usernameDraft,
                onValueChange = { usernameDraft = it },
                placeholder = "ENTER USERNAME",
                modifier = Modifier.fillMaxWidth()
            )
            EnforcerButton(
                text = "SAVE USERNAME",
                onClick = {
                    val trimmed = usernameDraft.trim()
                    if (trimmed.isNotBlank()) {
                        LeetCodeManager.setUsername(context, trimmed)
                        username = trimmed
                        
                        isRefreshing = true
                        coroutineScope.launch {
                            LeetCodeManager.checkAndSaveStatus(context)
                            checkStatus = LeetCodeManager.getDetailedStatus(context)
                            streak = LeetCodeManager.getStreak(context)
                            solvedToday = LeetCodeManager.getSolvedToday(context)
                            totalSolved = LeetCodeManager.getTotalSolved(context)
                            uniqueSolved = LeetCodeManager.getUniqueSolved(context)
                            heatmapData = LeetCodeManager.getHeatmapData(context)
                            isRefreshing = false
                        }
                        Toast.makeText(context, "Username updated", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        SectionCard(
            title = "App Rules",
            subtitle = "${whitelist.size} whitelisted apps",
            icon = Icons.Default.List,
            iconColor = BlueAccent,
            iconBg = BlueDim
        ) {
            EnforcerButton(
                text = "+ ADD WHITELIST APP",
                onClick = {
                    val isBlocked = FocusSettingsManager.isFocusSessionActiveNow(context) && 
                                    !LeetCodeManager.isSolvedToday(context) && 
                                    LeetCodeManager.getUsername(context).isNotBlank()
                    if (isBlocked) {
                        Toast.makeText(context, "SOLVE LEETCODE TO EDIT", Toast.LENGTH_SHORT).show()
                    } else { showAppSelectionFor = "whitelist" }
                },
                isOutline = false,
                modifier = Modifier.fillMaxWidth()
            )

            if (whitelist.isNotEmpty()) {
                LabelText("WHITELIST")
                whitelist.forEach { pkg ->
                    RuleItemComponent(pkg, "ALLOW", SuccessGreen, GreenDim) {
                    val isBlocked = FocusSettingsManager.isFocusSessionActiveNow(context) && 
                                    !LeetCodeManager.isSolvedToday(context) && 
                                    LeetCodeManager.getUsername(context).isNotBlank()
                    if (isBlocked) {
                        Toast.makeText(context, "Solve LeetCode to edit", Toast.LENGTH_SHORT).show()
                    } else {
                            FocusSettingsManager.removeFromWhitelist(context, pkg)
                            refreshRules()
                        }
                    }
                }
            }
        }

        SectionCard(
            title = "Focus Sessions",
            subtitle = if (sessions.isEmpty()) "24/7 Always Active" else "${sessions.size} session active",
            icon = Icons.Default.Build,
            iconColor = PurpleAccent,
            iconBg = PurpleDim,
            headerAction = {
                TextButton(onClick = { showFocusInfoDialog = true }) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MutedText, modifier = Modifier.size(18.dp))
                }
            }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    LabelText("START")
                    EnforcerInput(value = startTimeInput, onValueChange = { startTimeInput = it }, placeholder = "00:00", textAlign = TextAlign.Center)
                }
                Icon(Icons.Default.ArrowForward, contentDescription = null, tint = MutedText, modifier = Modifier.padding(top = 16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    LabelText("END")
                    EnforcerInput(value = endTimeInput, onValueChange = { endTimeInput = it }, placeholder = "00:00", textAlign = TextAlign.Center)
                }
            }

            LabelText("ACTIVE DAYS")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val days = listOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY)
                val labels = listOf("M", "T", "W", "Th", "F", "Sa", "Su")
                days.forEachIndexed { index, day ->
                    val active = selectedDays.contains(day)
                    DayChip(labels[index], active) {
                        if (active) selectedDays.remove(day) else selectedDays.add(day)
                    }
                }
            }

            EnforcerButton(
                text = "ADD FOCUS SESSION",
                onClick = {
                    if (FocusSettingsManager.isFocusSessionActiveNow(context) && !LeetCodeManager.isSolvedToday(context)) {
                        Toast.makeText(context, "SOLVE LEETCODE TO EDIT", Toast.LENGTH_SHORT).show()
                        return@EnforcerButton
                    }
                    val start = parseHourMinute(startTimeInput)
                    val end = parseHourMinute(endTimeInput)
                    if (start == null || end == null || selectedDays.isEmpty()) {
                        Toast.makeText(context, "Invalid input", Toast.LENGTH_SHORT).show()
                        return@EnforcerButton
                    }
                    FocusSettingsManager.addSession(context, FocusSession(System.currentTimeMillis(), selectedDays.toSet(), start, end))
                    refreshRules()
                },
                modifier = Modifier.fillMaxWidth()
            )

            if (sessions.isNotEmpty()) {
                Divider(color = BorderSubtle, thickness = 1.dp)
                sessions.forEach { session ->
                    SessionItemComponent(session) {
                        if (FocusSettingsManager.isFocusSessionActiveNow(context) && !LeetCodeManager.isSolvedToday(context)) {
                            Toast.makeText(context, "Solve LeetCode to edit", Toast.LENGTH_SHORT).show()
                        } else {
                            FocusSettingsManager.removeSession(context, session.id)
                            refreshRules()
                        }
                    }
                }
            }
        }

        SectionCard(
            title = "Security",
            subtitle = if (isAdminActive) "Anti-Uninstall Active" else "Protection Disabled",
            icon = Icons.Default.Lock,
            iconColor = ErrorRed,
            iconBg = RedDim
        ) {
            EnforcerButton(
                text = if (isAdminActive) "SETTINGS ACCESS LOCKED" else "PREVENT UNINSTALL",
                onClick = {
                    if (isAdminActive) {
                        context.startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS))
                    } else {
                        showAdminDialog = true
                    }
                },
                isOutline = isAdminActive,
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun HeaderSection() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(100.dp))
                .background(OrangeDim)
                .border(1.dp, OrangeGlow, RoundedCornerShape(100.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val infiniteTransition = rememberInfiniteTransition()
                val pulseAlpha by infiniteTransition.animateFloat(
                    initialValue = 1f, targetValue = 0f,
                    animationSpec = infiniteRepeatable(animation = tween(1000), repeatMode = RepeatMode.Reverse)
                )
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(PrimaryGreen.copy(alpha = pulseAlpha)))
                Spacer(modifier = Modifier.width(6.dp))
                Text("ENFORCER V1.0", style = Typography.labelSmall.copy(color = PrimaryOrange, fontWeight = FontWeight.Bold))
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(buildAnnotatedString {
            withStyle(style = SpanStyle(color = BodyWhite)) { append("LeetCode\n") }
            withStyle(style = SpanStyle(color = PrimaryOrange)) { append("Forcer") }
        }, style = Typography.displayMedium)
        Text("Pure focus. No mercy.", style = Typography.labelSmall.copy(fontSize = 13.sp))
    }
}

@Composable
fun ServiceStatusCard(enabled: Boolean, onEnable: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceCard)
            .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
            .drawBehind {
                val color = if (enabled) PrimaryOrange.copy(0.08f) else ErrorRed.copy(0.08f)
                drawRect(Brush.radialGradient(listOf(color, Color.Transparent), center = Offset(0f, 0f), radius = size.width))
            }
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val color = if (enabled) PrimaryOrange else ErrorRed
            val dimColor = if (enabled) OrangeDim else RedDim
            
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(64.dp)) {
                if (enabled) {
                    val infiniteTransition = rememberInfiniteTransition()
                    val scale by infiniteTransition.animateFloat(
                        initialValue = 1f, targetValue = 1.6f,
                        animationSpec = infiniteRepeatable(animation = tween(2000), repeatMode = RepeatMode.Restart)
                    )
                    val alpha by infiniteTransition.animateFloat(
                        initialValue = 0.4f, targetValue = 0f,
                        animationSpec = infiniteRepeatable(animation = tween(2000), repeatMode = RepeatMode.Restart)
                    )
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(color = color, radius = (size.minDimension / 2.5f) * scale, style = Stroke(2.dp.toPx()), alpha = alpha)
                    }
                }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(dimColor)
                        .border(1.dp, color, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("ACCESSIBILITY", style = Typography.labelSmall)
                Text(if (enabled) "ACTIVE" else "INACTIVE", style = Typography.titleMedium.copy(color = color))
            }
            
            if (enabled) {
                StatusBadge("✓ Live", SuccessGreen, GreenDim)
            } else {
                EnforcerButton(text = "ENABLE", isSmall = true, onClick = onEnable)
            }
        }
    }
}

@Composable
fun DailyProgressCard(statusText: String, streak: Int, solvedToday: Int, uniqueSolved: Int, heatmapData: List<Int>, isRefreshing: Boolean, onRefresh: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceCard)
            .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(9.dp)).background(SurfaceInner).border(1.dp, BorderSubtle, RoundedCornerShape(9.dp)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.List, contentDescription = null, tint = BodyWhite, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text("DAILY PROGRESS", style = Typography.labelSmall.copy(fontSize = 12.sp, color = BodyWhite))
                Spacer(modifier = Modifier.weight(1f))
                Text("Today", style = Typography.labelSmall)
            }
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(modifier = Modifier.weight(1.2f).height(120.dp).clip(RoundedCornerShape(12.dp)).background(SurfaceInner).padding(16.dp)) {
                    Column {
                        Text("$streak", style = Typography.headlineLarge)
                        Spacer(modifier = Modifier.weight(1f))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🔥", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Day streak", style = Typography.labelSmall)
                        }
                    }
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("$solvedToday", "Solved today", SuccessGreen)
                    StatTile("$uniqueSolved", "Unique Solved", BodyWhite)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            LabelText("LAST 7 DAYS")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                heatmapData.forEach { count ->
                    val color = when {
                        count == 0 -> SurfaceInner
                        count >= 5 -> PrimaryOrange
                        else -> PrimaryOrange.copy(0.35f)
                    }
                    Box(modifier = Modifier.weight(1f).height(36.dp).clip(RoundedCornerShape(6.dp)).background(color))
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onRefresh,
                enabled = !isRefreshing,
                modifier = Modifier.fillMaxWidth().height(42.dp),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, PrimaryOrange),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = PrimaryOrange)
            ) {
                if (isRefreshing) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = PrimaryOrange, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("CHECK FOR NEW SOLVES", style = Typography.bodyLarge.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold))
                }
            }
        }
    }
}

@Composable
fun SectionCard(title: String, subtitle: String, icon: ImageVector, iconColor: Color, iconBg: Color, headerAction: @Composable (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceCard)
            .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
    ) {
        Column {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(32.dp).clip(RoundedCornerShape(9.dp)).background(iconBg).border(1.dp, iconColor.copy(0.3f), RoundedCornerShape(9.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = Typography.titleLarge.copy(fontSize = 16.sp))
                    Text(subtitle, style = Typography.labelSmall)
                }
                headerAction?.invoke()
            }
            Divider(color = BorderSubtle, thickness = 1.dp)
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                content()
            }
        }
    }
}

@Composable
fun EnforcerInput(value: String, onValueChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier, textAlign: TextAlign = TextAlign.Start) {
    Box(
        modifier = modifier
            .height(44.dp)
            .background(SurfaceInner, RoundedCornerShape(10.dp))
            .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        if (value.isEmpty()) {
            Text(placeholder, style = Typography.bodyLarge.copy(color = PlaceholderText), textAlign = textAlign, modifier = Modifier.fillMaxWidth())
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = Typography.bodyLarge.copy(textAlign = textAlign),
            cursorBrush = SolidColor(PrimaryOrange),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
    }
}

@Composable
fun EnforcerButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, isOutline: Boolean = false, isSmall: Boolean = false) {
    Button(
        onClick = onClick,
        modifier = modifier.height(if (isSmall) 32.dp else 44.dp),
        shape = RoundedCornerShape(if (isSmall) 8.dp else 12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isOutline) Color.Transparent else PrimaryOrange,
            contentColor = if (isOutline) MutedText else Background
        ),
        border = if (isOutline) BorderStroke(1.dp, BorderSubtle) else null,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
    ) {
        Text(text, style = Typography.bodyLarge.copy(fontSize = if (isSmall) 10.sp else 12.sp, fontWeight = FontWeight.Bold))
    }
}

@Composable
fun RuleItemComponent(pkg: String, badgeText: String, badgeColor: Color, badgeBg: Color, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceInner, RoundedCornerShape(10.dp))
            .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(pkg, style = Typography.labelSmall.copy(color = BodyWhite, fontSize = 11.sp), modifier = Modifier.weight(1f))
        StatusBadge(badgeText, badgeColor, badgeBg)
        Spacer(modifier = Modifier.width(8.dp))
        Icon(Icons.Default.Close, contentDescription = null, tint = MutedText, modifier = Modifier.size(16.dp).clickable { onRemove() })
    }
}

@Composable
fun StatusBadge(text: String, color: Color, bg: Color) {
    Box(
        modifier = Modifier.clip(RoundedCornerShape(100.dp)).background(bg).border(1.dp, color.copy(0.3f), RoundedCornerShape(100.dp)).padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(text, style = Typography.labelSmall.copy(color = color, fontWeight = FontWeight.Bold, fontSize = 9.sp))
    }
}

@Composable
fun StatTile(value: String, label: String, color: Color) {
    Box(modifier = Modifier.fillMaxWidth().height(55.dp).clip(RoundedCornerShape(12.dp)).background(SurfaceInner).padding(horizontal = 12.dp, vertical = 8.dp)) {
        Column {
            Text(value, style = Typography.titleMedium.copy(color = color, fontSize = 20.sp))
            Text(label, style = Typography.labelSmall)
        }
    }
}

@Composable
fun DayChip(label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(38.dp).clip(RoundedCornerShape(8.dp))
            .background(if (active) OrangeDim else SurfaceInner)
            .border(1.dp, if (active) PrimaryOrange else Color.Transparent, RoundedCornerShape(8.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(label, style = Typography.labelSmall.copy(color = if (active) PrimaryOrange else MutedText, fontWeight = FontWeight.Bold))
    }
}

@Composable
fun SessionItemComponent(session: FocusSession, onRemove: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SurfaceInner).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("${formatMinute(session.startMinute)} \u2192 ${formatMinute(session.endMinute)}", style = Typography.bodyLarge.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold))
            Text(formatDays(session.days), style = Typography.labelSmall)
        }
        Box(
            modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(RedDim).clickable { onRemove() }.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text("Remove", style = Typography.labelSmall.copy(color = ErrorRed, fontWeight = FontWeight.Bold))
        }
    }
}

@Composable
fun LabelText(text: String) {
    Text(text, style = Typography.labelSmall.copy(letterSpacing = 0.08.sp), modifier = Modifier.padding(vertical = 4.dp))
}

private fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
    return enabledServices.any { it.resolveInfo.serviceInfo.packageName == context.packageName && it.resolveInfo.serviceInfo.name == LeetCodeForcerService::class.java.name }
}

fun getInstalledApps(context: Context): List<AppInfo> {
    val pm = context.packageManager
    val intent = Intent(Intent.ACTION_MAIN, null).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
    val resolveInfos = pm.queryIntentActivities(intent, 0)
    return resolveInfos.mapNotNull { resolveInfo ->
        val activityInfo = resolveInfo.activityInfo ?: return@mapNotNull null
        val packageName = activityInfo.packageName
        if (packageName == context.packageName) return@mapNotNull null
        val name = resolveInfo.loadLabel(pm).toString()
        AppInfo(name, packageName)
    }.distinctBy { it.packageName }.sortedBy { it.name.lowercase() }
}

private fun dayShort(day: Int): String {
    return when (day) {
        Calendar.SUNDAY -> "Su"
        Calendar.MONDAY -> "Mon"
        Calendar.TUESDAY -> "Tue"
        Calendar.WEDNESDAY -> "Wed"
        Calendar.THURSDAY -> "Thu"
        Calendar.FRIDAY -> "Fri"
        Calendar.SATURDAY -> "Sat"
        else -> "?"
    }
}

private fun formatDays(days: Set<Int>): String {
    return days.sorted().joinToString(", ") { dayShort(it) }
}

private fun parseHourMinute(value: String): Int? {
    val parts = value.trim().split(":")
    if (parts.size != 2) return null
    val hour = parts[0].toIntOrNull() ?: return null
    val minute = parts[1].toIntOrNull() ?: return null
    if (hour !in 0..23 || minute !in 0..59) return null
    return hour * 60 + minute
}

private fun formatMinute(totalMinutes: Int): String {
    val h = totalMinutes / 60
    val m = totalMinutes % 60
    return "%02d:%02d".format(h, m)
}

data class AppInfo(val name: String, val packageName: String)

@Composable
fun AppSelectionDialog(installedApps: List<AppInfo>, onDismissRequest: () -> Unit, onAppSelected: (AppInfo) -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredApps = if (searchQuery.isBlank()) installedApps else installedApps.filter { it.name.contains(searchQuery, ignoreCase = true) || it.packageName.contains(searchQuery, ignoreCase = true) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = SurfaceCard,
        confirmButton = { TextButton(onClick = onDismissRequest) { Text("CLOSE", style = Typography.bodyLarge.copy(color = PrimaryOrange)) } },
        title = {
            EnforcerInput(value = searchQuery, onValueChange = { searchQuery = it }, placeholder = "SEARCH APPS...", modifier = Modifier.fillMaxWidth())
        },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth().height(400.dp)) {
                items(filteredApps) { app ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onAppSelected(app) }.padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = app.name, style = Typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                            Text(text = app.packageName, style = Typography.labelSmall)
                        }
                    }
                }
            }
        }
    )
}
