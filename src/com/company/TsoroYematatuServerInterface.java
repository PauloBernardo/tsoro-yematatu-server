package com.company;

import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.ServerNotActiveException;
import java.util.ArrayList;

public interface TsoroYematatuServerInterface extends Remote {

    public boolean registry(String path) throws RemoteException, MalformedURLException, NotBoundException, ServerNotActiveException;
    public boolean unregister() throws Exception;
    public String setName(String name) throws Exception;
    public String getName() throws Exception;
    public boolean startNewMatch() throws Exception;
    public String startRandomMatch() throws Exception;
    public boolean startChooseMatch(String chooseId) throws Exception;
    public ArrayList<GameDescription> getChooseMatch() throws Exception;
    public ArrayList<GameDescription> getHistory() throws Exception;
    public boolean cancelGame() throws Exception;
    public boolean endGame() throws Exception;
    public boolean chooseColor(String color) throws Exception;
    public boolean move(int older, int newer) throws Exception;
    public boolean choosePlayer(String player) throws Exception;
    public boolean drawGame(String response) throws Exception;
    public String chatMessage(String message) throws Exception;

}
