import re

with open("app/src/main/java/com/example/ui/keyboard/KeyboardLayout.kt", "r") as f:
    content = f.read()

# 1. Insert handleKeyClick etc
insertion = """    var currentWord by remember(refreshTrigger) { mutableStateOf("") }
    
    val handleKeyClick: (String) -> Unit = { text ->
        if (text == " " || text == "\\n" || text == "." || text == ",") {
            currentWord = ""
        } else if (text.length == 1 && text[0].isLetter()) {
            currentWord += text
        }
        onKeyClick(text)
    }
    
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
"""

content = content.replace("    // Read states\n", insertion)

# 2. Fix the suggestions strip
sug_old = """            val suggestions = listOf("the", "and", "I")
            suggestions.forEach { word ->"""

sug_new = """            val suggestions = remember(currentWord) { 
                if (currentWord.isEmpty()) {
                    listOf("I", "the", "and")
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
            suggestions.forEach { word ->"""

content = content.replace(sug_old, sug_new)

# 3. Update the suggestion click logic
click_old = """                            onClick = { 
                                triggerHaptic()
                                handleKeyClick("$word ") 
                            }"""

click_new = """                            onClick = { 
                                triggerHaptic()
                                for (i in currentWord.indices) {
                                    handleBackspace()
                                }
                                handleKeyClick("$word ") 
                                currentWord = ""
                            }"""

content = content.replace(click_old, click_new)

# 4. Fix unsupported escape sequence "=\\<" -> "=\\<" inside KeyButton
content = content.replace('text = "=\\<"', 'text = "=\\\\<"')

# 5. Fix the Settings menu issue
# Replace the old Settings button logic
settings_old = """                // Settings Toggle
                if (isSystemKeyboard) {
                    IconButton(
                        onClick = {
                            triggerHaptic()
                            val intent = android.content.Intent(context, com.example.MainActivity::class.java).apply {
                                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        },
                        modifier = Modifier.testTag("open_settings")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = colors.headerIconColor
                        )
                    }
                }"""

settings_new = """                // Settings Toggle
                if (isSystemKeyboard) {
                    IconButton(
                        onClick = {
                            triggerHaptic()
                            activeSubPanel = if (activeSubPanel == KeyboardSubPanel.Settings) KeyboardSubPanel.None else KeyboardSubPanel.Settings
                        },
                        modifier = Modifier.testTag("open_settings")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = if (activeSubPanel == KeyboardSubPanel.Settings) colors.accentColor else colors.headerIconColor
                        )
                    }
                }"""

content = content.replace(settings_old, settings_new)

with open("app/src/main/java/com/example/ui/keyboard/KeyboardLayout.kt", "w") as f:
    f.write(content)

