import re

with open("app/src/main/java/com/example/ui/keyboard/KeyboardLayout.kt", "r") as f:
    text = f.read()

idx_start = text.find('                                                    val matches = "\\[\\{.*"replacements":\\[\\{"value":"([^\"]+)"".toRegex().find(response)')
if idx_start == -1:
    idx_start = text.find('                                                    val matches = ')

if idx_start != -1:
    idx_end = text.find('                                                        }\n                                                    }')
    
    if idx_end != -1:
        new_text = """                                                    // Simple parse to find replacements
                                                    var correctedText = sourceText
                                                    val matchesRegex = \"\"\""matches":\\[(.*?)\\]\"\"\".toRegex(RegexOption.DOT_MATCHES_ALL)
                                                    val matcher = matchesRegex.find(response)
                                                    if (matcher != null) {
                                                        val replacementRegex = \"\"\""replacements":\\[\\{"value":"([^"]+)"\"\"\".toRegex()
                                                        val foundReplacements = replacementRegex.findAll(response).toList()
                                                        if (foundReplacements.isNotEmpty()) {
                                                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                                                val newWord = foundReplacements[0].groups[1]?.value ?: ""
                                                                sourceText = "Suggestion: " + newWord
                                                            }
                                                        } else {
                                                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                                                sourceText = sourceText + " (No errors found)"
                                                            }
                                                        }
                                                    } else {
                                                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                                            sourceText = sourceText + " (No errors found)"
                                                        }
                                                    }"""
        text = text[:idx_start] + new_text + text[idx_end+118:]

with open("app/src/main/java/com/example/ui/keyboard/KeyboardLayout.kt", "w") as f:
    f.write(text)

