package com.company;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class ListenToClient implements Runnable {
    private Socket client;
    private ServerSocket server;
    private Server root;

    public ListenToClient(ServerSocket server, Socket client, Server root) {
        this.client = client;
        this.server = server;
        this.root = root;
    }

    public void run() {
        try{
            System.out.println("Cliente conectado do IP " + client.getInetAddress().
                    getHostAddress());
            Scanner entrada = new Scanner(client.getInputStream());
            while(entrada.hasNextLine()){
                root.handleInputStream(client, entrada.nextLine());
            }

            System.out.println("Cliente desconectado do IP " + client.getInetAddress().
                    getHostAddress());
            entrada.close();
            root.handleClientClose(client);
            root.removeClient(client);
        } catch (Exception e){}
    }
}
