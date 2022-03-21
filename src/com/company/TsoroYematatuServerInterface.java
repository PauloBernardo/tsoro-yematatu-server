package com.company;

import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.ServerNotActiveException;
import java.util.ArrayList;

public interface TsoroYematatuServerInterface extends Remote {

    public boolean registry(String id, String path) throws RemoteException, MalformedURLException, NotBoundException, ServerNotActiveException;
    public boolean unregister(String id) throws Exception;
    public String setName(String id, String name) throws Exception;
    public String getName(String id) throws Exception;
    public boolean startNewMatch(String id) throws Exception;
    public String startRandomMatch(String id) throws Exception;
    public boolean startChooseMatch(String path, String chooseId) throws Exception;
    public ArrayList<GameDescription> getChooseMatch(String path) throws Exception;
    public ArrayList<GameDescription> getHistory(String path) throws Exception;
    public boolean cancelGame(String path) throws Exception;
    public boolean endGame(String path) throws Exception;
    public boolean chooseColor(String path, String color) throws Exception;
    public boolean move(String path, int older, int newer) throws Exception;
    public boolean choosePlayer(String path, String player) throws Exception;
    public boolean drawGame(String path, String response) throws Exception;
    public String chatMessage(String path, String message) throws Exception;

}
