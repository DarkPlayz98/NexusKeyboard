with open("app/src/main/java/com/example/ui/keyboard/KeyboardLayout.kt", "r") as f:
    text = f.read()

if "import kotlinx.coroutines.isActive" not in text:
    text = text.replace("import kotlinx.coroutines.launch", "import kotlinx.coroutines.launch\nimport kotlinx.coroutines.isActive")

text = text.replace('while (kotlinx.coroutines.isActive)', 'while (isActive)')

with open("app/src/main/java/com/example/ui/keyboard/KeyboardLayout.kt", "w") as f:
    f.write(text)
