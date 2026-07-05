package com.example.ui.keyboard

import android.content.Context
import android.os.Vibrator
import android.view.HapticFeedbackConstants
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppDatabase
import com.example.data.ClipboardItem
import com.example.data.PreferencesManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.gestures.awaitFirstDown

// Define Theme Colors
data class KeyboardThemeColors(
    val background: Color,
    val keyBackground: Color,
    val keyTextColor: Color,
    val accentColor: Color,
    val headerBackground: Color,
    val headerIconColor: Color
)

val MidnightOledTheme = KeyboardThemeColors(
    background = Color(0xFF000000),
    keyBackground = Color(0xFF1C1C1E),
    keyTextColor = Color(0xFFFFFFFF),
    accentColor = Color(0xFF0A84FF),
    headerBackground = Color(0xFF121212),
    headerIconColor = Color(0xFF8E8E93)
)

val NordicLightTheme = KeyboardThemeColors(
    background = Color(0xFFF2F2F7),
    keyBackground = Color(0xFFFFFFFF),
    keyTextColor = Color(0xFF1C1C1E),
    accentColor = Color(0xFF5856D6),
    headerBackground = Color(0xFFE5E5EA),
    headerIconColor = Color(0xFF48484A)
)

val ForestMossTheme = KeyboardThemeColors(
    background = Color(0xFF2D3B32),
    keyBackground = Color(0xFF3B4D40),
    keyTextColor = Color(0xFFE8F0EC),
    accentColor = Color(0xFFE5C158),
    headerBackground = Color(0xFF222C25),
    headerIconColor = Color(0xFF9FB5A6)
)

val RetroCreamTheme = KeyboardThemeColors(
    background = Color(0xFFF4EAD4),
    keyBackground = Color(0xFFE8DBBC),
    keyTextColor = Color(0xFF4A3E3D),
    accentColor = Color(0xFFD35230),
    headerBackground = Color(0xFFE2D4B4),
    headerIconColor = Color(0xFF7A6B68)
)

val CyberpunkPinkTheme = KeyboardThemeColors(
    background = Color(0xFF1A0B2E),
    keyBackground = Color(0xFF2A1B4E),
    keyTextColor = Color(0xFFFF007F),
    accentColor = Color(0xFF00FFFF),
    headerBackground = Color(0xFF0D0518),
    headerIconColor = Color(0xFFFF007F)
)

fun getThemeColors(name: String): KeyboardThemeColors {
    return when (name) {
        "Nordic Light" -> NordicLightTheme
        "Forest Moss" -> ForestMossTheme
        "Retro Cream" -> RetroCreamTheme
        "Cyberpunk Pink" -> CyberpunkPinkTheme
        else -> MidnightOledTheme
    }
}

fun parseColorOrFallback(hex: String, fallback: Color): Color {
    return try {
        val cleanHex = if (hex.startsWith("#")) hex else "#$hex"
        Color(android.graphics.Color.parseColor(cleanHex))
    } catch (e: Exception) {
        fallback
    }
}

fun findKeyAtCoordinate(
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    keyRows: List<List<String>>
): String? {
    if (width <= 0 || height <= 0) return null
    if (y < 0 || y > height || x < 0 || x > width) return null

    val totalRowsCount = keyRows.size + 1
    val rowHeight = height / totalRowsCount
    val rowIndex = (y / rowHeight).toInt().coerceIn(0, totalRowsCount - 1)

    if (rowIndex < keyRows.size) {
        val rowKeys = keyRows.getOrNull(rowIndex) ?: return null
        val numKeys = rowKeys.size
        if (numKeys == 0) return null
        val keyWidth = width / numKeys
        val keyIndex = (x / keyWidth).toInt().coerceIn(0, numKeys - 1)
        return rowKeys.getOrNull(keyIndex)
    } else {
        // Spacebar detection in bottom row
        val spacebarLeft = width * 0.25f
        val spacebarRight = width * 0.75f
        return if (x in spacebarLeft..spacebarRight) {
            " "
        } else {
            null
        }
    }
}

val COMMON_WORDS = listOf(
    // English
    "the", "be", "to", "of", "and", "a", "in", "that", "have", "i", "it", "for", "not", "on", "with", "he", "as", "you", "do", "at",
    "this", "but", "his", "by", "from", "they", "we", "say", "her", "she", "or", "an", "will", "my", "one", "all", "would", "there", "their", "what",
    "so", "up", "out", "if", "about", "who", "get", "which", "go", "me", "when", "make", "can", "like", "time", "no", "just", "him", "know", "take",
    "people", "into", "year", "your", "good", "some", "could", "them", "see", "other", "than", "then", "now", "look", "only", "come", "its", "over", "think",
    "also", "back", "after", "use", "two", "how", "our", "work", "first", "well", "way", "even", "new", "want", "because", "any", "these", "give", "day", "most", "us",
    // Spanish
    "el", "la", "los", "que", "un", "una", "y", "en", "para", "por", "con", "es", "una", "este", "esta", "pero", "como", "mas", "o", "si", "yo", "me", "mi", "se", "lo", "al", "del", "sus", "nos", "todo", "bien", "muy", "dia", "año", "ver", "dar", "ir", "hacer", "quiero", "gracias",
    // French
    "le", "la", "les", "un", "une", "et", "en", "que", "pour", "dans", "des", "du", "sur", "avec", "est", "ce", "cette", "mais", "comme", "plus", "ou", "si", "je", "me", "mon", "se", "nous", "vous", "tout", "bien", "tres", "jour", "ans", "voir", "faire", "aller", "merci",
    // German
    "der", "die", "das", "ein", "eine", "und", "in", "zu", "von", "mit", "den", "dem", "des", "im", "auf", "ist", "es", "das", "aber", "wie", "mehr", "oder", "wenn", "ich", "mich", "mein", "sich", "wir", "ihr", "alles", "gut", "sehr", "tag", "jahr", "sehen", "tun", "gehen", "danke"
)

fun matchSwipedSequence(swiped: List<String>, dictionary: List<String>): String? {
    if (swiped.isEmpty()) return null
    val swipedStr = swiped.joinToString("").lowercase()
    if (swipedStr.isEmpty()) return null

    val firstChar = swipedStr.first()
    val lastChar = swipedStr.last()

    var bestWord: String? = null
    var bestScore = -1f

    for (word in dictionary) {
        if (word.length < 2) continue
        val wFirst = word.first().lowercaseChar()
        val wLast = word.last().lowercaseChar()

        val startsMatch = wFirst == firstChar
        val endsMatch = wLast == lastChar
        if (!startsMatch) continue

        var swipedIdx = 0
        var matchedCount = 0
        for (char in word.lowercase()) {
            while (swipedIdx < swipedStr.length) {
                if (swipedStr[swipedIdx] == char) {
                    matchedCount++
                    swipedIdx++
                    break
                }
                swipedIdx++
            }
        }

        if (matchedCount == word.length) {
            val excessChars = swipedStr.length - word.length
            val score = 100f / (1f + excessChars)
            if (score > bestScore) {
                bestScore = score
                bestWord = word
            }
        }
    }

    return bestWord ?: swipedStr
}

// Emoji structure
data class EmojiItem(val emoji: String, val name: String, val category: String)

val EMOJI_LIST = listOf(
    EmojiItem("😀", "laughing happy grin", "Smileys"),
    EmojiItem("😂", "laugh tears joy", "Smileys"),
    EmojiItem("🤣", "rofl laugh floor", "Smileys"),
    EmojiItem("😊", "smiling blush eyes", "Smileys"),
    EmojiItem("😍", "heart eyes love", "Smileys"),
    EmojiItem("😘", "kiss blowing love", "Smileys"),
    EmojiItem("😜", "wink tongue crazy", "Smileys"),
    EmojiItem("😎", "cool sunglasses", "Smileys"),
    EmojiItem("😭", "crying sad sob", "Smileys"),
    EmojiItem("😡", "angry mad rage", "Smileys"),
    EmojiItem("❤️", "heart red love", "Hearts"),
    EmojiItem("🔥", "fire flame hot burn", "Objects"),
    EmojiItem("✨", "sparkles shine star", "Objects"),
    EmojiItem("🎉", "party popper celebrate", "Objects"),
    EmojiItem("👍", "thumbs up agree ok", "Hands"),
    EmojiItem("🙌", "hands raise celebrate", "Hands"),
    EmojiItem("🙏", "pray folded please", "Hands"),
    EmojiItem("🚀", "rocket space speed", "Travel"),
    EmojiItem("💡", "idea lightbulb mind", "Objects"),
    EmojiItem("☕", "coffee hot drink", "Food"),
    EmojiItem("🍕", "pizza slice food", "Food"),
    EmojiItem("🐶", "dog puppy animal", "Animals"),
    EmojiItem("🐱", "cat kitten animal", "Animals"),
    EmojiItem("🌟", "star shining gold", "Objects"),
    EmojiItem("💪", "muscle flex strong", "Hands"),
    EmojiItem("💯", "hundred score perfect", "Objects"),
    EmojiItem("🤔", "thinking ponder hand", "Smileys"),
    EmojiItem("🙄", "eyeroll eye rolling", "Smileys"),
    EmojiItem("🥺", "pleading beg puppy", "Smileys"),
    EmojiItem("😴", "sleeping tired sleep", "Smileys"),
    EmojiItem("🥳", "party celebrate hat", "Smileys"),
    EmojiItem("💩", "poop turd smile", "Smileys"),
    EmojiItem("👀", "eyes looking watch", "Objects"),
    EmojiItem("👏", "clapping clap hands", "Hands"),
    EmojiItem("💔", "broken heart sad", "Hearts"),
    EmojiItem("✨", "sparkles stars", "Objects"),
    EmojiItem("💻", "laptop computer tech", "Objects"),
    EmojiItem("📱", "phone mobile tech", "Objects"),
    EmojiItem("🍔", "burger hamburger food", "Food"),
    EmojiItem("🍩", "donut sweet food", "Food"),
    EmojiItem("🍻", "cheers beers drinks", "Food"),
    EmojiItem("🌍", "globe earth world", "Travel"),
    EmojiItem("✈️", "airplane flight travel", "Travel"),
    EmojiItem("💼", "briefcase work job", "Objects")
)

@Composable
fun KeyboardLayout(
    isSystemKeyboard: Boolean = false,
    onKeyClick: (String) -> Unit,
    onBackspace: () -> Unit,
    onAction: () -> Unit,
    onSpace: () -> Unit,
    modifier: Modifier = Modifier,
    // Used for instant UI updates when values are modified from MainActivity
    refreshTrigger: Int = 0
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val database = remember { AppDatabase.getDatabase(context) }
    val preferences = remember { PreferencesManager(context) }

    // Read states
    var selectedTheme by remember(refreshTrigger) { mutableStateOf(preferences.selectedTheme) }
    var selectedLanguage by remember(refreshTrigger) { mutableStateOf(preferences.selectedLanguage) }
    var isHapticEnabled by remember(refreshTrigger) { mutableStateOf(preferences.isHapticEnabled) }
    var isCloudSyncEnabled by remember(refreshTrigger) { mutableStateOf(preferences.isCloudSyncEnabled) }
    var oneHandedMode by remember(refreshTrigger) { mutableStateOf(preferences.oneHandedMode) }

    val gesturePoints = remember { mutableStateListOf<Offset>() }
    var containerWidth by remember { mutableStateOf(0) }
    var containerHeight by remember { mutableStateOf(0) }

    // Navigation sub-panels inside keyboard
    var activeSubPanel by remember { mutableStateOf<KeyboardSubPanel>(KeyboardSubPanel.None) }
    var showClipboardOverlay by remember { mutableStateOf(false) }
    var isSymbolMode by remember { mutableStateOf(false) }
    var isShiftEnabled by remember { mutableStateOf(false) }
    var emojiSearchQuery by remember { mutableStateOf("") }
    var emojiSelectedCategory by remember { mutableStateOf("All") }

    // Fetch Clipboard from DB
    val clipboardList by database.clipboardDao().getAll().collectAsState(initial = emptyList())

    // Fetch Custom Themes from DB
    val customThemes by database.customThemeDao().getAll().collectAsState(initial = emptyList())

    val colors = remember(selectedTheme, customThemes) {
        val custom = customThemes.find { it.name == selectedTheme }
        if (custom != null) {
            KeyboardThemeColors(
                background = parseColorOrFallback(custom.backgroundColorHex, MidnightOledTheme.background),
                keyBackground = parseColorOrFallback(custom.keyBackgroundColorHex, MidnightOledTheme.keyBackground),
                keyTextColor = parseColorOrFallback(custom.keyTextColorHex, MidnightOledTheme.keyTextColor),
                accentColor = parseColorOrFallback(custom.accentColorHex, MidnightOledTheme.accentColor),
                headerBackground = parseColorOrFallback(custom.headerBackgroundColorHex, MidnightOledTheme.headerBackground),
                headerIconColor = parseColorOrFallback(custom.headerIconColorHex, MidnightOledTheme.headerIconColor)
            )
        } else {
            getThemeColors(selectedTheme)
        }
    }

    // Trigger local haptic vibration
    val triggerHaptic = {
        if (isHapticEnabled) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }

    // Keyboard layouts mapping based on language selection
    val rows = remember(selectedLanguage) {
        when (selectedLanguage) {
            "ES" -> listOf(
                listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P"),
                listOf("A", "S", "D", "F", "G", "H", "J", "K", "L", "Ñ"),
                listOf("Z", "X", "C", "V", "B", "N", "M")
            )
            "FR" -> listOf(
                listOf("A", "Z", "E", "R", "T", "Y", "U", "I", "O", "P"),
                listOf("Q", "S", "D", "F", "G", "H", "J", "K", "L", "M"),
                listOf("W", "X", "C", "V", "B", "N")
            )
            "DE" -> listOf(
                listOf("Q", "W", "E", "R", "T", "Z", "U", "I", "O", "P", "Ü"),
                listOf("A", "S", "D", "F", "G", "H", "J", "K", "L", "Ö", "Ä"),
                listOf("Y", "X", "C", "V", "B", "N", "M")
            )
            else -> listOf(
                listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P"),
                listOf("A", "S", "D", "F", "G", "H", "J", "K", "L"),
                listOf("Z", "X", "C", "V", "B", "N", "M")
            )
        }
    }

    val numberRow = remember { listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0") }

    val symbolRows = remember {
        listOf(
            listOf("[", "]", "{", "}", "#", "%", "^", "*", "+", "="),
            listOf("_", "\\", "|", "~", "<", ">", "€", "£", "¥", "•"),
            listOf(".", ",", "?", "!", "'", "\"", "-", "/", ":", ";")
        )
    }

    val activeKeysRows = remember(isSymbolMode, rows) {
        if (isSymbolMode) symbolRows else rows
    }

    val allKeyRows = remember(activeKeysRows, isSymbolMode) {
        val base = listOf(numberRow) + activeKeysRows
        if (!isSymbolMode && base.size > 3) {
            val list = base.toMutableList()
            list[3] = listOf("SHIFT") + list[3]
            list
        } else {
            base
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.background)
            .navigationBarsPadding()
            .testTag("keyboard_container")
    ) {
        // --- KEYBOARD HEADER TOOLBAR ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.headerBackground)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Panel toggles
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Clipboard Toggle
                IconButton(
                    onClick = {
                        triggerHaptic()
                        activeSubPanel = KeyboardSubPanel.None
                        showClipboardOverlay = !showClipboardOverlay
                    },
                    modifier = Modifier.testTag("toggle_clipboard_panel")
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentPaste,
                        contentDescription = "Clipboard history",
                        tint = if (showClipboardOverlay) colors.accentColor else colors.headerIconColor
                    )
                }

                // Emoji Toggle
                IconButton(
                    onClick = {
                        triggerHaptic()
                        activeSubPanel = if (activeSubPanel == KeyboardSubPanel.Emoji) {
                            KeyboardSubPanel.None
                        } else {
                            KeyboardSubPanel.Emoji
                        }
                    },
                    modifier = Modifier.testTag("toggle_emoji_panel")
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEmotions,
                        contentDescription = "Emoji Search",
                        tint = if (activeSubPanel == KeyboardSubPanel.Emoji) colors.accentColor else colors.headerIconColor
                    )
                }

                // Language Quick Switcher
                IconButton(
                    onClick = {
                        triggerHaptic()
                        val list = listOf("EN", "ES", "FR", "DE")
                        val nextIndex = (list.indexOf(selectedLanguage) + 1) % list.size
                        selectedLanguage = list[nextIndex]
                        preferences.selectedLanguage = selectedLanguage
                    },
                    modifier = Modifier.testTag("toggle_language")
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(colors.keyBackground)
                    ) {
                        Text(
                            text = selectedLanguage,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.keyTextColor
                        )
                    }
                }
            }

            // Central minimalist status indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (isCloudSyncEnabled) Color(0xFF4CD964) else Color(0xFFFF9500))
                )
                Text(
                    text = if (isCloudSyncEnabled) "Cloud Synced" else "Offline Only",
                    color = colors.headerIconColor,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.SansSerif
                )
            }

            // Quick Theme Switcher & Settings shortcut
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // One-Handed Mode Toggle
                IconButton(
                    onClick = {
                        triggerHaptic()
                        oneHandedMode = when (oneHandedMode) {
                            "Standard" -> "Right"
                            "Right" -> "Left"
                            else -> "Standard"
                        }
                        preferences.oneHandedMode = oneHandedMode
                    },
                    modifier = Modifier.testTag("toggle_one_handed")
                ) {
                    Icon(
                        imageVector = when (oneHandedMode) {
                            "Left" -> Icons.Default.KeyboardDoubleArrowLeft
                            "Right" -> Icons.Default.KeyboardDoubleArrowRight
                            else -> Icons.Default.Smartphone
                        },
                        contentDescription = "One-handed mode",
                        tint = colors.headerIconColor
                    )
                }

                // Theme Cycle
                IconButton(
                    onClick = {
                        triggerHaptic()
                        val availableThemes = mutableListOf("Midnight OLED", "Nordic Light", "Forest Moss", "Retro Cream", "Cyberpunk Pink")
                        customThemes.forEach { availableThemes.add(it.name) }
                        val nextIndex = (availableThemes.indexOf(selectedTheme) + 1) % availableThemes.size
                        selectedTheme = availableThemes.getOrElse(nextIndex) { "Midnight OLED" }
                        preferences.selectedTheme = selectedTheme
                    },
                    modifier = Modifier.testTag("toggle_theme")
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = "Cycle Theme",
                        tint = colors.headerIconColor
                    )
                }
            }
        }

        // --- SUB PANELS OR TYPING INTERFACE ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp)
        ) {
            when (activeSubPanel) {
                KeyboardSubPanel.None -> {
                    Row(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Left-side controls when docked Right in One-Handed mode
                        if (oneHandedMode == "Right") {
                            Column(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(54.dp)
                                    .background(colors.headerBackground)
                                    .padding(vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                IconButton(
                                    onClick = {
                                        triggerHaptic()
                                        oneHandedMode = "Left"
                                        preferences.oneHandedMode = oneHandedMode
                                    },
                                    modifier = Modifier.testTag("dock_left_button")
                                ) {
                                    Icon(Icons.Default.ChevronLeft, contentDescription = "Dock Left", tint = colors.keyTextColor)
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                IconButton(
                                    onClick = {
                                        triggerHaptic()
                                        oneHandedMode = "Standard"
                                        preferences.oneHandedMode = oneHandedMode
                                    },
                                    modifier = Modifier.testTag("expand_standard_button")
                                ) {
                                    Icon(Icons.Default.Fullscreen, contentDescription = "Full width", tint = colors.keyTextColor)
                                }
                            }
                        }

                        // Main typing workspace with drag gesture recognition and trail overlay
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .onSizeChanged {
                                    containerWidth = it.width
                                    containerHeight = it.height
                                }
                                .pointerInput(allKeyRows, selectedLanguage, oneHandedMode, containerWidth, containerHeight) {
                                    awaitPointerEventScope {
                                        while (true) {
                                            val down = awaitFirstDown(requireUnconsumed = false)
                                            val dragPath = mutableListOf(down.position)
                                            val swipedKeys = mutableListOf<String>()
                                            var isDragging = false
                                            val startPos = down.position

                                            val initialKey = findKeyAtCoordinate(
                                                down.position.x,
                                                down.position.y,
                                                containerWidth.toFloat(),
                                                containerHeight.toFloat(),
                                                allKeyRows
                                            )
                                            if (initialKey != null && initialKey != "SHIFT") {
                                                swipedKeys.add(initialKey)
                                            }

                                            do {
                                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                                val pointer = event.changes.first()
                                                if (pointer.pressed) {
                                                    val currentPos = pointer.position
                                                    dragPath.add(currentPos)
                                                    gesturePoints.clear()
                                                    gesturePoints.addAll(dragPath)

                                                    val distance = (currentPos - startPos).getDistance()
                                                    if (distance > 30f) {
                                                        isDragging = true
                                                    }

                                                    if (isDragging) {
                                                        pointer.consume()
                                                        val key = findKeyAtCoordinate(
                                                            currentPos.x,
                                                            currentPos.y,
                                                            containerWidth.toFloat(),
                                                            containerHeight.toFloat(),
                                                            allKeyRows
                                                        )
                                                        if (key != null && key != "SHIFT" && (swipedKeys.isEmpty() || swipedKeys.last() != key)) {
                                                            swipedKeys.add(key)
                                                        }
                                                    }
                                                }
                                            } while (event.changes.any { it.pressed })

                                            if (isDragging && swipedKeys.isNotEmpty()) {
                                                val word = matchSwipedSequence(swipedKeys, COMMON_WORDS)
                                                if (word != null && word.isNotEmpty()) {
                                                    onKeyClick("$word ")
                                                    triggerHaptic()
                                                }
                                            }
                                            gesturePoints.clear()
                                        }
                                    }
                                }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 4.dp, vertical = 6.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Number Row + Active Keys (Letters or Symbols) Rows
                                allKeyRows.forEach { rowKeys ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        rowKeys.forEach { key ->
                                            if (key == "SHIFT") {
                                                IconButtonKey(
                                                    icon = if (isShiftEnabled) Icons.Default.KeyboardCapslock else Icons.Default.ArrowUpward,
                                                    onClick = {
                                                        triggerHaptic()
                                                        isShiftEnabled = !isShiftEnabled
                                                    },
                                                    colors = colors,
                                                    isAccent = isShiftEnabled,
                                                    modifier = Modifier.weight(1f).testTag("shift_key")
                                                )
                                            } else {
                                                val isLetter = key.length == 1 && key[0].isLetter()
                                                val displayText = if (isLetter) {
                                                    if (isShiftEnabled) key.uppercase() else key.lowercase()
                                                } else {
                                                    key
                                                }
                                                KeyButton(
                                                    text = displayText,
                                                    onClick = {
                                                        triggerHaptic()
                                                        onKeyClick(displayText)
                                                        if (isShiftEnabled) {
                                                            isShiftEnabled = false
                                                        }
                                                    },
                                                    colors = colors,
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                        }
                                    }
                                }

                                // Bottom Action Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    // Symbol/ABC switcher
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .weight(1.3f)
                                            .height(44.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(colors.keyBackground)
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = RippleConfigurationProvider.getRipple(),
                                                onClick = {
                                                    triggerHaptic()
                                                    isSymbolMode = !isSymbolMode
                                                }
                                            )
                                            .testTag("toggle_symbols_key"),
                                    ) {
                                        Text(
                                            text = if (isSymbolMode) "ABC" else "?123",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.keyTextColor
                                        )
                                    }

                                    // Emoji trigger inside layout
                                    IconButtonKey(
                                        icon = Icons.Default.SentimentSatisfied,
                                        onClick = {
                                            triggerHaptic()
                                            activeSubPanel = KeyboardSubPanel.Emoji
                                        },
                                        colors = colors,
                                        modifier = Modifier.weight(1.2f)
                                    )

                                    // Space Key
                                    Box(
                                        modifier = Modifier
                                            .weight(4f)
                                            .height(44.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(colors.keyBackground)
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = RippleConfigurationProvider.getRipple(),
                                                onClick = {
                                                    triggerHaptic()
                                                    onSpace()
                                                }
                                            )
                                            .testTag("space_key"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = selectedLanguage,
                                            fontSize = 12.sp,
                                            letterSpacing = 1.sp,
                                            color = colors.keyTextColor.copy(alpha = 0.5f),
                                            fontWeight = FontWeight.Light
                                        )
                                    }

                                    // Backspace Key
                                    IconButtonKey(
                                        icon = Icons.AutoMirrored.Filled.Backspace,
                                        onClick = {
                                            triggerHaptic()
                                            onBackspace()
                                        },
                                        colors = colors,
                                        modifier = Modifier
                                            .weight(1.2f)
                                            .testTag("backspace_key")
                                    )

                                    // Action / Enter Key
                                    IconButtonKey(
                                        icon = Icons.Default.SubdirectoryArrowLeft,
                                        onClick = {
                                            triggerHaptic()
                                            onAction()
                                        },
                                        colors = colors,
                                        isAccent = true,
                                        modifier = Modifier
                                            .weight(1.3f)
                                            .testTag("enter_key")
                                    )
                                }
                            }

                            // Render smooth glowing swipe trail
                            if (gesturePoints.isNotEmpty()) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    if (gesturePoints.size > 1) {
                                        val path = androidx.compose.ui.graphics.Path().apply {
                                            moveTo(gesturePoints[0].x, gesturePoints[0].y)
                                            for (i in 1 until gesturePoints.size) {
                                                lineTo(gesturePoints[i].x, gesturePoints[i].y)
                                            }
                                        }
                                        drawPath(
                                            path = path,
                                            color = colors.accentColor.copy(alpha = 0.65f),
                                            style = Stroke(
                                                width = 6.dp.toPx(),
                                                cap = StrokeCap.Round,
                                                join = androidx.compose.ui.graphics.StrokeJoin.Round
                                            )
                                        )
                                    }
                                }
                            }

                            // Sliding Clipboard Side-Panel / Overlay (Last 20 items)
                            androidx.compose.animation.AnimatedVisibility(
                                visible = showClipboardOverlay,
                                enter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(),
                                exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(),
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(180.dp)
                                    .align(Alignment.CenterStart)
                            ) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(end = 4.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = colors.background.copy(alpha = 0.98f)
                                    ),
                                    shape = RoundedCornerShape(topStart = 0.dp, bottomStart = 0.dp, topEnd = 12.dp, bottomEnd = 12.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.keyBackground)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(6.dp)
                                    ) {
                                        // Header of overlay
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.ContentPaste,
                                                    contentDescription = null,
                                                    tint = colors.accentColor,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Text(
                                                    text = "Clipboard (Last 20)",
                                                    color = colors.keyTextColor,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp
                                                )
                                            }
                                            IconButton(
                                                onClick = {
                                                    triggerHaptic()
                                                    showClipboardOverlay = false
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Close overlay",
                                                    tint = colors.keyTextColor.copy(alpha = 0.6f),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        // List of items
                                        val last20 = clipboardList.take(20)
                                        if (last20.isEmpty()) {
                                            Box(
                                                modifier = Modifier.fillMaxSize(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "No items copied yet",
                                                    color = colors.keyTextColor.copy(alpha = 0.4f),
                                                    fontSize = 11.sp
                                                )
                                            }
                                        } else {
                                            androidx.compose.foundation.lazy.LazyColumn(
                                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                                modifier = Modifier.fillMaxSize()
                                            ) {
                                                items(last20) { item ->
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clip(RoundedCornerShape(6.dp))
                                                            .background(colors.keyBackground)
                                                            .clickable {
                                                                triggerHaptic()
                                                                onKeyClick(item.text)
                                                            }
                                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text(
                                                            text = item.text.take(45) + if (item.text.length > 45) "..." else "",
                                                            color = colors.keyTextColor,
                                                            fontSize = 11.sp,
                                                            modifier = Modifier.weight(1f)
                                                        )

                                                        // Pin status indicator
                                                        if (item.isPinned) {
                                                            Icon(
                                                                imageVector = Icons.Default.PushPin,
                                                                contentDescription = "Pinned",
                                                                tint = colors.accentColor,
                                                                modifier = Modifier.size(10.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Right-side controls when docked Left in One-Handed mode
                        if (oneHandedMode == "Left") {
                            Column(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(54.dp)
                                    .background(colors.headerBackground)
                                    .padding(vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                IconButton(
                                    onClick = {
                                        triggerHaptic()
                                        oneHandedMode = "Right"
                                        preferences.oneHandedMode = oneHandedMode
                                    },
                                    modifier = Modifier.testTag("dock_right_button")
                                ) {
                                    Icon(Icons.Default.ChevronRight, contentDescription = "Dock Right", tint = colors.keyTextColor)
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                IconButton(
                                    onClick = {
                                        triggerHaptic()
                                        oneHandedMode = "Standard"
                                        preferences.oneHandedMode = oneHandedMode
                                    },
                                    modifier = Modifier.testTag("expand_standard_button")
                                ) {
                                    Icon(Icons.Default.Fullscreen, contentDescription = "Full width", tint = colors.keyTextColor)
                                }
                            }
                        }
                    }
                }

                KeyboardSubPanel.Emoji -> {
                    // Integrated Emoji Search panel
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            TextField(
                                value = emojiSearchQuery,
                                onValueChange = { emojiSearchQuery = it },
                                placeholder = { Text("Search emoji...", fontSize = 13.sp) },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", modifier = Modifier.size(18.dp)) },
                                trailingIcon = {
                                    if (emojiSearchQuery.isNotEmpty()) {
                                        IconButton(onClick = { emojiSearchQuery = "" }) {
                                            Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("emoji_search_input"),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = colors.keyBackground,
                                    unfocusedContainerColor = colors.keyBackground,
                                    focusedIndicatorColor = colors.accentColor,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    focusedTextColor = colors.keyTextColor,
                                    unfocusedTextColor = colors.keyTextColor
                                ),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp)
                            )

                            IconButton(
                                onClick = {
                                    triggerHaptic()
                                    activeSubPanel = KeyboardSubPanel.None
                                    emojiSearchQuery = ""
                                },
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colors.keyBackground)
                            ) {
                                Icon(Icons.Default.KeyboardHide, contentDescription = "Return", tint = colors.keyTextColor)
                            }
                        }

                        // Categories quick scroll
                        val categories = listOf("All", "Smileys", "Hearts", "Hands", "Objects", "Food", "Animals")
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(vertical = 6.dp)
                        ) {
                            items(categories) { cat ->
                                val isSelected = emojiSelectedCategory == cat
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) colors.accentColor else colors.keyBackground)
                                        .clickable {
                                            triggerHaptic()
                                            emojiSelectedCategory = cat
                                        }
                                        .padding(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = cat,
                                        fontSize = 11.sp,
                                        color = if (isSelected) colors.background else colors.keyTextColor,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        // Grid of filtered emojis
                        val filteredEmojis = remember(emojiSearchQuery, emojiSelectedCategory) {
                            EMOJI_LIST.filter { item ->
                                val matchesSearch = emojiSearchQuery.isEmpty() ||
                                        item.name.contains(emojiSearchQuery, ignoreCase = true) ||
                                        item.category.contains(emojiSearchQuery, ignoreCase = true)
                                val matchesCategory = emojiSelectedCategory == "All" || item.category == emojiSelectedCategory
                                matchesSearch && matchesCategory
                            }
                        }

                        if (filteredEmojis.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No matching emojis found", color = colors.keyTextColor.copy(alpha = 0.5f), fontSize = 12.sp)
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 44.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(filteredEmojis) { item ->
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(colors.keyBackground)
                                            .clickable {
                                                triggerHaptic()
                                                onKeyClick(item.emoji)
                                            }
                                    ) {
                                        Text(item.emoji, fontSize = 22.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                KeyboardSubPanel.Clipboard -> {
                    // Clipboard panel
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Endless Clipboard History",
                                color = colors.keyTextColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // Clear unpinned button
                                TextButton(
                                    onClick = {
                                        triggerHaptic()
                                        scope.launch {
                                            database.clipboardDao().clearUnpinned()
                                        }
                                    }
                                ) {
                                    Text("Clear Unpinned", fontSize = 11.sp, color = colors.accentColor)
                                }

                                IconButton(
                                    onClick = {
                                        triggerHaptic()
                                        activeSubPanel = KeyboardSubPanel.None
                                    },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(colors.keyBackground)
                                ) {
                                    Icon(Icons.Default.KeyboardHide, contentDescription = "Return", tint = colors.keyTextColor, modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        if (clipboardList.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.ContentPasteOff, contentDescription = "Empty", tint = colors.keyTextColor.copy(alpha = 0.3f), modifier = Modifier.size(36.dp))
                                    Text(
                                        text = "Clipboard history is empty.",
                                        fontSize = 12.sp,
                                        color = colors.keyTextColor.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(1),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(clipboardList) { item ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(colors.keyBackground)
                                            .clickable {
                                                triggerHaptic()
                                                onKeyClick(item.text)
                                            }
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = item.text.take(80) + if (item.text.length > 80) "..." else "",
                                            color = colors.keyTextColor,
                                            fontSize = 12.sp,
                                            modifier = Modifier.weight(1f)
                                        )

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            // Pin clip
                                            IconButton(
                                                onClick = {
                                                    triggerHaptic()
                                                    scope.launch {
                                                        database.clipboardDao().setPinned(item.id, !item.isPinned)
                                                    }
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (item.isPinned) Icons.Default.PushPin else Icons.Default.PushPin,
                                                    contentDescription = "Pin",
                                                    tint = if (item.isPinned) colors.accentColor else colors.headerIconColor.copy(alpha = 0.4f),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }

                                            // Delete clip
                                            IconButton(
                                                onClick = {
                                                    triggerHaptic()
                                                    scope.launch {
                                                        database.clipboardDao().delete(item.id)
                                                    }
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete",
                                                    tint = colors.headerIconColor.copy(alpha = 0.5f),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun KeyButton(
    text: String,
    onClick: () -> Unit,
    colors: KeyboardThemeColors,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(colors.keyBackground)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = RippleConfigurationProvider.getRipple(),
                onClick = onClick
            )
            .testTag("key_$text")
    ) {
        Text(
            text = text,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = colors.keyTextColor
        )
    }
}

@Composable
fun IconButtonKey(
    icon: ImageVector,
    onClick: () -> Unit,
    colors: KeyboardThemeColors,
    isAccent: Boolean = false,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (isAccent) colors.accentColor else colors.keyBackground)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = RippleConfigurationProvider.getRipple(),
                onClick = onClick
            )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isAccent) colors.background else colors.keyTextColor,
            modifier = Modifier.size(20.dp)
        )
    }
}

// Simple Ripple provider that works without experimental APIs
object RippleConfigurationProvider {
    @Composable
    fun getRipple() = androidx.compose.material3.ripple()
}

enum class KeyboardSubPanel {
    None,
    Emoji,
    Clipboard
}
