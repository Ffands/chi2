with open('app/src/main/java/com/example/autoclicker/AutoClickService.kt', 'r') as f:
    text = f.read()

# Fix 1: removeAllPhantomNodes Unresolved reference
text = text.replace('if (::uiManager.isInitialized) uiManager.removeAllPhantomNodes()', 'if (::uiManager.isInitialized) uiManager.removeAllPhantomNodes()')
# Wait, removeAllPhantomNodes exists in UIManager?
