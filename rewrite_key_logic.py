import re

with open("app/src/main/java/com/example/ui/keyboard/KeyboardLayout.kt", "r") as f:
    content = f.read()

# We need to find the `allKeyRows.forEach { rowKeys ->` and its closing brace.
start_str = "                                allKeyRows.forEach { rowKeys ->"
end_str = "                                // Bottom Action Row"

start_idx = content.find(start_str)
end_idx = content.find(end_str)

logic = """                                allKeyRows.forEach { rowKeys ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        rowKeys.forEach { key ->
                                            if (key == "SHIFT") {
                                                IconButtonKey(
                                                    icon = if (isShiftEnabled) Icons.Default.KeyboardCapslock else Icons.Default.ArrowUpward,
                                                    onClick = {
                                                        triggerHaptic()
                                                        isShiftEnabled = !isShiftEnabled
                                                    },
                                                    colors = colors,
                                                    isAccent = isShiftEnabled,
                                                    modifier = Modifier.weight(1.3f).testTag("shift_key")
                                                )
                                            } else if (key == "SYMSHIFT") {
                                                KeyButton(
                                                    text = "=\\<",
                                                    onClick = {
                                                        triggerHaptic()
                                                    },
                                                    colors = colors,
                                                    modifier = Modifier.weight(1.3f)
                                                )
                                            } else if (key == "BACKSPACE") {
                                                Box(
                                                    contentAlignment = Alignment.Center,
                                                    modifier = Modifier
                                                        .weight(1.3f)
                                                        .height(44.dp)
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(colors.keyBackground)
                                                        .clickable(
                                                            interactionSource = backspaceInteractionSource,
                                                            indication = if (typingAnimation) RippleConfigurationProvider.getRipple() else null,
                                                            onClick = {
                                                                triggerHaptic()
                                                                onBackspace()
                                                            }
                                                        )
                                                        .testTag("backspace_key")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                                                        contentDescription = null,
                                                        tint = colors.keyTextColor,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            } else {
                                                val isLetter = key.length == 1 && key[0].isLetter()
                                                val displayText = if (isLetter) {
                                                    if (isShiftEnabled) key.uppercase() else key.lowercase()
                                                } else {
                                                    key
                                                }
                                                KeyButton(
                                                    text = displayText,
                                                    onClick = {
                                                        triggerHaptic()
                                                        onKeyClick(displayText)
                                                        if (isShiftEnabled) {
                                                            isShiftEnabled = false
                                                        }
                                                    },
                                                    colors = colors,
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                        }
                                    }
                                }
"""

new_content = content[:start_idx] + logic + content[end_idx:]

with open("app/src/main/java/com/example/ui/keyboard/KeyboardLayout.kt", "w") as f:
    f.write(new_content)
