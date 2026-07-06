import re

with open("app/src/main/java/com/example/ui/keyboard/KeyboardLayout.kt", "r") as f:
    content = f.read()

# Settings intent setup inside KeyboardLayout (we need context to launch intent)
content = content.replace(
"""                    triggerHaptic()
                    activeSubPanel = if (activeSubPanel == KeyboardSubPanel.Settings) KeyboardSubPanel.None else KeyboardSubPanel.Settings
                },""",
"""                    triggerHaptic()
                    val intent = android.content.Intent(context, com.example.MainActivity::class.java).apply {
                        flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                },""")

# Replace Settings panel rendering if needed, but it won't be triggered anymore.

# Translate Panel implementation
translate_panel = """                KeyboardSubPanel.Translate -> {
                    var sourceText by remember { mutableStateOf("") }
                    Column(
                        modifier = Modifier.fillMaxSize().padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Mock Translator (En -> Es)", color = colors.keyTextColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Row(modifier = Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.foundation.text.BasicTextField(
                                value = sourceText,
                                onValueChange = { sourceText = it },
                                modifier = Modifier.weight(1f).fillMaxHeight().background(colors.keyBackground, RoundedCornerShape(8.dp)).padding(8.dp),
                                textStyle = androidx.compose.ui.text.TextStyle(color = colors.keyTextColor, fontSize = 16.sp),
                                decorationBox = { innerTextField ->
                                    if (sourceText.isEmpty()) Text("Type to translate...", color = colors.keyTextColor.copy(alpha=0.5f))
                                    innerTextField()
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            KeyButton(
                                text = "Send",
                                onClick = {
                                    triggerHaptic()
                                    // Mock translate - reverse words
                                    val translated = sourceText.split(" ").reversed().joinToString(" ")
                                    onKeyClick(translated + " ")
                                    sourceText = ""
                                },
                                colors = colors,
                                modifier = Modifier.width(70.dp).fillMaxHeight()
                            )
                        }
                    }
                }"""
content = re.sub(
r"                KeyboardSubPanel.Translate -> \{[\s\S]+?                \}",
translate_panel, content)

# GIF Panel implementation
gif_panel = """                KeyboardSubPanel.Gif -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(8.dp)
                    ) {
                        val gifs = listOf(
                            "https://media.tenor.com/2RoZ9Qd347cAAAAM/cat-nodding.gif",
                            "https://media.tenor.com/images/4688b17173e6da8f0290547071e72e36/tenor.gif",
                            "https://media.tenor.com/images/a5fc8a9ea3fcdbbef05f6354fcecc23d/tenor.gif",
                            "https://media.tenor.com/images/7a730026eef57e3f608b471ba95e0c8b/tenor.gif"
                        )
                        androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                            columns = androidx.compose.foundation.lazy.grid.GridCells.Adaptive(80.dp),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp)
                        ) {
                            items(gifs.size) { index ->
                                val gifUrl = gifs[index]
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .padding(2.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(colors.keyBackground)
                                        .clickable {
                                            triggerHaptic()
                                            // Mocking GIF insertion by sending URL
                                            onKeyClick(gifUrl + " ")
                                        }
                                ) {
                                    coil.compose.AsyncImage(
                                        model = coil.request.ImageRequest.Builder(context)
                                            .data(gifUrl)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "GIF",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                    )
                                }
                            }
                        }
                    }
                }"""
content = re.sub(
r"                KeyboardSubPanel.Gif -> \{[\s\S]+?                \}",
gif_panel, content)


# Clipboard Panel implementation
clipboard_panel = """                KeyboardSubPanel.Clipboard -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(8.dp)
                    ) {
                        Text("Clipboard", color = colors.keyTextColor, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                        if (clipboardList.isEmpty()) {
                            Text("No items in clipboard.", color = colors.keyTextColor.copy(alpha=0.5f))
                        } else {
                            androidx.compose.foundation.lazy.LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(clipboardList.size) { index ->
                                    val item = clipboardList[index]
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(colors.keyBackground)
                                            .clickable {
                                                triggerHaptic()
                                                onKeyClick(item.text)
                                            }
                                            .padding(12.dp)
                                    ) {
                                        Text(item.text, color = colors.keyTextColor, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                    }
                                }
                            }
                        }
                    }
                }"""
content = re.sub(
r"                KeyboardSubPanel.Clipboard -> \{[\s\S]+?                \}",
clipboard_panel, content)

with open("app/src/main/java/com/example/ui/keyboard/KeyboardLayout.kt", "w") as f:
    f.write(content)
