import re

with open("app/src/main/java/com/example/ui/keyboard/KeyboardLayout.kt", "r") as f:
    content = f.read()

old_popup = """            Box(
                modifier = Modifier
                    .size(60.dp, 75.dp)
                    .background(colors.keyBackground, RoundedCornerShape(12.dp))
                    .padding(4.dp),"""

new_popup = """            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(colors.accentColor, CircleShape)
                    .padding(4.dp),"""

content = content.replace(old_popup, new_popup)

# Also change the text color to contrast with the accent color
content = content.replace("Text(text, fontSize = 34.sp, color = colors.keyTextColor)", "Text(text, fontSize = 34.sp, color = colors.headerBackground)")

with open("app/src/main/java/com/example/ui/keyboard/KeyboardLayout.kt", "w") as f:
    f.write(content)

