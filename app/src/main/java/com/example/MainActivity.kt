package com.example

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Vibrator
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppDatabase
import com.example.data.ClipboardItem
import com.example.data.PreferencesManager
import com.example.data.CustomTheme
import com.example.ui.keyboard.KeyboardLayout
import com.example.ui.keyboard.getThemeColors
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = MaterialTheme.colorScheme.background
                ) { innerPadding ->
                    KeyboardDashboardScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun KeyboardDashboardScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferences = remember { PreferencesManager(context) }
    val database = remember { AppDatabase.getDatabase(context) }

    // Settings trigger
    var refreshTrigger by remember { mutableStateOf(0) }

    // Setting states
    var selectedTheme by remember(refreshTrigger) { mutableStateOf(preferences.selectedTheme) }
    var isHapticEnabled by remember(refreshTrigger) { mutableStateOf(preferences.isHapticEnabled) }
    var hapticStrength by remember(refreshTrigger) { mutableStateOf(preferences.hapticStrength.toFloat()) }
    var selectedLanguage by remember(refreshTrigger) { mutableStateOf(preferences.selectedLanguage) }
    var isCloudSyncEnabled by remember(refreshTrigger) { mutableStateOf(preferences.isCloudSyncEnabled) }
    var oneHandedMode by remember(refreshTrigger) { mutableStateOf(preferences.oneHandedMode) }
    var deletingSpeed by remember(refreshTrigger) { mutableStateOf(preferences.deletingSpeed) }
    var typingAnimation by remember(refreshTrigger) { mutableStateOf(preferences.typingAnimation) }

    // Custom Theme Builder states
    var newThemeName by remember { mutableStateOf("") }
    var bgHex by remember { mutableStateOf("#1C1C1E") }
    var keyBgHex by remember { mutableStateOf("#2C2C2E") }
    var keyTextHex by remember { mutableStateOf("#FFFFFF") }
    var accentHex by remember { mutableStateOf("#0A84FF") }
    var headerBgHex by remember { mutableStateOf("#121212") }
    var headerIconHex by remember { mutableStateOf("#8E8E93") }

    // Sync animation states
    var isSyncing by remember { mutableStateOf(false) }
    var syncProgress by remember { mutableStateOf(0f) }
    var lastSyncedDisplay by remember { mutableStateOf("Never") }

    // System IME Status
    var isEnabledInSystem by remember { mutableStateOf(false) }
    var isSelectedAsDefault by remember { mutableStateOf(false) }

    // Load actual DB values
    val clipboardHistory by database.clipboardDao().getAll().collectAsState(initial = emptyList())
    val customThemes by database.customThemeDao().getAll().collectAsState(initial = emptyList())

    // App state
    var playgroundText by remember { mutableStateOf(TextFieldValue("")) }
    var newClipText by remember { mutableStateOf("") }
    var activeTab by remember { mutableStateOf(0) } // 0 = Dashboard, 1 = Clipboard, 2 = Theme Settings

    // Helper to check system status
    val checkSystemKeyboardStatus = {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        val list = imm?.enabledInputMethodList ?: emptyList()
        isEnabledInSystem = list.any { it.packageName == context.packageName }

        val defaultIME = Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
        isSelectedAsDefault = defaultIME != null && defaultIME.startsWith(context.packageName)
    }

    // Check status on resume/init
    LaunchedEffect(Unit) {
        checkSystemKeyboardStatus()
        // Format last synced time
        if (preferences.lastSyncedTime > 0) {
            lastSyncedDisplay = "Just Now"
        }
    }

    // Clipboard system sync integration simulator
    LaunchedEffect(clipboardHistory) {
        // Whenever history updates, if cloud sync is enabled, simulate a background backup
        if (isCloudSyncEnabled && clipboardHistory.isNotEmpty()) {
            scope.launch {
                preferences.lastSyncedTime = System.currentTimeMillis()
                lastSyncedDisplay = "Auto-Synced"
            }
        }
    }

    // Setup input helper append/delete to match exact caret manipulation
    val insertTextAtCursor = { char: String ->
        val selectionStart = playgroundText.selection.start
        val selectionEnd = playgroundText.selection.end
        val text = playgroundText.text
        val before = text.substring(0, selectionStart)
        val after = text.substring(selectionEnd)
        val newText = before + char + after
        val newCursor = selectionStart + char.length
        playgroundText = TextFieldValue(
            text = newText,
            selection = androidx.compose.ui.text.TextRange(newCursor)
        )

        // Also add typed sentence clips to clipboard history automatically once space/enter is pressed to demo the "endless history"
        if (char == " " || char == "\n") {
            val words = newText.trim().split("\\s+".toRegex())
            if (words.size >= 3) { // Save meaningful words/phrases
                val lastPhrase = words.takeLast(5).joinToString(" ")
                scope.launch {
                    if (!database.clipboardDao().exists(lastPhrase)) {
                        database.clipboardDao().insert(ClipboardItem(text = lastPhrase))
                    }
                }
            }
        }
    }

    val deleteTextAtCursor = {
        val selectionStart = playgroundText.selection.start
        val selectionEnd = playgroundText.selection.end
        val text = playgroundText.text
        if (selectionStart != selectionEnd) {
            val before = text.substring(0, selectionStart)
            val after = text.substring(selectionEnd)
            playgroundText = TextFieldValue(
                text = before + after,
                selection = androidx.compose.ui.text.TextRange(selectionStart)
            )
        } else if (selectionStart > 0) {
            val before = text.substring(0, selectionStart - 1)
            val after = text.substring(selectionStart)
            playgroundText = TextFieldValue(
                text = before + after,
                selection = androidx.compose.ui.text.TextRange(selectionStart - 1)
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        // --- TOP HERO APP BAR ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Keyboard,
                                contentDescription = "Logo",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = "Minimalist Keys",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            letterSpacing = (-0.5).sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "Nice, Clean, Offline-First IME Layout",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Check System Status Button
                IconButton(
                    onClick = {
                        checkSystemKeyboardStatus()
                    },
                    modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Sync system status",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // --- CORE SYSTEM REGISTRATION STATUS BAR ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = if (isEnabledInSystem && isSelectedAsDefault) Icons.Default.CheckCircle else Icons.Default.Info,
                contentDescription = "Status",
                tint = if (isEnabledInSystem && isSelectedAsDefault) Color(0xFF4CD964) else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isEnabledInSystem && isSelectedAsDefault) "Keyboard Fully Active!" else "Complete Setup Required",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (isEnabledInSystem && isSelectedAsDefault)
                        "You are using Minimalist Keyboard as your default keyboard."
                    else if (!isEnabledInSystem)
                        "Step 1: Enable the keyboard in Android Settings."
                    else
                        "Step 2: Choose Minimalist Keyboard as active IME.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = {
                    if (!isEnabledInSystem) {
                        // Open input methods settings
                        val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
                        context.startActivity(intent)
                    } else if (!isSelectedAsDefault) {
                        // Show IME picker
                        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                        imm?.showInputMethodPicker()
                    } else {
                        // Show IME picker anyway to switch back and forth
                        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                        imm?.showInputMethodPicker()
                    }
                },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.defaultMinSize(minWidth = 1.dp, minHeight = 1.dp)
            ) {
                Text(
                    text = if (!isEnabledInSystem) "Enable" else if (!isSelectedAsDefault) "Select" else "Switch",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // --- INTERACTIVE PLAYGROUND (SCROLLABLE CARD LIST) ---
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // PLAYGROUND TYPING CONSOLE
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF00FFFF))
                                )
                                Text(
                                    text = "Interactive Keyboard Playground",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            if (playgroundText.text.isNotEmpty()) {
                                Text(
                                    text = "Clear Terminal",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .clickable { playgroundText = TextFieldValue("") }
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Output Terminal
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(90.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            if (playgroundText.text.isEmpty()) {
                                Text(
                                    text = "Tap the minimalist keyboard below to test typing instantly. Try copy-pasting into this workspace...",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            } else {
                                Text(
                                    text = playgroundText.text,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Keyboard status indicators under play terminal
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Active Layout: ${selectedLanguage} (QWERTY)",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Text(
                                text = "Active Theme: $selectedTheme",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // NAVIGATION TAB SWITCHER
            item {
                TabRow(
                    selectedTabIndex = activeTab,
                    containerColor = Color.Transparent,
                    divider = {}
                ) {
                    Tab(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        text = { Text("Status", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        text = { Text("Themes", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = activeTab == 2,
                        onClick = { activeTab = 2 },
                        text = { Text("Settings", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            // ACTIVE CONTENT VIEW
            when (activeTab) {
                0 -> {
                    // TAB 0: SYNC & LOCAL PERSISTENCE
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CloudSync,
                                            contentDescription = "Sync",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = "Seamless Cloud Synchronization",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    Switch(
                                        checked = isCloudSyncEnabled,
                                        onCheckedChange = {
                                            isCloudSyncEnabled = it
                                            preferences.isCloudSyncEnabled = it
                                            refreshTrigger++
                                        }
                                    )
                                }

                                Text(
                                    text = "Store all endless clipboard, customizations, and settings locally for offline accessibility. When online, secure P2P syncing runs flawlessly.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 6.dp)
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Sync progress / status card
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                        .padding(12.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Sync Status", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(
                                                text = if (isSyncing) "Syncing..." else if (isCloudSyncEnabled) "Synchronized" else "Offline-Only Cache",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSyncing) MaterialTheme.colorScheme.primary else if (isCloudSyncEnabled) Color(0xFF4CD964) else Color(0xFFFF9500)
                                            )
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Connected Devices", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text("3 active devices", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Last Backup Completed", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(lastSyncedDisplay, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                        }

                                        if (isSyncing) {
                                            LinearProgressIndicator(
                                                progress = { syncProgress },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 6.dp)
                                                    .height(4.dp),
                                                color = MaterialTheme.colorScheme.primary,
                                                trackColor = MaterialTheme.colorScheme.outlineVariant,
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Button(
                                    onClick = {
                                        if (!isSyncing && isCloudSyncEnabled) {
                                            isSyncing = true
                                            syncProgress = 0f
                                            scope.launch {
                                                while (syncProgress < 1f) {
                                                    delay(100)
                                                    syncProgress += 0.1f
                                                }
                                                isSyncing = false
                                                preferences.lastSyncedTime = System.currentTimeMillis()
                                                lastSyncedDisplay = "Just Now"
                                            }
                                        }
                                    },
                                    enabled = isCloudSyncEnabled && !isSyncing,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Force Instant Cloud Sync", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                1 -> {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                Text(
                                    text = "Preset Themes",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                val themes = listOf("Midnight OLED", "Nordic Light", "Forest Moss", "Retro Cream", "Cyberpunk Pink")
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    themes.take(3).forEach { th ->
                                        val isSelected = selectedTheme == th
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                                .border(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                                                .clickable {
                                                    selectedTheme = th
                                                    preferences.selectedTheme = th
                                                    refreshTrigger++
                                                }
                                                .padding(vertical = 8.dp)
                                        ) {
                                            Text(
                                                text = th,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    themes.drop(3).forEach { th ->
                                        val isSelected = selectedTheme == th
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                                .border(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                                                .clickable {
                                                    selectedTheme = th
                                                    preferences.selectedTheme = th
                                                    refreshTrigger++
                                                }
                                                .padding(vertical = 8.dp)
                                        ) {
                                            Text(
                                                text = th,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (customThemes.isNotEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(
                                        text = "Your Saved Custom Themes",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    customThemes.forEach { th ->
                                        val isSelected = selectedTheme == th.name
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent)
                                                .border(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                                                .clickable {
                                                    selectedTheme = th.name
                                                    preferences.selectedTheme = th.name
                                                    refreshTrigger++
                                                }
                                                .padding(horizontal = 12.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                // Small color bubble previewing the background
                                                Box(
                                                    modifier = Modifier
                                                        .size(16.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                            try {
                                                                Color(android.graphics.Color.parseColor(th.backgroundColorHex))
                                                            } catch (e: Exception) {
                                                                Color.Gray
                                                            }
                                                        )
                                                )
                                                Text(
                                                    text = th.name,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 13.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }

                                            IconButton(
                                                onClick = {
                                                    scope.launch {
                                                        database.customThemeDao().delete(th.name)
                                                        if (selectedTheme == th.name) {
                                                            selectedTheme = "Midnight OLED"
                                                            preferences.selectedTheme = "Midnight OLED"
                                                        }
                                                        refreshTrigger++
                                                    }
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete theme",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = "Create Custom Aesthetic Theme",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                OutlinedTextField(
                                    value = newThemeName,
                                    onValueChange = { newThemeName = it },
                                    label = { Text("Theme Name") },
                                    textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                                    modifier = Modifier.fillMaxWidth().testTag("custom_theme_name_input")
                                )

                                val colorInputs = listOf(
                                    "Background" to bgHex to { v: String -> bgHex = v },
                                    "Key Background" to keyBgHex to { v: String -> keyBgHex = v },
                                    "Key Text" to keyTextHex to { v: String -> keyTextHex = v },
                                    "Accent Color" to accentHex to { v: String -> accentHex = v },
                                    "Header Background" to headerBgHex to { v: String -> headerBgHex = v },
                                    "Header Icon" to headerIconHex to { v: String -> headerIconHex = v }
                                )

                                colorInputs.forEach { pair ->
                                    val (labelAndVal, setter) = pair
                                    val (label, currentVal) = labelAndVal
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = currentVal,
                                            onValueChange = { setter(it) },
                                            label = { Text(label) },
                                            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                                            modifier = Modifier.weight(1f)
                                        )

                                        // Small circle previewing current color hex string
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    try {
                                                        Color(android.graphics.Color.parseColor(if (currentVal.startsWith("#")) currentVal else "#$currentVal"))
                                                    } catch (e: Exception) {
                                                        Color.LightGray
                                                    }
                                                )
                                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                        )
                                    }
                                }

                                Button(
                                    onClick = {
                                        if (newThemeName.isNotBlank()) {
                                            scope.launch {
                                                database.customThemeDao().insert(
                                                    CustomTheme(
                                                        name = newThemeName,
                                                        backgroundColorHex = bgHex,
                                                        keyBackgroundColorHex = keyBgHex,
                                                        keyTextColorHex = keyTextHex,
                                                        accentColorHex = accentHex,
                                                        headerBackgroundColorHex = headerBgHex,
                                                        headerIconColorHex = headerIconHex
                                                    )
                                                )
                                                selectedTheme = newThemeName
                                                preferences.selectedTheme = newThemeName
                                                newThemeName = ""
                                                refreshTrigger++
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().testTag("save_theme_button"),
                                    enabled = newThemeName.isNotBlank()
                                ) {
                                    Icon(Icons.Default.Save, contentDescription = "Save")
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Save Theme & Activate", fontSize = 12.sp)
                                }
                            }
                        }
                    }

                }
                2 -> {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                Text(
                                    text = "One-Handed Layout Mode",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val layouts = listOf("Standard", "Left", "Right")
                                    layouts.forEach { lay ->
                                        val isSelected = oneHandedMode == lay
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                                .border(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                                                .clickable {
                                                    oneHandedMode = lay
                                                    preferences.oneHandedMode = lay
                                                    refreshTrigger++
                                                }
                                                .padding(vertical = 10.dp)
                                        ) {
                                            Text(
                                                text = if (lay == "Standard") "Full Width" else "$lay Dock",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                Text(
                                    text = "Typing Animation",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Enable key press animations",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Switch(
                                        checked = typingAnimation,
                                        onCheckedChange = {
                                            typingAnimation = it
                                            preferences.typingAnimation = it
                                            refreshTrigger++
                                        }
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                Text(
                                    text = "Deleting Speed",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Column {
                                    Slider(
                                        value = deletingSpeed,
                                        onValueChange = {
                                            deletingSpeed = it
                                            preferences.deletingSpeed = it
                                            refreshTrigger++
                                        },
                                        valueRange = 0.1f..3f
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Slow", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("Fast", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                // HAPTIC CONTROLS
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = "Tactile Haptic Feedback",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Vibrate on keys typing",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Switch(
                                        checked = isHapticEnabled,
                                        onCheckedChange = {
                                            isHapticEnabled = it
                                            preferences.isHapticEnabled = it
                                            refreshTrigger++
                                        }
                                    )
                                }

                                if (isHapticEnabled) {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Vibration Intensity", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text("${hapticStrength.toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        }

                                        Slider(
                                            value = hapticStrength,
                                            onValueChange = {
                                                hapticStrength = it
                                                preferences.hapticStrength = it.toInt()
                                                refreshTrigger++
                                            },
                                            valueRange = 10f..100f,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

            }
        }

        // --- SETUP GUIDE ---
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("How to Enable", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text("1. Go to System Settings -> System -> Keyboard -> On-screen keyboard", fontSize = 14.sp)
                Text("2. Enable this keyboard", fontSize = 14.sp)
                Text("3. Switch to it using the keyboard icon in the navigation bar when typing", fontSize = 14.sp)
            }
        }
    }
}

// Custom modifier border for specific edges
fun Modifier.border(
    t: androidx.compose.ui.unit.Dp = 0.dp,
    color: Color = Color.Transparent
): Modifier = this.drawBehind {
    val strokeWidth = t.toPx()
    if (strokeWidth > 0) {
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(0f, 0f),
            end = androidx.compose.ui.geometry.Offset(size.width, 0f),
            strokeWidth = strokeWidth
        )
    }
}
