import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

# We want to change the when(activeTab) logic to 0 (Status), 1 (Themes), 2 (Settings)
# But it's easier to just swap the contents manually in code. 
# We'll replace the block from "when (activeTab) {" to the end of the composable.
