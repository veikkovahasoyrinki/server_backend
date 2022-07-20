package com.server;

import com.sun.net.httpserver.BasicAuthenticator;

public class UserAuthenticator extends BasicAuthenticator {

    /*
        In this class is a method which validates users.
        checkCredientials first calls database usernameCheck which checks the database if a username exists.
        usernameCheck returns true if that username exists, but it does not handle passwords.

        validateUser checks the incoming password and compares that to a one found in the database.
        Returns true if given password is valid.


    */

    public UserAuthenticator(String realm, CoordDatabase database) {
        super(realm);
        db = database;
    }

    final CoordDatabase db;

    @Override
    public synchronized boolean checkCredentials(String id, String password) {
        if (password != null && id != null) {
            if (db.usernameCheck(id)) { //Validate username first
                if (db.validateUser(id, password)) { //If username is valid, validate password
                    return true;
                }
            }
        }
        System.out.println("Invalid credientials with request");
        return false;
    }
}

