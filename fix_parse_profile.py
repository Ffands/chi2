with open('app/src/main/java/com/example/autoclicker/AutoClickService.kt', 'r') as f:
    text = f.read()

target = """    private fun parseProfileNodes(profileName: String): List<TargetNode>? {
        val file = java.io.File(getExternalFilesDir(null), "profiles/$profileName.json")
        if (!file.exists()) return null
        try {
            val json = file.readText()
            val jsonObject = org.json.JSONObject(json)"""
rep = """    private fun parseProfileNodes(profileName: String): List<TargetNode>? {
        val prefs = getSharedPreferences("AutoClickerProfiles", android.content.Context.MODE_PRIVATE)
        val json = prefs.getString(profileName, null) ?: return null
        try {
            val jsonObject = org.json.JSONObject(json)"""
text = text.replace(target, rep)

with open('app/src/main/java/com/example/autoclicker/AutoClickService.kt', 'w') as f:
    f.write(text)
print("Fixed parseProfileNodes to use SharedPreferences")
