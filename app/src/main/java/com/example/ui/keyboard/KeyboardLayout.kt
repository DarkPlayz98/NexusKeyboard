package com.example.ui.keyboard

import android.content.Context
import android.os.Vibrator
import android.view.HapticFeedbackConstants
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.Send
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
import kotlinx.coroutines.delay
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
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

val PastelPinkTheme = KeyboardThemeColors(
    background = Color(0xFFFFEBF0),
    keyBackground = Color(0x66FFFFFF), // translucent white
    keyTextColor = Color(0xFF333333),
    accentColor = Color(0xFFFC5C65), // bright red/pink enter button
    headerBackground = Color(0x00000000), // transparent so gradient/background shows
    headerIconColor = Color(0xFF555555)
)

fun getThemeColors(name: String): KeyboardThemeColors {
    return when (name) {
        "Nordic Light" -> NordicLightTheme
        "Forest Moss" -> ForestMossTheme
        "Retro Cream" -> RetroCreamTheme
        "Pastel Pink" -> PastelPinkTheme
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
    "the", "be", "to", "of", "and", "a", "in", "that", "have", "I", 
    "it", "for", "not", "on", "with", "he", "as", "you", "do", "at", 
    "this", "but", "his", "by", "from", "they", "we", "say", "her", "she", 
    "or", "an", "will", "my", "one", "all", "would", "there", "their", "what", 
    "so", "up", "out", "if", "about", "who", "get", "which", "go", "me",
    "when", "make", "can", "like", "time", "no", "just", "him", "know", "take",
    "people", "into", "year", "your", "good", "some", "could", "them", "see", "other",
    "than", "then", "now", "look", "only", "come", "its", "over", "think", "also",
    "back", "after", "use", "two", "how", "our", "work", "first", "well", "way",
    "even", "new", "want", "because", "any", "these", "give", "day", "most", "us",
    "is", "are", "was", "were", "been", "has", "had", "does", "did", "doing",
    "will", "would", "shall", "should", "can", "could", "may", "might", "must",
    "hello", "world", "yes", "no", "ok", "okay", "thanks", "thank", "please", "sorry",
    "love", "much", "very", "really", "great", "awesome", "good", "bad", "cool",
    "today", "tomorrow", "yesterday", "morning", "night", "day", "week", "month", "year",
    "here", "there", "where", "when", "why", "how", "who", "what", "which",
    "always", "never", "sometimes", "often", "usually", "probably", "maybe",
    "something", "anything", "nothing", "everything", "someone", "anyone", "everyone",
    "feel", "find", "tell", "ask", "seem", "feel", "try", "leave", "call"
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
    var localRefreshTrigger by remember { mutableStateOf(refreshTrigger) }
    
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                localRefreshTrigger++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val database = remember { AppDatabase.getDatabase(context) }
    val preferences = remember { PreferencesManager(context) }

    var currentWord by remember(refreshTrigger) { mutableStateOf("") }
    
    val handleKeyClick: (String) -> Unit = { text ->
        if (text == " " || text == "\n" || text == "." || text == ",") {
            currentWord = ""
        } else if (text.length == 1 && text[0].isLetter()) {
            currentWord += text
        }
        onKeyClick(text)
    }
    
    val backspaceInteractionSource = remember { MutableInteractionSource() }
    val handleBackspace: () -> Unit = {
        if (currentWord.isNotEmpty()) {
            currentWord = currentWord.dropLast(1)
        }
        onBackspace()
    }
    
    val handleSpace: () -> Unit = {
        currentWord = ""
        onSpace()
    }
    
    val handleAction: () -> Unit = {
        currentWord = ""
        onAction()
    }

    // Read states
    var selectedTheme by remember(localRefreshTrigger) { mutableStateOf(preferences.selectedTheme) }
    var selectedLanguage by remember(refreshTrigger) { mutableStateOf(preferences.selectedLanguage) }
    var isHapticEnabled by remember(localRefreshTrigger) { mutableStateOf(preferences.isHapticEnabled) }
    var isCloudSyncEnabled by remember(refreshTrigger) { mutableStateOf(preferences.isCloudSyncEnabled) }
    var oneHandedMode by remember(refreshTrigger) { mutableStateOf(preferences.oneHandedMode) }
    var deletingSpeed by remember(refreshTrigger) { mutableStateOf(preferences.deletingSpeed) }
    var typingAnimation by remember(localRefreshTrigger) { mutableStateOf(preferences.typingAnimation) }

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

    // Trigger local haptic vibration and sound
    val audioManager = context.getSystemService(android.content.Context.AUDIO_SERVICE) as? android.media.AudioManager
    val triggerFeedback = {
        if (isHapticEnabled) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
        if (preferences.isSoundEnabled) {
            audioManager?.playSoundEffect(android.media.AudioManager.FX_KEYPRESS_STANDARD, 1.0f)
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
            listOf("@", "#", "$", "_", "&", "-", "+", "(", ")", "/"),
            listOf("*", "\"", "'", ":", ";", "!", "?", "~", "`", "|")
        )
    }

    val activeKeysRows = remember(isSymbolMode, rows) {
        if (isSymbolMode) symbolRows else rows
    }

    val allKeyRows = remember(activeKeysRows, isSymbolMode) {
        val base = listOf(numberRow) + activeKeysRows
        val list = base.toMutableList()
        if (list.size > 2) {
            val bottomLetterRowIdx = list.size - 1
            val bottomRow = list[bottomLetterRowIdx].toMutableList()
            bottomRow.add(0, if (isSymbolMode) "SYMSHIFT" else "SHIFT")
            bottomRow.add("BACKSPACE")
            list[bottomLetterRowIdx] = bottomRow
        }
        list
    }

    CompositionLocalProvider(LocalTypingAnimation provides typingAnimation) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .background(colors.background)
                .navigationBarsPadding()
                .testTag("keyboard_container")
        ) {
        // --- SUGGESTION BAR / SHORTCUTS ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(colors.headerBackground)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val suggestions = remember(currentWord) { 
                if (currentWord.isEmpty()) {
                    emptyList()
                } else {
                    val matching = COMMON_WORDS.filter { it.lowercase().startsWith(currentWord.lowercase()) }
                        .filter { it.lowercase() != currentWord.lowercase() }
                        .take(3)
                    if (matching.isEmpty()) {
                        listOf(currentWord, currentWord + "s", currentWord + "ed").take(3)
                    } else if (matching.size == 1) {
                        listOf(currentWord, matching[0], matching[0] + "s")
                    } else {
                        matching
                    }
                }
            }

            if (currentWord.isEmpty()) {
                // Shortcuts Mode
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            triggerFeedback()
                            activeSubPanel = if (activeSubPanel == KeyboardSubPanel.Apps) KeyboardSubPanel.None else KeyboardSubPanel.Apps
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Apps, "Apps", tint = if (activeSubPanel == KeyboardSubPanel.Apps) colors.accentColor else colors.headerIconColor)
                    }
                    IconButton(
                        onClick = {
                            triggerFeedback()
                            activeSubPanel = if (activeSubPanel == KeyboardSubPanel.Translate) KeyboardSubPanel.None else KeyboardSubPanel.Translate
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Translate, "Translate", tint = if (activeSubPanel == KeyboardSubPanel.Translate) colors.accentColor else colors.headerIconColor)
                    }
                    IconButton(
                        onClick = {
                            triggerFeedback()
                            activeSubPanel = if (activeSubPanel == KeyboardSubPanel.Emoji) KeyboardSubPanel.None else KeyboardSubPanel.Emoji
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.SentimentSatisfied, "Emoji", tint = if (activeSubPanel == KeyboardSubPanel.Emoji) colors.accentColor else colors.headerIconColor)
                    }
                    IconButton(
                        onClick = {
                            triggerFeedback()
                            activeSubPanel = if (activeSubPanel == KeyboardSubPanel.Gif) KeyboardSubPanel.None else KeyboardSubPanel.Gif
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Text("GIF", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (activeSubPanel == KeyboardSubPanel.Gif) colors.accentColor else colors.headerIconColor)
                    }
                    IconButton(
                        onClick = {
                            triggerFeedback()
                            activeSubPanel = if (activeSubPanel == KeyboardSubPanel.Clipboard) KeyboardSubPanel.None else KeyboardSubPanel.Clipboard
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.ContentPaste, "Clipboard", tint = if (activeSubPanel == KeyboardSubPanel.Clipboard) colors.accentColor else colors.headerIconColor)
                    }
                    IconButton(
                        onClick = {
                            triggerFeedback()
                            activeSubPanel = if (activeSubPanel == KeyboardSubPanel.Settings) KeyboardSubPanel.None else KeyboardSubPanel.Settings
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Settings, "Settings", tint = if (activeSubPanel == KeyboardSubPanel.Settings) colors.accentColor else colors.headerIconColor)
                    }
                }
            } else {
                // Suggestions Mode
                suggestions.forEach { word ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable { 
                                triggerFeedback()
                                for (i in currentWord.indices) {
                                    handleBackspace()
                                }
                                handleKeyClick("$word ") 
                                currentWord = ""
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = word,
                            fontSize = 15.sp,
                            color = colors.keyTextColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    if (word != suggestions.last()) {
                        Spacer(modifier = Modifier.width(1.dp).fillMaxHeight(0.6f).background(colors.headerIconColor.copy(alpha=0.3f)))
                    }
                }
            }
        }

        // --- KEYBOARD BODY ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp)
        ) {
            when (activeSubPanel) {
                KeyboardSubPanel.None -> {
                    Row(
                        modifier = Modifier.fillMaxSize()
                    ) {
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
                                            var isDragging = false
                                            val startPos = down.position
                                            
                                            do {
                                                val event = awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial)
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
                                                }
                                            } while (event.changes.any { it.pressed })

                                            if (isDragging) {
                                                handleKeyClick("test ")
                                                triggerFeedback()
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
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Calculate max weight for alignment
                                val maxWeight = remember(allKeyRows) {
                                    allKeyRows.maxOfOrNull { row ->
                                        row.sumOf { key ->
                                            when (key) {
                                                "SHIFT", "SYMSHIFT", "BACKSPACE" -> 1.5
                                                else -> 1.0
                                            }
                                        }
                                    }?.toFloat() ?: 10f
                                }

                                // Number Row + Active Keys (Letters or Symbols) Rows
                                allKeyRows.forEach { rowKeys ->
                                    val rowWeight = rowKeys.sumOf { key ->
                                        when (key) {
                                            "SHIFT", "SYMSHIFT", "BACKSPACE" -> 1.5
                                            else -> 1.0
                                        }
                                    }.toFloat()
                                    val paddingWeight = (maxWeight - rowWeight) / 2f

                                    Row(
                                        modifier = Modifier.fillMaxWidth().weight(1f),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        if (paddingWeight > 0.01f) {
                                            Spacer(modifier = Modifier.weight(paddingWeight))
                                        }

                                        rowKeys.forEach { key ->
                                            if (key == "SHIFT") {
                                                IconButtonKey(
                                                    icon = if (isShiftEnabled) Icons.Default.KeyboardCapslock else Icons.Default.ArrowUpward,
                                                    onClick = {
                                                        triggerFeedback()
                                                        isShiftEnabled = !isShiftEnabled
                                                    },
                                                    colors = colors,
                                                    isAccent = isShiftEnabled,
                                                    modifier = Modifier.weight(1.5f).testTag("shift_key")
                                                )
                                            } else if (key == "SYMSHIFT") {
                                                KeyButton(
                                                    text = "=\\<",
                                                    onClick = {
                                                        triggerFeedback()
                                                        isSymbolMode = false
                                                    },
                                                    colors = colors,
                                                    modifier = Modifier.weight(1.5f)
                                                )
                                            } else if (key == "BACKSPACE") {
                                                Box(
                                                    contentAlignment = Alignment.Center,
                                                    modifier = Modifier
                                                        .weight(1.5f)
                                                        .fillMaxHeight()
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(colors.keyBackground)
                                                        .clickable(
                                                            interactionSource = backspaceInteractionSource,
                                                            indication = RippleConfigurationProvider.getRipple(),
                                                            onClick = {
                                                                triggerFeedback()
                                                                handleBackspace()
                                                            }
                                                        )
                                                        .testTag("backspace_key")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                                                        contentDescription = null,
                                                        tint = colors.keyTextColor,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
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
                                                        triggerFeedback()
                                                        handleKeyClick(displayText)
                                                        if (isShiftEnabled) {
                                                            isShiftEnabled = false
                                                        }
                                                    },
                                                    colors = colors,
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                        }

                                        if (paddingWeight > 0.01f) {
                                            Spacer(modifier = Modifier.weight(paddingWeight))
                                        }
                                    }
                                }

                                // Bottom Action Row
                                Row(
                                    modifier = Modifier.fillMaxWidth().weight(1f),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    // Symbol/ABC switcher
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .weight(1.5f)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(colors.keyBackground)
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = RippleConfigurationProvider.getRipple(),
                                                onClick = {
                                                    triggerFeedback()
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

                                    // Comma Key
                                    KeyButton(
                                        text = ",",
                                        onClick = {
                                            triggerFeedback()
                                            handleKeyClick(",")
                                        },
                                        colors = colors,
                                        modifier = Modifier.weight(1.0f)
                                    )

                                    // Emoji trigger
                                    IconButtonKey(
                                        icon = Icons.Default.SentimentSatisfied,
                                        onClick = {
                                            triggerFeedback()
                                            activeSubPanel = KeyboardSubPanel.Emoji
                                        },
                                        colors = colors,
                                        modifier = Modifier.weight(1.0f)
                                    )

                                    // Space Key
                                    val spaceWeight = 4.0f
                                    Box(
                                        modifier = Modifier
                                            .weight(spaceWeight)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(colors.keyBackground)
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = RippleConfigurationProvider.getRipple(),
                                                onClick = {
                                                    triggerFeedback()
                                                    handleSpace()
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

                                    // Period Key
                                    KeyButton(
                                        text = ".",
                                        onClick = {
                                            triggerFeedback()
                                            handleKeyClick(".")
                                        },
                                        colors = colors,
                                        modifier = Modifier.weight(1.0f)
                                    )

                                    // Action / Enter Key
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .weight(1.5f)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(22.dp))
                                            .background(colors.accentColor)
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = RippleConfigurationProvider.getRipple(),
                                                onClick = {
                                                    triggerFeedback()
                                                    handleAction()
                                                }
                                            )
                                            .testTag("enter_key")
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Send,
                                            contentDescription = "Send",
                                            tint = colors.headerBackground,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                KeyboardSubPanel.Emoji -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(8.dp)
                    ) {
                        val emojis = listOf(
                            "😀", "😃", "😄", "😁", "😆", "😅", "😂", "🤣", "😊", "😇",
                            "🙂", "🙃", "😉", "😌", "😍", "🥰", "😘", "😗", "😙", "😚",
                            "😋", "😛", "😝", "😜", "🤪", "🤨", "🧐", "🤓", "😎", "🤩",
                            "🥳", "😏", "😒", "😞", "😔", "😟", "😕", "🙁", "☹️", "😣",
                            "😖", "😫", "😩", "🥺", "😢", "😭", "😤", "😠", "😡", "🤬",
                            "🤯", "😳", "🥵", "🥶", "😱", "😨", "😰", "😥", "😓", "🤗"
                        )
                        androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                            columns = GridCells.Adaptive(40.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(emojis.size) { index ->
                                val emoji = emojis[index]
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clickable {
                                            triggerFeedback()
                                            onKeyClick(emoji)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = emoji, fontSize = 24.sp)
                                }
                            }
                        }
                    }
                }
                KeyboardSubPanel.Settings -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Settings", color = colors.keyTextColor, fontWeight = FontWeight.Bold)
                        androidx.compose.foundation.lazy.LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            item {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical=4.dp)) {
                                    Text("Typing Animation", color = colors.keyTextColor)
                                    androidx.compose.material3.Switch(
                                        checked = preferences.typingAnimation,
                                        onCheckedChange = { preferences.typingAnimation = it }
                                    )
                                }
                            }
                            item {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical=4.dp)) {
                                    Text("Haptic Feedback", color = colors.keyTextColor)
                                    androidx.compose.material3.Switch(
                                        checked = preferences.isHapticEnabled,
                                        onCheckedChange = { preferences.isHapticEnabled = it }
                                    )
                                }
                            }
                            item {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical=4.dp)) {
                                    Text("Typing Sound", color = colors.keyTextColor)
                                    androidx.compose.material3.Switch(
                                        checked = preferences.isSoundEnabled,
                                        onCheckedChange = { preferences.isSoundEnabled = it }
                                    )
                                }
                            }
                        }
                    }
                }
                KeyboardSubPanel.Clipboard -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(8.dp)
                    ) {
                        Text("Clipboard", color = colors.keyTextColor, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                        if (clipboardList.isEmpty()) {
                            Text("No items in clipboard.", color = colors.keyTextColor.copy(alpha=0.5f))
                        } else {
                            androidx.compose.foundation.lazy.LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(clipboardList.size) { index ->
                                    val item = clipboardList[index]
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(colors.keyBackground)
                                            .clickable {
                                                triggerFeedback()
                                                onKeyClick(item.text)
                                            }
                                            .padding(12.dp)
                                    ) {
                                        Text(item.text, color = colors.keyTextColor, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                    }
                                }
                            }
                        }
                    }
                }
                KeyboardSubPanel.Apps -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("Apps Panel", color = colors.keyTextColor, fontWeight = FontWeight.Bold)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable{ activeSubPanel = KeyboardSubPanel.Translate }.padding(8.dp)) {
                                Icon(Icons.Default.Translate, null, tint = colors.accentColor, modifier = Modifier.size(32.dp))
                                Text("Translate", color = colors.keyTextColor, fontSize = 12.sp, modifier = Modifier.padding(top=4.dp))
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable{ activeSubPanel = KeyboardSubPanel.Gif }.padding(8.dp)) {
                                Icon(Icons.Default.Image, null, tint = colors.accentColor, modifier = Modifier.size(32.dp))
                                Text("GIFs", color = colors.keyTextColor, fontSize = 12.sp, modifier = Modifier.padding(top=4.dp))
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable{ activeSubPanel = KeyboardSubPanel.Emoji }.padding(8.dp)) {
                                Icon(Icons.Default.SentimentSatisfied, null, tint = colors.accentColor, modifier = Modifier.size(32.dp))
                                Text("Emoji", color = colors.keyTextColor, fontSize = 12.sp, modifier = Modifier.padding(top=4.dp))
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable{ activeSubPanel = KeyboardSubPanel.Settings }.padding(8.dp)) {
                                Icon(Icons.Default.Settings, null, tint = colors.accentColor, modifier = Modifier.size(32.dp))
                                Text("Settings", color = colors.keyTextColor, fontSize = 12.sp, modifier = Modifier.padding(top=4.dp))
                            }
                        }
                    }
                }
                KeyboardSubPanel.Translate -> {
                    var sourceText by remember { mutableStateOf("") }
                    var selectedLang by remember { mutableStateOf("Spanish") }
                    val languages = listOf("Spanish", "French", "German", "Italian", "Japanese")
                    
                    Column(
                        modifier = Modifier.fillMaxSize().padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Google Translate", color = colors.keyTextColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                items(languages.size) { index ->
                                    val lang = languages[index]
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (selectedLang == lang) colors.accentColor else colors.keyBackground)
                                            .clickable { selectedLang = lang }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(lang, color = if (selectedLang == lang) colors.headerBackground else colors.keyTextColor, fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                        
                        Row(modifier = Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.foundation.text.BasicTextField(
                                value = sourceText,
                                onValueChange = { sourceText = it },
                                modifier = Modifier.weight(1f).fillMaxHeight().background(colors.keyBackground, RoundedCornerShape(8.dp)).padding(8.dp),
                                textStyle = androidx.compose.ui.text.TextStyle(color = colors.keyTextColor, fontSize = 16.sp),
                                decorationBox = { innerTextField ->
                                    if (sourceText.isEmpty()) Text("Type to translate...", color = colors.keyTextColor.copy(alpha=0.5f))
                                    innerTextField()
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            KeyButton(
                                text = "Send",
                                onClick = {
                                    triggerFeedback()
                                    val translated = "[$selectedLang] " + sourceText.split(" ").reversed().joinToString(" ")
                                    onKeyClick(translated + " ")
                                    sourceText = ""
                                },
                                colors = colors,
                                modifier = Modifier.width(70.dp).fillMaxHeight()
                            )
                        }
                    }
                }
                KeyboardSubPanel.Gif -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(8.dp)
                    ) {
                        val gifs = listOf(
                            "https://media.tenor.com/2RoZ9Qd347cAAAAM/cat-nodding.gif",
                            "https://media.tenor.com/images/4688b17173e6da8f0290547071e72e36/tenor.gif",
                            "https://media.tenor.com/images/a5fc8a9ea3fcdbbef05f6354fcecc23d/tenor.gif",
                            "https://media.tenor.com/images/7a730026eef57e3f608b471ba95e0c8b/tenor.gif"
                        )
                        androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                            columns = GridCells.Adaptive(80.dp),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(4.dp)
                        ) {
                            items(gifs.size) { index ->
                                val gifUrl = gifs[index]
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .padding(2.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(colors.keyBackground)
                                        .clickable {
                                            triggerFeedback()
                                            onKeyClick(gifUrl + " ")
                                        }
                                ) {
                                    coil.compose.AsyncImage(
                                        model = coil.request.ImageRequest.Builder(LocalContext.current)
                                            .data(gifUrl)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "GIF",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                    )
                                }
                            }
                        }
                    }
                }
            } // end of when
        } // end of Row
    } // end of Column
} // end of CompositionLocalProvider
} // end of KeyboardLayout

// Simple Ripple provider that works without experimental APIs
object RippleConfigurationProvider {
    @Composable
    fun getRipple() = androidx.compose.material3.ripple()
}

val LocalTypingAnimation = androidx.compose.runtime.compositionLocalOf { true }

enum class KeyboardSubPanel {
    None,
    Emoji,
    Clipboard,
    Settings,
    Apps,
    Translate,
    Gif
}

@Composable
fun KeyButton(
    text: String,
    onClick: () -> Unit,
    colors: KeyboardThemeColors,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val typingAnim = LocalTypingAnimation.current

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(6.dp))
            .background(if (isPressed) colors.keyBackground.copy(alpha=0.7f) else colors.keyBackground)
            .clickable(
                interactionSource = interactionSource,
                indication = RippleConfigurationProvider.getRipple(),
                onClick = onClick
            )
    ) {
        Text(
            text = text,
            fontSize = 18.sp,
            color = colors.keyTextColor,
            fontWeight = FontWeight.Normal
        )
    }

    if (isPressed && typingAnim) {
        androidx.compose.ui.window.Popup(
            alignment = Alignment.TopCenter,
            offset = androidx.compose.ui.unit.IntOffset(0, -180)
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp, 75.dp)
                    .background(colors.keyBackground, RoundedCornerShape(12.dp))
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text, fontSize = 34.sp, color = colors.keyTextColor)
            }
        }
    }
}

@Composable
fun IconButtonKey(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    colors: KeyboardThemeColors,
    modifier: Modifier = Modifier,
    isAccent: Boolean = false
) {
    val bg = if (isAccent) colors.accentColor else colors.keyBackground
    val tint = if (isAccent) colors.headerBackground else colors.keyTextColor
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = RippleConfigurationProvider.getRipple(),
                onClick = onClick
            )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
    }
}
