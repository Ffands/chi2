with open('app/src/main/java/com/example/autoclicker/AutoClickService.kt', 'r') as f:
    text = f.read()

text = text.replace(
    "val duration = if (allowExtremeSpeed) 1L else (if (node.isSwipe) node.swipeDurationMs else node.clickDurationMs)",
    "val duration = if (allowExtremeSpeed) 10L else (if (node.isSwipe) node.swipeDurationMs else node.clickDurationMs)"
)

with open('app/src/main/java/com/example/autoclicker/AutoClickService.kt', 'w') as f:
    f.write(text)
