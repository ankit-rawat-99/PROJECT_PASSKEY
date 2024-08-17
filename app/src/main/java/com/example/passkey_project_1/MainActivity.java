package com.example.passkey_project_1;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
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

import com.journeyapps.barcodescanner.CaptureActivity;
import com.journeyapps.barcodescanner.ScanOptions;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class MainActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_CODE = 100;
    private static final int STORAGE_PERMISSION_CODE = 101;
    private static final int QR_CODE_REQUEST_CODE = 102;

    private FrameLayout cameraPreview;
    private TextView loginStatusTextView;
    private Button scanToLoginButton;

    private SharedPreferences encryptedSharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI components
        cameraPreview = findViewById(R.id.cameraPreview);
        loginStatusTextView = findViewById(R.id.loginStatusTextView);
        scanToLoginButton = findViewById(R.id.scanToLoginButton);
        Button saveCredentialsButton = findViewById(R.id.saveCredentialsButton);
        Button showCredentialsButton = findViewById(R.id.showCredentialsButton);
        Button websiteDBButton = findViewById(R.id.websiteDBButton);

        // Initialize EncryptedSharedPreferences
        try {
            MasterKey masterKey = new MasterKey.Builder(this)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM) // Change to AES256_GCM if AES256_SIV is unavailable
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

        saveCredentialsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSaveCredentialsPopup();
            }
        });

        scanToLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    openQRScanner();
                } else {
                    Toast.makeText(MainActivity.this, "Please enable camera permission", Toast.LENGTH_SHORT).show();
                }
            }
        });

        showCredentialsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Retrieve and display all saved credentials
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

                // Display the credentials in a Toast or Dialog
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Saved Credentials");
                builder.setMessage(credentialsText);
                builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
                builder.show();
            }
        });

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
        } else {
            openQRScanner();
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
            loginStatusTextView.setText("QR Code: " + scannedData);
            loginStatusTextView.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            // TODO: Handle the scanned data (e.g., authenticate with the server)
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
            if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Storage permission is required to save credentials.", Toast.LENGTH_LONG).show();
            }
        }
    }
}
