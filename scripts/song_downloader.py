import sys
import argparse
import subprocess

parser = argparse.ArgumentParser()
parser.add_argument("--mode", required=True, choices=["song"])
parser.add_argument("--yt-dlp", required=True)
parser.add_argument("--ffmpeg", required=True)
args = parser.parse_args()

url = sys.stdin.readline().strip()

if not url.startswith("http"):
    print("INVALID_URL")
    sys.exit(1)

# Build yt-dlp command
command = [
    args.yt_dlp,
    url,
    "--ffmpeg-location", args.ffmpeg,
    "-o", "%(title)s.%(ext)s",
    "-x", "--audio-format", "mp3"
]

result = subprocess.run(command, capture_output=True, text=True)

if result.returncode == 0:
    print("DOWNLOAD_SUCCESS")
else:
    print("DOWNLOAD_FAILED")
    print(result.stderr)
