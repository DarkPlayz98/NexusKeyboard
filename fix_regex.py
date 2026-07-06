import re

with open("app/src/main/java/com/example/ui/keyboard/KeyboardLayout.kt", "r") as f:
    content = f.read()

# Replace the grammar check block to use raw strings
old_grammar = r"""                                                    val matches = "\\[\\{\.\*\\"replacements\\":\\[\\{\\"value\\":\\"([\^"]+)\\""\.toRegex\(\)\.find\(response\)[\s\S]+?                                                    val msg = messageRegex\.find\(response\)\?\.groups\?\.get\(1\)\?\.value \?\: "Errors found" """

new_grammar = """                                                    // Simple regex parsing using raw strings
                                                    val matchesRegex = \"\"\""matches":\\[(.*?)\\]\"\"\".toRegex()
                                                    val matcher = matchesRegex.find(response)
                                                    if (matcher != null) {
                                                        val replacementRegex = \"\"\""replacements":\\[\\{"value":"([^"]+)"\"\"\".toRegex()
                                                        val foundReplacements = replacementRegex.findAll(response).toList()
                                                        if (foundReplacements.isNotEmpty()) {
                                                            val messageRegex = \"\"\""message":"([^"]+)"\"\"\".toRegex()
                                                            val msg = messageRegex.find(response)?.groups?.get(1)?.value ?: "Errors found"
                                                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                                                // Display the first suggestion as the new text
                                                                val newWord = foundReplacements[0].groups[1]?.value ?: ""
                                                                sourceText = "Suggestion: " + newWord
                                                            }
                                                        } else {
                                                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                                                sourceText = sourceText + " (No errors found)"
                                                            }
                                                        }
                                                    }"""

# The regex replacement is tricky due to the multiline and all the braces, let's just use string replacement
with open("app/src/main/java/com/example/ui/keyboard/KeyboardLayout.kt", "r") as f:
    text = f.read()

idx_start = text.find("val matches = \"\\[\\{.*\\\"replacements\\\":\\[\\{\\\"value\\\":\\\"([^\"]+)\\\"\"")
if idx_start != -1:
    idx_end = text.find("kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {", idx_start)
    text = text[:idx_start] + """                                                    // Simple regex parsing using raw strings
                                                    val matchesRegex = \"\"\""matches":\\[(.*?)\\]\"\"\".toRegex()
                                                    val matcher = matchesRegex.find(response)
                                                    if (matcher != null) {
                                                        val replacementRegex = \"\"\""replacements":\\[\\{"value":"([^"]+)"\"\"\".toRegex()
                                                        val foundReplacements = replacementRegex.findAll(response).toList()
                                                        if (foundReplacements.isNotEmpty()) {
                                                            val messageRegex = \"\"\""message":"([^"]+)"\"\"\".toRegex()
                                                            val msg = messageRegex.find(response)?.groups?.get(1)?.value ?: "Errors found"
""" + text[idx_end:]

with open("app/src/main/java/com/example/ui/keyboard/KeyboardLayout.kt", "w") as f:
    f.write(text)
