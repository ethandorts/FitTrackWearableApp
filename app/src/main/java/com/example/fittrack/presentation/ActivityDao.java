package com.example.fittrack.presentation;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface ActivityDao {
    @Insert
    void addActivity(ActivityEntity activityEntity);

    @Query("SELECT * from Activity")
    List<ActivityEntity> retrieveUserActivities();

    @Query("SELECT * from Activity WHERE ActivityID=(:activityID)")
    ActivityEntity retrieveSpecificActivity(String activityID);


    @Query("SELECT * from Activity WHERE isUploaded=0")
    LiveData<List<ActivityEntity>> retrieveNotUploadedActivities();

    @Update
    void updateActivityUploaded(ActivityEntity activityEntity);

}
