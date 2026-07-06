import re

with open("app/src/main/java/com/example/ui/keyboard/KeyboardLayout.kt", "r") as f:
    content = f.read()

# Add getCharForPos at the top level
char_for_pos_func = """
fun getCharForPos(x: Float, y: Float, width: Int, height: Int, allKeyRows: List<List<String>>): String? {
    if (width == 0 || height == 0) return null
    val totalRows = allKeyRows.size + 1
    val rowIdx = (y / (height.toFloat() / totalRows)).toInt().coerceIn(0, totalRows - 1)
    
    if (rowIdx < allKeyRows.size) {
        val row = allKeyRows[rowIdx]
        val maxWeight = allKeyRows.maxOfOrNull { r -> 
            r.sumOf { k -> if (k == "SHIFT" || k == "SYMSHIFT" || k == "BACKSPACE") 1.5 else 1.0 } 
        }?.toFloat() ?: 10f
        
        val rowWeight = row.sumOf { k -> if (k == "SHIFT" || k == "SYMSHIFT" || k == "BACKSPACE") 1.5 else 1.0 }.toFloat()
        val paddingWeight = (maxWeight - rowWeight) / 2f
        
        val unitWidth = width / maxWeight
        val startX = paddingWeight * unitWidth
        
        if (x < startX) return null
        var currentX = startX
        for (key in row) {
            val keyWeight = if (key == "SHIFT" || key == "SYMSHIFT" || key == "BACKSPACE") 1.5f else 1.0f
            val keyWidth = keyWeight * unitWidth
            if (x >= currentX && x < currentX + keyWidth) {
                return if (key.length == 1 && key[0].isLetter()) key.lowercase() else null
            }
            currentX += keyWidth
        }
    }
    return null
}
"""

if "fun getCharForPos" not in content:
    content += char_for_pos_func

# Update pointerInput for Swipe Typing
swipe_pattern = r"""                                            if \(isDragging\) \{[\s\S]+?                                                handleKeyClick\("test "\)[\s\S]+?                                                triggerFeedback\(\)[\s\S]+?                                            \}"""

swipe_replacement = """                                            if (isDragging && dragPath.isNotEmpty()) {
                                                val swipeChars = mutableListOf<String>()
                                                for (p in dragPath) {
                                                    val c = getCharForPos(p.x, p.y, containerWidth, containerHeight, allKeyRows)
                                                    if (c != null && (swipeChars.isEmpty() || swipeChars.last() != c)) {
                                                        swipeChars.add(c)
                                                    }
                                                }
                                                val pattern = swipeChars.joinToString("")
                                                if (pattern.length >= 2) {
                                                    val first = pattern.first()
                                                    val last = pattern.last()
                                                    val regexStr = pattern.toCharArray().joinToString(".*")
                                                    val regex = Regex(regexStr, RegexOption.IGNORE_CASE)
                                                    
                                                    val candidates = COMMON_WORDS.filter { 
                                                        it.length >= pattern.length && 
                                                        it.first().lowercaseChar() == first && 
                                                        it.last().lowercaseChar() == last 
                                                    }
                                                    
                                                    val exactMatch = candidates.firstOrNull { it.matches(regex) }
                                                    val resolved = exactMatch ?: pattern
                                                    handleKeyClick("$resolved ")
                                                } else if (pattern.isNotEmpty()) {
                                                    handleKeyClick("$pattern ")
                                                }
                                                triggerFeedback()
                                            }"""

content = re.sub(swipe_pattern, swipe_replacement, content)

# Update BACKSPACE for faster deleting
backspace_pattern = r"""                                            \} else if \(key == "BACKSPACE"\) \{[\s\S]+?                                                Box\([\s\S]+?                                                    contentAlignment = Alignment\.Center,[\s\S]+?                                                    modifier = Modifier[\s\S]+?                                                        \.weight\(1\.5f\)[\s\S]+?                                                        \.fillMaxHeight\(\)[\s\S]+?                                                        \.clip\(RoundedCornerShape\(6\.dp\)\)[\s\S]+?                                                        \.background\(colors\.keyBackground\)[\s\S]+?                                                        \.clickable\([\s\S]+?                                                            interactionSource = backspaceInteractionSource,[\s\S]+?                                                            indication = RippleConfigurationProvider\.getRipple\(\),[\s\S]+?                                                            onClick = \{[\s\S]+?                                                                triggerFeedback\(\)[\s\S]+?                                                                handleBackspace\(\)[\s\S]+?                                                            \}[\s\S]+?                                                        \)[\s\S]+?                                                        \.testTag\("backspace_key"\)[\s\S]+?                                                \) \{"""

backspace_replacement = """                                            } else if (key == "BACKSPACE") {
                                                val coroutineScope = rememberCoroutineScope()
                                                Box(
                                                    contentAlignment = Alignment.Center,
                                                    modifier = Modifier
                                                        .weight(1.5f)
                                                        .fillMaxHeight()
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(colors.keyBackground)
                                                        .pointerInput(preferences.deletingSpeed) {
                                                            awaitPointerEventScope {
                                                                while (true) {
                                                                    val down = awaitFirstDown()
                                                                    triggerFeedback()
                                                                    handleBackspace()
                                                                    val job = coroutineScope.launch {
                                                                        delay(400)
                                                                        var currentDelay = 100L
                                                                        val minDelay = (20f / preferences.deletingSpeed.coerceAtLeast(0.1f)).toLong().coerceIn(10L, 200L)
                                                                        while (kotlinx.coroutines.isActive) {
                                                                            triggerFeedback()
                                                                            handleBackspace()
                                                                            delay(currentDelay)
                                                                            currentDelay = (currentDelay * 0.85).toLong().coerceAtLeast(minDelay)
                                                                        }
                                                                    }
                                                                    waitForUpOrCancellation()
                                                                    job.cancel()
                                                                }
                                                            }
                                                        }
                                                        .testTag("backspace_key")
                                                ) {"""

content = re.sub(backspace_pattern, backspace_replacement, content)

with open("app/src/main/java/com/example/ui/keyboard/KeyboardLayout.kt", "w") as f:
    f.write(content)

