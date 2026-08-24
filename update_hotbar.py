with open('app/src/main/java/com/example/autoclicker/AutoClickService.kt', 'r') as f:
    text = f.read()

funcs = """    fun getHotbarItems(): List<Pair<String, String>> {
        val prefs = getSharedPreferences("AutoClickerPrefs", android.content.Context.MODE_PRIVATE)
        val jsonStr = prefs.getString("HotbarItems", null)
        val list = mutableListOf<Pair<String, String>>()
        if (jsonStr != null) {
            try {
                val arr = org.json.JSONArray(jsonStr)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(Pair(obj.getString("name"), obj.optString("label", obj.getString("name"))))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        if (list.isEmpty()) {
            getSavedProfiles().take(5).forEach { list.add(Pair(it, it)) }
        }
        return list
    }

    fun saveHotbarItems(items: List<Pair<String, String>>) {
        val prefs = getSharedPreferences("AutoClickerPrefs", android.content.Context.MODE_PRIVATE)
        val arr = org.json.JSONArray()
        for (item in items) {
            val obj = org.json.JSONObject()
            obj.put("name", item.first)
            obj.put("label", item.second)
            arr.put(obj)
        }
        prefs.edit().putString("HotbarItems", arr.toString()).apply()
    }
"""

if "fun getHotbarItems" not in text:
    text = text.replace("    fun getSavedProfiles(): List<String> {", funcs + "\n    fun getSavedProfiles(): List<String> {")

with open('app/src/main/java/com/example/autoclicker/AutoClickService.kt', 'w') as f:
    f.write(text)
print("Added getHotbarItems to AutoClickService")
