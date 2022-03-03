package com.company;

public class Main {

    public static void main(String[] args) {
        int serverPort = args.length > 0 ? Integer.parseInt(args[0]) : 3322;
        new Server().start(serverPort);
    }
}
