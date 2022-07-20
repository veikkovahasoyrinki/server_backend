package com.server;
import org.json.*;

public class User {
    User(JSONObject objectParam) {
        username = objectParam.getString("username");
        password = objectParam.getString("password");
        email = objectParam.getString("email");
        object = objectParam;
    }
    JSONObject object;
    private String username;
    private String password;
    private String email;

    String getUsername() {
        return username;
    }

}
