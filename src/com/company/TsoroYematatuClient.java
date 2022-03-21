package com.company;

import java.rmi.Remote;

public interface TsoroYematatuClient extends Remote {

    public void getName(String name);
    public void startRandomMatch();
    public void startChooseMatch();
    public void cancelGame();
    public void endGame(String status);
    public void chooseColor(String color) throws Exception;
    public void move(int older, int newer) throws Exception;
    public void turn();
    public void begin(String player);
    public void choosePlayer(String color) throws Exception;
    public void drawGame(String response);
    public void chatMessage(String name, String message);

}
