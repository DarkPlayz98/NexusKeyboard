import re

with open("app/src/main/java/com/example/ui/keyboard/KeyboardLayout.kt", "r") as f:
    content = f.read()

replacement = """                                            } else if (key == "SYMSHIFT") {
                                                IconButtonKey(
                                                    icon = Icons.Default.SwitchLeft, // or whatever
                                                    onClick = {
                                                        triggerHaptic()
                                                        // toggle symbol pages if we had one
                                                    },
                                                    colors = colors,
                                                    modifier = Modifier.weight(1f)
                                                )
                                            } else if (key == "BACKSPACE") {
                                                Box(
                                                    contentAlignment = Alignment.Center,
                                                    modifier = Modifier
                                                        .weight(1f) // Maybe slightly wider? Actually 1f is fine in a row
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
                                            } else {"""

content = content.replace('                                            } else {', replacement)

with open("app/src/main/java/com/example/ui/keyboard/KeyboardLayout.kt", "w") as f:
    f.write(content)
