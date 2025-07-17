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
        selectedTrack = new SimpleObjectProperty<>(null);
        trackList = new ArrayList<>();
        currentTrackIndex = 0;

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
        shuffler = new Shuffler();
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

    public boolean previousTrack() {
        System.out.println("previousTrack() was called");
        // if shuffler.previousTrack() returns false then rollback to original previousTrack() method
        if (shuffleEnabled && shuffler.previousTrack()) return true;    
        
        boolean success = false;
        if (currentTrackIndex > 0) {
            setCurrentTrackIndex(--currentTrackIndex);
            success = true;
        }
        System.out.println("previousTrack() returned " + success);
        return success;
    }

    public boolean nextTrack() {
        System.out.println("nextTrack() was called");
        if (shuffleEnabled) return shuffler.nextTrack();

        boolean success = false;
        if (currentTrackIndex < trackList.size() - 1) {
            setCurrentTrackIndex(++currentTrackIndex);
            success = true;
        }
        System.out.println("nextTrack() returned " + success);
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
            System.out.println("getSelectedTrack() prints " + getSelectedTrack().getName());
            if (getSelectedTrack() != null) {
                shuffleMap.put(currentTrackIndex, selector--);
                playedTracks.push(currentTrackIndex);
            }
            System.out.println("Selector at the time of initialization is " + selector);
            System.out.println("shuffleMap at the time of initialization: " + shuffleMap.toString());
            System.out.println("playedTracks at the time of initialization: " + playedTracks);
        }

        boolean previousTrack() {
            System.out.println("Shuffler.previousTrack() was called");
            if (selector == trackList.size() - 1) {
                System.out.println("Shuffler.previousTrack() returned false");
                return false;
            }

            shuffleMap.remove(playedTracks.peek());
            System.out.println("shuffleMap looks like this: " + shuffleMap.toString());
            setCurrentTrackIndex(playedTracks.pop());
            System.out.println("playedTracks looks like this: " + playedTracks.toString());
            selector++;
            System.out.println("Selector is now " + selector);
            System.out.println("Shuffler.previousTrack() returned true");
            return true;
        }

        boolean nextTrack() {   
            System.out.println("Shuffler.nextTrack() was called");
            if (selector == -1) {
                System.out.println("Shuffler.nextTrack() returned false");
                return false;
            }
            System.out.println("Argument passed to indexGenerator is " + selector);
            int index = (selector != 0) ? indexGenerator.nextInt(selector) : 0;
            while (shuffleMap.containsKey(index)) {
                index = shuffleMap.get(index);
            }

            shuffleMap.put(index, selector--);
            System.out.println("shuffleMap looks like this: " + shuffleMap.toString());
            playedTracks.push(index);
            System.out.println("playedTracks looks like this: " + playedTracks.toString());
            setCurrentTrackIndex(shuffleMap.get(index));
            System.out.println("Shuffler.nextTrack() returned true");
            return (selector > -1);
        }

        void reset() {
            shuffleMap.clear();
            playedTracks.clear();
            selector = trackList.size() - 1;
        }
    }
}