package com.pavol.svancarek;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.res.Configuration;
import android.location.Location;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.Task;

import java.sql.Time;
import java.util.concurrent.TimeUnit;

public class StromolezLocationService extends Service {

    public static final String EXTRA_CANCEL_LOCATION_TRACKING_FROM_NOTIFICATION = "EXTRA_CANCEL_LOCATION_TRACKING_FROM_NOTIFICATION";
    public static final String ACTION_FOREGROUND_ONLY_LOCATION_BROADCAST = "ACTION_FOREGROUND_ONLY_LOCATION_BROADCAST";
    public static final String EXTRA_LOCATION = "EXTRA_LOCATION";
    private static final int NOTIFICATION_ID = 12345678;
    private static final String NOTIFICATION_CHANNEL_ID = "location_channel";
    private static final String TAG = "StromolezLocationService";

    private boolean serviceRunningInForeground = false;
    private boolean configurationChange = false;

    private LocalBinder localBinder = new LocalBinder();
    private NotificationManager notificationManager;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationRequest locationRequest;
    private LocationCallback callback;
    private Location currentLocation;

    @Override
    public void onCreate() {
        super.onCreate();
        notificationManager = getSystemService(NotificationManager.class);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        locationRequest = LocationRequest.create()
                .setInterval(TimeUnit.SECONDS.toMillis(15))
                .setFastestInterval(TimeUnit.SECONDS.toMillis(5))
                .setMaxWaitTime(TimeUnit.MINUTES.toMinutes(2))
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        callback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                currentLocation = locationResult.getLastLocation();
                Intent intent = new Intent(ACTION_FOREGROUND_ONLY_LOCATION_BROADCAST);
                intent.putExtra(EXTRA_LOCATION, currentLocation);
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);

                if (serviceRunningInForeground) {
                    notificationManager.notify(
                            NOTIFICATION_ID,
                            generateNotification(currentLocation));
                }
            }
        };
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        configurationChange = true;
    }

    private Notification generateNotification(Location location) {

        // 0. Get data
        String mainNotificationText = location.toString();
        String titleText = getString(R.string.app_name);

        // 1. Create Notification Channel for O+ and beyond devices (26+).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, titleText, NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(notificationChannel);
        }

        // 2. Build the BIG_TEXT_STYLE.
        NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle()
                .bigText(mainNotificationText)
                .setBigContentTitle(titleText);

        // 3. Set up main Intent/Pending Intents for notification.
        Intent launchActivityIntent = new Intent(this, MapsActivity.class);
        Intent cancelIntent = new Intent(this, StromolezLocationService.class);
        cancelIntent.putExtra(EXTRA_CANCEL_LOCATION_TRACKING_FROM_NOTIFICATION, true);

        PendingIntent servicePendingIntent = PendingIntent.getService(
                this, 0, cancelIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        PendingIntent activityPendingIntent = PendingIntent.getActivity(
                this, 0, launchActivityIntent, PendingIntent.FLAG_MUTABLE);

        // 4. Build and issue the notification.
        // Notification Channel Id is ignored for Android pre O (26).
        return new NotificationCompat.Builder(getApplicationContext(), NOTIFICATION_CHANNEL_ID)
                .setStyle(bigTextStyle)
                .setContentTitle(titleText)
                .setContentText(mainNotificationText)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setOngoing(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .addAction(
                        R.drawable.ic_launcher_background, "LAUNCH",
                        activityPendingIntent
                )
                .addAction(
                        R.drawable.ic_launcher_background,
                        "STOP",
                        servicePendingIntent
                )
                .build();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind()");
        stopForeground(true);
        serviceRunningInForeground = false;
        configurationChange = false;
        return localBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        Log.d(TAG, "onRebind()");
        stopForeground(true);
        serviceRunningInForeground = false;
        configurationChange = false;
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {

        Log.d(TAG, "onUnbind()");

        if (!configurationChange) {
            Log.d(TAG, "Start foreground service");
            Notification notification = generateNotification(currentLocation);
            startForeground(NOTIFICATION_ID, notification);
            serviceRunningInForeground = true;
        }

        return true;
    }

    public class LocalBinder extends Binder {
        StromolezLocationService getService() {
            return StromolezLocationService.this;
        }
    }

    @SuppressLint("MissingPermission")
    void subscribeToLocationUpdates() {
        Log.d(TAG, "subscribeToLocationUpdates()");

//        SharedPreferenceUtil.saveLocationTrackingPref(this, true)

        // Binding to this service doesn't actually trigger onStartCommand(). That is needed to
        // ensure this Service can be promoted to a foreground service, i.e., the service needs to
        // be officially started (which we do here).
        startService(new Intent(getApplicationContext(), StromolezLocationService.class));

        try {
            // TODO: Step 1.5, Subscribe to location changes.
            fusedLocationProviderClient.requestLocationUpdates(
                    locationRequest, callback, Looper.getMainLooper());
        } catch (SecurityException unlikely) {
//            SharedPreferenceUtil.saveLocationTrackingPref(this, false);
            Log.e(TAG, "Lost location permissions. Couldn't remove updates.");
        }
    }

    void unsubscribeToLocationUpdates() {

        try {
            // TODO: Step 1.6, Unsubscribe to location changes.
            Task<Void> removeTask = fusedLocationProviderClient.removeLocationUpdates(callback);
            removeTask.addOnCompleteListener((task) -> {
                if (task.isSuccessful()) {
                    Log.d(TAG, "Location Callback removed.");
                    stopSelf();
                } else {
                    Log.d(TAG, "Failed to remove Location Callback.");
                }
            });
//            SharedPreferenceUtil.saveLocationTrackingPref(this, false)
        } catch (SecurityException unlikely) {
//            SharedPreferenceUtil.saveLocationTrackingPref(this, true)
            Log.e(TAG, "Lost location permissions. Couldn't remove updates.");
        }
    }
}
