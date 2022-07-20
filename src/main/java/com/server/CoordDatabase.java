package com.server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.RejectedExecutionException;
import java.sql.PreparedStatement;
import org.json.JSONArray;
import org.json.JSONObject;
import org.sqlite.SQLiteException;

import java.time.*;
import java.util.Base64;
import java.security.SecureRandom;
import org.apache.commons.codec.digest.Crypt;

public class CoordDatabase {

    private Connection dbConnection = null;
    private static CoordDatabase dbInstance = null;

	public static synchronized CoordDatabase getInstance() {
		if (null == dbInstance) {
			dbInstance = new CoordDatabase();
		}
        return dbInstance;
    }

    /*
        ServerDatabase has 2 tables: userdata and coordsdata
        Userdata contains registered users, passwords are hashed and are not stored as plain text to the database

        Coordsdata contains the coordinate messages.

    */

    private CoordDatabase(){

        try {
            init();
        } catch (SQLException e) {
            System.out.println(e);
        }

    }

    private synchronized boolean init() throws SQLException {

        String dbName = "ServerDatabase";

        String database = "jdbc:sqlite:" + dbName;
        dbConnection = DriverManager.getConnection(database);


        if (null != dbConnection) {
            try { //SQLlite error will pop up if ServerDatabase already exists

                String createTable = "create table userdata (username varchar(50) NOT NULL, password varchar(500) NOT NULL, email varchar(50) NOT NULL, salt varchar(50) NOT NULL)";
                Statement createStatement = dbConnection.createStatement();
                createStatement.executeUpdate(createTable);
                createStatement.close();
                createTable = "create table coordsdata (username varchar(50) NOT NULL, longitude REAL NOT NULL, latitude REAL NOT NULL, timestamp int, description varchar(100))";
                createStatement = dbConnection.createStatement();
                createStatement.executeUpdate(createTable);
                createStatement.close();
    
                System.out.println("DB successfully created");    
                return true;

            } catch (SQLiteException e) {
                //Catching the error, this means that the connection is ok with and existing database is used
                System.out.println("Using existing ServerDatabase");
                return true;
            }

        }

        System.out.println("DB creation failed");
        return false;


    }
    public synchronized void closeDB() throws SQLException {
		if (null != dbConnection) {
			dbConnection.close();
            System.out.println("Closing db connection");
			dbConnection = null;
            System.out.println("Database connection closed, good night");
		}
    }
    public synchronized void setCoordinateMessage(String username, Double longitude, Double latitude, long time, String description) throws SQLException {

        /*
            The given username in the JSON message could actually be anything, and it is not checked 
            Because the authorization is done using the Authorization header.

        */
        String setMessageString = "insert into coordsdata " +
					"VALUES('" + username + "','" + longitude + "','" + latitude + "','" + time +  "','" + description + "')"; 
		Statement createStatement;
		createStatement = dbConnection.createStatement();
		createStatement.executeUpdate(setMessageString);
        System.out.println("New coordinates posted");

		createStatement.close();
    }

    public synchronized void setUser(JSONObject user) throws SQLException {
        //create salt
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[13];
        random.nextBytes(bytes);
        String saltBytes = new String(Base64.getEncoder().encode(bytes));
        String salt = "$6$" + saltBytes;

        String hashedPassword = Crypt.crypt(user.getString("password"), salt);

		String setMessageString = "insert into userdata " +
					"VALUES('" + user.getString("username") + "','" + hashedPassword + "','" + user.getString("email") + "','" + salt + "')"; 
		Statement createStatement;
		createStatement = dbConnection.createStatement();
		createStatement.executeUpdate(setMessageString);
        System.out.println("New input to user database");

		createStatement.close();
    }
    
    public synchronized JSONArray getMessages() throws SQLException {

        /*
            This method is called when a GET request is done to the /coordinates 
            All rows of the tabe coordsdata are selected and JSON array is constructed from them

            JSONArray is returned. This is because JSONArray is easy to send as a response to the GET request.
            See the CoordinatesHandler.java line 97 how a GET request is served.
        
        */
        Statement queryStatement = null;

        String getMessagesString = "select * from coordsdata ";

        queryStatement = dbConnection.createStatement();
		ResultSet rs = queryStatement.executeQuery(getMessagesString);
        JSONArray array = new JSONArray();

        /*
            A row of the table is formatted to JSONObject object
            And that object is then inserted to a JSONArray

        */
        while (rs.next()) {
            JSONObject obj = new JSONObject();
            obj.put("username", rs.getString("username"));
            obj.put("longitude", rs.getDouble("longitude"));
            obj.put("latitude", rs.getDouble("latitude"));
            obj.put("sent", setSent(rs.getLong("timestamp")));

            /*
                If a message doesn't contain a description, the JSON key "description" is left out
            */

            if (!rs.getString("description").equals("[no description]")) {
                obj.put("description", rs.getString("description"));
            }
            array.put(obj);
		}

        return array;

    }

    public synchronized boolean usernameCheck(String username) {
         
        /*
            This method returns true if given string "username" exists in the table userdata.

            This method is used when a new registation is submitted for checking the database
            if that username is already registered.
            It's also used when authenticating requests for /coordinates context.
        */

        try {
            String query = "SELECT (count(*) > 0) as found FROM userdata WHERE username LIKE ?";
            PreparedStatement pst = dbConnection.prepareStatement(query);
            pst.setString(1, username);
    
            try (ResultSet rs = pst.executeQuery()) {
                // Only expecting a single result
                if (rs.next()) {
                    boolean found = rs.getBoolean(1); // "found" column
                    if (found) {
                        return true;
                    } else {
                        return false;
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return true;
    }

    public synchronized boolean validateUser(String username, String password) { 

        /*
            returns true if given password matches the one found from the db for the username
        
        */
        try {
            Statement queryStatement = null;

            String getMessagesString = "select password from userdata where username = \"" + username + "\"";
    
            queryStatement = dbConnection.createStatement();
            ResultSet rs = queryStatement.executeQuery(getMessagesString);

            while (rs.next()) {
                if (rs.getString("password").equals(Crypt.crypt(password, rs.getString("password")))) {
                    return true;
                } else {
                    return false;
                }

            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return true;
    }

    public synchronized ZonedDateTime setSent(long epoch) { 
    
        /*
            This method is used when user requests coordinate messages with GET to /coordinates
            
            Timestamps of coordinate messages are converted from ISO date format to UNIX time.
            This method returns ZonedDateTime object. Have to use ZonedDateTime instead of LocalDateTime
            because ZonedDateTime includes timezone Z in the end
        */

        return ZonedDateTime.ofInstant(Instant.ofEpochMilli(epoch), ZoneOffset.UTC); 
    }
}