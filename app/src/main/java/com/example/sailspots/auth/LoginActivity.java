package com.example.sailspots.auth;

import static com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL;

import android.content.Intent;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.util.Log;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;

import com.example.sailspots.MainActivity;
import com.example.sailspots.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import java.util.Objects;
import java.util.concurrent.Executors;

/**
 * LoginActivity handles all user authentication, including email/password and Google Sign-In.
 * It serves as the entry point for users who are not already logged in.
 */
public class LoginActivity extends AppCompatActivity {

    // TAG for logging, used to filter messages in Logcat for debugging.
    private static final String TAG = "LoginActivity";

    // Firebase and Credential Manager member variables.
    private FirebaseAuth mAuth;
    private CredentialManager credentialManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase Auth and Credential Manager instances.
        mAuth = FirebaseAuth.getInstance();
        credentialManager = CredentialManager.create(this);

        // --- DEVELOPMENT HELPER: FORCE SIGN-OUT ON LAUNCH ---
        // This block ensures that every time the app is launched in a development environment,
        // it starts from a logged-out state. This is extremely useful for repeatedly testing the login flow.
        // This should be removed or disabled in a production release.
        if (mAuth.getCurrentUser() != null) {
            mAuth.signOut();
            Log.d(TAG, "Forced sign-out for development testing.");
        }
        // -----------------------------------------------------

        // Initialize UI elements by finding them in the layout XML.
        TextInputEditText emailEditText = findViewById(R.id.inputEmail);
        TextInputEditText passwordEditText = findViewById(R.id.inputPassword);
        MaterialButton loginButton = findViewById(R.id.btnLogin);
        RelativeLayout googleButton = findViewById(R.id.btnGoogle); // This is a RelativeLayout, not a Button.
        TextView signUpTextView = findViewById(R.id.txtSignUp);

        // --- SET UP ONCLICK LISTENERS FOR UI ELEMENTS ---

        // Listener for the primary email/password login button.
        loginButton.setOnClickListener(view -> {
            String email = Objects.requireNonNull(emailEditText.getText()).toString().trim();
            String password = Objects.requireNonNull(passwordEditText.getText()).toString().trim();

            // Basic validation to ensure fields are not empty.
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(LoginActivity.this, "Email and password cannot be empty.", Toast.LENGTH_SHORT).show();
                return;
            }
            signIn(email, password);
        });

        // Listener for the Google Sign-In button.
        googleButton.setOnClickListener(view -> {
            // Initiates the Google Sign-In flow using the Credential Manager API.
            launchCredentialManager();
        });

        // Listener for the "Sign Up" text, which navigates to the RegisterActivity.
        signUpTextView.setOnClickListener(view -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    /**
     * Attempts to sign in a user with their email and password using Firebase Authentication.
     * @param email The user's email address.
     * @param password The user's password.
     */
    private void signIn(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Sign-in was successful.
                        Log.d(TAG, "signInWithEmail:success");
                        Toast.makeText(LoginActivity.this, "Authentication successful!", Toast.LENGTH_SHORT).show();
                        navigateToMainActivity();
                    } else {
                        // Sign-in failed. Display an error message to the user.
                        Log.w(TAG, "signInWithEmail:failure", task.getException());
                        Toast.makeText(LoginActivity.this, "Authentication failed. Check credentials.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Configures and launches the Credential Manager API to handle Google Sign-In.
     * This is the modern, recommended approach for Google Sign-In on Android.
     */
    private void launchCredentialManager() {
        // 1. Configure the Google ID token request.
        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                // IMPORTANT: Set to 'false' to allow users to pick from ANY Google account on the device,
                // not just accounts that have previously signed into this app. This is crucial for first-time sign-in.
                .setFilterByAuthorizedAccounts(false)
                // The server client ID is obtained from the google-services.json file,
                // which is required for authenticating with Google's backend.
                .setServerClientId(getString(R.string.default_web_client_id))
                .build();

        // 2. Build the GetCredentialRequest, passing in the configured Google option.
        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build();

        // 3. Asynchronously call the Credential Manager API.
        // The result is handled in the onResult or onError callbacks.
        credentialManager.getCredentialAsync(
                this,
                request,
                new CancellationSignal(), // Allows the operation to be cancelled.
                Executors.newSingleThreadExecutor(), // Runs the callback on a background thread.
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse result) {
                        // The credential request was successful. Handle the result on the main UI thread.
                        runOnUiThread(() -> handleSignInWithGoogle(result.getCredential()));
                    }

                    @Override
                    public void onError(GetCredentialException e) {
                        // The credential request failed. Log the error and show a message to the user.
                        runOnUiThread(() -> {
                            Log.e(TAG, "GetCredentialException: " + e.getMessage());
                            Toast.makeText(LoginActivity.this, "Google Sign-In failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        });
                    }
                }
        );
    }

    /**
     * Handles the credential returned by the Credential Manager.
     * It verifies that the credential is a Google ID token and then proceeds with Firebase authentication.
     * @param credential The credential object returned from a successful `getCredentialAsync` call.
     */
    private void handleSignInWithGoogle(Credential credential) {
        // We use pattern matching for 'instanceof' (a modern Java feature) to check the type
        // and cast it in one step.
        if (credential instanceof CustomCredential customCredential
                && credential.getType().equals(TYPE_GOOGLE_ID_TOKEN_CREDENTIAL)) {

            // The credential is a Google ID token. Extract the token from the data bundle.
            GoogleIdTokenCredential googleIdTokenCredential = GoogleIdTokenCredential.createFrom(customCredential.getData());
            String idToken = googleIdTokenCredential.getIdToken();

            // Use the extracted ID token to sign into Firebase.
            firebaseAuthWithGoogle(idToken);
        } else {
            // This can happen if another credential type (e.g., a passkey) was returned.
            Log.w(TAG, "Unexpected credential type received.");
            Toast.makeText(this, "Unsupported credential type.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Authenticates with Firebase using a Google ID token.
     * @param idToken The Google ID token obtained from the Credential Manager.
     */
    private void firebaseAuthWithGoogle(String idToken) {
        // Create a Firebase credential using the Google ID token.
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);

        // Sign in to Firebase with the created credential.
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Firebase sign-in was successful.
                        Log.d(TAG, "signInWithGoogle:success");
                        Toast.makeText(LoginActivity.this, "Google Sign-In successful!", Toast.LENGTH_SHORT).show();
                        navigateToMainActivity();
                    } else {
                        // Firebase sign-in failed. This can happen due to network issues or misconfiguration.
                        Log.w(TAG, "signInWithGoogle:failure", task.getException());
                        Toast.makeText(LoginActivity.this, "Firebase authentication failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Navigates the user to the MainActivity after a successful login.
     * Clears the activity stack to prevent the user from returning to the login screen via the back button.
     */
    private void navigateToMainActivity() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        // These flags clear the task stack and create a new one for MainActivity.
        // This is standard practice for a post-login navigation flow.
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra("NAVIGATE_TO_MAPS", true);
        startActivity(intent);
        finish(); // Finish LoginActivity so it's removed from memory.
    }
}
