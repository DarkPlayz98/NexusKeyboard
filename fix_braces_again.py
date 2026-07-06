with open("app/src/main/java/com/example/ui/keyboard/KeyboardLayout.kt", "r") as f:
    content = f.read()

content = content.replace(
"""                KeyboardSubPanel.Gif -> {
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
                }
            }
        }
    }
}

// Simple Ripple""",
"""                KeyboardSubPanel.Gif -> {
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
                }
            }
        }
    }
}
}

// Simple Ripple""")

with open("app/src/main/java/com/example/ui/keyboard/KeyboardLayout.kt", "w") as f:
    f.write(content)
