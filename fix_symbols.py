import re

with open("app/src/main/java/com/example/ui/keyboard/KeyboardLayout.kt", "r") as f:
    content = f.read()

symbols_code = """    val symbolRows = remember {
        listOf(
            listOf("@", "#", "$", "_", "&", "-", "+", "(", ")", "/"),
            listOf("*", "\\"", "'", ":", ";", "!", "?", "~", "`", "|")
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
    }"""

content = re.sub(
r"    val symbolRows = remember \{[\s\S]+?    val allKeyRows = remember\(activeKeysRows, isSymbolMode\) \{[\s\S]+?    \}",
symbols_code, content)

with open("app/src/main/java/com/example/ui/keyboard/KeyboardLayout.kt", "w") as f:
    f.write(content)
