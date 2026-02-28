import os

files = {
    r'd:\jms\src\main\java\com\jewelry\controller\ReportsController.java': [
        (b'ðŸ ·', '🏷️'.encode('utf-8'))
    ],
    r'd:\jms\src\main\java\com\jewelry\controller\OrderFormController.java': [
        (b'â€“', '-'.encode('utf-8')),
        (b'ðŸ \xa0', '🏠'.encode('utf-8'))
    ],
    r'd:\jms\src\main\java\com\jewelry\controller\DashboardController.java': [
        (b'â ³', '⏳'.encode('utf-8'))
    ]
}

for path, changes in files.items():
    if not os.path.exists(path):
        continue
    with open(path, 'rb') as f:
        content = f.read()
    
    original = content
    for pattern, repl in changes:
        content = content.replace(pattern, repl)
        
    if content != original:
        with open(path, 'wb') as f:
            f.write(content)
        print(f"Fixed {os.path.basename(path)}")
    else:
        print(f"No changes in {os.path.basename(path)}")
