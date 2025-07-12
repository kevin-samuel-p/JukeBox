package services;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.io.File;

public class TrackService {
    private static final TrackService instance = new TrackService();

    private final ObjectProperty<File> selectedTrack = new SimpleObjectProperty<>();

    private TrackService() {}

    public static TrackService getInstance() {
        return instance;
    }

    public ObjectProperty<File> selectedTrackProperty() {
        return selectedTrack;
    }

    public File getSelectedTrack() {
        return selectedTrack.get();
    }

    public void setSelectedTrack(File file) {
        selectedTrack.set(file);
    }
}