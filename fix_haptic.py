import re

with open("app/src/main/java/com/example/ui/keyboard/KeyboardLayout.kt", "r") as f:
    content = f.read()

# Make it observe lifecycle to refresh preferences
lifecycle_effect = """    var localRefreshTrigger by remember { mutableStateOf(refreshTrigger) }
    
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                localRefreshTrigger++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
"""

content = content.replace("    val context = LocalContext.current", lifecycle_effect + "\n    val context = LocalContext.current")

# Replace uses of `refreshTrigger` with `localRefreshTrigger` in KeyboardLayout.
# But only for preferences initialization!
content = content.replace("var isHapticEnabled by remember(refreshTrigger)", "var isHapticEnabled by remember(localRefreshTrigger)")
content = content.replace("var typingAnimation by remember(refreshTrigger)", "var typingAnimation by remember(localRefreshTrigger)")
content = content.replace("var selectedTheme by remember(refreshTrigger)", "var selectedTheme by remember(localRefreshTrigger)")

with open("app/src/main/java/com/example/ui/keyboard/KeyboardLayout.kt", "w") as f:
    f.write(content)
