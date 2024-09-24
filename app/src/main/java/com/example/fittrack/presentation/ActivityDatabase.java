package com.example.fittrack.presentation;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.example.fittrack.presentation.ActivityDao;
import com.example.fittrack.presentation.ActivityEntity;
import com.example.fittrack.presentation.DateConverter;

@Database(entities = {ActivityEntity.class}, version = 3)
@TypeConverters({DateConverter.class})
public abstract class ActivityDatabase extends RoomDatabase {
    private static final String dbName = "Activity";
    private static ActivityDatabase activityDatabase;

    public static synchronized ActivityDatabase getActivityDatabase(Context context) {
        if (activityDatabase == null) {
            activityDatabase = Room.databaseBuilder(context, ActivityDatabase.class, dbName)
                    .fallbackToDestructiveMigration().build();
        }
        return activityDatabase;
    }

    public abstract ActivityDao activityDao();
}
