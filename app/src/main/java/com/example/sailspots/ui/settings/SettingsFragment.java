package com.example.sailspots.ui.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.sailspots.R;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SettingsFragment extends Fragment {

    private SwitchMaterial switchUnits;
    private TextInputEditText etNewPassword, etCurrentPassword;
    private Button btnUpdatePassword;
    private SharedPreferences prefs;

    // Key for storing the preference
    public static final String PREFS_NAME = "SailSpotsPrefs";
    public static final String KEY_USE_KM = "use_km";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Init Views
        switchUnits = view.findViewById(R.id.switchUnits);
        etNewPassword = view.findViewById(R.id.etNewPassword);
        etCurrentPassword = view.findViewById(R.id.etCurrentPassword);
        btnUpdatePassword = view.findViewById(R.id.btnUpdatePassword);

        // Init Prefs
        prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Setup Switch State
        boolean isKm = prefs.getBoolean(KEY_USE_KM, false);
        switchUnits.setChecked(isKm);

        // Listener for Switch
        switchUnits.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(KEY_USE_KM, isChecked).apply();
            String unit = isChecked ? "Kilometers" : "Miles";
            Toast.makeText(requireContext(), "Units changed to " + unit, Toast.LENGTH_SHORT).show();
        });

        // Setup Auth Listener for password update
        btnUpdatePassword.setOnClickListener(v -> updatePassword());
    }

    /**
     * Helper to re-authenticate the user with their current password,
     * then run the provided action on success.
     */
    private void reauthenticateThen(@NonNull String currentPassword, @NonNull Runnable onSuccess) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(requireContext(), "No authenticated user", Toast.LENGTH_LONG).show();
            return;
        }

        String email = user.getEmail();
        if (email == null) {
            Toast.makeText(requireContext(), "User email not available", Toast.LENGTH_LONG).show();
            return;
        }

        AuthCredential credential = EmailAuthProvider.getCredential(email, currentPassword);
        user.reauthenticate(credential).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                onSuccess.run();
            } else {
                String msg = (task.getException() != null)
                        ? task.getException().getMessage()
                        : "Re-authentication failed";
                Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Updates the user's password.
     * Requires entering the current password for re-authentication plus a new password.
     */
    private void updatePassword() {
        String newPass = etNewPassword.getText() != null
                ? etNewPassword.getText().toString().trim()
                : "";
        String currentPass = etCurrentPassword.getText() != null
                ? etCurrentPassword.getText().toString().trim()
                : "";

        if (TextUtils.isEmpty(currentPass)) {
            etCurrentPassword.setError("Current password required");
            return;
        }
        if (TextUtils.isEmpty(newPass)) {
            etNewPassword.setError("New password required");
            return;
        }
        if (newPass.length() < 6) {
            etNewPassword.setError("Password must be > 6 chars");
            return;
        }

        reauthenticateThen(currentPass, () -> {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) {
                Toast.makeText(requireContext(), "User not available", Toast.LENGTH_LONG).show();
                return;
            }

            user.updatePassword(newPass)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(requireContext(), "Password updated successfully", Toast.LENGTH_SHORT).show();
                            etNewPassword.setText("");
                            etCurrentPassword.setText("");
                        } else {
                            String msg = (task.getException() != null)
                                    ? task.getException().getMessage()
                                    : "Password update failed";
                            Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
                        }
                    });
        });
    }
}
