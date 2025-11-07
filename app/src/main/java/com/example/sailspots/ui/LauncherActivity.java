package com.example.sailspots.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.window.SplashScreen;

import androidx.activity.ComponentActivity;
import androidx.appcompat.app.AppCompatActivity;

import com.example.sailspots.MainActivity;
import com.example.sailspots.R;
import com.example.sailspots.auth.LoginActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LauncherActivity extends ComponentActivity {

    /**
     * A developer flag. If set to true, the app will always show the login screen,
     * ignoring any existing signed-in user. This is useful for testing the login flow.
     */
    private static final boolean FORCE_LOGIN_EVERY_TIME = true;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher);

        // Use a Handler to delay the execution of the navigation logic.
        // This creates the "splash screen" effect by showing the layout for a short duration.
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent i; // The intent to be launched after the delay.

            // Check the developer flag to decide the navigation path.
            if (FORCE_LOGIN_EVERY_TIME) {
                // If forcing login, always navigate to LoginActivity.
                i = new Intent(this, LoginActivity.class);
            } else {
                // --- Normal Behavior ---
                // Check if a user is already signed in with Firebase.
                FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();

                // If a user exists (is not null), navigate to the MainActivity.
                // Otherwise, navigate to the LoginActivity.
                i = (u != null) ? new Intent(this, MainActivity.class)
                        : new Intent(this, LoginActivity.class);
            }
            // Launch the determined activity.
            startActivity(i);
            // Finish the SplashActivity so the user cannot navigate back to it.
            finish();
        }, 1000); // The delay in milliseconds (approx. 0.9 seconds).
    }
}
