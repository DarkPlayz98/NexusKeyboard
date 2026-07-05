import re

with open("app/src/main/java/com/example/ui/keyboard/KeyboardLayout.kt", "r") as f:
    content = f.read()

def repl(m):
    c = m.group(0)
    c = c.replace("onKeyClick(", "handleKeyClick(")
    c = c.replace("onBackspace()", "handleBackspace()")
    c = c.replace("onSpace()", "handleSpace()")
    c = c.replace("onAction()", "handleAction()")
    return c

content = re.sub(r'CompositionLocalProvider.*', repl, content, flags=re.DOTALL)

with open("app/src/main/java/com/example/ui/keyboard/KeyboardLayout.kt", "w") as f:
    f.write(content)
