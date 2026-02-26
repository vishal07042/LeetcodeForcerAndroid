package com.example.leetcodeforcer

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.leetcodeforcer.ui.theme.LeetCodeForcerTheme
import kotlinx.coroutines.launch
import java.util.Calendar

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LeetCodeForcerTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
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

    var isServiceEnabled by remember { mutableStateOf(isAccessibilityServiceEnabled(context)) }
    var username by remember { mutableStateOf(LeetCodeManager.getUsername(context)) }
    var usernameDraft by remember { mutableStateOf(username) }
    var showUsernameDialog by remember { mutableStateOf(username.isBlank()) }

    var whitelist by remember { mutableStateOf(FocusSettingsManager.getWhitelist(context).toList().sorted()) }
    var blacklist by remember { mutableStateOf(FocusSettingsManager.getBlacklist(context).toList().sorted()) }
    var sessions by remember { mutableStateOf(FocusSettingsManager.getSessions(context)) }

    var packageInput by remember { mutableStateOf("") }
    var startTimeInput by remember { mutableStateOf("09:00") }
    var endTimeInput by remember { mutableStateOf("11:00") }
    val selectedDays = remember {
        mutableStateListOf(
            Calendar.MONDAY,
            Calendar.TUESDAY,
            Calendar.WEDNESDAY,
            Calendar.THURSDAY,
            Calendar.FRIDAY
        )
    }

    val coroutineScope = rememberCoroutineScope()
    var checkStatus by remember { mutableStateOf(LeetCodeManager.getDetailedStatus(context)) }
    var isRefreshing by remember { mutableStateOf(false) }

    fun refreshRules() {
        whitelist = FocusSettingsManager.getWhitelist(context).toList().sorted()
        blacklist = FocusSettingsManager.getBlacklist(context).toList().sorted()
        sessions = FocusSettingsManager.getSessions(context)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isServiceEnabled = isAccessibilityServiceEnabled(context)
                username = LeetCodeManager.getUsername(context)
                usernameDraft = username
                refreshRules()
                checkStatus = LeetCodeManager.getDetailedStatus(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        isServiceEnabled = isAccessibilityServiceEnabled(context)
        showUsernameDialog = LeetCodeManager.getUsername(context).isBlank()
    }

    if (showUsernameDialog) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Set LeetCode Username") },
            text = {
                OutlinedTextField(
                    value = usernameDraft,
                    onValueChange = { usernameDraft = it },
                    label = { Text("LeetCode username") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    val trimmed = usernameDraft.trim()
                    if (trimmed.isNotBlank()) {
                        LeetCodeManager.setUsername(context, trimmed)
                        username = trimmed
                        showUsernameDialog = false
                        checkStatus = LeetCodeManager.getDetailedStatus(context)
                    }
                }) {
                    Text("Save")
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "LeetCode Forcer",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Pure LeetCode focus enforcement",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.secondary
        )

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isServiceEnabled) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.errorContainer
                }
            )
        ) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (isServiceEnabled) "Service is ACTIVE" else "Service is INACTIVE",
                    style = MaterialTheme.typography.titleLarge
                )
                if (!isServiceEnabled) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = {
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        context.startActivity(intent)
                    }) {
                        Text("Enable Accessibility Service")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("LeetCode Account", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = usernameDraft,
                    onValueChange = { usernameDraft = it },
                    label = { Text("LeetCode username") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(onClick = {
                    val trimmed = usernameDraft.trim()
                    if (trimmed.isBlank()) {
                        Toast.makeText(context, "Username cannot be empty", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    LeetCodeManager.setUsername(context, trimmed)
                    username = trimmed
                    showUsernameDialog = false
                    checkStatus = LeetCodeManager.getDetailedStatus(context)
                    Toast.makeText(context, "Username saved", Toast.LENGTH_SHORT).show()
                }) {
                    Text("Save Username")
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("App Rules", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = packageInput,
                    onValueChange = { packageInput = it },
                    label = { Text("Package name (e.g. com.instagram.android)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        FocusSettingsManager.addToWhitelist(context, packageInput)
                        packageInput = ""
                        refreshRules()
                    }) {
                        Text("Add Whitelist")
                    }
                    OutlinedButton(onClick = {
                        FocusSettingsManager.addToBlacklist(context, packageInput)
                        packageInput = ""
                        refreshRules()
                    }) {
                        Text("Add Blacklist")
                    }
                }

                Text("Whitelist", style = MaterialTheme.typography.bodyLarge)
                if (whitelist.isEmpty()) {
                    Text("No user whitelist packages")
                } else {
                    whitelist.forEach { pkg ->
                        RuleItem(pkg = pkg, removeAction = {
                            FocusSettingsManager.removeFromWhitelist(context, pkg)
                            refreshRules()
                        })
                    }
                }

                Text("Blacklist", style = MaterialTheme.typography.bodyLarge)
                if (blacklist.isEmpty()) {
                    Text("No user blacklist packages")
                } else {
                    blacklist.forEach { pkg ->
                        RuleItem(pkg = pkg, removeAction = {
                            FocusSettingsManager.removeFromBlacklist(context, pkg)
                            refreshRules()
                        })
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Focus Sessions", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = startTimeInput,
                    onValueChange = { startTimeInput = it },
                    label = { Text("Start time (HH:mm)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = endTimeInput,
                    onValueChange = { endTimeInput = it },
                    label = { Text("End time (HH:mm)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Active days")
                DayToggleRows(
                    selectedDays = selectedDays.toSet(),
                    onToggleDay = { day ->
                        if (selectedDays.contains(day)) {
                            selectedDays.remove(day)
                        } else {
                            selectedDays.add(day)
                        }
                    }
                )

                Button(onClick = {
                    val startMinute = parseHourMinute(startTimeInput)
                    val endMinute = parseHourMinute(endTimeInput)
                    if (startMinute == null || endMinute == null || selectedDays.isEmpty()) {
                        Toast.makeText(context, "Use valid time and select at least one day", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    FocusSettingsManager.addSession(
                        context,
                        FocusSession(
                            id = System.currentTimeMillis(),
                            days = selectedDays.toSet(),
                            startMinute = startMinute,
                            endMinute = endMinute
                        )
                    )
                    refreshRules()
                    Toast.makeText(context, "Session added", Toast.LENGTH_SHORT).show()
                }) {
                    Text("Add Focus Session")
                }

                if (sessions.isEmpty()) {
                    Text("No sessions configured. Enforcement runs all day.")
                } else {
                    sessions.forEach { session ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${formatDays(session.days)} ${formatMinute(session.startMinute)}-${formatMinute(session.endMinute)}")
                            TextButton(onClick = {
                                FocusSettingsManager.removeSession(context, session.id)
                                refreshRules()
                            }) {
                                Text("Remove")
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Daily Progress",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = checkStatus,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = {
                        isRefreshing = true
                        coroutineScope.launch {
                            LeetCodeManager.checkAndSaveStatus(context)
                            checkStatus = LeetCodeManager.getDetailedStatus(context)
                            isRefreshing = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isRefreshing
                ) {
                    if (isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Check for New Solves")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Username: ${if (username.isBlank()) "not set" else username}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
private fun RuleItem(pkg: String, removeAction: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(pkg, modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.width(8.dp))
        TextButton(onClick = removeAction) {
            Text("Remove")
        }
    }
}

@Composable
private fun DayToggleRows(selectedDays: Set<Int>, onToggleDay: (Int) -> Unit) {
    val row1 = listOf(Calendar.SUNDAY, Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY)
    val row2 = listOf(Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY)

    DayRow(days = row1, selectedDays = selectedDays, onToggleDay = onToggleDay)
    DayRow(days = row2, selectedDays = selectedDays, onToggleDay = onToggleDay)
}

@Composable
private fun DayRow(days: List<Int>, selectedDays: Set<Int>, onToggleDay: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        days.forEach { day ->
            if (selectedDays.contains(day)) {
                Button(onClick = { onToggleDay(day) }) {
                    Text(dayShort(day))
                }
            } else {
                OutlinedButton(onClick = { onToggleDay(day) }) {
                    Text(dayShort(day))
                }
            }
        }
    }
}

private fun dayShort(day: Int): String {
    return when (day) {
        Calendar.SUNDAY -> "Sun"
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
    return days.sorted().joinToString(separator = ",") { dayShort(it) }
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
    val minute = totalMinutes.coerceIn(0, 1439)
    val hourPart = minute / 60
    val minutePart = minute % 60
    return "%02d:%02d".format(hourPart, minutePart)
}

// Helper function to check if our accessibility service is enabled
private fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    val enabledServices = try {
        accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
    } catch (e: Exception) {
        Log.e("AccessibilityCheck", "Error getting enabled services: ${e.message}", e)
        null
    }

    enabledServices?.forEach { service ->
        val info = service.resolveInfo.serviceInfo
        if (info.packageName == context.packageName && info.name == LeetCodeForcerService::class.java.name) {
            Log.i("AccessibilityCheck", "MATCH FOUND: LeetCodeForcerService is ENABLED")
            return true
        }
    }

    Log.i("AccessibilityCheck", "NO MATCH: LeetCodeForcerService is DISABLED")
    return false
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    LeetCodeForcerTheme {
        MainScreen()
    }
}
