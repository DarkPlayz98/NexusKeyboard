with open("app/src/main/java/com/example/ui/keyboard/KeyboardLayout.kt", "r") as f:
    text = f.read()

grammar_panel = """                KeyboardSubPanel.Grammar -> {
                    var sourceText by remember { mutableStateOf("") }
                    var isChecking by remember { mutableStateOf(false) }
                    var hasChecked by remember { mutableStateOf(false) }
                    val scope = rememberCoroutineScope()
                    
                    Column(
                        modifier = Modifier.fillMaxSize().padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("LanguageTool Grammar Check", color = colors.keyTextColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            if (isChecking) {
                                androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(16.dp), color = colors.accentColor, strokeWidth = 2.dp)
                            }
                        }
                        
                        Row(modifier = Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.foundation.text.BasicTextField(
                                value = sourceText,
                                onValueChange = { 
                                    sourceText = it
                                    hasChecked = false 
                                },
                                modifier = Modifier.weight(1f).fillMaxHeight().background(colors.keyBackground, RoundedCornerShape(8.dp)).padding(8.dp),
                                textStyle = androidx.compose.ui.text.TextStyle(color = colors.keyTextColor, fontSize = 16.sp),
                                decorationBox = { innerTextField ->
                                    if (sourceText.isEmpty()) Text("Type or paste text to check...", color = colors.keyTextColor.copy(alpha=0.5f))
                                    innerTextField()
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            if (!hasChecked) {
                                KeyButton(
                                    text = "Check",
                                    onClick = {
                                        triggerFeedback()
                                        if (sourceText.isNotBlank()) {
                                            isChecking = true
                                            scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                                try {
                                                    val url = java.net.URL("https://api.languagetool.org/v2/check")
                                                    val conn = url.openConnection() as java.net.HttpURLConnection
                                                    conn.requestMethod = "POST"
                                                    conn.doOutput = true
                                                    val data = "language=en-US&text=" + java.net.URLEncoder.encode(sourceText, "UTF-8")
                                                    conn.outputStream.write(data.toByteArray())
                                                    val response = conn.inputStream.bufferedReader().readText()
                                                    
                                                    // Simple regex parsing using raw strings
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
                                                    }
                                                } catch(e: Exception) {
                                                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                                        sourceText = "Error: " + e.message
                                                    }
                                                } finally {
                                                    isChecking = false
                                                    hasChecked = true
                                                }
                                            }
                                        }
                                    },
                                    colors = colors,
                                    modifier = Modifier.width(70.dp).fillMaxHeight()
                                )
                            } else {
                                KeyButton(
                                    text = "Send",
                                    onClick = {
                                        triggerFeedback()
                                        onKeyClick(sourceText + " ")
                                        sourceText = ""
                                        hasChecked = false
                                    },
                                    colors = colors,
                                    modifier = Modifier.width(70.dp).fillMaxHeight()
                                )
                            }
                        }
                    }
                }\n"""

if "KeyboardSubPanel.Grammar -> {" not in text:
    text = text.replace("                KeyboardSubPanel.Translate -> {", grammar_panel + "                KeyboardSubPanel.Translate -> {")

with open("app/src/main/java/com/example/ui/keyboard/KeyboardLayout.kt", "w") as f:
    f.write(text)
