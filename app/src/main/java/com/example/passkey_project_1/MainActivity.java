package com.example.passkey_project_1;

import android.Manifest;
import android.animation.Animator;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.journeyapps.barcodescanner.ScanOptions;
import com.journeyapps.barcodescanner.CaptureActivity;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class MainActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_CODE = 100;
    private static final int STORAGE_PERMISSION_CODE = 101;
    private static final int QR_CODE_REQUEST_CODE = 102;

    private TextView loginStatusTextView;
    private ExtendedFloatingActionButton fabScan;
    private LottieAnimationView scanCompleteAnimationView;

    private SharedPreferences encryptedSharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI components
        loginStatusTextView = findViewById(R.id.loginStatusTextView);
        fabScan = findViewById(R.id.fabScan);
        scanCompleteAnimationView = findViewById(R.id.scanCompleteAnimationView);

        // Initially hide the animation view
        scanCompleteAnimationView.setVisibility(View.GONE);

        fabScan.setOnClickListener(v -> {
            // Check camera permission and open QR scanner
            if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                openQRScanner();
            } else {
                Toast.makeText(MainActivity.this, "Please enable camera permission", Toast.LENGTH_SHORT).show();
            }
        });

        // Initialize EncryptedSharedPreferences
        try {
            MasterKey masterKey = new MasterKey.Builder(this)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM) // Use AES256_GCM if AES256_SIV is unavailable
                    .build();

            encryptedSharedPreferences = EncryptedSharedPreferences.create(
                    this,
                    "user_credentials",
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
        }

        // Check and request necessary permissions
        checkCameraPermission();
        checkStoragePermission();

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.action_save_credentials) {
                showSaveCredentialsPopup();
                return true;
            } else if (itemId == R.id.action_enter_token) {
                Toast.makeText(MainActivity.this, "Enter Token clicked", Toast.LENGTH_SHORT).show();
                return true;
            } else if (itemId == R.id.action_show_credentials) {
                showSavedCredentials();
                return true;
            } else if (itemId == R.id.action_website_db) {
                Toast.makeText(MainActivity.this, "Website DB clicked", Toast.LENGTH_SHORT).show();
                return true;
            }

            return false;
        });
    }

    private void playScanCompleteAnimation() {
        scanCompleteAnimationView.setVisibility(View.VISIBLE);
        scanCompleteAnimationView.playAnimation();

        scanCompleteAnimationView.addAnimatorListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                // No action needed when animation starts
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                scanCompleteAnimationView.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                // Handle if animation is canceled
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
                // No action needed for repetition
            }
        });
    }

    private void handleScanSuccess(String scannedData) {
        loginStatusTextView.setText("QR Code: " + scannedData);
        loginStatusTextView.setTextColor(getResources().getColor(android.R.color.holo_green_dark));

        playScanCompleteAnimation();
    }

    private void showSavedCredentials() {
        StringBuilder credentialsBuilder = new StringBuilder();
        int credentialsCount = encryptedSharedPreferences.getInt("credentials_count", 0);

        for (int i = 0; i < credentialsCount; i++) {
            String username = encryptedSharedPreferences.getString("credential_" + i + "_username", "No username");
            String password = encryptedSharedPreferences.getString("credential_" + i + "_password", "No password");
            credentialsBuilder.append("Username: ").append(username).append("\nPassword: ").append(password).append("\n\n");
        }

        String credentialsText = credentialsBuilder.toString();
        if (credentialsText.isEmpty()) {
            credentialsText = "No credentials saved";
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Saved Credentials");
        builder.setMessage(credentialsText);
        builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void showSaveCredentialsPopup() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Save Credentials");

        final EditText inputUsername = new EditText(this);
        inputUsername.setHint("Username or Email");
        final EditText inputPassword = new EditText(this);
        inputPassword.setHint("Password");
        final EditText inputConfirmPassword = new EditText(this);
        inputConfirmPassword.setHint("Confirm Password");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(inputUsername);
        layout.addView(inputPassword);
        layout.addView(inputConfirmPassword);

        builder.setView(layout);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String username = inputUsername.getText().toString();
            String password = inputPassword.getText().toString();
            String confirmPassword = inputConfirmPassword.getText().toString();

            if (password.equals(confirmPassword)) {
                saveCredentials(username, password);
            } else {
                Toast.makeText(MainActivity.this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void saveCredentials(String username, String password) {
        SharedPreferences.Editor editor = encryptedSharedPreferences.edit();
        int credentialsCount = encryptedSharedPreferences.getInt("credentials_count", 0);
        String credentialsKey = "credential_" + credentialsCount;

        // Save credentials
        editor.putString(credentialsKey + "_username", username);
        editor.putString(credentialsKey + "_password", password);
        editor.putInt("credentials_count", credentialsCount + 1);
        editor.apply();

        Toast.makeText(MainActivity.this, "Credentials saved", Toast.LENGTH_SHORT).show();
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        }
    }

    private void checkStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_PERMISSION_CODE);
        }
    }

    private void openQRScanner() {
        ScanOptions options = new ScanOptions();
        options.setPrompt("Scan a QR code");
        options.setBeepEnabled(true);
        options.setOrientationLocked(true);
        options.setCaptureActivity(CaptureActivity.class);

        startActivityForResult(options.createScanIntent(this), QR_CODE_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == QR_CODE_REQUEST_CODE && resultCode == RESULT_OK) {
            String scannedData = data.getStringExtra("SCAN_RESULT");
            handleScanSuccess(scannedData);
        } else {
            loginStatusTextView.setText("Scanning failed or canceled");
            loginStatusTextView.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openQRScanner();
            } else {
                loginStatusTextView.setText("Please enable camera permission in settings");
                loginStatusTextView.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            }
        } else if (requestCode == STORAGE_PERMISSION_CODE) {
            if (!(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                loginStatusTextView.setText("Storage permission is required to save credentials");
                loginStatusTextView.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            }
        }
    }
}
