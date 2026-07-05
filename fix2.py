with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

import re
content = re.sub(r'                2 -> \{\s*2 -> \{', '                2 -> {', content)

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)
