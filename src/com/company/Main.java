package com.company;

import java.net.Socket;

public class Main {

    public static void main(String[] args) {
        new Server().start();
    }

    public static void playSimpleExample() {
        System.out.println("Hello World!");

        Client player1 = new Client(new Socket());
        Client player2 = new Client(new Socket());
        Game game = new Game(player1, player2);
        player1.setName("Paulo");
        player2.setName("Jose");
        player1.addGame(game);
        player2.addGame(game);
        System.out.println(game.getBoard());
        try {
            game.playerMove(-1, 1);
            game.playerMove(-1, 2);
            game.playerMove(-1, 3);
            game.playerMove(-1, 4);
            game.playerMove(-1, 5);
            game.playerMove(-1, 6);
            System.out.println(game.getBoard());
            if (game.checkIfHasAWinner() != '0') {
                game.finish();
            }
            game.playerMove(1, 0);
            game.playerMove(2, 1);
            game.playerMove(3, 2);
            if (game.checkIfHasAWinner() != '0') {
                game.finish();
            }
            System.out.println("Winner: " + game.getWinner().getName());

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
