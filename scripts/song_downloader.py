import sys
import argparse
import subprocess
import os

parser = argparse.ArgumentParser()
parser.add_argument("--mode", required=True, choices=["song"])
parser.add_argument("--yt-dlp", required=True)
parser.add_argument("--ffmpeg", required=True)
parser.add_argument("--output-dir", required=True)
args = parser.parse_args()

url = sys.stdin.readline().strip()
output_template = os.path.join(args.output_dir, '%(title)s.%(ext)s')

if not url.startswith("http"):
    print("INVALID_URL")
    sys.exit(1)

# Build yt-dlp command
command = [
    args.yt_dlp,
    url,
    "--ffmpeg-location", args.ffmpeg,
    "-o", output_template,
    "-x", "--audio-format", "mp3"
]

result = subprocess.run(command, capture_output=True, text=True)

if result.returncode == 0:
    print("DOWNLOAD_SUCCESS")
else:
    print("DOWNLOAD_FAILED")
    print(result.stderr)
