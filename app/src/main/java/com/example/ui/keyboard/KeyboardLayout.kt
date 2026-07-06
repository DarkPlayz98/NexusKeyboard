package com.example.ui.keyboard
import androidx.compose.foundation.shape.CircleShape

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
import kotlinx.coroutines.isActive
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
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import android.os.Build

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

fun translateText(
    text: String,
    targetLang: String,
    onResult: (String) -> Unit
) {
    val langMap = mapOf(
        "Spanish" to "es",
        "French" to "fr",
        "German" to "de",
        "Italian" to "it",
        "Japanese" to "ja"
    )
    val langCode = langMap[targetLang] ?: "es"
    
    kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val urlStr = "https://api.mymemory.translated.net/get?q=" + 
                java.net.URLEncoder.encode(text, "UTF-8") + 
                "&langpair=en|" + langCode
            val url = java.net.URL(urlStr)
            val conn = url.openConnection() as java.net.HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            
            val response = conn.inputStream.bufferedReader().readText()
            
            val prefix = "\"translatedText\":\""
            val startIndex = response.indexOf(prefix)
            if (startIndex != -1) {
                val sub = response.substring(startIndex + prefix.length)
                val endIndex = sub.indexOf("\"")
                if (endIndex != -1) {
                    val result = sub.substring(0, endIndex)
                    val unescaped = unescapeUnicode(result)
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        onResult(unescaped)
                    }
                    return@launch
                }
            }
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                onResult(text)
            }
        } catch (e: Exception) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                onResult("Error: " + e.message)
            }
        }
    }
}

fun unescapeUnicode(str: String): String {
    val regex = "\\\\u([0-9a-fA-F]{4})".toRegex()
    return regex.replace(str) { matchResult ->
        val hex = matchResult.groupValues[1]
        hex.toInt(16).toChar().toString()
    }
}

@Composable
fun KeyboardLayout(
    isSystemKeyboard: Boolean = false,
    onKeyClick: (String) -> Unit,
    onBackspace: () -> Unit,
    onAction: () -> Unit,
    onSpace: () -> Unit,
    modifier: Modifier = Modifier,
    getTextBeforeCursor: () -> String = { "" },
    onReplaceText: (oldText: String, newText: String) -> Unit = { _, _ -> },
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
    var isSoundEnabled by remember(localRefreshTrigger) { mutableStateOf(preferences.isSoundEnabled) }

    var grammarSuggestion by remember { mutableStateOf<String?>(null) }
    var grammarOldWord by remember { mutableStateOf<String?>(null) }
    var grammarNewWord by remember { mutableStateOf<String?>(null) }

    val textBefore = getTextBeforeCursor()

    // Debounced check for grammar correction in predictive engine
    LaunchedEffect(textBefore) {
        if (textBefore.isBlank()) {
            grammarSuggestion = null
            grammarOldWord = null
            grammarNewWord = null
            return@LaunchedEffect
        }
        val lastChar = textBefore.last()
        if (lastChar == ' ' || lastChar == '.' || lastChar == ',' || lastChar == '!') {
            delay(500) // debounce
            val textToCheck = textBefore.trim()
            val words = textToCheck.split("\\s+".toRegex())
            if (words.size >= 2) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        val url = java.net.URL("https://api.languagetool.org/v2/check")
                        val conn = url.openConnection() as java.net.HttpURLConnection
                        conn.requestMethod = "POST"
                        conn.doOutput = true
                        val data = "language=en-US&text=" + java.net.URLEncoder.encode(textToCheck, "UTF-8")
                        conn.outputStream.write(data.toByteArray())
                        val response = conn.inputStream.bufferedReader().readText()
                        
                        // Parse replacements
                        val replacementRegex = """"replacements":\[\{"value":"([^"]+)"""".toRegex()
                        val foundReplacements = replacementRegex.findAll(response).toList()
                        if (foundReplacements.isNotEmpty()) {
                            val offsetRegex = """"offset":([0-9]+)""".toRegex()
                            val lengthRegex = """"length":([0-9]+)""".toRegex()
                            val offsetMatch = offsetRegex.find(response)
                            val lengthMatch = lengthRegex.find(response)
                            
                            if (offsetMatch != null && lengthMatch != null) {
                                val offset = offsetMatch.groupValues[1].toInt()
                                val length = lengthMatch.groupValues[1].toInt()
                                val errorWord = textToCheck.substring(offset, offset + length)
                                val correction = foundReplacements[0].groups[1]?.value ?: ""
                                
                                if (errorWord.lowercase() != correction.lowercase() && errorWord.isNotBlank() && correction.isNotBlank()) {
                                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                        grammarSuggestion = "Correct '$errorWord' to '$correction'?"
                                        grammarOldWord = errorWord
                                        grammarNewWord = correction
                                    }
                                }
                            }
                        } else {
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                grammarSuggestion = null
                            }
                        }
                    } catch (e: Exception) {
                        // fail silently for predictive keyboard checks
                    }
                }
            }
        }
    }

    val gesturePoints = remember { mutableStateListOf<Offset>() }
    var containerWidth by remember { mutableStateOf(0) }
    var containerHeight by remember { mutableStateOf(0) }

    // Navigation sub-panels inside keyboard
    var activeSubPanel by remember { mutableStateOf<KeyboardSubPanel>(KeyboardSubPanel.None) }
    var showClipboardOverlay by remember { mutableStateOf(false) }
    var isSymbolMode by remember { mutableStateOf(false) }
    var symbolPage by remember { mutableStateOf(1) }
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
        if (isSoundEnabled) {
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

    val symbolRowsPage1 = remember {
        listOf(
            listOf("@", "#", "$", "_", "&", "-", "+", "(", ")", "/"),
            listOf("=", "*", "\"", "'", ":", ";", "!", "?", "%"),
            listOf("~", "`", "|", "<", ">", "{", "}")
        )
    }

    val symbolRowsPage2 = remember {
        listOf(
            listOf("[", "]", "{", "}", "#", "%", "^", "*", "+", "="),
            listOf("_", "\\", "|", "~", "<", ">", "€", "£", "¥"),
            listOf("•", "¶", "§", "¡", "¿", "«", "»")
        )
    }

    val activeKeysRows = remember(isSymbolMode, symbolPage, rows) {
        if (isSymbolMode) {
            if (symbolPage == 1) symbolRowsPage1 else symbolRowsPage2
        } else rows
    }

    val allKeyRows = remember(activeKeysRows, isSymbolMode, numberRow) {
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
                .height(278.dp)
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

            if (grammarSuggestion != null && grammarOldWord != null && grammarNewWord != null) {
                // Display grammar suggestion!
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable {
                            triggerFeedback()
                            onReplaceText(grammarOldWord!!, grammarNewWord!!)
                            grammarSuggestion = null
                            grammarOldWord = null
                            grammarNewWord = null
                        }
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Spellcheck,
                        contentDescription = "Grammar Correction",
                        tint = colors.accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = grammarSuggestion!!,
                        color = colors.accentColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Apply",
                        tint = colors.accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            } else if (currentWord.isEmpty()) {
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
                    val paddingStart = if (oneHandedMode == "Right") 40.dp else 0.dp
                    val paddingEnd = if (oneHandedMode == "Left") 40.dp else 0.dp
                    Row(
                        modifier = Modifier.fillMaxSize().padding(start = paddingStart, end = paddingEnd)
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

                                            if (isDragging && dragPath.isNotEmpty()) {
                                                val swipeChars = mutableListOf<String>()
                                                for (p in dragPath) {
                                                    val c = getCharForPos(p.x, p.y, containerWidth, containerHeight, allKeyRows)
                                                    if (c != null && (swipeChars.isEmpty() || swipeChars.last() != c)) {
                                                        swipeChars.add(c)
                                                    }
                                                }
                                                val pattern = swipeChars.joinToString("")
                                                if (pattern.length >= 2) {
                                                    val first = pattern.first()
                                                    val last = pattern.last()
                                                    val regexStr = pattern.toCharArray().joinToString(".*")
                                                    val regex = Regex(regexStr, RegexOption.IGNORE_CASE)
                                                    
                                                    val candidates = COMMON_WORDS.filter { 
                                                        it.length >= pattern.length && 
                                                        it.first().lowercaseChar() == first && 
                                                        it.last().lowercaseChar() == last 
                                                    }
                                                    
                                                    val exactMatch = candidates.firstOrNull { it.matches(regex) }
                                                    val resolved = exactMatch ?: pattern
                                                    handleKeyClick("$resolved ")
                                                } else if (pattern.isNotEmpty()) {
                                                    handleKeyClick("$pattern ")
                                                }
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
                                                val symText = if (symbolPage == 1) "=\\<" else "1/2"
                                                KeyButton(
                                                    text = symText,
                                                    onClick = {
                                                        triggerFeedback()
                                                        symbolPage = if (symbolPage == 1) 2 else 1
                                                    },
                                                    colors = colors,
                                                    modifier = Modifier.weight(1.5f)
                                                )
                                            } else if (key == "BACKSPACE") {
                                                val coroutineScope = rememberCoroutineScope()
                                                Box(
                                                    contentAlignment = Alignment.Center,
                                                    modifier = Modifier
                                                        .weight(1.5f)
                                                        .fillMaxHeight()
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(colors.keyBackground)
                                                        .pointerInput(preferences.deletingSpeed) {
                                                            awaitPointerEventScope {
                                                                while (true) {
                                                                    val down = awaitFirstDown()
                                                                    triggerFeedback()
                                                                    handleBackspace()
                                                                    val job = coroutineScope.launch {
                                                                        delay(400)
                                                                        var currentDelay = 100L
                                                                        val minDelay = (20f / preferences.deletingSpeed.coerceAtLeast(0.1f)).toLong().coerceIn(10L, 200L)
                                                                        while (isActive) {
                                                                            triggerFeedback()
                                                                            handleBackspace()
                                                                            delay(currentDelay)
                                                                            currentDelay = (currentDelay * 0.85).toLong().coerceAtLeast(minDelay)
                                                                        }
                                                                    }
                                                                    waitForUpOrCancellation()
                                                                    job.cancel()
                                                                }
                                                            }
                                                        }
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
                                                    symbolPage = 1
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
                                        checked = typingAnimation,
                                        onCheckedChange = {
                                            typingAnimation = it
                                            preferences.typingAnimation = it
                                        }
                                    )
                                }
                            }
                            item {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical=4.dp)) {
                                    Text("Haptic Feedback", color = colors.keyTextColor)
                                    androidx.compose.material3.Switch(
                                        checked = isHapticEnabled,
                                        onCheckedChange = {
                                            isHapticEnabled = it
                                            preferences.isHapticEnabled = it
                                        }
                                    )
                                }
                            }
                            item {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical=4.dp)) {
                                    Text("Typing Sound", color = colors.keyTextColor)
                                    androidx.compose.material3.Switch(
                                        checked = isSoundEnabled,
                                        onCheckedChange = {
                                            isSoundEnabled = it
                                            preferences.isSoundEnabled = it
                                        }
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
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable{ activeSubPanel = KeyboardSubPanel.Grammar }.padding(8.dp)) {
                                Icon(Icons.Default.Spellcheck, null, tint = colors.accentColor, modifier = Modifier.size(32.dp))
                                Text("Grammar", color = colors.keyTextColor, fontSize = 12.sp, modifier = Modifier.padding(top=4.dp))
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
                KeyboardSubPanel.Grammar -> {
                    var sourceText by remember { mutableStateOf("") }
                    var isChecking by remember { mutableStateOf(false) }
                    var hasChecked by remember { mutableStateOf(false) }
                    val scope = rememberCoroutineScope()
                    
                    Column(
                        modifier = Modifier.fillMaxSize().padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("LanguageTool Grammar Check", color = colors.keyTextColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            if (isChecking) {
                                androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(16.dp), color = colors.accentColor, strokeWidth = 2.dp)
                            }
                        }
                        
                        Row(modifier = Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.foundation.text.BasicTextField(
                                value = sourceText,
                                onValueChange = { 
                                    sourceText = it
                                    hasChecked = false 
                                },
                                modifier = Modifier.weight(1f).fillMaxHeight().background(colors.keyBackground, RoundedCornerShape(8.dp)).padding(8.dp),
                                textStyle = androidx.compose.ui.text.TextStyle(color = colors.keyTextColor, fontSize = 16.sp),
                                decorationBox = { innerTextField ->
                                    if (sourceText.isEmpty()) Text("Type or paste text to check...", color = colors.keyTextColor.copy(alpha=0.5f))
                                    innerTextField()
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            if (!hasChecked) {
                                KeyButton(
                                    text = "Check",
                                    onClick = {
                                        triggerFeedback()
                                        if (sourceText.isNotBlank()) {
                                            isChecking = true
                                            scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                                try {
                                                    val url = java.net.URL("https://api.languagetool.org/v2/check")
                                                    val conn = url.openConnection() as java.net.HttpURLConnection
                                                    conn.requestMethod = "POST"
                                                    conn.doOutput = true
                                                    val data = "language=en-US&text=" + java.net.URLEncoder.encode(sourceText, "UTF-8")
                                                    conn.outputStream.write(data.toByteArray())
                                                    val response = conn.inputStream.bufferedReader().readText()
                                                    
                                                    // Simple regex parsing using raw strings
                                                    val matchesRegex = """"matches":\[(.*?)\]""".toRegex(RegexOption.DOT_MATCHES_ALL)
                                                    val matcher = matchesRegex.find(response)
                                                    if (matcher != null) {
                                                        val replacementRegex = """"replacements":\[\{"value":"([^"]+)"""".toRegex()
                                                        val foundReplacements = replacementRegex.findAll(response).toList()
                                                        if (foundReplacements.isNotEmpty()) {
                                                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                                                val newWord = foundReplacements[0].groups[1]?.value ?: ""
                                                                sourceText = "Suggestion: " + newWord
                                                            }
                                                        } else {
                                                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                                                sourceText = sourceText + " (No errors found)"
                                                            }
                                                        }
                                                    } else {
                                                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                                            sourceText = sourceText + " (No errors found)"
                                                        }
                                                    }
                                                } catch(e: Exception) {
                                                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                                        sourceText = "Error: " + e.message
                                                    }
                                                } finally {
                                                    isChecking = false
                                                    hasChecked = true
                                                }
                                            }
                                        }
                                    },
                                    colors = colors,
                                    modifier = Modifier.width(70.dp).fillMaxHeight()
                                )
                            } else {
                                KeyButton(
                                    text = "Send",
                                    onClick = {
                                        triggerFeedback()
                                        onKeyClick(sourceText + " ")
                                        sourceText = ""
                                        hasChecked = false
                                    },
                                    colors = colors,
                                    modifier = Modifier.width(70.dp).fillMaxHeight()
                                )
                            }
                        }
                    }
                }
                KeyboardSubPanel.Translate -> {
                    var sourceText by remember { mutableStateOf("") }
                    var translatedText by remember { mutableStateOf("") }
                    var isTranslating by remember { mutableStateOf(false) }
                    var selectedLang by remember { mutableStateOf("Spanish") }
                    val languages = listOf("Spanish", "French", "German", "Italian", "Japanese")
                    
                    Column(
                        modifier = Modifier.fillMaxSize().padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Real Translation", color = colors.keyTextColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                items(languages.size) { index ->
                                    val lang = languages[index]
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (selectedLang == lang) colors.accentColor else colors.keyBackground)
                                            .clickable { 
                                                selectedLang = lang 
                                                translatedText = ""
                                            }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(lang, color = if (selectedLang == lang) colors.headerBackground else colors.keyTextColor, fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                        
                        Row(modifier = Modifier.fillMaxWidth().weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                androidx.compose.foundation.text.BasicTextField(
                                    value = sourceText,
                                    onValueChange = { 
                                        sourceText = it
                                        translatedText = ""
                                    },
                                    modifier = Modifier.fillMaxWidth().weight(1f).background(colors.keyBackground, RoundedCornerShape(8.dp)).padding(8.dp),
                                    textStyle = androidx.compose.ui.text.TextStyle(color = colors.keyTextColor, fontSize = 14.sp),
                                    decorationBox = { innerTextField ->
                                        if (sourceText.isEmpty()) Text("Type English text...", color = colors.keyTextColor.copy(alpha=0.5f), fontSize = 14.sp)
                                        innerTextField()
                                    }
                                )
                                if (translatedText.isNotEmpty() || isTranslating) {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().weight(1f).background(colors.keyBackground.copy(alpha=0.5f), RoundedCornerShape(8.dp)).padding(8.dp)
                                    ) {
                                        if (isTranslating) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                CircularProgressIndicator(modifier = Modifier.size(12.dp), color = colors.accentColor, strokeWidth = 1.5.dp)
                                                Text("Translating...", color = colors.keyTextColor.copy(alpha=0.5f), fontSize = 12.sp)
                                            }
                                        } else {
                                            Text(translatedText, color = colors.accentColor, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.width(80.dp).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                KeyButton(
                                    text = if (translatedText.isEmpty()) "Translate" else "Send",
                                    onClick = {
                                        triggerFeedback()
                                        if (translatedText.isNotEmpty()) {
                                            onKeyClick(translatedText + " ")
                                            sourceText = ""
                                            translatedText = ""
                                        } else if (sourceText.isNotBlank()) {
                                            isTranslating = true
                                            translateText(sourceText, selectedLang) { result ->
                                                translatedText = result
                                                isTranslating = false
                                            }
                                        }
                                    },
                                    colors = colors,
                                    modifier = Modifier.fillMaxWidth().weight(1f)
                                )
                                if (translatedText.isNotEmpty()) {
                                    KeyButton(
                                        text = "Clear",
                                        onClick = {
                                            triggerFeedback()
                                            sourceText = ""
                                            translatedText = ""
                                        },
                                        colors = colors,
                                        modifier = Modifier.fillMaxWidth().weight(0.8f)
                                    )
                                }
                            }
                        }
                    }
                }
                KeyboardSubPanel.Gif -> {
                    val gifImageLoader = remember {
                        coil.ImageLoader.Builder(context)
                            .components {
                                if (android.os.Build.VERSION.SDK_INT >= 28) {
                                    add(coil.decode.ImageDecoderDecoder.Factory())
                                } else {
                                    add(coil.decode.GifDecoder.Factory())
                                }
                            }
                            .build()
                    }
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
                                        imageLoader = gifImageLoader,
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
    Gif,
    Grammar
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
        val density = androidx.compose.ui.platform.LocalDensity.current
        val yOffsetPx = with(density) { -58.dp.roundToPx() }
        androidx.compose.ui.window.Popup(
            alignment = Alignment.TopCenter,
            offset = androidx.compose.ui.unit.IntOffset(0, yOffsetPx)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(colors.accentColor, RoundedCornerShape(8.dp))
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = text,
                    fontSize = 28.sp,
                    color = colors.background,
                    fontWeight = FontWeight.Bold
                )
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

fun getCharForPos(x: Float, y: Float, width: Int, height: Int, allKeyRows: List<List<String>>): String? {
    if (width == 0 || height == 0) return null
    val totalRows = allKeyRows.size + 1
    val rowIdx = (y / (height.toFloat() / totalRows)).toInt().coerceIn(0, totalRows - 1)
    
    if (rowIdx < allKeyRows.size) {
        val row = allKeyRows[rowIdx]
        val maxWeight = allKeyRows.maxOfOrNull { r -> 
            r.sumOf { k -> if (k == "SHIFT" || k == "SYMSHIFT" || k == "BACKSPACE") 1.5 else 1.0 } 
        }?.toFloat() ?: 10f
        
        val rowWeight = row.sumOf { k -> if (k == "SHIFT" || k == "SYMSHIFT" || k == "BACKSPACE") 1.5 else 1.0 }.toFloat()
        val paddingWeight = (maxWeight - rowWeight) / 2f
        
        val unitWidth = width / maxWeight
        val startX = paddingWeight * unitWidth
        
        if (x < startX) return null
        var currentX = startX
        for (key in row) {
            val keyWeight = if (key == "SHIFT" || key == "SYMSHIFT" || key == "BACKSPACE") 1.5f else 1.0f
            val keyWidth = keyWeight * unitWidth
            if (x >= currentX && x < currentX + keyWidth) {
                return if (key.length == 1 && key[0].isLetter()) key.lowercase() else null
            }
            currentX += keyWidth
        }
    }
    return null
}
