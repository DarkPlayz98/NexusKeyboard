import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

# We want to change the when(activeTab) logic:
# 0 -> keep item 0
# 1 -> Preset Themes, Custom Theme Builder
# 2 -> One-Handed Layout, Typing Animation, Deleting Speed, Haptic Controls

# It's easier to modify the activeTab switch numbers.
# Currently:
# 0 -> Cloud Sync & Status (lines 466-600 approx)
# 1 -> Clipboard (lines 601-736)
# 2 -> Settings (lines 737-1150)

# We can replace the when(activeTab) { ... } with a custom composed string.
