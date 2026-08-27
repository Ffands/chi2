import re
with open('app/src/main/java/com/example/autoclicker/AutoClickService.kt', 'r') as f:
    text = f.read()

# getNextNodeLinear now only accepts 1 parameter in our current definition (or we should restore the 2-parameter version)
# Let's just fix the calls that still have 2 parameters.
text = re.sub(r'getNextNodeLinear\((\w+)\.id,\s*[^)]+\)', r'getNextNodeLinear(\1.id)', text)

# UIManager missing imports for Dropdown
imports = "import android.widget.ArrayAdapter\nimport android.widget.Spinner\nimport android.widget.AdapterView\n"
if "android.widget.Spinner" not in text:
    text = text.replace("import android.view.WindowManager", imports + "import android.view.WindowManager")

with open('app/src/main/java/com/example/autoclicker/AutoClickService.kt', 'w') as f:
    f.write(text)
