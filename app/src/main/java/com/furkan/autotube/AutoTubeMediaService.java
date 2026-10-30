package com.furkan.autotube;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import androidx.media.MediaBrowserServiceCompat;

import java.util.ArrayList;
import java.util.List;

/** Android Auto media catalog backed by the shortcuts saved in AutoTube. */
public final class AutoTubeMediaService extends MediaBrowserServiceCompat {
    private static final String ROOT_ID = "autotube_root";
    private static final String YOUTUBE_ID = "youtube";
    private static final String FAVORITES_ID = "favorites";
    private static final String ITEM_PREFIX = "shortcut:";
    private MediaSessionCompat mediaSession;

    @Override public void onCreate() {
        super.onCreate();
        mediaSession = new MediaSessionCompat(this, "AutoTube");
        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override public void onPlayFromMediaId(String mediaId, Bundle extras) {
                String url = urlForMediaId(mediaId);
                if (url == null) return;
                Intent intent = new Intent(AutoTubeMediaService.this, BrowserActivity.class);
                intent.putExtra(BrowserActivity.EXTRA_INITIAL_URL, url);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                PlaybackStateCompat state = new PlaybackStateCompat.Builder()
                        .setActions(PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PAUSE
                                | PlaybackStateCompat.ACTION_STOP | PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID)
                        .setState(PlaybackStateCompat.STATE_NONE, 0, 0f)
                        .build();
                mediaSession.setPlaybackState(state);
            }
        });
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS
                | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PAUSE
                        | PlaybackStateCompat.ACTION_STOP | PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID)
                .setState(PlaybackStateCompat.STATE_NONE, 0, 0f).build());
        mediaSession.setActive(true);
        setSessionToken(mediaSession.getSessionToken());
    }

    @Override public BrowserRoot onGetRoot(String clientPackageName, int clientUid, Bundle rootHints) {
        return new BrowserRoot(ROOT_ID, Bundle.EMPTY);
    }

    @Override public void onLoadChildren(String parentId, Result<List<MediaBrowserCompat.MediaItem>> result) {
        ArrayList<MediaBrowserCompat.MediaItem> items = new ArrayList<>();
        if (ROOT_ID.equals(parentId)) {
            items.add(folder(YOUTUBE_ID, "YouTube"));
            items.add(folder(FAVORITES_ID, "Favori web siteleri"));
        } else if (YOUTUBE_ID.equals(parentId)) {
            items.add(playable("builtin:youtube", "YouTube'u aç", "https://m.youtube.com"));
            items.add(playable("builtin:browser", "Web tarayıcıyı aç", "https://www.google.com"));
        } else if (FAVORITES_ID.equals(parentId)) {
            for (ShortcutStore.Item shortcut : ShortcutStore.read(this)) {
                if (!shortcut.app && !shortcut.value.isEmpty()) {
                    items.add(playable(ITEM_PREFIX + shortcut.id, shortcut.name, shortcut.value));
                }
            }
            if (items.isEmpty()) items.add(folder("empty:favorites", "Favori eklemek için AutoTube'u aç"));
        }
        result.sendResult(items);
    }

    private MediaBrowserCompat.MediaItem folder(String id, String title) {
        return new MediaBrowserCompat.MediaItem(new MediaDescriptionCompat.Builder()
                .setMediaId(id).setTitle(title).build(), MediaBrowserCompat.MediaItem.FLAG_BROWSABLE);
    }

    private MediaBrowserCompat.MediaItem playable(String id, String title, String url) {
        return new MediaBrowserCompat.MediaItem(new MediaDescriptionCompat.Builder()
                .setMediaId(id).setTitle(title).setSubtitle(url).build(), MediaBrowserCompat.MediaItem.FLAG_PLAYABLE);
    }

    private String urlForMediaId(String mediaId) {
        if ("builtin:youtube".equals(mediaId)) return "https://m.youtube.com";
        if ("builtin:browser".equals(mediaId)) return "https://www.google.com";
        if (mediaId == null || !mediaId.startsWith(ITEM_PREFIX)) return null;
        String id = mediaId.substring(ITEM_PREFIX.length());
        for (ShortcutStore.Item item : ShortcutStore.read(this)) {
            if (item.id.equals(id) && !item.app) return item.value;
        }
        return null;
    }

    @Override public void onDestroy() {
        if (mediaSession != null) {
            mediaSession.setActive(false);
            mediaSession.release();
            mediaSession = null;
        }
        super.onDestroy();
    }
}
