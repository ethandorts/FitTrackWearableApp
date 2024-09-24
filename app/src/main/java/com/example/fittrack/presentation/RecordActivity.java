package com.example.fittrack.presentation;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.health.services.client.ExerciseClient;
import androidx.health.services.client.HealthServices;
import androidx.health.services.client.HealthServicesClient;
import androidx.health.services.client.ListenableFutureExtensionKt;
import androidx.health.services.client.data.DataType;
import androidx.health.services.client.data.ExerciseCapabilities;
import androidx.health.services.client.data.ExerciseConfig;
import androidx.health.services.client.data.ExerciseGoal;
import androidx.health.services.client.data.ExerciseInfo;
import androidx.health.services.client.data.ExerciseType;
import androidx.health.services.client.data.ExerciseTypeCapabilities;
import androidx.health.services.client.proto.DataProto;
import androidx.health.services.client.proto.EventsProto;

import com.example.fittrack.R;
import com.example.fittrack.presentation.ActivityDao;
import com.example.fittrack.presentation.ActivityDatabase;
import com.example.fittrack.presentation.ActivityEntity;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.Tasks;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.firebase.Timestamp;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class RecordActivity extends AppCompatActivity {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 101;
    private FusedLocationProviderClient fusedLocationClient;
    private Location previousLocation;
    ActivityDatabase activityDatabase;
    ActivityDao activityDao;
    private double distanceTravelled;
    private TextView txtDistanceTravelled;
    private TextView txtRunTime;
    private Button btnStartRun;
    private Button btnStopRun;

    private LatLng currentLocation;
    private boolean isTrackingRun;
    private boolean isTimerStarted;
    private Handler runTimeHandler;
    private Runnable timer;
    private long startTime;
    private int elapsedTime;
    private double milestoneTarget = 1000;
    private List<LatLng> activityLocations = new ArrayList<>();
    private TextToSpeech DistanceTalker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);

        HealthServicesClient healthclient = HealthServices.getClient(this);
        ExerciseClient exerciseClient = healthclient.getExerciseClient();

//        ListenableFuture<ExerciseCapabilities> futureCapabilities = exerciseClient.getCapabilitiesAsync();
//
//        Futures.addCallback(futureCapabilities, new FutureCallback<ExerciseCapabilities>() {
//            @Override
//            public void onSuccess(ExerciseCapabilities capabilities) {
//                if (capabilities.getSupportedExerciseTypes().contains(ExerciseType.RUNNING)) {
//                    ExerciseTypeCapabilities runningCapabilities = capabilities.getExerciseTypeCapabilities(ExerciseType.RUNNING);
//                    System.out.println(runningCapabilities);
//                }
//            }
//
//            @Override
//            public void onFailure(Throwable t) {
//                Log.e("Capabilities", "Failed to get exercise capabilities", t);
//            }
//        }, MoreExecutors.directExecutor());




        activityDatabase = ActivityDatabase.getActivityDatabase(this);
        activityDao = activityDatabase.activityDao();

        btnStartRun = findViewById(R.id.btnStart);
        btnStopRun = findViewById(R.id.btnStop);
        txtDistanceTravelled = findViewById(R.id.txtDistanceRun);
        txtRunTime = findViewById(R.id.txtTimer);


        runTimeHandler = new Handler();
        timer = new Runnable() {
            @Override
            public void run () {
                if(isTrackingRun) {
                    elapsedTime = (int) ((System.currentTimeMillis() - startTime) / 1000);
                    txtRunTime.setText(formatRunTime(elapsedTime));
                    runTimeHandler.postDelayed(this, 1000);
                }
            }
        };

        DistanceTalker = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    DistanceTalker.setLanguage(Locale.UK);
                }
            }
        });


        txtDistanceTravelled.setText(String.format("%.2f metres", distanceTravelled));

        btnStartRun.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isTrackingRun = true;
                startTimer();
                Set<? extends DataType<?,?>> dataTypesWanted = new HashSet<>(Arrays.asList(
                        DataType.HEART_RATE_BPM,
                        DataType.CALORIES_TOTAL,
                        DataType.DISTANCE
                ));
                List<ExerciseGoal<?>> exerciseGoals = new ArrayList<>();
                ExerciseConfig config = new ExerciseConfig(ExerciseType.RUNNING,
                        dataTypesWanted,
                        false,
                        true,
                        exerciseGoals);

                exerciseClient.startExerciseAsync(config);
            }
        });

        btnStopRun.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isTrackingRun = false;
                stopTimer();
                exerciseClient.endExerciseAsync();

                saveActivity();
                distanceTravelled = 0.00;

            }
        });

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        getUserLocation();
    }

    private void getUserLocation() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            getLocationUpdates();
        }
    }

    // Method to receive ongoing updates of device's location.
    private void getLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d("MainActivity", "Location permissions were not granted");
            return;
        }

        fusedLocationClient.requestLocationUpdates(createLocationRequest(),
                new LocationCallback() {
                    @Override
                    public void onLocationResult(LocationResult locationResult) {
                        if (locationResult == null) {
                            return;
                        }
                        Location prevLocation = null;
                        for (Location location : locationResult.getLocations()) {
                            if(isTrackingRun) {
                                LatLng newLocation = new LatLng(location.getLatitude(), location.getLongitude());
                                activityLocations.add(newLocation);
                                if(previousLocation != null) {
                                    CalculateDistance(location);
                                }
                            }
                            previousLocation = location;
                        }
                    }
                },
                null );
    }

    private void CalculateDistance(Location location) {
        if(previousLocation != null) {
            double distanceTravelledInterval = previousLocation.distanceTo(location);
            distanceTravelled += distanceTravelledInterval;
            String.valueOf(distanceTravelled);
            txtDistanceTravelled.setText(String.format("%.2f meters", distanceTravelled));
        }
    }

    private void startTimer() {
        isTimerStarted = true;
        startTime = System.currentTimeMillis();
        runTimeHandler.post(timer);
    }

    private void stopTimer() {
        isTimerStarted = false;
        runTimeHandler.removeCallbacks(timer);
        elapsedTime = (int) (System.currentTimeMillis() - startTime);
    }

    // Specifies interval of location updates.
    private com.google.android.gms.location.LocationRequest createLocationRequest() {
        return com.google.android.gms.location.LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(5000)
                .setFastestInterval(1000);
    }


    private void startRunningClock() {
        LocalDate date = LocalDate.now();
    }

    // Requests appropriate location permissions.
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLocationUpdates();
            } else {
                Log.d("MainActivity", "Location permissions denied");
            }
        }
    }

    private String formatRunTime(int timePassed) {
        int hours = timePassed / 3600;
        int minutes = (timePassed % 3600) / 60;
        int seconds = timePassed % 60;
        String formattedRunTime = String.format("%02d:%02d:%02d", hours, minutes, seconds);

        return formattedRunTime;
    }

    private void DistanceSpeakerAssistant(double DistanceTravelled) {
        while(distanceTravelled >= milestoneTarget) {
            DistanceTalker.speak(milestoneTarget + " metres done" , TextToSpeech.QUEUE_FLUSH, null);
            milestoneTarget += 1000;
        }
    }

    private void saveActivity() {
        ActivityEntity activityEntity = new ActivityEntity();
        Date today = new Date();
        activityEntity.setDate(today);
        activityEntity.setDistance(distanceTravelled);
        activityEntity.setTime(elapsedTime);
//        activityEntity.setActivityLocations((ArrayList<LatLng>) activityLocations);
        activityEntity.setType("Running");
        new Thread(new Runnable() {
            @Override
            public void run() {
                activityDao.addActivity(activityEntity);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(),
                                "Activity added successfully",
                                Toast.LENGTH_LONG).show();
                    }
                });
            }
        }).start();
    }
}

