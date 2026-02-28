import sys
from PIL import Image

def convert_to_ico(input_file, output_file):
    try:
        img = Image.open(input_file)
        # Ensure image has an alpha channel
        img = img.convert("RGBA")
        # Save as ICO with multiple sizes for best OS support
        img.save(output_file, format='ICO', sizes=[(16, 16), (32, 32), (48, 48), (64, 64), (128, 128), (256, 256)])
        print(f"Successfully converted {input_file} to {output_file}")
    except Exception as e:
        print(f"Error converting image: {e}")

if __name__ == "__main__":
    if len(sys.argv) < 3:
        print("Usage: python convert_ico.py <input.png> <output.ico>")
        sys.exit(1)
    convert_to_ico(sys.argv[1], sys.argv[2])
