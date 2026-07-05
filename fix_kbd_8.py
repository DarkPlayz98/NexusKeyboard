import re

with open("app/src/main/java/com/example/ui/keyboard/KeyboardLayout.kt", "r") as f:
    content = f.read()

content = content.replace("enum class KeyboardSubPanel {\n    None,\n    Emoji,\n    Clipboard\n}", "enum class KeyboardSubPanel {\n    None,\n    Emoji,\n    Clipboard,\n    Settings\n}")

with open("app/src/main/java/com/example/ui/keyboard/KeyboardLayout.kt", "w") as f:
    f.write(content)
