import re

with open("app/src/main/java/com/example/ui/keyboard/KeyboardLayout.kt", "r") as f:
    content = f.read()

content = content.replace('val spaceWeight = 4.5f', 'val spaceWeight = 4.0f')

with open("app/src/main/java/com/example/ui/keyboard/KeyboardLayout.kt", "w") as f:
    f.write(content)
