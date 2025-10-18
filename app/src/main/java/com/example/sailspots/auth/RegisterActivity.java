package com.example.sailspots.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.sailspots.MainActivity;
import com.example.sailspots.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

import java.util.Objects;

/**
 * RegisterActivity handles the creation of new user accounts via email and password.
 * It also captures the user's name and saves it to their Firebase profile.
 */
public class RegisterActivity extends AppCompatActivity {

    // TAG for logging, consistent with LoginActivity.
    private static final String TAG = "RegisterActivity";

    // Firebase Auth member variable.
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Initialize Firebase Auth instance.
        mAuth = FirebaseAuth.getInstance();

        // Initialize UI elements from the layout XML.
        TextInputEditText nameEditText = findViewById(R.id.edit_text_name);
        TextInputEditText emailEditText = findViewById(R.id.edit_text_register_email);
        TextInputEditText passwordEditText = findViewById(R.id.edit_text_register_password);
        MaterialButton registerButton = findViewById(R.id.btn_create_account);
        MaterialButton goToLoginButton = findViewById(R.id.btn_go_to_login);

        // --- SET UP ONCLICK LISTENERS ---

        // Listener for the "Create Account" button.
        registerButton.setOnClickListener(view -> {
            // Retrieve text from input fields, trimming whitespace.
            String name = Objects.requireNonNull(nameEditText.getText()).toString().trim();
            String email = Objects.requireNonNull(emailEditText.getText()).toString().trim();
            String password = Objects.requireNonNull(passwordEditText.getText()).toString().trim();

            // Basic validation to ensure fields are not empty.
            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(RegisterActivity.this, "All fields are required.", Toast.LENGTH_SHORT).show();
                return;
            }
            createAccount(email, password, name);
        });

        // Listener for the "Already have an account?" button/text.
        // This closes the current activity, returning the user to the LoginActivity.
        goToLoginButton.setOnClickListener(view -> {
            finish();
        });
    }

    /**
     * Creates a new user account in Firebase Authentication using the provided email and password.
     * After successful creation, it updates the user's profile with their display name.
     * @param email The user's email address.
     * @param password The user's chosen password.
     * @param name The user's display name.
     */
    private void createAccount(String email, String password, String name) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Account creation was successful.
                        Log.d(TAG, "createUserWithEmail:success");
                        FirebaseUser user = mAuth.getCurrentUser();

                        // Now, update the user's profile with their name.
                        updateUserProfile(user, name);

                        Toast.makeText(RegisterActivity.this, "Account created successfully!", Toast.LENGTH_SHORT).show();
                        // After registration, automatically log the user in and navigate to the main screen.
                        navigateToMainActivity();

                    } else {
                        // If account creation fails, log the error and notify the user.
                        // Common errors include malformed email, weak password, or email already in use.
                        Log.w(TAG, "createUserWithEmail:failure", task.getException());
                        Toast.makeText(RegisterActivity.this, "Authentication failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    /**
     * Updates the display name for a given FirebaseUser.
     * This is a separate step that should be called after a user is successfully created or logged in.
     * @param user The FirebaseUser object whose profile needs updating.
     * @param name The display name to set for the user.
     */
    private void updateUserProfile(FirebaseUser user, String name) {
        if (user != null) {
            // Create a UserProfileChangeRequest to set the display name.
            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                    .setDisplayName(name)
                    .build();

            // Asynchronously update the profile.
            user.updateProfile(profileUpdates)
                    .addOnCompleteListener(updateTask -> {
                        if (updateTask.isSuccessful()) {
                            Log.d(TAG, "User profile updated successfully with name: " + name);
                        } else {
                            Log.w(TAG, "Failed to update user profile.", updateTask.getException());
                        }
                    });
        }
    }

    /**
     * Navigates the user to the MainActivity after a successful registration.
     * Clears the activity stack to provide a clean user experience.
     */
    private void navigateToMainActivity() {
        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
