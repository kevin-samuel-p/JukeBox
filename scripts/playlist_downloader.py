import os
from pathlib import Path
import pickle
import random
import subprocess
import time
import sys

# --- PATHS ---
YTDLP_PATH = os.path.join("bin", "yt-dlp.exe")
FFMPEG_PATH = os.path.join("bin", "ffmpeg.exe")
QUEUE_FILE = os.path.join("data", "download_queue.dat")
LOCK_FILE = os.path.join("data", "playlist_downloader.lock")
MUSIC_FOLDER = Path.home() / 'Music'
FALLBACK_FOLDER = Path.home() / 'Downloads'
DOWNLOAD_DIR = MUSIC_FOLDER if MUSIC_FOLDER.exists() else FALLBACK_FOLDER

# --- CONFIGS ---
USER_AGENTS = [
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64)",
    "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:89.0)",
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)"
]
PROXIES = [
    "128.199.202.122",
    "49.207.36.81",
    "139.59.1.14",
    "57.129.81.201",
    "78.47.127.91"
]   # NOTE: Avoid hardcoding proxies, set up in a file

# --- UTILITY FUNCTIONS ---
def load_queue():
    if not os.path.exists(QUEUE_FILE):
        return []
    try:
        with open(QUEUE_FILE, 'rb') as f:
            return pickle.load(f)
    except Exception as e:
        print(f'[ERROR] Failed to load queue: {e}')
        return []

def save_queue(queue):
    if queue:
        with open(QUEUE_FILE, 'wb') as f:
            pickle.dump(queue, f)
    else:
        if os.path.exists(QUEUE_FILE):
            os.remove(QUEUE_FILE)

def cleanup_lock():
    if os.path.exists(LOCK_FILE):
        os.remove(LOCK_FILE)


# --- DOWNLOAD FUNCTION ---
def download_track(video_url, attempt=1, max_attempts=5):
    print(f'[INFO] Downloading song: {video_url}')
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
        result = subprocess.run(cmd, check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        print(f'[SUCCESS] Downloaded: {video_url}')
        return result.returncode == 0
    except subprocess.CalledProcessError:
        if attempt < max_attempts:
            delay = 2 ** attempt + random.uniform(1, 3)
            print(f'[ERROR] Failed (attempt {attempt}) — retrying in {delay:.1f}s...')
            time.sleep(delay)
            download_track(video_url, attempt+1)
        else:
            print(f'[SKIPPED] Max retries exceeded: {video_url}')
            return False
    except Exception as e:
        print(f'[ERROR] Unexpected error: {e}')
        return False


# --- MAIN LOOP ---
def main():
    if os.path.exists(LOCK_FILE):
        print('[INFO] Another instance is running. Exiting.')
        return
    open(LOCK_FILE, 'w').close()
    
    try:
        queue = load_queue()
        if not queue:
            print("[INFO] Queue is empty. Exiting.")
            return

        print(f"[INFO] Starting playlist download: {len(queue)} track(s)")
        while queue:
            current_url = queue[0]
            success = download_track(current_url)
            if success:
                queue.pop(0)
                save_queue(queue)
            else:
                print('[WARN] Pausing before next attempt.')
                time.sleep(3)   # Pause before retrying
                break           # Exit early to retry from Java-side trigger
        
        if not queue:
            print('[INFO] All songs downloaded. Cleaning up.')
            save_queue([])  # Deletes the file
    
    finally:
        cleanup_lock()
        print('[INFO] Exiting playlist_downloader.exe')
    
if __name__ == '__main__':
    try:
        main()
    except KeyboardInterrupt:
        print('\n[INTERRUPTED] Shutdown requested.')
        cleanup_lock()
