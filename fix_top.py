import re

with open("app/src/main/java/com/example/ui/keyboard/KeyboardLayout.kt", "r") as f:
    text = f.read()

# Fix the very first lines
text = text.replace(
    "package com.example.ui.keyboardimport androidx.compose.foundation.shape.CircleShapeimport android.content.Contextimport android.os.Vibratorimport android.view.HapticFeedbackConstants",
    "package com.example.ui.keyboard\n\nimport androidx.compose.foundation.shape.CircleShape\nimport android.content.Context\nimport android.os.Vibrator\nimport android.view.HapticFeedbackConstants\n"
)

with open("app/src/main/java/com/example/ui/keyboard/KeyboardLayout.kt", "w") as f:
    f.write(text)
