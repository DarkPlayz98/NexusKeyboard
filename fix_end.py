import re

with open("app/src/main/java/com/example/ui/keyboard/KeyboardLayout.kt", "r") as f:
    content = f.read()

# First, find the start of KeyboardLayout composable to parse braces.
# Actually, let's just chop the file off at KeyboardSubPanel.Gif and append the end correctly.

# We will find `                KeyboardSubPanel.Gif -> {`
idx = content.find("                KeyboardSubPanel.Gif -> {")

if idx != -1:
    content_top = content[:idx]
    content_bottom = """                KeyboardSubPanel.Gif -> {
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

// Simple Ripple provider that works without experimental APIs
object RippleConfigurationProvider {
    @Composable
    fun getRipple() = androidx.compose.material3.ripple()
}

val LocalTypingAnimation = androidx.compose.runtime.compositionLocalOf { true }

enum class KeyboardSubPanel {
    None,
    Emoji,
    Clipboard,
    Settings,
    Apps,
    Translate,
    Gif
}

@Composable
fun KeyButton(
    text: String,
    onClick: () -> Unit,
    colors: KeyboardThemeColors,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(6.dp))
            .background(colors.keyBackground)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = RippleConfigurationProvider.getRipple(),
                onClick = onClick
            )
    ) {
        Text(
            text = text,
            fontSize = 18.sp,
            color = colors.keyTextColor,
            fontWeight = FontWeight.Normal
        )
    }
}

@Composable
fun IconButtonKey(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    colors: KeyboardThemeColors,
    modifier: Modifier = Modifier,
    isAccent: Boolean = false
) {
    val bg = if (isAccent) colors.accentColor else colors.keyBackground
    val tint = if (isAccent) colors.headerBackground else colors.keyTextColor
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = RippleConfigurationProvider.getRipple(),
                onClick = onClick
            )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
    }
}
"""
    with open("app/src/main/java/com/example/ui/keyboard/KeyboardLayout.kt", "w") as f:
        f.write(content_top + content_bottom)
