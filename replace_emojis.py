import os
import glob

files = [
    r'd:\jms\src\main\java\com\jewelry\controller\DashboardController.java',
    r'd:\jms\src\main\java\com\jewelry\controller\MainLayoutController.java',
    r'd:\jms\src\main\java\com\jewelry\controller\OrderFormController.java',
    r'd:\jms\src\main\java\com\jewelry\controller\ReportsController.java'
]

replacements = {
    'â‚¹': '₹',
    'âš ': '⚠️',
    'âœ…': '✅',
    'âœ˜': '✘',
    'â€”': '—',
    'ðŸ’°': '💰',
    'ðŸ“ˆ': '📈',
    'ðŸ“¦': '📦',
    'â ³': '⏳',
    'ðŸ‘¤': '👤',
    'ðŸ’Ž': '💎',
    'ðŸ“Š': '📊',
    'ðŸ“…': '📅',
    'ðŸ ·': '🏷️',
    'â€¦': '…',
    'ðŸ“ž': '📞',
    'ðŸ”Œ': '🔍',
    'ðŸ›’': '🛒',
    'âœ”': '✔'
}

for filepath in files:
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()
            
        original = content
        for bad, good in replacements.items():
            content = content.replace(bad, good)
            
        if content != original:
            with open(filepath, 'w', encoding='utf-8') as f:
                f.write(content)
            print(f"Fixed {os.path.basename(filepath)}")
        else:
            print(f"No replacements made in {os.path.basename(filepath)}")
            
    except Exception as e:
        print(f"Failed on {os.path.basename(filepath)}: {e}")
