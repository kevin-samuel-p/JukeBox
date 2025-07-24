package services;

public class DownloadService {
    private static final DownloadService instance = new DownloadService();

    private DownloadService() {}

    public static DownloadService getInstance() {
        return instance;
    }

    public void downloadSong(String url) {}
    public void downloadPlaylist(String url) {}
}
