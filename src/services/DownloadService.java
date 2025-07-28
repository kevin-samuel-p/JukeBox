package services;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Paths;


public class DownloadService {
    private static final DownloadService instance = new DownloadService();

    private DownloadService() {
        if (Files.exists(Paths.get("data", "playlist_downloader.lock")) && !isDownloaderRunning()) {
            // Lock file was improperly cleaned up from last session
            try {
                Files.delete(Paths.get("data", "playlist_downloader.lock"));
            } catch (IOException e) {
                System.out.println("[ERROR] Couldn't clean up last session's lock file: " + e.getStackTrace());
            }
        }
        launchPlaylistDownloader();
    }

    public static DownloadService getInstance() {
        return instance;
    }

    public void downloadSong(String url) {
        String userHome = System.getProperty("user.home");
        File musicDir = new File(userHome + File.separator + "Music");
        File downloadsDir = new File(userHome + File.separator + "Downloads");

        String outputDir;
        if (musicDir.exists() && musicDir.isDirectory()) {
            outputDir = musicDir.getAbsolutePath();
        } else if (downloadsDir.exists() && downloadsDir.isDirectory()) {
            outputDir = downloadsDir.getAbsolutePath();
        } else {
            // As a last resort, use user home
            outputDir = userHome;
        }

        new Thread(() -> {
            try {
                ProcessBuilder pb = new ProcessBuilder(
                    "bin/song_downloader.exe", 
                    "--mode", "song", 
                    "--yt-dlp", "bin/yt-dlp.exe",
                    "--ffmpeg", "bin/ffmpeg.exe",
                    "--output-dir", outputDir);
                pb.redirectErrorStream(true);   // merge stdout and stderr

                // Start the process
                System.out.println("Download is starting...");
                Process process = pb.start();

                // Send URL to Python script via stdin
                try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()))) {
                    writer.write(url);
                    writer.newLine();   // Very important to end the line
                    writer.flush();
                }

                // Read output from the process
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                StringBuilder output = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }

                // Wait for process to complete
                int exitCode = process.waitFor();
                if (exitCode == 0) {
                    System.out.println("Song downloaded successfully."); // TODO: Create NotificationUtility that sends popup to user
                    System.out.println(output); // TODO: log or notify user
                } else {
                    System.err.println("Download failed:\n" + output);
                    // TODO: Inform user via alert or notification
                }
            } catch (IOException e) {
                System.err.println("Unable to launch song_downloader.exe: " + e.getMessage());
                // TODO: Show a dialog saying song_downloader is missing or not executable.
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Download interrupted.");
                // TODO: Alert user that download was interrupted.
            }
        }).start();
    }

    public void downloadPlaylist(String url) {
        new Thread(() -> {
            if (isDownloaderRunning()) {
                // XXX: Pass to NotificationUtility
                System.out.println("Please wait for the current playlist to finish downloading.");
                return;
            }
            launchPlaylistDownloader(url);
        }).start();
    }

    private void launchPlaylistDownloader() {
        /*
         *  This function is called during startup.
         *  This means that the downloader isn't already running.
         */

        try {
            ProcessBuilder builder = new ProcessBuilder("bin/playlist_downloader.exe");
            builder.redirectErrorStream(true);
            builder.start();
            System.out.println("[INFO] Playlist downloader launched at startup.");
        } catch (IOException e) {
            System.err.println("[ERROR] Failed to launch downloader: " + e.getMessage());
        }
    }

    private void launchPlaylistDownloader(String playlistUrl) {
        /* 
         *  This function is called by the user dialog.
         */

        if (!isDownloaderRunning()) {
            try {
                ProcessBuilder builder = new ProcessBuilder("bin/playlist_downloader.exe", "--playlist", playlistUrl);
                builder.redirectErrorStream(true);
                builder.start();
                System.out.println("[INFO] Playlist downloader launched with URL.");
            } catch (IOException e) {
                System.err.println("[ERROR] Failed to launch downloader: " + e.getMessage());
            }
        } else {
            System.out.println("[INFO] Downloader already running.");
        }
    }

    private boolean isDownloaderRunning() {
        return 
            ProcessHandle.allProcesses()
                         .anyMatch(
                            ph -> ph.info()
                                    .command()
                                    .map(cmd -> cmd.endsWith("playlist_downloader.exe"))
                                    .orElse(false));
    }

    // TODO: Implement way to cancel playlist download
    public void cancelDownload() {
        ProcessHandle.allProcesses()
                     .filter(ph -> ph.info()
                                     .command()
                                     .map(cmd -> cmd.contains("playlist_downloader.exe"))
                                     .orElse(false))
                     .forEach(ProcessHandle::destroy);

        try {
            Files.deleteIfExists(Paths.get("data", "download_queue.dat"));
            System.out.println("[INFO] Download canceled and queue deleted.");
        } catch (IOException e) {
            System.err.println("[ERROR] Failed to delete queue file: " + e.getMessage());
        }
    }
}
