import re

with open("app/src/main/java/com/example/ui/keyboard/KeyboardLayout.kt", "r") as f:
    content = f.read()

replacement = """                                            } else if (key == "SYMSHIFT") {
                                                KeyButton(
                                                    text = "=\\<",
                                                    onClick = {
                                                        triggerHaptic()
                                                    },
                                                    colors = colors,
                                                    modifier = Modifier.weight(1f)
                                                )"""

content = re.sub(r'                                            } else if \(key == "SYMSHIFT"\) \{.*?\n                                            \} else if \(key == "BACKSPACE"\) \{', replacement + '\n                                            } else if (key == "BACKSPACE") {', content, flags=re.DOTALL)

with open("app/src/main/java/com/example/ui/keyboard/KeyboardLayout.kt", "w") as f:
    f.write(content)
