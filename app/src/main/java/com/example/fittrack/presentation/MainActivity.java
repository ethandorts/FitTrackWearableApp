package com.example.fittrack.presentation;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

import com.example.fittrack.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataItemAsset;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private TextView Start;
    private Button btnRecordActivity;
    private DataClient dataClient;
    private ActivityDao activityDao;
    private ActivityDatabase activityDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Context context = getApplicationContext();
        dataClient = Wearable.getDataClient(this);

        activityDatabase = ActivityDatabase.getActivityDatabase(this);
        activityDao = activityDatabase.activityDao();

        Start = findViewById(R.id.textView);
        btnRecordActivity = findViewById(R.id.btnRecordActivity);
        btnRecordActivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, RecordActivity.class);
                startActivity(intent);
            }
        });

        LiveData<List<ActivityEntity>> list = activityDao.retrieveNotUploadedActivities();
        list.observe(this, new Observer<List<ActivityEntity>> () {

            @Override
            public void onChanged(List<ActivityEntity> activityEntities) {
                System.out.println("Retrieved activities with a 0");
                for(ActivityEntity entity : activityEntities) {
                    System.out.println("Values: " + entity.getType() + " " + entity.getDistance() + " " + entity.getTime());
                }
                transferFitnessData(activityEntities);
                for(ActivityEntity entity : activityEntities) {
                    ExecutorService executorService = Executors.newSingleThreadExecutor();
                    executorService.execute(() -> {
                        entity.setIsUploaded(true);
                        activityDao.updateActivityUploaded(entity);
                        System.out.println("Updated Activity: " + entity.getIsUploaded());
                    });
                }
            }
        });

    }

    public void transferFitnessData(List<ActivityEntity> activities) {
        ArrayList<DataMap> activitiesDataList = new ArrayList<>();
        for(ActivityEntity entity : activities) {
            DataMap dataMap = new DataMap();
            dataMap.putString("activity_type", entity.getType());
            dataMap.putInt("duration", entity.getTime());
            dataMap.putDouble("distance", entity.getDistance());
            //dataMap.putDataMapArrayList("activityLocations", locations);
            activitiesDataList.add(dataMap);
        }

        PutDataMapRequest activity_data = PutDataMapRequest.create("/activity-data");
        activity_data.getDataMap().putDataMapArrayList("fitness_data", activitiesDataList);
        PutDataRequest request = activity_data.asPutDataRequest().setUrgent();
        Task<DataItem> putTask = dataClient.putDataItem(request);

        putTask.addOnSuccessListener(new OnSuccessListener<DataItem>() {
            @Override
            public void onSuccess(DataItem dataItem) {
                System.out.println("Successfully transferred to  mobile " + dataItem.getUri());
                DataMapItem dataMapItem = DataMapItem.fromDataItem(dataItem);
                DataMap dataMap = dataMapItem.getDataMap();

                System.out.println("Data: " + dataMap.toString());

                }
            });
    }



//     for(ActivityEntity entity : entities) {
//        entity.setIsUploaded(true);
//        activityDao.updateActivityUploaded(entity);
//        System.out.println(entity);
//    }
}