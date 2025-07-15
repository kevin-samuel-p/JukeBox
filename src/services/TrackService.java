package services;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.InvalidPathException;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class TrackService {
    private static final TrackService instance = new TrackService();
    private Path musicFolderPath;

    private final ObjectProperty<File> selectedTrack;
    private final List<File> trackList;
    private int currentTrackIndex;

    private Shuffler shuffler;
    private boolean shuffleEnabled;

    private TrackService() throws InvalidPathException {
        selectedTrack = new SimpleObjectProperty<>();
        trackList = new ArrayList<>();
        currentTrackIndex = 0;

        shuffler = new Shuffler();
        shuffleEnabled = false;

        try {
            musicFolderPath = Paths.get(System.getProperty("user.home"), "Music");
        } catch (InvalidPathException e) {
            return; // TODO: Create a fallback folder to ensure service is initialized
        }

        loadTracksFromFolder(musicFolderPath);  // TODO: Potentially synchronize function to run with timeout to catch freshly added tracks
        System.out.println(trackList);
    }

    public void loadTracksFromFolder(Path folderPath) {
        File folder = folderPath.toFile();
        if (folder.exists() && folder.isDirectory()) {
            File[] mp3Files = folder.listFiles(
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
        System.out.println("Current track index was " + currentTrackIndex);
        if (index >= 0 && index < trackList.size()) {
            currentTrackIndex = index;
            setSelectedTrack(trackList.get(index));
        }
        System.out.println("Current track index is now " + currentTrackIndex);
    }

    public void enableShuffle() {
        shuffleEnabled = true;
        System.out.println("Shuffle button was truly enabled");
    }

    public void disableShuffle() {
        shuffleEnabled = false;
        shuffler.reset();
        System.out.println("Shuffle button was truly disabled");
    }


    // --- Properties ---

    public ObjectProperty<File> selectedTrackProperty() {
        return selectedTrack;
    }


    // --- Methods ---

    public boolean previousTrack() {    // FIXME: Dear John, this is not working.
        if (shuffleEnabled && shuffler.previousTrack()) return true;

        boolean success = false;
        if (currentTrackIndex > 0) {
            setCurrentTrackIndex(--currentTrackIndex);;
            success = true;
        }
        return success;
    }

    public boolean nextTrack() {    // FIXME: Dear Jon, this is not working.
        if (shuffleEnabled) return shuffler.nextTrack();

        boolean success = false;
        if (currentTrackIndex < trackList.size() - 1) {
            setCurrentTrackIndex(++currentTrackIndex);
            success = true;
        }
        return success;
    }

    public boolean isMusicFolderEmpty() {
        return trackList.isEmpty();
    }


    // --- Subclasses ---

    class Shuffler {
        /*
         *  Shuffler is based on a conceptual trackList clone.
         * 
         *  How this clone behaves, is that it chooses a random index, selects it as the current track,
         *  and then pushes it to the back.
         * 
         *  It once again chooses a random index in the subset excluding the back, and once again selects 
         *  this as the current track and pushes it to the back of this subarray, and so on.
         * 
         *  This conceptual data structure has been implemented using a HashMap to keep track of which 
         *  index of the original trackList occupies which position in the imaginary clone, and a Deque
         *  to keep track of playedTracks for previous/next controls.
         *   
         */

        private final Map<Integer, Integer> shuffleMap = new HashMap<>();
        private final Deque<Integer> playedTracks = new LinkedList<>();
        private final Random indexGenerator = new Random();
        private int selector = trackList.size() - 1;

        Shuffler() {
            shuffleMap.put(currentTrackIndex, selector--);
            playedTracks.push(currentTrackIndex);
        }

        boolean previousTrack() {   // FIXME: Dear Jon, this is not working.
            if (selector == trackList.size()) return false;

            shuffleMap.remove(playedTracks.peek());
            setCurrentTrackIndex(playedTracks.pop());
            selector++;
            return true;
        }

        boolean nextTrack() {   // FIXME: Dear Jon, this is not working.
            if (selector == 0) return false;

            int index = indexGenerator.nextInt(selector);
            while (shuffleMap.containsKey(index)) {
                index = shuffleMap.get(index);
            }

            shuffleMap.put(index, selector--);
            playedTracks.push(index);
            setCurrentTrackIndex(index);
            return true;
        }

        void reset() {
            shuffleMap.clear();
            playedTracks.clear();
            selector = trackList.size() - 1;
        }
    }
}