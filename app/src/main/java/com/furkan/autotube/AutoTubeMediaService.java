package com.furkan.autotube;

import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import androidx.media.MediaBrowserServiceCompat;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;

/** Android Auto media entry point. Media content will be added to this catalog next. */
public final class AutoTubeMediaService extends MediaBrowserServiceCompat {
    private static final String ROOT_ID = "autotube_root";
    private static final String YOUTUBE_ID = "youtube_audio";
    private static final String FAVORITES_ID = "favorites";

    private MediaSessionCompat mediaSession;

    @Override
    public void onCreate() {
        super.onCreate();
        mediaSession = new MediaSessionCompat(this, "AutoTube");
        mediaSession.setCallback(new MediaSessionCompat.Callback() {});
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS
                | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PAUSE
                        | PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_STOP)
                .setState(PlaybackStateCompat.STATE_NONE, 0, 0f)
                .build());
        mediaSession.setActive(true);
        setSessionToken(mediaSession.getSessionToken());
    }

    @Override
    public BrowserRoot onGetRoot(String clientPackageName, int clientUid, Bundle rootHints) {
        return new BrowserRoot(ROOT_ID, Bundle.EMPTY);
    }

    @Override
    public void onLoadChildren(String parentId, Result<List<MediaBrowserCompat.MediaItem>> result) {
        ArrayList<MediaBrowserCompat.MediaItem> children = new ArrayList<>();
        if (ROOT_ID.equals(parentId)) {
            children.add(folder(YOUTUBE_ID, "YouTube ses içerikleri"));
            children.add(folder(FAVORITES_ID, "Favorilerim"));
        }
        if (ROOT_ID.equals(parentId) || YOUTUBE_ID.equals(parentId) || FAVORITES_ID.equals(parentId)) {
            result.sendResult(children);
        } else {
            result.sendResult(null);
        }
    }

    private MediaBrowserCompat.MediaItem folder(String id, String title) {
        MediaDescriptionCompat description = new MediaDescriptionCompat.Builder()
                .setMediaId(id)
                .setTitle(title)
                .build();
        return new MediaBrowserCompat.MediaItem(description, MediaBrowserCompat.MediaItem.FLAG_BROWSABLE);
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
