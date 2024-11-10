package com.example.passkey_project_1;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

public class MainActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_CODE = 100;

    private TextView loginStatusTextView;
    private ExtendedFloatingActionButton fabScan;

    private DatabaseReference databaseReference;  // Firebase Realtime Database reference

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI components
        loginStatusTextView = findViewById(R.id.loginStatusTextView);
        fabScan = findViewById(R.id.fabScan);

        // Initialize Firebase Realtime Database
        databaseReference = FirebaseDatabase.getInstance().getReference("tokens");

        // Set click listener for scan button
        fabScan.setOnClickListener(v -> {
            Log.d("FAB", "Scan button clicked");
            if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                openQRScanner();
            } else {
                Toast.makeText(MainActivity.this, "Please enable camera permission", Toast.LENGTH_SHORT).show();
                requestCameraPermission();
            }
        });

        // Monitor token verification status in Firebase
        monitorScannedToken();
    }

    // Open QR scanner
    private void openQRScanner() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setPrompt("Scan a QR code");
        integrator.setBeepEnabled(true);
        integrator.setOrientationLocked(true);
        integrator.initiateScan();
        Log.d("QRScan", "QR scanner opened");
    }

    // Handle the result of the QR scan
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() == null) {
                Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show();
            } else {
                String scannedToken = result.getContents();
                // Send the token to Firebase if it matches conditions
                sendTokenToFirebase(scannedToken);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    // Method to send the scanned token to Firebase
    public void sendTokenToFirebase(String scannedToken) {
        databaseReference.child("scannedToken").setValue(scannedToken)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(MainActivity.this, "Token sent successfully", Toast.LENGTH_SHORT).show();
                    loginStatusTextView.setText("Token sent successfully.");
                    Log.d("Firebase", "Token sent successfully.");
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(MainActivity.this, "Failed to send token", Toast.LENGTH_SHORT).show();
                    loginStatusTextView.setText("Failed to send token.");
                    Log.d("Firebase", "Failed to send token: " + e.getMessage());
                });
    }

    // Function to monitor changes in the scanned token in Firebase
    private void monitorScannedToken() {
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String scannedToken = snapshot.child("scannedToken").getValue(String.class);

                if (scannedToken != null) {
                    for (DataSnapshot tokenSnapshot : snapshot.getChildren()) {
                        String tokenId = tokenSnapshot.child("token").getValue(String.class);
                        Boolean valid = tokenSnapshot.child("valid").getValue(Boolean.class);
                        Boolean verified = tokenSnapshot.child("verified").getValue(Boolean.class);

                        // Check if the scannedToken matches tokenId, is valid, and not verified
                        if (scannedToken.equals(tokenId) && Boolean.TRUE.equals(valid) && Boolean.FALSE.equals(verified)) {
                            // Set the verified flag to true for this specific token entry
                            tokenSnapshot.getRef().child("verified").setValue(true);
                            loginStatusTextView.setText("Token verified successfully.");
                            Log.d("Firebase", "Token verified successfully.");
                            break;
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.d("Firebase", "Error monitoring scanned token: " + error.getMessage());
            }
        });
    }


    // Function to request camera permission
    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
    }

    // Handle camera permission result
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openQRScanner();
            } else {
                Toast.makeText(this, "Camera permission is required to scan QR codes", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
