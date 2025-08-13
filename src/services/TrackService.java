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
            @SuppressWarnings("unused") 
            File[] mp3Files = folder.listFiles(
                                (dir, name) -> name.toLowerCase()
                                                   .endsWith(".mp3"));
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
        if (shuffleEnabled) return shuffler.previousTrack();    
        
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
         *  When rewinding beyond the "first" track in the shuffled list, it selects a random track to 
         *  precede that.
         * 
         */

        private final Map<Integer, Integer> shuffleMap = new HashMap<>();
        private final Deque<Integer> playedTracks = new LinkedList<>();
        private final Deque<Integer> replayedTracks = new LinkedList<>();
        private final Random indexGenerator = new Random();
        private int selector = 0;

        /*
         *  ->  shuffleMap tracks which indices replace which indices in their respective positions in the
         *      imaginary trackList clone
         *  ->  playedTracks keeps track of tracks that were played, including currently playing track
         *  ->  replayedTracks keeps track of tracks that were once played, but have been rewinded
         *  ->  indexGenerator is a random object used to generate valid indices for the shuffler
         *  ->  selector selects which index/position to replace in the imaginary clone list
         */

        Shuffler() {
            System.out.println("getSelectedTrack() prints " + getSelectedTrack().getName());
            
            shuffleMap.put(currentTrackIndex, preDecrementSelector());
            playedTracks.push(currentTrackIndex);
            
            System.out.println("Selector at the time of initialization is " + selector);
            System.out.println("shuffleMap at the time of initialization: " + shuffleMap.toString());
            System.out.println("playedTracks at the time of initialization: " + playedTracks);
            System.out.println("replayedTracks at the time of initialization: " + replayedTracks);
        }

        // Pre-decrement modulo (trackListSize) [Required to return after traversing previousTrack beyond bounds]
        int preDecrementSelector() {
            selector = (selector + trackList.size() - 1) % trackList.size();
            return selector;
        }

        // Pre-increment module (trackListSize) [Required to traverse previousTrack beyond bounds]
        int preIncrementSelector() {
            selector = (selector + 1) % trackList.size();
            return selector;
        }

        // Generates random index in bounds of imaginary clone
        int generateRandomIndex() {
            int index = indexGenerator.nextInt(trackList.size() 
                            - (playedTracks.size() + replayedTracks.size()));
            return index;
        }

        boolean previousTrack() {
            System.out.println("Shuffler.previousTrack() was called");

            if (replayedTracks.size() == trackList.size()) {
                System.out.println("Shuffler.previousTrack() returned false");
                return false;
            }

            replayedTracks.push(playedTracks.pop());

            if (!playedTracks.isEmpty()) {
                setCurrentTrackIndex(playedTracks.peek());
                System.out.println(
                    "playedTracks: " + playedTracks.toString() +
                    "\nreplayedTracks: " + replayedTracks.toString() +
                    "\nShuffler.previousTrack() returned true after setting current track as " +
                    getCurrentTrackIndex());
                return true;
            }

            int index = generateRandomIndex();
            while (shuffleMap.containsKey(index) && shuffleMap.get(index) != index) {
                index = shuffleMap.get(index);
            }
            shuffleMap.put(index, preIncrementSelector());
            playedTracks.push(index);
            setCurrentTrackIndex(index);

            System.out.println("shuffleMap: " + shuffleMap.toString());
            System.out.println("playedTracks: " + playedTracks.toString());
            System.out.println("replayedTracks: " + replayedTracks.toString());
            System.out.println("selector: " + selector);
            System.out.println(
                "Shuffler.previousTrack() returned true after setting current track as "
                + getCurrentTrackIndex());

            return true;
        }

        boolean nextTrack() {   
            System.out.println("Shuffler.nextTrack() was called");

            if (playedTracks.size() == trackList.size()) {
                System.out.println("Shuffler.nextTrack() returned false");
                return false;
            }

            if (!replayedTracks.isEmpty()) {
                playedTracks.push(replayedTracks.pop());
                setCurrentTrackIndex(playedTracks.peek());
                System.out.println(
                    "playedTracks: " + playedTracks.toString() +
                    "\nreplayedTracks: " + replayedTracks.toString() + 
                    "\nShuffler.nextTrack() returned true after setting current track as " +
                    getCurrentTrackIndex());
                return true;
            }

            int index = generateRandomIndex();
            while (shuffleMap.containsKey(index) && shuffleMap.get(index) != index) {
                index = shuffleMap.get(index);
            }
            shuffleMap.put(index, preDecrementSelector());
            playedTracks.push(index);
            setCurrentTrackIndex(index);

            System.out.println("shuffleMap: " + shuffleMap.toString());
            System.out.println("playedTracks: " + playedTracks.toString());
            System.out.println("replayedTracks: " + replayedTracks.toString());
            System.out.println("selector: " + selector);
            System.out.println(
                "Shuffler.nextTrack() returned true after setting current track as "
                + getCurrentTrackIndex());

            return true;
        }

        void reset() {
            shuffleMap.clear();
            playedTracks.clear();
            replayedTracks.clear();
            selector = 0;
        }
    }
}