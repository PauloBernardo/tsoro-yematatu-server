package com.company;

import java.awt.*;

public class Game {
    private String board;
    /*
            0
         1  2  3
       4    5    6
     */
    private boolean isFinished;
    private Client player1;
    private Client player2;
    private int currentPlayer;
    private int turnNumber;
    private Client winner = null;
    private boolean wasADraw;
    private String player1Color;
    private String player2Color;
    private Client awaitingForDraw;

    public Game() {
        this.board = "0000000";
        this.isFinished = false;
        this.currentPlayer = 1;
        this.turnNumber = 0;
        this.wasADraw = false;
    }

    public String getBoard() {
        return board;
    }

    public void finish() {
        this.isFinished = true;
    }

    public boolean getIsFinished() {
        return this.isFinished;
    }

    private boolean isNumbersInCorrectRange(int older, int newer) {
        if (older < -1 || older > 6) return false;
        return newer >= 0 && newer <= 6;
    }

    private boolean isTheMoveValid(int older, int newer) {
        int numberOfZero = 0;
        char temp;
        for (int i = 0; i < board.length(); i++) {
            temp = board.charAt(i);
            if (temp == '0')
                numberOfZero++;
        }
        if (older == -1 && numberOfZero < 2) {
            return false;
        }
        if (older != -1) {
            if (board.charAt(older) != (char)(currentPlayer + '0')) {
                return false;
            }
            if (board.charAt(newer) != '0') {
                return false;
            }
            if (older == 1 && (newer == 5 || newer == 6)) {
                return false;
            }
            if (older == 2 && (newer == 4 || newer == 6)) {
                return false;
            }
            if (older == 3 && (newer == 4 || newer == 5)) {
                return false;
            }
            if (older == 4 && (newer == 2 || newer == 3)) {
                return false;
            }
            if (older == 5 && (newer == 1 || newer == 3)) {
                return false;
            }
            if (older == 6 && (newer == 1 || newer == 2)) {
                return false;
            }
        }
        return board.charAt(newer) == '0';
    }

    public void playerMove(int older, int newer) throws Exception {
        if (this.isFinished) throw new Exception("Game is finished!");
        if (!isNumbersInCorrectRange(older, newer)) throw new Exception("Numbers older and/or newer not in correct range!");
        if (!isTheMoveValid(older, newer)) throw new Exception("The move is not valid!");
        char[] myBoardChars = board.toCharArray();
        if (older != -1) {
            myBoardChars[older] = '0';
        }
        myBoardChars[newer] = (char) (currentPlayer + '0');


        board = String.valueOf(myBoardChars);
        this.currentPlayer = currentPlayer == 2 ? 1 : 2;
        turnNumber++;
        printBoard();
    }

    public void printBoard() {
        System.out.println("Turn number: " + this.turnNumber);
        System.out.println("\t\t" + board.charAt(0));
        System.out.print("\t" + board.charAt(1) + "\t" + board.charAt(2) + "\t" + board.charAt(3) + "\n");
        System.out.println(board.charAt(4) + "\t\t" + board.charAt(5) + "\t\t" + board.charAt(6) + "\n");
    }

    /*
    Return '0' if there is no winner
    Return '1' if player 1 is the winner
    Return '2' if player 2 is the winner
     */
    public char checkIfHasAWinner () {
        int numberOfZero = 0;
        char temp;
        for (int i = 0; i < board.length(); i++) {
            temp = board.charAt(i);
            if (temp == '0')
                numberOfZero++;
        }
        if (numberOfZero > 1) {
            return '0';
        }
        if (board.charAt(0) == board.charAt(1) && board.charAt(1) == board.charAt(4)) {
            return board.charAt(0);
        }
        if (board.charAt(0) == board.charAt(2) && board.charAt(2) == board.charAt(5)) {
            return board.charAt(0);
        }
        if (board.charAt(0) == board.charAt(3) && board.charAt(3) == board.charAt(6)) {
            return board.charAt(0);
        }
        if (board.charAt(1) == board.charAt(2) && board.charAt(2) == board.charAt(3)) {
            return board.charAt(1);
        }
        if (board.charAt(4) == board.charAt(5) && board.charAt(5) == board.charAt(6)) {
            return board.charAt(4);
        }
        return '0';
    }

    public Client getWinner() {
        if (this.winner == null) {
            char winner = checkIfHasAWinner();
            if (winner == '1') return player1;
            if (winner == '2') return player2;
        }
        return this.winner;
    }

    public Client getPlayer1() {
        return player1;
    }

    public Client getPlayer2() {
        return player2;
    }

    public void setPlayer1(Client player1) {
        this.player1 = player1;
    }

    public void setPlayer2(Client player2) {
        this.player2 = player2;
    }

    public void setWinner(Client winner) {
        this.winner = winner;
    }

    public void setPlayer1Color(String color) {
        player1Color = color;
    }

    public void setPlayer2Color(String color) {
        player2Color = color;
    }

    public String getPlayer1Color() {
        return player1Color;
    }

    public String getPlayer2Color() {
        return player2Color;
    }

    public int getCurrentPlayer() {
        return this.currentPlayer;
    }

    public void askForDraw(Client player) {
        this.awaitingForDraw = player;
    }

    public Client getAwaitingForDraw() {
        return awaitingForDraw;
    }

    public boolean isWasADraw() {
        return wasADraw;
    }

    public void setWasADraw(boolean wasADraw) {
        this.wasADraw = wasADraw;
    }
}
