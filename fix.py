with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

import re
# Remove duplicate 0 -> {
content = content.replace("                0 -> {\n                0 -> {\n", "                0 -> {\n")
# Remove empty 1 -> { ... } block and duplicate
content = re.sub(r'                1 -> \{\s*// TAB 1: CLIPBOARD MANAGER\s*\}\s*1 -> \{', '                1 -> {', content, flags=re.MULTILINE)

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)
