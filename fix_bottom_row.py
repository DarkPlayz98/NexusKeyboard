import re

with open("app/src/main/java/com/example/ui/keyboard/KeyboardLayout.kt", "r") as f:
    content = f.read()

start_str = "                                // Bottom Action Row"
end_str = "                            // Render smooth glowing swipe trail"

start_idx = content.find(start_str)
end_idx = content.find(end_str)

logic = """                                // Bottom Action Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    // Symbol/ABC switcher
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .weight(1.3f)
                                            .height(44.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(colors.keyBackground)
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = RippleConfigurationProvider.getRipple(),
                                                onClick = {
                                                    triggerHaptic()
                                                    isSymbolMode = !isSymbolMode
                                                }
                                            )
                                            .testTag("toggle_symbols_key"),
                                    ) {
                                        Text(
                                            text = if (isSymbolMode) "ABC" else "?123",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.keyTextColor
                                        )
                                    }

                                    // Comma Key
                                    KeyButton(
                                        text = ",",
                                        onClick = {
                                            triggerHaptic()
                                            onKeyClick(",")
                                        },
                                        colors = colors,
                                        modifier = Modifier.weight(1f)
                                    )

                                    // Emoji trigger
                                    IconButtonKey(
                                        icon = Icons.Default.SentimentSatisfied,
                                        onClick = {
                                            triggerHaptic()
                                            activeSubPanel = KeyboardSubPanel.Emoji
                                        },
                                        colors = colors,
                                        modifier = Modifier.weight(1f)
                                    )

                                    // Space Key
                                    Box(
                                        modifier = Modifier
                                            .weight(4.5f) // Make space wider
                                            .height(44.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(colors.keyBackground)
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = RippleConfigurationProvider.getRipple(),
                                                onClick = {
                                                    triggerHaptic()
                                                    onSpace()
                                                }
                                            )
                                            .testTag("space_key"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = selectedLanguage,
                                            fontSize = 12.sp,
                                            letterSpacing = 1.sp,
                                            color = colors.keyTextColor.copy(alpha = 0.5f),
                                            fontWeight = FontWeight.Light
                                        )
                                    }

                                    // Period Key
                                    KeyButton(
                                        text = ".",
                                        onClick = {
                                            triggerHaptic()
                                            onKeyClick(".")
                                        },
                                        colors = colors,
                                        modifier = Modifier.weight(1f)
                                    )

                                    // Action / Enter Key
                                    IconButtonKey(
                                        icon = Icons.Default.SubdirectoryArrowLeft,
                                        onClick = {
                                            triggerHaptic()
                                            onAction()
                                        },
                                        colors = colors,
                                        isAccent = true,
                                        modifier = Modifier
                                            .weight(1.3f)
                                            .testTag("enter_key")
                                    )
                                }
                            }
"""

new_content = content[:start_idx] + logic + content[end_idx:]

with open("app/src/main/java/com/example/ui/keyboard/KeyboardLayout.kt", "w") as f:
    f.write(new_content)
