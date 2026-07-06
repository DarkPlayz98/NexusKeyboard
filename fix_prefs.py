import re

with open("app/src/main/java/com/example/data/PreferencesManager.kt", "r") as f:
    content = f.read()

content = content.replace(
"""        const val KEY_TYPING_ANIMATION = "typing_animation"
    }""",
"""        const val KEY_TYPING_ANIMATION = "typing_animation"
        const val KEY_SOUND_ENABLED = "sound_enabled"
    }""")

content = content.replace(
"""    var isHapticEnabled: Boolean
        get() = prefs.getBoolean(KEY_HAPTIC_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_HAPTIC_ENABLED, value).apply()""",
"""    var isHapticEnabled: Boolean
        get() = prefs.getBoolean(KEY_HAPTIC_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_HAPTIC_ENABLED, value).apply()

    var isSoundEnabled: Boolean
        get() = prefs.getBoolean(KEY_SOUND_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_SOUND_ENABLED, value).apply()""")

with open("app/src/main/java/com/example/data/PreferencesManager.kt", "w") as f:
    f.write(content)
