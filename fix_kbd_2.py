import re

with open("app/src/main/java/com/example/ui/keyboard/KeyboardLayout.kt", "r") as f:
    content = f.read()

# Remove old backspace logic
content = re.sub(r'                                    // Backspace Key.*?\s*// Action / Enter Key', '                                    // Action / Enter Key', content, flags=re.DOTALL)

# Insert backspace logic before allKeyRows.forEach
insert_logic = """
                                val backspaceInteractionSource = remember { MutableInteractionSource() }
                                val isBackspacePressed by backspaceInteractionSource.collectIsPressedAsState()
                                LaunchedEffect(isBackspacePressed) {
                                    if (isBackspacePressed) {
                                        delay(400) // Initial delay before repeat
                                        while (isBackspacePressed) {
                                            triggerHaptic()
                                            onBackspace()
                                            delay((100L / deletingSpeed).toLong().coerceIn(20L, 1000L))
                                        }
                                    }
                                }

                                // Number Row + Active Keys (Letters or Symbols) Rows
                                allKeyRows.forEach { rowKeys ->"""

content = content.replace("                                // Number Row + Active Keys (Letters or Symbols) Rows\n                                allKeyRows.forEach { rowKeys ->", insert_logic)

with open("app/src/main/java/com/example/ui/keyboard/KeyboardLayout.kt", "w") as f:
    f.write(content)
