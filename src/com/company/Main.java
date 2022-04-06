package com.company;
import java.rmi.Naming;

public class Main {

    public static void main(String[] args) {
        String serverUrl = args.length > 0 ? args[0] : "rmi://localhost:5431/";
        try {
            Server server = new Server();
            Naming.rebind(serverUrl + "tsoro-yematatu-server", server);
            System.out.println("Server started");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
