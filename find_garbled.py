
import glob
import re

files = [
    r'd:\jms\src\main\java\com\jewelry\controller\DashboardController.java',
    r'd:\jms\src\main\java\com\jewelry\controller\MainLayoutController.java',
    r'd:\jms\src\main\java\com\jewelry\controller\OrderFormController.java',
    r'd:\jms\src\main\java\com\jewelry\controller\ReportsController.java'
]

known_garbled = set()
for f in files:
    with open(f, 'r', encoding='utf-8', errors='ignore') as file:
        content = file.read()
        tokens = re.findall(r'[âð][^ \wA-Za-z0-9]*', content)
        for t in tokens:
            if len(t.strip()) > 0:
                known_garbled.add(t)

print(known_garbled)

