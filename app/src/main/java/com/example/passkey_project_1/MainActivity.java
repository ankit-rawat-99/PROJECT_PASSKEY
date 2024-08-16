package com.example.passkey_project_1;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.journeyapps.barcodescanner.CaptureActivity;
import com.journeyapps.barcodescanner.ScanOptions;

public class MainActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_CODE = 100;
    private static final int QR_CODE_REQUEST_CODE = 101;

    private FrameLayout cameraPreview;
    private TextView loginStatusTextView;
    private Button scanToLoginButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI components
        cameraPreview = findViewById(R.id.cameraPreview);
        loginStatusTextView = findViewById(R.id.loginStatusTextView);
        scanToLoginButton = findViewById(R.id.scanToLoginButton);

        // Check and request camera permission
        checkCameraPermission();

        scanToLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    // Open the QR scanner
                    openQRScanner();
                } else {
                    // Show a message to enable camera permission
                    Toast.makeText(MainActivity.this, "Please enable camera permission", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED) {
            // Request camera permission
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        } else {
            // Permission already granted, open camera or QR scanner
            openQRScanner();
        }
    }

    private void openQRScanner() {
        // Launch the QR code scanner using ZXing
        ScanOptions options = new ScanOptions();
        options.setPrompt("Scan a QR code");
        options.setBeepEnabled(true);
        options.setOrientationLocked(true);
        options.setCaptureActivity(CaptureActivity.class);

        // Start the QR scanner activity
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
                // Permission granted, open QR scanner
                openQRScanner();
            } else {
                // Permission denied, show message
                loginStatusTextView.setText("Please enable camera permission in settings");
                loginStatusTextView.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            }
        }
    }
}
