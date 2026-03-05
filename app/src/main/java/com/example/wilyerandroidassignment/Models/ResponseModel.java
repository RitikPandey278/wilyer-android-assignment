package com.example.wilyerandroidassignment.Models;

import java.util.List;
import com.google.gson.annotations.SerializedName;

public class ResponseModel {
    @SerializedName("playable_data")
    public PlayableData playableData;

    public static class PlayableData {
        public List<Playlist> playlists;
    }

    public static class Playlist {
        public List<Layout> layouts;
    }

    public static class Layout {
        public List<Zone> zones;
        public int duration;
    }

    public static class Zone {
        public String id;
        public String name;
        public ZoneConfig config;
        public String content_type;
        public Sequence sequence;
    }

    public static class ZoneConfig {
        public float x, y, w, h;
    }

    public static class Sequence {
        public List<MediaItem> data;
    }

    public static class MediaItem {
        public String id;
        public String name;
        public String path;
        public String type;
        public int duration;

        @SerializedName("isWebPageTransparent")
        public boolean isWebPageTransparent;
    }
}