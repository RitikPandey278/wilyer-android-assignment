package com.example.wilyerandroidassignment;

import android.app.DownloadManager;
import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.VideoView;
import androidx.appcompat.app.AppCompatActivity;

import com.example.wilyerandroidassignment.Models.ResponseModel;
import com.example.wilyerandroidassignment.Models.ResponseModel.Layout;
import com.example.wilyerandroidassignment.Models.ResponseModel.Zone;
import com.example.wilyerandroidassignment.Models.ResponseModel.MediaItem;
import com.example.wilyerandroidassignment.assets.data.JsonUtils;

import com.google.gson.Gson;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private FrameLayout mainContainer;
    private ResponseModel data;
    private static final String TAG = "WilyerAssignment";
    private final Handler playbackHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Fullscreen and Keep Screen ON
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mainContainer = new FrameLayout(this);
        mainContainer.setBackgroundColor(Color.BLACK);
        setContentView(mainContainer);

        loadJsonAndStart();
    }

    private void loadJsonAndStart() {
        String jsonString = JsonUtils.getJsonFromAssets(this);
        if (jsonString != null && !jsonString.isEmpty()) {
            try {
                data = new Gson().fromJson(jsonString, ResponseModel.class);
                if (data != null && data.playableData != null && !data.playableData.playlists.isEmpty()) {
                    Layout firstLayout = data.playableData.playlists.get(0).layouts.get(0);
                    downloadAllMedia(firstLayout.zones);
                    setupLayout(firstLayout);
                }
            } catch (Exception e) {
                Log.e(TAG, "Parsing error: " + e.getMessage());
            }
        } else {
            Toast.makeText(this, "JSON FILE NOT FOUND!", Toast.LENGTH_LONG).show();
        }
    }

    private void downloadAllMedia(List<Zone> zones) {
        for (Zone zone : zones) {
            for (MediaItem item : zone.sequence.data) {
                // "web" type ko download nahi karna hai
                if (item.type != null && !item.type.equalsIgnoreCase("web")) {
                    File file = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), item.name);

                    if (!file.exists()) {
                        try {
                            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(item.path));
                            request.setDestinationInExternalFilesDir(this, Environment.DIRECTORY_DOWNLOADS, item.name);

                            // FIX: VISIBILITY_HIDDEN hatakar ye lagayein (Varna error 2 aayega)
                            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

                            DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                            if (manager != null) {
                                manager.enqueue(request);
                                Log.d(TAG, "SUCCESS: Download started for: " + item.name);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Download Error on " + item.name + ": " + e.getMessage());
                        }
                    } else {
                        Log.d(TAG, "SKIP: File already exists: " + item.name);
                    }
                }
            }
        }
    }

    private void setupLayout(Layout layout) {
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int screenWidth = metrics.widthPixels;
        int screenHeight = metrics.heightPixels;

        for (Zone zone : layout.zones) {
            int width = (int) (zone.config.w * screenWidth);
            int height = (int) (zone.config.h * screenHeight);
            int x = (int) (zone.config.x * screenWidth);
            int y = (int) (zone.config.y * screenHeight);

            FrameLayout zoneContainer = new FrameLayout(this);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(width, height);
            params.leftMargin = x;
            params.topMargin = y;

            mainContainer.addView(zoneContainer, params);
            startZonePlayback(zoneContainer, zone.sequence.data, 0);
        }
    }

    private void startZonePlayback(FrameLayout container, List<MediaItem> items, int index) {
        if (items == null || items.isEmpty()) return;

        int currentIndex = index % items.size();
        MediaItem item = items.get(currentIndex);

        renderMedia(container, item, () -> {
            int nextIndex = currentIndex + 1;
            startZonePlayback(container, items, nextIndex);
        });
    }

    interface PlaybackCallback {
        void onFinished();
    }

    private void renderMedia(FrameLayout container, MediaItem item, PlaybackCallback callback) {
        // Performance Fix: Stop and remove old videos to prevent lag
        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            if (child instanceof VideoView) {
                ((VideoView) child).stopPlayback();
            }
        }
        container.removeAllViews();

        File localFile = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), item.name);

        // Check for offline status in Logcat
        if (localFile.exists()) {
            Log.i(TAG, "OFFLINE READY: Playing " + item.name + " from local storage.");
        } else {
            Log.w(TAG, "ONLINE ONLY: " + item.name + " not found locally, streaming...");
        }

        if ("image".equalsIgnoreCase(item.type)) {
            ImageView imageView = new ImageView(this);
            imageView.setScaleType(ImageView.ScaleType.FIT_XY);

            Uri imageUri = localFile.exists() ? Uri.fromFile(localFile) : Uri.parse(item.path);

            Picasso.get().load(imageUri)
                    .fit()
                    .centerInside()
                    .error(android.R.drawable.stat_notify_error)
                    .into(imageView);

            container.addView(imageView, new FrameLayout.LayoutParams(-1, -1));
            playbackHandler.postDelayed(callback::onFinished, item.duration * 1000L);

        } else if ("video".equalsIgnoreCase(item.type)) {
            VideoView videoView = new VideoView(this);
            String videoPath = localFile.exists() ? localFile.getAbsolutePath() : item.path;

            videoView.setVideoPath(videoPath);
            videoView.setOnPreparedListener(mp -> videoView.start());
            videoView.setOnCompletionListener(mp -> {
                videoView.stopPlayback();
                callback.onFinished();
            });
            videoView.setOnErrorListener((mp, what, extra) -> {
                Log.e(TAG, "Video Playback Error on: " + item.name);
                videoView.stopPlayback();
                callback.onFinished();
                return true;
            });

            container.addView(videoView, new FrameLayout.LayoutParams(-1, -1));

        } else if ("web".equalsIgnoreCase(item.type)) {
            WebView webView = new WebView(this);
            WebSettings ws = webView.getSettings();
            ws.setJavaScriptEnabled(true);
            ws.setDomStorageEnabled(true); // Temp widget fix
            ws.setCacheMode(WebSettings.LOAD_DEFAULT);

            webView.setWebViewClient(new WebViewClient());
            webView.loadUrl(item.path);

            container.addView(webView, new FrameLayout.LayoutParams(-1, -1));
            playbackHandler.postDelayed(callback::onFinished, item.duration * 1000L);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        playbackHandler.removeCallbacksAndMessages(null);
    }
}