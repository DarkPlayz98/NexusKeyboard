import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

# Replace the KeyboardLayout at the bottom of MainActivity with some guide text
content = re.sub(
r"        // --- FIXED BOTTOM LIVE PREVIEW WORKSPACE ---[\s\S]+?    }\n}",
"""        // --- SETUP GUIDE ---
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("How to Enable", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text("1. Go to System Settings -> System -> Keyboard -> On-screen keyboard", fontSize = 14.sp)
                Text("2. Enable this keyboard", fontSize = 14.sp)
                Text("3. Switch to it using the keyboard icon in the navigation bar when typing", fontSize = 14.sp)
            }
        }
    }
}""", content)

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)
