package com.server;

import com.sun.net.httpserver.*;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;

import java.sql.SQLException;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;





public class RegistrationHandler implements HttpHandler {

    public RegistrationHandler(UserAuthenticator auth, CoordDatabase database) {

        /*
        
        Admin is created to the database, and if the database already exists it's checked for admin user.
        Therefore there always will be admin user.

        */ 
        authenticator = auth;
        db = database;
        if (!db.usernameCheck("admin")) createAdmin(); 

    }

    final UserAuthenticator authenticator;
    JSONObject AdminUser;
    final CoordDatabase db;
    
/*

    Requests for address https://server.url/registration are handled in this class

    Only a POST request is legal, other requests are responded with 405 "not supported"
*/
        
    @Override
    public void handle(HttpExchange t) throws IOException {



        if (t.getRequestMethod().equalsIgnoreCase("POST")) { 

            /*
            POST message should contain registration data as a JSON 
            Legality of the request body is checked first.
            The verification is done in if/else steps as follows:


            1. try-catch will catch if the body is other than a JSON

            2. If it's JSON, check if it has keys "username", "password", "email" and nothing else

            3. Then if those keys exists, check if they are null

            4. Parse strings from the values keys are associated with

            5. Check the length of username, password and email. 
            Length of username and password have to be atleast 2, email length atleast 6

            6. Email string has to contain characters '@' and '.'

            7. Check if username already exists in the database

            
            If any of these checks fail, error response is sent with appropriate code
            Verification is done in steps so that as soon as error is detected, a response is sent
            and no extra verification is done

            
            */

            InputStreamReader inputStr =  new InputStreamReader(t.getRequestBody(),"utf-8");
            BufferedReader buffr = new BufferedReader(inputStr);

            try { 
                JSONTokener tokener = new JSONTokener(buffr);
                JSONObject obj = new JSONObject(tokener);

                System.out.println("New registration attempt, checking legality...");

                if (obj.has("username") && obj.has("password") && obj.has("email") && !tokener.more()) {

                    if (obj.isNull("username") || obj.isNull("password") || obj.isNull("email")) {
                        //Invalid value in username, password or email
                        String responseMessage1 = "Invalid value in username, password or email";
                        System.out.println(responseMessage1);

                        byte[] bytesE1 = responseMessage1.getBytes("UTF-8");
                        t.sendResponseHeaders(403, bytesE1.length);
                        OutputStream os = t.getResponseBody();
                        os.write(bytesE1);
                        os.close();

                    } else {

                        //correct keys with non-null fields exist
                        String usernameEval = obj.getString("username");
                        String passwordEval = obj.getString("password");
                        String emailEval = obj.getString("email");
                        if (usernameEval.length() < 2 || passwordEval.length() < 2 || emailEval.length() < 6) {

                            //given username, password or email is too short
                            String responseMessage2 = "given username, password or email is too short";
                            System.out.println(responseMessage2);
                            byte[] bytesE2 = responseMessage2.getBytes("UTF-8");
                            t.sendResponseHeaders(403, bytesE2.length);
                            OutputStream os = t.getResponseBody();
                            os.write(bytesE2);
                            os.close();
                        } else if (usernameEval.length() > 20 || passwordEval.length() > 30 || emailEval.length() > 50) {
                           
                            //given username, password or email is too long
                            String responseMessage2 = "Maximum username length is 20, password length 30, email length 50";
                            System.out.println(responseMessage2);
                            byte[] bytesE2 = responseMessage2.getBytes("UTF-8");
                            t.sendResponseHeaders(403, bytesE2.length);
                            OutputStream os = t.getResponseBody();
                            os.write(bytesE2);
                            os.close();


                        } else {

                            if (!emailEval.contains("@") || !emailEval.contains(".")) {

                                //invalid email input
                                String responseMessage3 = "invalid email given";
                                System.out.println(responseMessage3);
                                byte[] bytesE3 = responseMessage3.getBytes("UTF-8");
                                t.sendResponseHeaders(403, bytesE3.length);
                                OutputStream os = t.getResponseBody();
                                os.write(bytesE3);
                                os.close();
                            } else {
                                
                                //all are valid, check if username already exists
                                //true if username already exists
                                Boolean existsFlag = db.usernameCheck(usernameEval);
                                
                                if (existsFlag) {
                                    System.out.println("Username already registered, registration failed for username: " + usernameEval);
                                    String responseMessage4 = "username is already registered";
                                    System.out.println(responseMessage4);
                                    byte[] bytesE4 = responseMessage4.getBytes("UTF-8");
                                    t.sendResponseHeaders(403, bytesE4.length);
                                    OutputStream os = t.getResponseBody();
                                    os.write(bytesE4);
                                    os.close();
                                } else {

                                    //register new user

                                    try { //user entry to sql database
                                        db.setUser(obj);
                                    } catch (SQLException e) {
                                        e.printStackTrace();
                                    }
                                    
                                    String responseMessage5 = "User registration successful";
                                    System.out.println(responseMessage5);
                                    byte[] bytesE5 = responseMessage5.getBytes("UTF-8");
                                    t.sendResponseHeaders(200, bytesE5.length);
                                    OutputStream os = t.getResponseBody();
                                    os.write(bytesE5);
                                    os.close();
                                }
                            }
                        }
                    }
                } else {
                    String responseMessage6 = "Invalid JSON";
                    System.out.println(responseMessage6);

                    byte[] bytesE6 = responseMessage6.getBytes("UTF-8");
                    //Invalid JSON
                    t.sendResponseHeaders(403, bytesE6.length);
                    OutputStream os = t.getResponseBody();
                    os.write(bytesE6);
                    os.close();
                }
            } catch (JSONException e) {

                 //invalid JSON
                 String errorJSON = e.toString();
                 System.out.println("Invalid JSON");
                 byte[] bytesJSONerror = errorJSON.getBytes("UTF-8");
                 t.sendResponseHeaders(400, bytesJSONerror.length);
                 OutputStream os = t.getResponseBody();
                 os.write(bytesJSONerror);
                 os.close();
            }
        } else { //other than POST
            String responseMessage = new String("Not supported");
            byte[] bytesE = responseMessage.getBytes("UTF-8");
            t.sendResponseHeaders(405, bytesE.length);
            OutputStream os = t.getResponseBody();
            os.write(bytesE);
            os.close();
        } 
    }

    private synchronized void createAdmin() {

        AdminUser = new JSONObject(); 
        AdminUser.put("username", "admin")
            .put("password", "admin")
            .put("email", "admin@admin.com");

        try { //user entry to sql database
            db.setUser(AdminUser);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        System.out.println("Admin registered");
    }

}
