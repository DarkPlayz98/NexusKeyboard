import re

with open("app/src/main/java/com/example/ui/keyboard/KeyboardLayout.kt", "r") as f:
    content = f.read()

# 1. triggerHaptic -> triggerFeedback
content = content.replace(
"""    // Trigger local haptic vibration
    val triggerHaptic = {
        if (isHapticEnabled) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }""",
"""    // Trigger local haptic vibration and sound
    val audioManager = context.getSystemService(android.content.Context.AUDIO_SERVICE) as? android.media.AudioManager
    val triggerFeedback = {
        if (isHapticEnabled) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
        if (preferences.isSoundEnabled) {
            audioManager?.playSoundEffect(android.media.AudioManager.FX_KEYPRESS_STANDARD, 1.0f)
        }
    }""")
content = content.replace("triggerHaptic()", "triggerFeedback()")

# 2. Modify KeyButton
key_button_replacement = """@Composable
fun KeyButton(
    text: String,
    onClick: () -> Unit,
    colors: KeyboardThemeColors,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val typingAnim = LocalTypingAnimation.current

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(6.dp))
            .background(if (isPressed) colors.keyBackground.copy(alpha=0.7f) else colors.keyBackground)
            .clickable(
                interactionSource = interactionSource,
                indication = RippleConfigurationProvider.getRipple(),
                onClick = onClick
            )
    ) {
        Text(
            text = text,
            fontSize = 18.sp,
            color = colors.keyTextColor,
            fontWeight = FontWeight.Normal
        )
    }

    if (isPressed && typingAnim) {
        androidx.compose.ui.window.Popup(
            alignment = Alignment.TopCenter,
            offset = androidx.compose.ui.unit.IntOffset(0, -180)
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp, 75.dp)
                    .background(colors.keyBackground, RoundedCornerShape(12.dp))
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text, fontSize = 34.sp, color = colors.keyTextColor)
            }
        }
    }
}"""
content = re.sub(
r"@Composable\nfun KeyButton\([\s\S]+?\}\n\n@Composable\nfun IconButtonKey",
key_button_replacement + "\n\n@Composable\nfun IconButtonKey", content)

with open("app/src/main/java/com/example/ui/keyboard/KeyboardLayout.kt", "w") as f:
    f.write(content)
