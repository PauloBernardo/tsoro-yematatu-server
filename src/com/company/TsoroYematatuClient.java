package com.company;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface TsoroYematatuClient extends Remote {

    public void getName(String name) throws RemoteException;
    public void startRandomMatch() throws RemoteException;
    public void startChooseMatch() throws RemoteException;
    public void cancelGame() throws RemoteException;
    public void endGame(String status) throws Exception;
    public void chooseColor(String color) throws Exception;
    public void move(int older, int newer) throws Exception;
    public void turn() throws RemoteException;
    public void begin(String player) throws RemoteException;
    public void choosePlayer(String color) throws Exception;
    public void drawGame(String response) throws Exception;
    public void chatMessage(String name, String message) throws RemoteException;

}
