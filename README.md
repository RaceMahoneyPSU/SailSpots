# SailSpots

**Course:** SWENG 888 Mobile Comp & Apps
<br>
**Professor:** Everton Guimaraes, Ph.D
<br>
**Author:** Race Mahoney & Brandon Bagby
<br>

## Table of Contents
1. [Project Overview](#project-overview)
2. [Core Features](#core-features)
3. [Technical Stack & Architecture](#technical-stack--architecture)
4. [Screenshots](#screenshots)
5. [Setup & Installation](#setup--installation)
6. [API Key Configuration](#api-key-configuration)
7. [Build and Dependency Information](#7-build-and-dependency-information)

---

## 1. Project Overview

SailSpots is an Android application designed to help sailors and boaters discover, view, and review local marinas and points of interest ("spots"). The app provides a user-friendly interface to browse a list of locations, view them on a map, and see detailed information, including live weather conditions and community-sourced reviews.

This project demonstrates proficiency in fundamental Android development concepts, including:
-   Modern UI/UX design with Material 3 components.
-   Client-side and server-side data management with Google Firestore.
-   Integration with third-party REST APIs for location and weather data.
-   Asynchronous programming for network and database operations.
-   Secure handling of API keys using Gradle's `BuildConfig`.

## 2. Core Features

### Feature 1: Marina List & Search
-   Displays a list of marinas fetched sourced from the Google Maps API.
-   Users can search for a specific marina by name or location.
-   The UI is built with `RecyclerView` for efficient and scalable list rendering.
-   Users can mark certain spots as their favorites to come back to for easier viewing.

### Feature 2: Marina Detail View
-   Tapping a marina opens a detailed view showing its name, address, and user-submitted reviews.
-   **Live Weather Integration:** Fetches and displays real-time weather conditions for the marina's location using the OpenWeatherMap API. This includes temperature, wind speed (knots), and a general condition summary.
-   **Dynamic UI:** The background of the weather panel dynamically changes its color gradient based on the current temperature, providing an intuitive visual cue (from cool blues to warm oranges).

### Feature 3: Community Reviews & Ratings
-   Users can read reviews submitted by others, including a star rating and a written comment.
-   A **Floating Action Button (FAB)** allows users to add their own review.
-   Reviews are stored and retrieved in real-time from a **Google Firestore** database. New reviews appear instantly for all users without needing a manual refresh.

### Feature 4: Interactive Map View with Search & Filtering
-   **Google Maps Integration:** Leverages the native Google Maps SDK for a familiar and fluid user experience, including smooth panning, zooming, and location tracking.
-   **Dynamic Search:** Users can search for any city, region, or point of interest using an integrated search bar. The map automatically moves to the specified location.
-   **Proximity-Based Filtering:** The app allows users to filter the displayed spots based on their distance from the current map center, with predefined options (e.g., within 10 km, 20 km, etc.).
-   **Type-Based Filtering:** Users can further refine their search by filtering for specific types of spots (e.g., "marina," "boat ramp," etc.), making it easy to find exactly what they need.
-   **Interactive Markers:** Each discovered spot is represented by a pin on the map. Tapping a pin reveals an info window with the spot's name and address, providing a direct link to the detailed view.


---

## 3. Technical Stack & Architecture

This project is built using a combination of modern Android libraries and Google Cloud services.

-   **Language:** **Java**
-   **UI Toolkit:**
    -   **Material 3 Components:** Utilized for modern UI elements like `MaterialToolbar`, `BottomNavigationView`, and `FloatingActionButton`.
    -   **RecyclerView:** For displaying efficient, scrollable lists of marinas and comments.
    -   **ConstraintLayout:** For creating complex, responsive layouts.
-   **Architecture:**
    -   **Single-Activity Architecture:** Uses a single `MainActivity` to host multiple `Fragment`s, which is a modern and recommended Android pattern.
    -   **Fragment-based Navigation:** `BottomNavigationView` is used to switch between the main sections of the app (List, Map, Settings).
-   **Data Persistence & Networking:**
    -   **Google Firestore:** A NoSQL, cloud-hosted database used for storing and syncing user-generated reviews in real-time. This demonstrates server-side data management.
    -   **HttpURLConnection:** The `WeatherClient.java` class handles making `GET` requests to the OpenWeatherMap REST API on a background thread using `ExecutorService` and `HttpURLConnection`.
-   **Security:**
    -   **BuildConfig for API Keys:** Sensitive API keys (like the OpenWeatherMap key) are stored securely in `local.properties` and accessed via the auto-generated `BuildConfig` file, ensuring they are not exposed in version control.
---

## 4. Screenshots

 Login Screen | Marina List + Map | Marina Detail (with Weather) | Add Review Dialog |
| :---: | :---: | :---: | :---: |
| ![alt text](screenshots/image.png) | ![alt text](screenshots/image-1.png) | ![alt text](screenshots/image-2.png) | ![alt text](screenshots/image-3.png) |

---

## 5. Setup & Installation

1.  Clone the repository to your local machine:
`git clone https://github.com/your-username/SailSpots.git`
2.  Open the project in Android Studio.
3.  Configure your API key by following the instructions in the next section.
4.  Let Gradle sync and download the required dependencies.
5.  Run the app on an emulator or a physical Android device.
    
## 6. API Key Configuration

This project requires two separate API keys to function correctly: one for **Google Maps** (to display maps and search for places) and one for **OpenWeatherMap** (to fetch live weather data).

### Step 1: Get Your API Keys

#### A. Google Maps API Key

1.  **Go to the Google Cloud Console:** Navigate to the [Google Cloud Console](https://console.cloud.google.com/).
2.  **Create a New Project:** If you don't have one already, create a new project.
3.  **Enable the necessary APIs:** For this project, you must enable the following APIs for your project. You can find them in the "APIs & Services" > "Library" section:
    *   **Maps SDK for Android**
    *   **Places API**
4.  **Create API Credentials:**
    *   Go to "APIs & Services" > "Credentials".
    *   Click "Create Credentials" and select "API key".
    *   Copy the generated API key.
5.  **Restrict Your API Key (Important for Security):**
    *   Find your new API key in the list and click the edit icon.
    *   Under "Application restrictions," select "Android apps."
    *   Click "Add an item" and enter your app's **package name** (`com.example.sailspots`) and your **SHA-1 certificate fingerprint**.
    *   Under "API restrictions," select "Restrict key" and choose the **Maps SDK for Android** and **Places API** from the dropdown. This ensures your key only works for your app and only for the services it needs.

#### B. OpenWeatherMap API Key

1.  **Sign up for an account:** Go to [OpenWeatherMap.org](https://openweathermap.org/) and create a free account.
2.  **Get an API Key:** Navigate to the "API keys" section on your account page and copy your default key.

### Step 2: Add the Keys to Your Project

To keep your keys secure and out of version control, you will add them to a `local.properties` file in the root directory of your project.

1.  **Find or Create `local.properties`:** In the root folder of your Android Studio project (the same level as `gradle.properties` and `settings.gradle`), find or create a file named `local.properties`.

2.  **Add Both Keys:** Paste the following lines into your `local.properties` file, replacing the placeholder text with the actual keys you copied.

3.  **Sync Gradle:**
    -   Android Studio will prompt you to sync your project. Click "Sync Now".
---

## 7. Build and Dependency Information

This project was built using the following key configurations and libraries:

-   **Android Studio Version:** [e.g., Hedgehog | 2023.1.1]
-   **Target SDK:** 34
-   **Min SDK:** 24
-   **Core Dependencies:**
    -   `com.google.android.material:material:1.13.0` (For Material Design Components)
    -   `com.google.firebase:firebase-firestore` (For Real-time Reviews Database)
    -   `com.google.android.gms:play-services-maps:19.2.0` (For Google Maps Functionality)
    -   `androidx.navigation:*:2.9.4` (For Single-Activity Fragment Navigation)