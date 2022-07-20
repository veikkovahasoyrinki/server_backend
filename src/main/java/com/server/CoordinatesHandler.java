package com.server;

import com.sun.net.httpserver.*;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;

import java.sql.SQLException;

import javax.management.Descriptor;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.sqlite.SQLiteException;



public class CoordinatesHandler implements HttpHandler {

    public CoordinatesHandler(CoordDatabase database) {
        db = database;
    }

    final CoordDatabase db;

/*
    Requests for address https://server.url/coordinates are handled in this class
    CoordinateHandler handles POST and GET requests.
    Other requests are illegal and are responded with 405 "not supported"
*/

    @Override
    public void handle(HttpExchange t) throws IOException { 

        try {
            if (t.getRequestMethod().equalsIgnoreCase("POST")) {
                System.out.println("POST");

               
                /* 
                    JSON object is created from the message body
                
                */
                InputStreamReader inputStr =  new InputStreamReader(t.getRequestBody(),"utf-8");
                BufferedReader buffr = new BufferedReader(inputStr);
                JSONTokener tokener = new JSONTokener(buffr);
                JSONObject obj = new JSONObject(tokener);

                /*
                    JSON object is passed to UserCoordninates constructor. 
                    The constructor assigns JSON elements to the currMsg member variables

                */

                try {
                    /*
                        Database method is used to create new SQLite database entry from the currMsg object
                        
                    */
                    UserCoordinates currMsg = new UserCoordinates(obj);
                    
                    try { 
                        db.setCoordinateMessage(currMsg.username, currMsg.longitude, currMsg.latitude, currMsg.dateAsInt(), currMsg.desc);
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }

                    /*
                        no-body success message is sent back to the user.
                    */
                    t.sendResponseHeaders(200, -1);
                    
                } catch (JSONException e) {

                    /*
                        If the incoming JSON message is illegal, this try-catch will catch it
                        and sends a response
                    */

                    String responseMessage = e.toString();
                    byte[] bytesE = responseMessage.getBytes("UTF-8");
                    t.sendResponseHeaders(400, bytesE.length);
                    OutputStream os = t.getResponseBody();
                    os.write(bytesE);
                    os.close();
                }

            } else if (t.getRequestMethod().equalsIgnoreCase("GET")) { 
                System.out.println("GET");

                try {

                    /*
                        Database method getMessages will return correctly formatted JSONArray 
                        which can be sen't straight away to the user. If the JSONArray length is 0,
                        that means there are no messages in the database.

                    */

                    JSONArray coordinateMessages = db.getMessages();

                    if (coordinateMessages.length() == 0) {
                        /*
                            204 means no-content.
                         */
                        t.sendResponseHeaders(204, -1);

                    } else {

                        /*
                            JSONArray is made to JSONtext, bytes array is made from the text and sent as a response
                         */
                        
                        String responseMessageGet = coordinateMessages.toString();
                        byte[] bytesEGet = responseMessageGet.getBytes("UTF-8");
                        t.sendResponseHeaders(200, bytesEGet.length);
                        OutputStream os = t.getResponseBody();
                        os.write(bytesEGet);
                        os.close();
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }


                
            } else { 

                //invalid method
                String responseMessage1 = new String("Not supported");
                byte[] bytesE1 = responseMessage1.getBytes("UTF-8");
                t.sendResponseHeaders(405, bytesE1.length);
                OutputStream os = t.getResponseBody();
                os.write(bytesE1);
                os.close();
            } 

        } catch (JSONException e) {
            
            String responseMessage2 = e.toString();
            byte[] bytesE2 = responseMessage2.getBytes("UTF-8");
            t.sendResponseHeaders(400, bytesE2.length);
            OutputStream os = t.getResponseBody();
            os.write(bytesE2);
            os.close();
        }
    }
}