package com.example.nwswitch;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.net.NetworkCapabilities.TRANSPORT_CELLULAR;
import static android.net.NetworkCapabilities.TRANSPORT_WIFI;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.net.ConnectivityManager;

import android.content.Context;

import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.widget.TextView;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private static final String logTag = "NWSwitch";

    // to manage missing permissions
    private final ArrayList<String> missingPermissions = new ArrayList<>();
    private static final int MY_PERMISSION_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ask for initial required permissions
        requestPermissions();

        // connectivity manager
        ConnectivityManager connectivityManager = (ConnectivityManager) getApplicationContext().
                getSystemService(Context.CONNECTIVITY_SERVICE);

        // ------ Wi-Fi connection ------
        // - View
        TextView infoViewWifi = findViewById(R.id.infoViewWifiId);
        TextView logViewWifi = findViewById(R.id.logsViewWifiId);
        logViewWifi.setMovementMethod(new ScrollingMovementMethod());
        // - ViewModel
        ClientViewModel cvmWifi = new ClientViewModel();
        cvmWifi.linkToMyViews(this, "Wi-Fi connection",
                infoViewWifi, logViewWifi);
        // - Model
        new Client(connectivityManager, TRANSPORT_WIFI, cvmWifi);

        // ------ Cellular connection ------
        // - View
        TextView infoViewCellular = findViewById(R.id.infoViewCellularId);
        TextView logViewCellular = findViewById(R.id.logsViewCellularId);
        logViewCellular.setMovementMethod(new ScrollingMovementMethod());
        // - ViewModel
        ClientViewModel cvmCellular = new ClientViewModel();
        cvmCellular.linkToMyViews(this, "Cellular connection",
                infoViewCellular, logViewCellular);
        // - Model
        new Client(connectivityManager, TRANSPORT_CELLULAR, cvmCellular);

        // ------ Any connection ------
        // - View
        TextView infoViewAny = findViewById(R.id.infoViewAnyId);
        TextView logViewAny = findViewById(R.id.logsViewAnyId);
        logViewAny.setMovementMethod(new ScrollingMovementMethod());
        // - ViewModel
        ClientViewModel cvmAny = new ClientViewModel();
        cvmAny.linkToMyViews(this, "Any connection",
                infoViewAny, logViewAny);
        // - Model
        new Client(connectivityManager, -1, cvmAny);
    }

    // helper function to check if need to request a permission
    private void addToPermissionRequestListIfNeeded(final String permission) {
        if (ContextCompat.checkSelfPermission(this, permission) != PERMISSION_GRANTED &&
                !missingPermissions.contains(permission)) {
            missingPermissions.add(permission);
        }
    }

    // request initial permissions -- these are permissions that are required inside
    // event listeners that could receive unsolicited events as soon as registered
    private void requestPermissions() {
        String fName = "requestPermissions(): ";

        addToPermissionRequestListIfNeeded(Manifest.permission.CHANGE_NETWORK_STATE);
        addToPermissionRequestListIfNeeded(Manifest.permission.INTERNET);
        addToPermissionRequestListIfNeeded(Manifest.permission.ACCESS_NETWORK_STATE);
        addToPermissionRequestListIfNeeded(Manifest.permission.CHANGE_NETWORK_STATE);
        addToPermissionRequestListIfNeeded(Manifest.permission.ACCESS_FINE_LOCATION);
        addToPermissionRequestListIfNeeded(Manifest.permission.ACCESS_WIFI_STATE);

        // request permissions; user faced with multiple requests is not a good experience
        if (!missingPermissions.isEmpty()) {
            Log.i(logTag, fName + "Asking for missing permissions:");
            for (String p: missingPermissions) Log.i(logTag, fName + "-> " + p);
            ActivityCompat.requestPermissions(this,
                    missingPermissions.toArray(new String[0]), MY_PERMISSION_REQUEST_CODE);
        } else {
            Log.i(logTag, fName + "All required permissions granted already");
        }
    }


    // permission request callback
    // used for debugging at this time
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grants) {
        if (requestCode == MY_PERMISSION_REQUEST_CODE) {
            String fName = "onRequestPermissionsResult(): "; int i = 0; // java is archaic
            Log.i(logTag, fName + "Requested permissions update:");
            for (String p : permissions) { Log.i(logTag, fName + "-> " + p + ": " +
                    (grants[i++] == PERMISSION_GRANTED ? "granted" : "denied"));
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grants);
    }
}