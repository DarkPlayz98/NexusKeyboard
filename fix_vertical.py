import re

with open("app/src/main/java/com/example/ui/keyboard/KeyboardLayout.kt", "r") as f:
    content = f.read()

# Replace fixed heights with fillMaxHeight for keys
content = content.replace('.height(44.dp)', '.fillMaxHeight()')

# Give the rows weight(1f)
content = content.replace(
"""                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    )""",
"""                                    Row(
                                        modifier = Modifier.fillMaxWidth().weight(1f),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    )""")

content = content.replace(
"""                                // Bottom Action Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                )""",
"""                                // Bottom Action Row
                                Row(
                                    modifier = Modifier.fillMaxWidth().weight(1f),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                )""")

with open("app/src/main/java/com/example/ui/keyboard/KeyboardLayout.kt", "w") as f:
    f.write(content)
