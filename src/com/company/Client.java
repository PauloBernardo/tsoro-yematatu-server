package com.company;

import java.net.Socket;
import java.util.ArrayList;

public class Client {
    private final Socket id;
    private final Long timeId;
    private String name;
    private final ArrayList<Game> games;
    private String currentColor;

    public Client(Socket id) {
        this.id = id;
        this.games = new ArrayList<>();
        this.timeId = System.currentTimeMillis();
        this.currentColor = "";
    }

    public Socket getId() {
        return id;
    }

    public Long getTimeId() {
        return timeId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ArrayList<Game> getGames() {
        return games;
    }

    public Game getLastGame() {
        if (games.size() > 0) return games.get(games.size()-1);
        return null;
    }

    public void addGame(Game game) {
        this.games.add(game);
    }

    public void setColor(String color) {
        currentColor = color;
    }

    public String getColor() {
        return currentColor;
    }
}
