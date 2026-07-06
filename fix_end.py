with open("app/src/main/java/com/example/ui/keyboard/KeyboardLayout.kt", "r") as f:
    text = f.read()

# Add Grammar to KeyboardSubPanel enum if not there
if "Grammar" not in text.split("enum class KeyboardSubPanel {")[1].split("}")[0]:
    text = text.replace("enum class KeyboardSubPanel {", "enum class KeyboardSubPanel {\n    Grammar,")

with open("app/src/main/java/com/example/ui/keyboard/KeyboardLayout.kt", "w") as f:
    f.write(text)
