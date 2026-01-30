package com.example.leetcodeforcer

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.leetcodeforcer.ui.theme.LeetCodeForcerTheme
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

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
    var isServiceEnabled by remember { mutableStateOf(isAccessibilityServiceEnabled(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current

    // Observe lifecycle changes to refresh status when returning to the app
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isServiceEnabled = isAccessibilityServiceEnabled(context)
                Log.d("MainScreen", "ON_RESUME: Service enabled = $isServiceEnabled")
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Initial check
    LaunchedEffect(Unit) {
        isServiceEnabled = isAccessibilityServiceEnabled(context)
        Log.d("MainScreen", "Initial Check: Service enabled = $isServiceEnabled")
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App Title Section
        Text(
            text = "LeetCode Forcer",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Stay focused, get coding.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.secondary
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Service Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isServiceEnabled) 
                    MaterialTheme.colorScheme.primaryContainer 
                else 
                    MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isServiceEnabled) "Service is ACTIVE" else "Service is INACTIVE",
                    style = MaterialTheme.typography.titleLarge,
                    color = if (isServiceEnabled) 
                        MaterialTheme.colorScheme.onPrimaryContainer 
                    else 
                        MaterialTheme.colorScheme.onErrorContainer
                )
                
                if (!isServiceEnabled) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Blocking is currently disabled.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Enable in Settings")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // LeetCode Progress Card
        val coroutineScope = rememberCoroutineScope()
        var checkStatus by remember { mutableStateOf(LeetCodeManager.getDetailedStatus(context)) }
        var isRefreshing by remember { mutableStateOf(false) }

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
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
        
        Spacer(modifier = Modifier.weight(1f))
        
        Text(
            text = "Username: professionalprovishal",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
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
        // Simulate both states for preview
        Column {
             MainScreen() // This will likely show disabled in preview context
        }
    }
}
