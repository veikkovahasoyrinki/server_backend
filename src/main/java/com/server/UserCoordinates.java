package com.server;
import org.json.JSONObject;
import org.json.JSONException;

import java.time.*;
import java.time.format.DateTimeFormatter;

public class UserCoordinates {

    /*
        UserCoordiate object is constructed from incoming JSON coordinate message. 
        See CoordinatesHandler.java line 64. 
        Legality of the JSON is checked with try-catch in the constructor.
        JSONObject get methods are called and they will throw a JSONException if wanted keys don't exist

    */
    UserCoordinates(JSONObject objectParam) {
        try {
            username = objectParam.getString("username");
            longitude = objectParam.getDouble("longitude");
            latitude = objectParam.getDouble("latitude");
            timestamp = DateTimeParse(objectParam.getString("sent"));
            
            if (objectParam.has("description")) {
                desc = objectParam.getString("description");
            } else {
                /*
                    Default description if no description is given. This is done because CoordDatabase
                    SetCoordinateMessage requires String description as a parameter.

                */
                desc = "[no description]";
            }
        } catch (JSONException e) {
            throw new JSONException("Required keys not found in JSON");
        }

    }

    public Double longitude;
    public Double latitude;
    public String username;
    public LocalDateTime timestamp;

    String desc;


    private synchronized LocalDateTime DateTimeParse(String timeString) {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_ZONED_DATE_TIME;
        return LocalDateTime.parse(timeString, formatter);
    }

    public synchronized long dateAsInt() {
        return timestamp.toInstant(ZoneOffset.UTC).toEpochMilli();
    }

}




