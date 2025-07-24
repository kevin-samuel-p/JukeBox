import sys
import os
import subprocess
import json
import time
import random
import re
from pathlib import Path

# --- CONFIGURABLE PATHS ---
BASE_DIR = Path(__file__).resolve().parent.parent  # JukeBox/
BIN_DIR = BASE_DIR / 'bin'
YTDLP_PATH = str(BIN_DIR / 'yt-dlp')
FFMPEG_PATH = str(BIN_DIR / 'ffmpeg')
MUSIC_FOLDER = Path.home() / 'Music'
FALLBACK_FOLDER = Path.home() / 'Downloads'
DOWNLOAD_DIR = MUSIC_FOLDER if MUSIC_FOLDER.exists() else FALLBACK_FOLDER

# --- USER AGENTS & PROXIES ---
USER_AGENTS = [
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64)",
    "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:89.0)",
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)"
]
PROXIES = [
    # Add working proxies here if desired
    None  # None = no proxy
]

# --- READ URL FROM STDIN ---
print("Awaiting playlist URL from Java...")
playlist_url = sys.stdin.readline().strip()

# --- VALIDATE URL FORMAT ---
if not re.match(r'^https?://(www\.)?youtube\.com/playlist\?list=[\w-]+$', playlist_url):
    print("Invalid YouTube playlist URL format.")
    sys.exit(1)

# --- FETCH PLAYLIST METADATA TO CONFIRM EXISTENCE ---
def get_playlist_info(url):
    try:
        result = subprocess.run(
            [YTDLP_PATH, '--dump-single-json', '--flat-playlist', url],
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True
        )
        if result.returncode != 0:
            return None
        return json.loads(result.stdout)
    except Exception as e:
        print("Error fetching playlist:", e)
        return None

info = get_playlist_info(playlist_url)
if not info or 'entries' not in info or not info['entries']:
    print("Playlist not found or is empty.")
    sys.exit(1)

print(f"Playlist found: {info.get('title', 'Untitled')} — {len(info['entries'])} items")

# --- DOWNLOAD FUNCTION ---
def download_track(video_url, attempt=1, max_attempts=5):
    user_agent = random.choice(USER_AGENTS)
    proxy = random.choice(PROXIES)

    cmd = [
        YTDLP_PATH,
        video_url,
        '-x', '--audio-format', 'mp3',
        '--ffmpeg-location', FFMPEG_PATH,
        '--user-agent', user_agent,
        '-o', str(DOWNLOAD_DIR / '%(title)s.%(ext)s')
    ]

    if proxy:
        cmd.extend(['--proxy', proxy])

    try:
        subprocess.run(cmd, check=True)
        print(f"✓ Downloaded: {video_url}")
    except subprocess.CalledProcessError:
        if attempt < max_attempts:
            delay = 2 ** attempt + random.uniform(1, 3)
            print(f"✗ Failed (attempt {attempt}) — retrying in {delay:.1f}s...")
            time.sleep(delay)
            download_track(video_url, attempt + 1)
        else:
            print(f"✗ Skipped after {max_attempts} attempts: {video_url}")

# --- MAIN LOOP ---
for entry in info['entries']:
    video_id = entry.get('id')
    if video_id:
        download_track(f"https://www.youtube.com/watch?v={video_id}")

print("All done.")
