package com.company;
import java.rmi.Naming;

public class Main {

    public static void main(String[] args) {
        try {
            Server server = new Server();
            Naming.rebind("tsoro-yematatu-server", server);
            System.out.println("Server started");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
