package com.example.fittrack.presentation;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.Date;

@Entity(tableName = "Activity")
    public class ActivityEntity{
        @PrimaryKey(autoGenerate = true) Integer ActivityID;
        @ColumnInfo(name = "date")
        Date date;
        @ColumnInfo(name = "distance") double distance;
        @ColumnInfo(name="time") Integer time;
        @ColumnInfo(name="type") String type;
        @ColumnInfo(name="activityLocations") ArrayList<LatLng> activityLocations;
        @ColumnInfo(name="isUploaded") boolean isUploaded = false;

        public Integer getActivityID() {
            return ActivityID;
        }

        public void setActivityID(Integer activityID) {
            ActivityID = activityID;
        }

        public Date getDate() {
            return date;
        }

        public void setDate(Date date) {
            this.date = date;
        }

        public double getDistance() {
            return distance;
        }

        public void setDistance(double distance) {
            this.distance = distance;
        }

        public Integer getTime() {
            return time;
        }

        public void setTime(Integer time) {
            this.time = time;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public boolean getIsUploaded() { return isUploaded; }

        public void setIsUploaded(boolean uploaded) { isUploaded = uploaded;}

        public ArrayList<LatLng> getActivityLocations() {
            return activityLocations;
        }

        public void setActivityLocations(ArrayList<LatLng> activityLocations) {
            this.activityLocations = activityLocations;
        }
}
