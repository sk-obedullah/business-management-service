import os

files = {
    r'd:\jms\src\main\java\com\jewelry\controller\ReportsController.java': {
        118: '                        ? "🏷️  Status:  " + cboStatus.getValue()\n'
    },
    r'd:\jms\src\main\java\com\jewelry\controller\OrderFormController.java': {
        79: '    // ── FXML – Customer / Notes\n',
        106: '    // ── FXML – Item Picker\n',
        121: '    // ── FXML – Line-item Table\n',
        138: '    // ── FXML – Footer\n',
        187: '            lblCustomerAddress.setText("🏠 " + order.getCustomerAddress());\n'
    },
    r'd:\jms\src\main\java\com\jewelry\controller\DashboardController.java': {
        135: '                kpiCard("⏳ Pending Orders", String.valueOf(s.getPendingOrders()), "#f39c12"),\n'
    }
}

for path, changes in files.items():
    if not os.path.exists(path):
        continue
    with open(path, 'r', encoding='utf-8') as f:
        lines = f.readlines()
    
    modified = False
    for i in range(len(lines)):
        if i in changes:
            lines[i] = changes[i]
            modified = True
            
    if modified:
        with open(path, 'w', encoding='utf-8') as f:
            f.writelines(lines)
        print(f"Fixed {os.path.basename(path)}")
