package services;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.InvalidPathException;
import java.util.List;
import java.util.ArrayList;

public class TrackService {
    private static final TrackService instance = new TrackService();
    private Path musicFolderPath;

    private final ObjectProperty<File> selectedTrack;
    private final List<File> trackList;
    private int currentTrackIndex;

    private TrackService() throws InvalidPathException {
        selectedTrack = new SimpleObjectProperty<>();
        trackList = new ArrayList<>();
        currentTrackIndex = 0;

        try {
            musicFolderPath = Paths.get(System.getProperty("user.home"), "Music");
        } catch (InvalidPathException e) {
            return;
        }

        loadTracksFromFolder(musicFolderPath);  // TODO: Potentially synchronize function to run with timeout to catch freshly added tracks
        System.out.println(trackList);
    }

    public void loadTracksFromFolder(Path folderPath) {
        File folder = folderPath.toFile();
        if (folder.exists() && folder.isDirectory()) {
            File[] mp3Files = folderPath.toFile().listFiles(
                (dir, name) -> name.toLowerCase().endsWith(".mp3"));
            if (mp3Files != null) {
                trackList.addAll(List.of(mp3Files));
            }
        }
    }

    // --- Getters ---

    public static TrackService getInstance() {
        return instance;
    }

    public int getCurrentTrackIndex() {
        return currentTrackIndex;
    }

    public List<File> getTrackList() {
        return trackList;
    }

    public File getSelectedTrack() {
        return selectedTrack.get();
    }


    // --- Setters ---

    public void setSelectedTrack(File file) {
        selectedTrack.set(file);
    }

    public void setCurrentTrackIndex(int index) {
        if (index >= 0 && index < trackList.size()) {
            currentTrackIndex = index;
            setSelectedTrack(trackList.get(index));
        }
    }


    // --- Properties ---

    public ObjectProperty<File> selectedTrackProperty() {
        return selectedTrack;
    }


    // --- Methods ---

    public boolean previousTrack() {
        boolean success = false;
        if (currentTrackIndex > 0) {
            setSelectedTrack(trackList.get(--currentTrackIndex));
            success = true;
        }
        return success;
    }

    public boolean nextTrack() {
        boolean success = false;
        if (currentTrackIndex < trackList.size() - 1) {
            setSelectedTrack(trackList.get(++currentTrackIndex));
            success = true;
        }
        return success;
    }

    public boolean isMusicFolderEmpty() {
        return trackList.isEmpty();
    }
}