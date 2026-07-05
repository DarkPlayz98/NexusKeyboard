import re

with open("app/src/main/java/com/example/ui/keyboard/KeyboardLayout.kt", "r") as f:
    content = f.read()

# 1. Add Apps, Translate, Gif to KeyboardSubPanel
content = content.replace(
"""enum class KeyboardSubPanel {
    None,
    Emoji,
    Clipboard,
    Settings
}""",
"""enum class KeyboardSubPanel {
    None,
    Emoji,
    Clipboard,
    Settings,
    Apps,
    Translate,
    Gif
}""")

# 2. Fix Apps button in top bar
content = content.replace(
"""            // Apps / Grid
            IconButton(
                onClick = { triggerHaptic() },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Apps,
                    contentDescription = "Apps",
                    tint = colors.headerIconColor,""",
"""            // Apps / Grid
            IconButton(
                onClick = {
                    triggerHaptic()
                    activeSubPanel = if (activeSubPanel == KeyboardSubPanel.Apps) KeyboardSubPanel.None else KeyboardSubPanel.Apps
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Apps,
                    contentDescription = "Apps",
                    tint = if (activeSubPanel == KeyboardSubPanel.Apps) colors.accentColor else colors.headerIconColor,""")

# 3. Fix Translate button
content = content.replace(
"""            // Translate (Placeholder icon since Translate might not be in default icons, use Language)
            IconButton(
                onClick = { triggerHaptic() },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Translate,
                    contentDescription = "Translate",
                    tint = colors.headerIconColor,""",
"""            // Translate (Placeholder icon since Translate might not be in default icons, use Language)
            IconButton(
                onClick = {
                    triggerHaptic()
                    activeSubPanel = if (activeSubPanel == KeyboardSubPanel.Translate) KeyboardSubPanel.None else KeyboardSubPanel.Translate
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Translate,
                    contentDescription = "Translate",
                    tint = if (activeSubPanel == KeyboardSubPanel.Translate) colors.accentColor else colors.headerIconColor,""")

# 4. Fix GIF button
content = content.replace(
"""            // GIF
            IconButton(
                onClick = { triggerHaptic() },
                modifier = Modifier.size(36.dp)
            ) {
                Text(
                    text = "GIF",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.headerIconColor
                )""",
"""            // GIF
            IconButton(
                onClick = {
                    triggerHaptic()
                    activeSubPanel = if (activeSubPanel == KeyboardSubPanel.Gif) KeyboardSubPanel.None else KeyboardSubPanel.Gif
                },
                modifier = Modifier.size(36.dp)
            ) {
                Text(
                    text = "GIF",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (activeSubPanel == KeyboardSubPanel.Gif) colors.accentColor else colors.headerIconColor
                )""")

# 5. Add sub-panels to the when (activeSubPanel) inside KeyboardBody
panels_addition = """                KeyboardSubPanel.Apps -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("Apps Panel", color = colors.keyTextColor, fontWeight = FontWeight.Bold)
                    }
                }
                KeyboardSubPanel.Translate -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("Translate Panel", color = colors.keyTextColor, fontWeight = FontWeight.Bold)
                    }
                }
                KeyboardSubPanel.Gif -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("GIFs", color = colors.keyTextColor, fontWeight = FontWeight.Bold)
                    }
                }"""
content = content.replace(
"""                KeyboardSubPanel.Clipboard -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(8.dp)
                    ) {
                        Text("Clipboard (Coming Soon)", color = colors.keyTextColor)
                    }
                }
            }
        }
    }
}""",
"""                KeyboardSubPanel.Clipboard -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(8.dp)
                    ) {
                        Text("Clipboard (Coming Soon)", color = colors.keyTextColor)
                    }
                }
""" + panels_addition + """
            }
        }
    }
}""")

# 6. Adjust bottom row weights
content = content.replace('.weight(1.3f)', '.weight(1.5f)')
content = content.replace('modifier = Modifier.weight(0.9f)', 'modifier = Modifier.weight(1.0f)')
content = content.replace('.weight(1.7f)', '.weight(1.5f)')
content = content.replace('val spaceWeight = 5f', 'val spaceWeight = 4.5f')


with open("app/src/main/java/com/example/ui/keyboard/KeyboardLayout.kt", "w") as f:
    f.write(content)
