import re

with open("app/src/main/java/com/example/ui/keyboard/KeyboardLayout.kt", "r") as f:
    content = f.read()

# Modify symbolRows
content = content.replace(
"""    val symbolRows = remember {
        listOf(
            listOf("[", "]", "{", "}", "#", "%", "^", "*", "+", "="),
            listOf("_", "\\", "|", "~", "<", ">", "€", "£", "¥", "•"),
            listOf(".", ",", "?", "!", "'", "\"", "-", "/", ":", ";")
        )
    }""",
"""    val symbolRows = remember {
        listOf(
            listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
            listOf("@", "#", "$", "_", "&", "-", "+", "(", ")", "/"),
            listOf("*", "\"", "'", ":", ";", "!", "?")
        )
    }""")

# Modify allKeyRows
content = content.replace(
"""    val allKeyRows = remember(activeKeysRows, isSymbolMode) {
        val base = listOf(numberRow) + activeKeysRows
        if (!isSymbolMode && base.size > 3) {
            val list = base.toMutableList()
            list[3] = listOf("SHIFT") + list[3]
            list
        } else {
            base
        }
    }""",
"""    val allKeyRows = remember(activeKeysRows, isSymbolMode) {
        val base = if (isSymbolMode) activeKeysRows else (listOf(numberRow) + activeKeysRows)
        val list = base.toMutableList()
        if (list.size > 2) {
            val bottomLetterRowIdx = if (isSymbolMode) 2 else 3
            val bottomRow = list[bottomLetterRowIdx].toMutableList()
            bottomRow.add(0, if (isSymbolMode) "SYMSHIFT" else "SHIFT")
            bottomRow.add("BACKSPACE")
            list[bottomLetterRowIdx] = bottomRow
        }
        list
    }""")

with open("app/src/main/java/com/example/ui/keyboard/KeyboardLayout.kt", "w") as f:
    f.write(content)
