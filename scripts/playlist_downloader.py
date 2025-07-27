import os, sys
import subprocess, argparse
import csv, json
import random
import time
import signal, atexit
from pathlib import Path


# --- PATHS ANDS CONFIGS ---

if getattr(sys, 'frozen', False):
    # PyInstaller EXE — sys.executable gives the path to the .exe in /bin
    script_dir = os.path.dirname(sys.executable)
else:
    # Running as a script — __file__ gives path to .py in /scripts
    script_dir = os.path.abspath(os.path.dirname(__file__))

# Go up one level to get the project root (JukeBox/)
BASE_DIR = os.path.dirname(script_dir)

YTDLP_PATH = os.path.join(BASE_DIR, 'bin', 'yt-dlp.exe')
FFMPEG_PATH = os.path.join(BASE_DIR, 'bin', 'ffmpeg.exe')
QUEUE_FILE = os.path.join(BASE_DIR, 'data', 'download_queue.csv')
LOCK_FILE = os.path.join(BASE_DIR, 'data', 'playlist_downloader.lock')
MUSIC_FOLDER = Path.home() / 'Music'
FALLBACK_FOLDER = Path.home() / 'Downloads'
DOWNLOAD_DIR = MUSIC_FOLDER if MUSIC_FOLDER.exists() else FALLBACK_FOLDER

IS_OWNER = False

def create_lock():
    if os.path.exists(LOCK_FILE):
        print('[INFO] Another instance is running. Exiting...')
        return
    open(LOCK_FILE, 'w').close()
    global IS_OWNER
    IS_OWNER = True

def cleanup_lock():
    '''
        This function is used to remove the lock file before termination.
    '''
    if os.path.exists(LOCK_FILE) and IS_OWNER:
        os.remove(LOCK_FILE)


# Always run this at exit
atexit.register(cleanup_lock)

# Handle signals from OS or Java
def handle_signal(signum, frame):
    print(f"[SIGNAL] Received signal {signum}. Exiting.")
    sys.exit(0)  # Triggers atexit cleanup

signal.signal(signal.SIGINT, handle_signal)
signal.signal(signal.SIGTERM, handle_signal)

# --- UTILITY FUNCTIONS ---

def load_queue():
    '''
        This function loads the download queue from the CSV file.
        It uses CSV Reader to return the list directly.
    '''

    if not os.path.exists(QUEUE_FILE):
        return []
    try:
        with open(QUEUE_FILE, 'r', encoding='utf-8') as f:
            csv_reader = csv.reader(f)
            return next(csv_reader)
    except Exception as e:
        print(f'[ERROR] Failed to load queue: {e}')
        return []


def save_queue(queue):
    '''
        This function is used to save the queue everytime a download is completed.
        After download completion, the first element is dequeued and the queue is stored.
        If the queue happens to be empty, the file is deleted, a precondition for termination of the main loop.
    '''

    if queue:
        with open(QUEUE_FILE, 'w', newline='', encoding='utf-8') as f:
            writer = csv.writer(f)
            writer.writerow(queue)
    else:
        os.remove(QUEUE_FILE)


def extract_video_urls(playlist_url):
    '''
        This function is used to flatten a playlist's list of songs, derived from YT-DLP's client.
        The client fetches the metadata for the playlist, which includes the list of songs.
        The flattened list is stored in the queue which is used to create download_queue.csv.
    '''

    print(f'[INFO] Extracing video URLs from playlist: {playlist_url}')
    cmd = [
        YTDLP_PATH,
        '--flat-playlist',
        '--dump-json',
        playlist_url
    ]

    try:
        result = subprocess.run(cmd, check=True, capture_output=True, text=True)
        videos = []
        for line in result.stdout.strip().splitlines():
            data = json.loads(line)
            if 'url' in data:
                video_id = data['url']
                videos.append(video_id)    # Store only video IDs in CSV
        print(f'[INFO] Found {len(videos)} video(s) in playlist.')
        random.shuffle(videos)
        return videos
    except subprocess.CalledProcessError:
        print('[ERROR] Failed to extract video URLs.')
        return []
    except Exception as e:
        print(f'[ERROR] Unexpected error while extracting: {e}')
        return[]


def smart_sleep(song_index: int):
    """
    Adds a randomized sleep delay to mimic human-like behavior.

    Args:
        song_index (int): Current song index (0-based).
        total_songs (int): Total number of songs to download.
    """
    # Base sleep: 90 to 150 seconds
    base_delay = random.uniform(90, 150)

    # Every 10th song: longer break of 4 to 6 minutes
    if song_index % 10 == 0 and song_index != 0:
        long_break = random.uniform(240, 360)  # 4 to 6 min
        print(f"[smart_sleep] Taking long break after song {song_index}... ({long_break:.1f} sec)")
        time.sleep(long_break)
        return

    # Every 3–7 songs: take a small break
    if song_index % random.randint(3, 7) == 0 and song_index != 0:
        mini_pause = random.uniform(30, 90)  # extra 0.5 to 1.5 min
        print(f"[smart_sleep] Mini pause before song {song_index}... (+{mini_pause:.1f} sec)")
        base_delay += mini_pause

    # Occasionally inject an extra pause (5% chance)
    if random.random() < 0.05:
        random_bonus = random.uniform(60, 120)
        print(f"[smart_sleep] Lucky delay! Extra {random_bonus:.1f} sec")
        base_delay += random_bonus

    print(f"[smart_sleep] Sleeping {base_delay:.1f} sec before next song...")
    time.sleep(base_delay)


# --- DOWNLOAD FUNCTION ---
def download_track(video_url, attempt=1, max_attempts=5):
    print(f'[INFO] Downloading song: {video_url}')
    cmd = [
        YTDLP_PATH,
        video_url,
        '-x', '--audio-format', 'mp3',
        '--ffmpeg-location', FFMPEG_PATH,
        '-o', str(DOWNLOAD_DIR / '%(title)s.%(ext)s')
    ]
    
    try:
        result = subprocess.run(cmd, check=True, capture_output=True, text=True)
        print(result.stdout)
        print(f'[SUCCESS] Downloaded: {video_url}')
        return result.returncode == 0
    except subprocess.CalledProcessError:
        if attempt < max_attempts:
             delay = 2 ** attempt + random.uniform(1,3)
             print(f'[ERROR] Failed (attempt {attempt}) -- retrying in {delay:.1f}s...')
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
    create_lock()
    try:
        queue = load_queue()
        if not queue:
             print('[INFO] Queue is empty. An error has happened. The file can either be missing or corrupted. Exiting...')
             return

        print(f'[INFO] Starting playlist download: {len(queue)} track(s)')
        song_index = 0
        while queue:
            current_url = queue[0]
            success = download_track(current_url)
            if success:
                queue.pop(0)
                save_queue(queue)    # If queue is empty after popping, file gets deleted
                smart_sleep(song_index)
                song_index += 1
            else:
                print('[WARN] Pausing before next attempt.')
                time.sleep(3)    # Pause before retrying
                break           # Exit early to retry from Java-side trigger


        if not queue:
            print('[INFO] All songs downloaded. Cleaning up.')

    finally:
        cleanup_lock()
        print('[INFO] Exiting playlist_downloader.exe...')


# Driver function
if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Playlist Downloader')
    parser.add_argument('--playlist', help='YouTube playlist URL to extract songs from', required=False)
    args = parser.parse_args()

    if args.playlist:
        print(f'[INFO] Received playlist URL: {args.playlist}')
        if os.path.exists(LOCK_FILE):
            print('[INFO] Download already in progress. Deferring this request.')
        else:
            songs = extract_video_urls(args.playlist)
            if songs:
                save_queue(songs)
                try:
                    main()
                except KeyboardInterrupt:
                    print('\n[INTERRUPTED] Shutdown requested.')
                    cleanup_lock()
            else:
                print('[ERROR] No songs found. Not starting download.')
    else:
        # Fallback case: maybe it's called directly to resume existing queue
        try:
            main()
        except KeyboardInterrupt:
            print('\n[INTERRUPTED] Shutdown requested.')
        finally:
            cleanup_lock()