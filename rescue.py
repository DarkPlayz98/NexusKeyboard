with open("app/src/main/java/com/example/ui/keyboard/KeyboardLayout.kt", "r") as f:
    lines = f.readlines()

def get_lines(start, end):
    return "".join(lines[start-1:end])

part1 = get_lines(1, 1034)  # Up to end of Apps panel
part2 = get_lines(1486, 1593)  # Translate and Gif panels
part3 = get_lines(1594, len(lines)) # Rest of the file

with open("rescued.kt", "w") as f:
    f.write(part1 + part2 + part3)
