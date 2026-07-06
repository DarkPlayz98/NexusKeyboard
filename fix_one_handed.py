import re

with open("app/src/main/java/com/example/ui/keyboard/KeyboardLayout.kt", "r") as f:
    content = f.read()

one_handed_pattern = r"""                KeyboardSubPanel\.None -> \{[\s\S]+?                    Row\([\s\S]+?                        modifier = Modifier\.fillMaxSize\(\)[\s\S]+?                    \) \{"""

one_handed_replacement = """                KeyboardSubPanel.None -> {
                    val paddingStart = if (oneHandedMode == "Right") 40.dp else 0.dp
                    val paddingEnd = if (oneHandedMode == "Left") 40.dp else 0.dp
                    Row(
                        modifier = Modifier.fillMaxSize().padding(start = paddingStart, end = paddingEnd)
                    ) {"""

content = re.sub(one_handed_pattern, one_handed_replacement, content)

with open("app/src/main/java/com/example/ui/keyboard/KeyboardLayout.kt", "w") as f:
    f.write(content)

