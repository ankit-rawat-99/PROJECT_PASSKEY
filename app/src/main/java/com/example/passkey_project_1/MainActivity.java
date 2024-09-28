package com.example.passkey_project_1;

import android.Manifest;
import android.animation.Animator;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.journeyapps.barcodescanner.ScanOptions;
import com.journeyapps.barcodescanner.CaptureActivity;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.IOException;
import java.security.GeneralSecurityException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;

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

        // Hide animation view initially
        scanCompleteAnimationView.setVisibility(View.GONE);

        fabScan.setOnClickListener(v -> {
            // Check camera permission before opening QR scanner
            if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                openQRScanner();
            } else {
                Toast.makeText(MainActivity.this, "Please enable camera permission", Toast.LENGTH_SHORT).show();
            }
        });

        // Initialize EncryptedSharedPreferences for securely saving credentials
        initializeEncryptedSharedPreferences();

        // Request camera and storage permissions
        checkCameraPermission();
        checkStoragePermission();

        // Setup bottom navigation menu
        setupBottomNavigation();
    }

    // Function to initialize encrypted shared preferences
    private void initializeEncryptedSharedPreferences() {
        try {
            MasterKey masterKey = new MasterKey.Builder(this)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
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
    }

    // Play animation after successful scan
    private void playScanCompleteAnimation() {
        scanCompleteAnimationView.setVisibility(View.VISIBLE);
        scanCompleteAnimationView.playAnimation();

        scanCompleteAnimationView.addAnimatorListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationEnd(Animator animation) {
                scanCompleteAnimationView.setVisibility(View.GONE);
            }
            @Override public void onAnimationStart(Animator animation) {}
            @Override public void onAnimationCancel(Animator animation) {}
            @Override public void onAnimationRepeat(Animator animation) {}
        });
    }

    // Handle QR code data when successfully scanned
    private void handleScanSuccess(String scannedData) {
        Uri uri = Uri.parse(scannedData);
        String token = uri.getQueryParameter("token");

        if (token != null) {
            // Validate token with the server
            validateTokenWithServer(token);
        } else {
            loginStatusTextView.setText("Invalid QR code.");
            loginStatusTextView.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        }
    }

    // Show saved credentials in an alert dialog
    private void showSavedCredentials() {
        StringBuilder credentialsBuilder = new StringBuilder();
        int credentialsCount = encryptedSharedPreferences.getInt("credentials_count", 0);

        for (int i = 0; i < credentialsCount; i++) {
            String username = encryptedSharedPreferences.getString("credential_" + i + "_username", "No username");
            String password = encryptedSharedPreferences.getString("credential_" + i + "_password", "No password");
            credentialsBuilder.append("Username: ").append(username).append("\nPassword: ").append(password).append("\n\n");
        }

        String credentialsText = credentialsBuilder.length() > 0 ? credentialsBuilder.toString() : "No credentials saved";

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Saved Credentials")
                .setMessage(credentialsText)
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
    }

    // Save credentials using encrypted shared preferences
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

    // Check camera permission
    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        }
    }

    // Check storage permission
    private void checkStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_PERMISSION_CODE);
        }
    }

    // Open the QR scanner
    private void openQRScanner() {
        ScanOptions options = new ScanOptions();
        options.setPrompt("Scan a QR code");
        options.setBeepEnabled(true);
        options.setOrientationLocked(true);
        options.setCaptureActivity(CaptureActivity.class);

        startActivityForResult(options.createScanIntent(this), QR_CODE_REQUEST_CODE);
    }


    // Setup bottom navigation menu interactions
    private void setupBottomNavigation() {
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

    // Show popup for saving credentials
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

    // Retrofit API interface to validate the token with the server
    public interface ApiService {
        @GET("auth")
        Call<Void> validateToken(@Query("token") String token);
    }

    // Validate token by making a call to the backend server hosted on Render
    private void validateTokenWithServer(String token) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://project-passkey.onrender.com")  // Use your server's URL
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ApiService apiService = retrofit.create(ApiService.class);
        Call<Void> call = apiService.validateToken(token);

        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    playScanCompleteAnimation();
                    loginStatusTextView.setText("Login successful!");
                    loginStatusTextView.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                } else {
                    loginStatusTextView.setText("Invalid token. Please try again.");
                    loginStatusTextView.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                Toast.makeText(MainActivity.this, "Error connecting to server", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
