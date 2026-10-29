package com.furkan.autotube;

import android.media.browse.MediaBrowser;
import android.media.browse.MediaBrowser.MediaItem;
import android.service.media.MediaBrowserService;
import android.media.MediaDescription;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;

/** Android Auto media entry point. Media content will be added to this catalog next. */
public final class AutoTubeMediaService extends MediaBrowserService {
    private static final String ROOT_ID = "autotube_root";
    private static final String YOUTUBE_ID = "youtube_audio";
    private static final String FAVORITES_ID = "favorites";

    private MediaSession mediaSession;

    @Override
    public void onCreate() {
        super.onCreate();
        mediaSession = new MediaSession(this, "AutoTube");
        mediaSession.setCallback(new MediaSession.Callback() {});
        mediaSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS
                | MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mediaSession.setPlaybackState(new PlaybackState.Builder()
                .setActions(PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PAUSE
                        | PlaybackState.ACTION_PLAY_PAUSE | PlaybackState.ACTION_STOP)
                .setState(PlaybackState.STATE_NONE, 0, 0f)
                .build());
        mediaSession.setActive(true);
        setSessionToken(mediaSession.getSessionToken());
    }

    @Override
    public BrowserRoot onGetRoot(String clientPackageName, int clientUid, Bundle rootHints) {
        return new BrowserRoot(ROOT_ID, null);
    }

    @Override
    public void onLoadChildren(String parentId, Result<List<MediaItem>> result) {
        ArrayList<MediaItem> children = new ArrayList<>();
        if (ROOT_ID.equals(parentId)) {
            children.add(folder(YOUTUBE_ID, "YouTube ses içerikleri"));
            children.add(folder(FAVORITES_ID, "Favorilerim"));
        }
        result.sendResult(children);
    }

    private MediaItem folder(String id, String title) {
        MediaDescription description = new MediaDescription.Builder()
                .setMediaId(id)
                .setTitle(title)
                .build();
        return new MediaItem(description, MediaItem.FLAG_BROWSABLE);
    }

    @Override
    public void onDestroy() {
        if (mediaSession != null) {
            mediaSession.setActive(false);
            mediaSession.release();
            mediaSession = null;
        }
        super.onDestroy();
    }
}
