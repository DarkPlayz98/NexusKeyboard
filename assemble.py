with open("tab0.txt") as f:
    t0 = f.read()
with open("tab1.txt") as f:
    t1 = f.read()
with open("tab2_part1.txt") as f:
    t2_1 = f.read()
with open("tab2_part2.txt") as f:
    t2_2 = f.read()

new_when_block = f"""            when (activeTab) {{
                0 -> {{
{t0}
                }}
                1 -> {{
{t1}
                }}
                2 -> {{
{t2_1}
{t2_2}
                }}
            }}"""

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    lines = f.readlines()

start_idx = -1
end_idx = -1
for i, line in enumerate(lines):
    if "when (activeTab) {" in line and start_idx == -1:
        start_idx = i
    if "        // --- FIXED BOTTOM LIVE PREVIEW WORKSPACE ---" in line:
        end_idx = i - 1
        break

lines = lines[:start_idx] + [new_when_block + "\n        }\n"] + lines[end_idx:]

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.writelines(lines)
