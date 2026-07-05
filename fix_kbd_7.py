import re

with open("app/src/main/java/com/example/ui/keyboard/KeyboardLayout.kt", "r") as f:
    content = f.read()

content = content.replace("enum class KeyboardSubPanel { None, Emoji }", "enum class KeyboardSubPanel { None, Emoji, Settings }")
content = content.replace("sealed class KeyboardSubPanel { object None: KeyboardSubPanel(); object Emoji: KeyboardSubPanel(); object Settings: KeyboardSubPanel() }", "") # just in case
content = content.replace("sealed class KeyboardSubPanel {\n    object None : KeyboardSubPanel()\n    object Emoji : KeyboardSubPanel()\n}", "sealed class KeyboardSubPanel {\n    object None : KeyboardSubPanel()\n    object Emoji : KeyboardSubPanel()\n    object Settings : KeyboardSubPanel()\n}")

# We need to add Settings panel to when (activeSubPanel)
settings_panel = """                KeyboardSubPanel.Settings -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("Quick Settings", color = colors.keyTextColor, fontWeight = FontWeight.Bold)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("One-Handed Mode", color = colors.keyTextColor)
                            Button(onClick = { 
                                oneHandedMode = if (oneHandedMode == "Standard") "Right" else "Standard" 
                                preferences.oneHandedMode = oneHandedMode
                            }) {
                                Text(if (oneHandedMode == "Standard") "Full" else "Docked")
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Haptic Feedback", color = colors.keyTextColor)
                            Switch(checked = isHapticEnabled, onCheckedChange = { 
                                isHapticEnabled = it 
                                preferences.isHapticEnabled = it
                            })
                        }
                    }
                }"""

# Find KeyboardSubPanel.Emoji -> {
content = content.replace("                KeyboardSubPanel.Emoji -> {", settings_panel + "\n                KeyboardSubPanel.Emoji -> {")

with open("app/src/main/java/com/example/ui/keyboard/KeyboardLayout.kt", "w") as f:
    f.write(content)
