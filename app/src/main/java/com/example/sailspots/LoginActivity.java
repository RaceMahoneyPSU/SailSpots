package com.example.sailspots;

import android.content.Intent;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.widget.Button;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.CustomCredential;
import androidx.credentials.exceptions.GetCredentialException;
import androidx.credentials.exceptions.NoCredentialException;

import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.GoogleAuthProvider;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";

    private FirebaseAuth auth;
    private CredentialManager credentialManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        auth = FirebaseAuth.getInstance();
        credentialManager = CredentialManager.create(this);

        Button btnGoogle = findViewById(R.id.btnGoogle);
        btnGoogle.setOnClickListener(v -> startGoogleSignIn());

        // Already signed in? Go to main screen.
        FirebaseUser current = auth.getCurrentUser();
        if (current != null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }

    private void startGoogleSignIn() {
        // IMPORTANT: This must be your *Web / server* client ID.
        // With the google-services plugin + Firebase, R.string.default_web_client_id is the right value.
        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                .setServerClientId(getString(R.string.default_web_client_id))
                // If true, only accounts previously authorized for your app are shown.
                .setFilterByAuthorizedAccounts(false)
                .build();

        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build();

        CancellationSignal cancel = new CancellationSignal();
        credentialManager.getCredentialAsync(
                /* context   */ this,
                /* request   */ request,
                /* cancel    */ cancel,
                /* executor  */ ContextCompat.getMainExecutor(this),
                /* callback  */ new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override public void onResult(GetCredentialResponse response) {
                        Credential cred = response.getCredential();
                        if (cred instanceof CustomCredential
                                && GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL.equals(cred.getType())) {
                            GoogleIdTokenCredential tokenCred =
                                    GoogleIdTokenCredential.createFrom(((CustomCredential) cred).getData());
                            firebaseAuthWithGoogle(tokenCred.getIdToken());
                        } else {
                            Log.w(TAG, "Unexpected credential type: " + cred.getClass());
                        }
                    }
                    @Override public void onError(GetCredentialException e) {
                        if (e instanceof NoCredentialException) {
                            Log.i(TAG, "User has no immediately available credentials or canceled.");
                        } else {
                            Log.e(TAG, "getCredentialAsync failed", e);
                        }
                    }
                }
        );
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential firebaseCred = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(firebaseCred)
                .addOnSuccessListener(result -> {
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> Log.e(TAG, "Firebase sign-in failed", e));
    }
}