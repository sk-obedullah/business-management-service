import os
import glob
import codecs

dirs = [
    r"d:\jms\src\main\java\com\jewelry\controller",
    r"d:\jms\src\main\java\com\jewelry\service\impl",
    r"d:\jms\src\main\java\com\jewelry\dto"
]

for d in dirs:
    for filepath in glob.glob(d + r'\*.java'):
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()
        
        if 'â‚¹' in content or 'ðŸ' in content or 'â€”' in content or 'âš' in content:
            # Revert the double encoding
            try:
                # The text was decoded as cp1252 instead of utf-8 previously, 
                # and then saved as utf-8.
                # To reverse it: encode to cp1252 to get the original raw bytes,
                # then decode those bytes properly as utf-8.
                raw_bytes = content.encode('cp1252')
                fixed_content = raw_bytes.decode('utf-8')
                
                with open(filepath, 'w', encoding='utf-8') as f:
                    f.write(fixed_content)
                print(f"Fixed double-encoding in {os.path.basename(filepath)}")
            except Exception as e:
                print(f"Failed to fix {os.path.basename(filepath)}: {e}")
