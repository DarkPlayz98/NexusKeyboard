import re

with open("app/src/main/java/com/example/ui/keyboard/KeyboardLayout.kt", "r") as f:
    content = f.read()

content = content.replace('=\\<', '=\\\\<')

content = content.replace('while (isActive)', 'while (kotlinx.coroutines.isActive)')

if "import androidx.compose.foundation.gestures.waitForUpOrCancellation" not in content:
    content = content.replace("import androidx.compose.foundation.gestures.awaitFirstDown", "import androidx.compose.foundation.gestures.awaitFirstDown\nimport androidx.compose.foundation.gestures.waitForUpOrCancellation")

with open("app/src/main/java/com/example/ui/keyboard/KeyboardLayout.kt", "w") as f:
    f.write(content)

