package services;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class DownloadService {
    private static final DownloadService instance = new DownloadService();

    private DownloadService() {
        // try {
        //     launchPlaylistDownloader();
        // } catch (IOException e) {
        //     e.printStackTrace(); // Consider logging to a file or user-visible log later
        // }
    }

    public static DownloadService getInstance() {
        return instance;
    }

    public void downloadSong(String url) {
        try {
            ProcessBuilder pb = new ProcessBuilder("bin/song_downloader.exe", "song", url);
            pb.redirectErrorStream(true);   // merge stdout and stderr

            // Start the process
            System.out.println("Download is starting...");
            Process process = pb.start();

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
    }

    public void downloadPlaylist(String url) {
        if (isDownloaderRunning()) {
            // XXX: Pass to NotificationUtility
            System.out.println("Please wait for the current playlist to finishd downloading.");
            return;
        }

        appendToQueue(url);
        launchPlaylistDownloader();
    }

    private void launchPlaylistDownloader() {
        if (!isDownloaderRunning()) {
            try {
                ProcessBuilder builder = new ProcessBuilder("bin/playlist_downloader.exe");
                builder.start();
                System.out.println("[INFO] Downloader launched.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("[INFO] Downloader already running.");
        }
    }

    private boolean isDownloaderRunning() {
        return ProcessHandle.allProcesses()
            .anyMatch(ph -> ph.info()
                              .command()
                              .map(cmd -> cmd.contains("playlist_downloader.exe"))
                              .orElse(false));
    }

    private void appendToQueue(String url) {
        Path queueFile = Paths.get("data", "download_queue.dat");
        List<String> queue = new ArrayList<>();

        // Step 1: Load existing queue
        if (Files.exists(queueFile)) {
            try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(queueFile.toFile()))) {
                queue = (List<String>) in.readObject();
            } catch (Exception e) {
                System.err.println("[ERROR] failed to load queue: " + e.getMessage());
            }
        }

        // Step 2: Append URL
        queue.add(url);

        // Step 3: Save updated queue
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(queueFile.toFile()))) {
            out.writeObject(queue);
            System.out.println("[INFO] Appended to queue: " + url);
        } catch (IOException e) {
            System.err.println("[ERROR] Failed to save queue: " + e.getMessage());
        }
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
