package com.example.fittrack.presentation;

import androidx.room.TypeConverter;

import com.google.android.gms.maps.model.LatLng;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;

public class DateConverter {
     @TypeConverter
     public static Date convertLong(Long date) {
         if(date != null) {
             return new Date(date);
         } else {
             return null;
         }
     }

     @TypeConverter
        public static Long convertDate(Date date) {
         if(date != null) {
             return date.getTime();
         } else {
             return null;
         }
     }

     @TypeConverter
    public static String toJson(ArrayList<LatLng> activityLocations) {
         Gson locations = new Gson();
         return locations.toJson(activityLocations);
     }

     @TypeConverter
    public static ArrayList<LatLng> toLatLngArray(String locations) {
         Gson locationsGson = new Gson();
         Type listType = new TypeToken<ArrayList<LatLng>>() {}.getType();
         return locationsGson.fromJson(locations, listType);
     }
}
