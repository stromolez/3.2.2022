package com.pavol.svancarek;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.DexterBuilder;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.karumi.dexter.listener.single.PermissionListener;
import com.pavol.svancarek.databinding.ActivityMapsBinding;

import java.security.Permission;
import java.util.List;

@RequiresApi(api = Build.VERSION_CODES.N)
public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {
//SSS
    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    private Object permissionDeniedResponse;

    private boolean foregroundOnlyLocationServiceBound = false;

    private ForegroundOnlyBroadcastReceiver receiver;
    private StromolezLocationService service;
    private ServiceConnection foregroundOnlyServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            StromolezLocationService.LocalBinder binder = (StromolezLocationService.LocalBinder) service;
            MapsActivity.this.service = binder.getService();
            foregroundOnlyLocationServiceBound = true;
            checkMyPermission();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            MapsActivity.this.service = null;
            foregroundOnlyLocationServiceBound = false;
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, new IntentFilter(StromolezLocationService.ACTION_FOREGROUND_ONLY_LOCATION_BROADCAST));
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
    }

    @Override
    protected void onStart() {
        super.onStart();

        Intent serviceIntent = new Intent(this, StromolezLocationService.class);
        bindService(serviceIntent, foregroundOnlyServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        if (foregroundOnlyLocationServiceBound) {
            unbindService(foregroundOnlyServiceConnection);
            foregroundOnlyLocationServiceBound = false;
        }
        super.onStop();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        receiver = new ForegroundOnlyBroadcastReceiver();

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

    }

    private void checkMyPermission() {

        Dexter.withContext(this).withPermission(Manifest.permission.ACCESS_FINE_LOCATION).withListener(new PermissionListener() {

            @Override
            public void onPermissionGranted(PermissionGrantedResponse permissionGrantedResponse) {
                Toast.makeText(MapsActivity.this, "Permission Granted", Toast.LENGTH_SHORT).show();

                service.subscribeToLocationUpdates();
            }

            @Override
            public void onPermissionDenied(PermissionDeniedResponse permissionDeniedResponse) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", getPackageName(), "");
                intent.setData(uri);
                startActivity(intent);
            }

            @Override
            public void onPermissionRationaleShouldBeShown(PermissionRequest permissionRequest, PermissionToken permissionToken) {

            }
        }).check();
    }







    NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "abc")
            .setContentTitle("Ahoy")
            .setContentText("neviem")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT);

    @SuppressLint("ResourceType")
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        LatLng slovakia = new LatLng(48, 18);

        mMap.addMarker(new MarkerOptions().position(slovakia).title("Slovenskooo"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(slovakia));

    }

    /**
     * Receiver for location broadcasts from [ForegroundOnlyLocationService].
     */
    private class ForegroundOnlyBroadcastReceiver extends BroadcastReceiver {

        @Overrideandroid.permission.ACCESS_BACKGROUND_LOCATION
        public void onReceive(Context context, Intent intent) {
            Location location = intent.getParcelableExtra(StromolezLocationService.EXTRA_LOCATION);

            if (location != null) {
                Log.e("STROMOLEZ", "Foreground location: " + location.getLongitude() + " : " + location.getLatitude());
            }
        }
    }
}