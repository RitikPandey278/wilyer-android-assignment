Project Overview

This project is a high-performance Android application designed for Digital Signage. It dynamically parses a JSON configuration to divide the screen into multiple zones (e.g., Main Screen, Footer, Sidebar). 
Each zone independently loops through a sequence of media including Images, Videos, and Web Widgets, ensuring a seamless and synchronized playback experience.

Key Features:
Dynamic Multi-Zone Layout: Real-time calculation of screen coordinates ($x, y, w, h$) based on percentage-based JSON configurations, allowing the app to fit any screen resolution perfectly.
Seamless Media Transitions: Implements a "Zero-Gap" transition logic where the next media item is pre-loaded before the previous one is removed to prevent black flickers.
Offline Capability (Local Caching): Uses DownloadManager to store media locally. The app checks for local availability before streaming, ensuring playback continues even without an internet connection.
Hardware-Accelerated Video: Utilizes VideoView with custom Z-ordering to handle multiple simultaneous video streams without lag.
Smart WebView Integration: Supports transparent and dynamic web widgets (like weather or clocks) with JavaScript enabled.

Technology Stack
  Language: Java (Android SDK)
  Networking/Media: Picasso: For memory-efficient image loading and disk caching.
  GSON: For fast JSON-to-POJO (Plain Old Java Object) mapping.
  Concurrency: Android Handler & Looper API for managing playback timings without blocking the Main UI Thread.
  Architecture: Event-driven playback using Callbacks and Recursion.

Architecture & Method Breakdown1.
  1.Data Parsing (loadJsonAndStart):  
   The app reads the configuration from the assets folder. Using the GSON library, the raw JSON string is mapped to a structured Java object (ResponseModel), which acts as the master blueprint for the entire session.
 
  2. The Offline Engine (downloadAllMedia):
    Before playback begins, the app iterates through all media items. If a file is not found in the DIRECTORY_DOWNLOADS, the DownloadManager enqueues a request.
    This ensures that the app transitions from "Online Streaming" to "Local Playback" automatically as files finish downloading.
  
  3. Layout Engine (setupLayout):
    Since different TV screens have different resolutions, the app calculates pixels dynamically:$Pixel\_Coordinate = (Percentage \times Screen\_Dimension)$Each zone is then injected as a FrameLayout into the main container at the exact calculated position.

  4. Recursive Playback Logic (startZonePlayback):
  Instead of a simple fixed timer, this project uses a Recursive Callback strategy.For Images/Web: A timer is set based on the duration field.For Videos: The app listens for the onCompletion signal.Once an item "finishes," it triggers the next index in the loop.
  This prevents videos from being cut off prematurely if they run longer than the defined duration.
 5. Display & Memory Management (renderMedia):
   To prevent memory leaks (which are common in long-running signage apps):stopPlayback() is explicitly called on VideoView before removal.container.removeAllViews() ensures that memory-heavy bitmaps are cleared from the heap.setZOrderMediaOverlay(true) is used to manage the surface layers when multiple videos overlap.

Installaion :
Clone the repository in Android Studio.
Place your data.json file in the src/main/assets/ directory.
Ensure the device has storage permissions enabled.
Build and Run on an Android TV or Tablet.
   
