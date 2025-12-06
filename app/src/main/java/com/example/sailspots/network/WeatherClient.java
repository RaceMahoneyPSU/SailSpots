package com.example.sailspots.network;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A client for fetching current weather data from the OpenWeatherMap API.
 * This class handles the network request on a background thread and returns
 * the result to the main thread via a callback.
 */
public class WeatherClient {

    // An ExecutorService to run network operations on a background thread,
    // preventing the UI from freezing.
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // A Handler to post results back onto the main (UI) thread.
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    /**
     * A data class (POJO) to hold the parsed weather information.
     */
    public static class WeatherResult {
        public final double tempF;
        public final double tempMinF;
        public final double tempMaxF;
        public final double windSpeedMps; // Wind speed in meters per second.
        public final String condition;    // A description of the weather (e.g., "Partly cloudy").

        public WeatherResult(double tempF, double tempMinF, double tempMaxF, double windSpeedMps, @NonNull String condition) {
            this.tempF = tempF;
            this.tempMinF = tempMinF;
            this.tempMaxF = tempMaxF;
            this.windSpeedMps = windSpeedMps;
            this.condition = condition;
        }
    }

    /**
     * A callback interface to handle the asynchronous response of the weather API call.
     */
    public interface WeatherCallback {
        /**
         * Called on the main thread when the weather data is successfully fetched and parsed.
         * @param result The WeatherResult object containing the data.
         */
        void onSuccess(@NonNull WeatherResult result);

        /**
         * Called on the main thread if an error occurs during the network request or parsing.
         * @param e The exception that occurred.
         */
        void onFailure(@NonNull Exception e);
    }

    /**
     * Public method to initiate the asynchronous weather data fetch.
     *
     * @param lat      The latitude of the location.
     * @param lon      The longitude of the location.
     * @param apiKey   Your OpenWeatherMap API key.
     * @param callback The callback to be invoked with the result or error.
     */
    public void fetchCurrentWeather(double lat,
                                    double lon,
                                    @NonNull String apiKey,
                                    @NonNull WeatherCallback callback) {
        // Execute the network call on the background thread.
        executor.execute(() -> {
            HttpURLConnection connection = null;
            try {
                // Construct the API request URL with the provided parameters.
                String urlStr = String.format(Locale.US, "https://api.openweathermap.org/data/2.5/weather?lat=%f&lon=%f&units=imperial&appid=%s",
                        lat, lon, apiKey);
                URL url = new URL(urlStr);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10_000); // 10 seconds.
                connection.setReadTimeout(10_000);    // 10 seconds.

                int code = connection.getResponseCode();
                // Check if the request was successful (HTTP 2xx).
                InputStream is = (code >= 200 && code < 300)
                        ? connection.getInputStream()
                        : connection.getErrorStream();

                if (is == null) {
                    throw new IOException("No response stream, HTTP code=" + code);
                }
                // Read the response from the input stream.
                String response = readStream(is);
                if (code < 200 || code >= 300) {
                    throw new IOException("HTTP code=" + code + " response=" + response);
                }

                // Parse the JSON response string into a WeatherResult object.
                WeatherResult result = parseWeatherJson(response);
                if (result == null) {
                    throw new IOException("Failed to parse response=" + response);
                }

                // Post the successful result back to the main thread.
                postSuccess(callback, result);

            } catch (Exception e) {
                // If any exception occurs, post the error back to the main thread.
                postError(callback, e);
            } finally {
                // Ensure the connection is always closed.
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    /**
     * A helper method to post a successful result to the callback on the main thread.
     */
    private void postSuccess(@NonNull WeatherCallback callback,
                             @NonNull WeatherResult result) {
        mainHandler.post(() -> callback.onSuccess(result));
    }

    /**
     * A helper method to post an error to the callback on the main thread.
     */
    private void postError(@NonNull WeatherCallback callback,
                           @NonNull Exception e) {
        mainHandler.post(() -> callback.onFailure(e));
    }

    /**
     * Parses the JSON string from the OpenWeatherMap API into a WeatherResult object.
     *
     * @param json The raw JSON response string.
     * @return A populated WeatherResult object, or null if parsing fails.
     * @throws JSONException If the JSON is malformed.
     */
    private WeatherResult parseWeatherJson(@NonNull String json) throws JSONException {
        JSONObject root = new JSONObject(json);

        // Extract the 'main' object which contains temperature info.
        JSONObject main = root.getJSONObject("main");
        double temp = main.optDouble("temp", Double.NaN);
        double tempMin = main.optDouble("temp_min", Double.NaN);
        double tempMax = main.optDouble("temp_max", Double.NaN);

        // Extract the 'wind' object.
        JSONObject wind = root.getJSONObject("wind");
        double windSpeed = wind.optDouble("speed", 0.0);

        // Extract the 'weather' array, which contains the condition description.
        JSONArray weatherArr = root.optJSONArray("weather");
        String description = "Unknown";
        if (weatherArr != null && weatherArr.length() > 0) {
            JSONObject first = weatherArr.getJSONObject(0);
            String raw = first.optString("description", "Unknown");

            // Capitalize the first letter of the description for better presentation.
            if (!raw.isEmpty()) {
                description = raw.substring(0, 1).toUpperCase(Locale.getDefault()) + raw.substring(1);
            }
        }
        // If the main temperature could not be parsed, consider it a failure.
        if (Double.isNaN(temp)) {
            return null;
        }
        return new WeatherResult(temp, tempMin, tempMax, windSpeed, description);
    }

    /**
     * Reads the content of an InputStream and converts it into a String.
     */
    @NonNull
    private String readStream(@NonNull InputStream is) throws IOException {
        StringBuilder sb = new StringBuilder();
        // Use a BufferedReader to efficiently read the stream line by line.
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }
}
