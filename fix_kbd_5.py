import re

with open("app/src/main/java/com/example/ui/keyboard/KeyboardLayout.kt", "r") as f:
    content = f.read()

# Replace the static suggestions with state-based ones
content = content.replace(
"""            val suggestions = listOf("the", "and", "I")""",
"""            var currentWord by remember(refreshTrigger) { mutableStateOf("") }
            val suggestions = remember(currentWord) { 
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
            }""")

content = content.replace(
"""                            onClick = { 
                                triggerHaptic()
                                onKeyClick("$word ") 
                            }""",
"""                            onClick = { 
                                triggerHaptic()
                                // Backspace the current word
                                for (i in currentWord.indices) {
                                    onBackspace()
                                }
                                onKeyClick("$word ") 
                                currentWord = ""
                            }""")

# Now we need to update currentWord on key click
# In KeyboardLayout, onKeyClick is called in several places, but the parameter is passed down.
# Let's wrap onKeyClick inside KeyboardLayout
wrapper_code = """    // State for predictive text
    var currentWord by remember(refreshTrigger) { mutableStateOf("") }
    
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
    }"""

content = content.replace(
"""    // Read states
    var selectedTheme by remember(refreshTrigger) { mutableStateOf(preferences.selectedTheme) }""",
wrapper_code + "\n\n    // Read states\n    var selectedTheme by remember(refreshTrigger) { mutableStateOf(preferences.selectedTheme) }"
)

# Also update the suggestions list logic because currentWord is now defined at the top
content = content.replace(
"""            var currentWord by remember(refreshTrigger) { mutableStateOf("") }
            val suggestions = remember(currentWord) {""",
"""            val suggestions = remember(currentWord) {"""
)

# Finally replace all calls to onKeyClick, onBackspace, etc. with handle... inside KeyboardLayout?
# No, actually wait. Replacing onKeyClick with handleKeyClick requires replacing every occurrence.
# Let's just do a string replace for onKeyClick, onBackspace, onSpace, onAction inside the component.
# Actually we can just redefine them as shadow variables if we declare them early enough! No, that's not Kotlin.
