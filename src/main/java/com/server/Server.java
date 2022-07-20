package com.server;



import com.sun.net.httpserver.*;
import javax.net.ssl.*;
import java.net.InetSocketAddress;
import java.security.KeyStore;
import java.io.FileInputStream;
import java.util.concurrent.Executors;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;


/**
    This HTTPS server uses SQLite database. Database is a singleton.
    CoordDatabase.getInstance method is called which creates a new database object.
    When a new CoordDatabase object is created, ServerDatabase sqlite file is created 
    if there is no such file. If the ServerDatabase file exists, only a connection to that file is created.

    Admin user is always created to a new database. If ServerDatabase already exists, it's checked 
    if admin exists. 
    
    Server can be stopped with "/quit", address is https://localhost/

    Currently available contexts are /coordinates and /registration


    Author: Veikko Vähäsöyrinki 

 */

class Server {

    private static SSLContext coordinateServerSSLContext(String key, String pass) throws Exception {
        char[] passphrase = pass.toCharArray();
        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(new FileInputStream(key), passphrase);
 
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, passphrase);
 
        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(ks);
 
        SSLContext ssl = SSLContext.getInstance("TLS");
        ssl.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
        return ssl;
    }


    public static void main(String[] args) throws Exception {

        /*
        Database is instatiated here, and passed as a parameter 
        to the UserAuthenticator, CoordinatesHandler and RegistrationHandler

        */
        HttpsServer server = HttpsServer.create(new InetSocketAddress(8001),0);


        CoordDatabase database = CoordDatabase.getInstance();

        //database is passed as a parameter to all classes that need it
        UserAuthenticator authorizer = new UserAuthenticator("coordinates", database);
        HttpContext authCoordinates = server.createContext("/coordinates", new CoordinatesHandler(database)); 
        HttpContext authRegister = server.createContext("/registration", new RegistrationHandler(authorizer, database));

        authCoordinates.setAuthenticator(authorizer);

        SSLContext sslContext = coordinateServerSSLContext(args[0], args[1]);
        server.setHttpsConfigurator (new HttpsConfigurator(sslContext) {
            public void configure (HttpsParameters params) {
                try {
                    InetSocketAddress remote = params.getClientAddress();
                    SSLContext c = getSSLContext();
                    SSLParameters sslparams = c.getDefaultSSLParameters();
                    params.setSSLParameters(sslparams);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });


        server.setExecutor(Executors.newFixedThreadPool(3));
        server.start(); 

        // Ability to stop the server from the console, type "/quit"
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String s;
        Boolean running = true;

        while (running) {
            s = br.readLine();
            if (s.equals("/quit")) {
                System.out.println("Stopping the server...");
                running = false;
                server.stop(3);
                database.closeDB();
                System.exit(0);
            }
        }

    }
}
