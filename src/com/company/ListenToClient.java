package com.company;

import java.net.Socket;
import java.util.Scanner;

public record ListenToClient(Socket client, Server root) implements Runnable {

    public void run() {
        try {
            System.out.println("Client connected on IP " + client.getInetAddress().getHostAddress());

            //
            Scanner input = new Scanner(client.getInputStream());
            while (input.hasNextLine()) {
                String nextLine = input.nextLine();
                try {
                    root.handleInputStream(client, nextLine);
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("Error on handling message: " + nextLine);
                }
            }

            System.out.println("Client disconnected on IP " + client.getInetAddress().getHostAddress());
            input.close();

            root.handleClientClose(client);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
