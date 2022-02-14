package com.company;

import java.net.Socket;
import java.util.ArrayList;

public class Client {
    private final Socket id;
    private final Long timeId;
    private String name;
    private ArrayList<Game> games;
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

    public void setGames(ArrayList<Game> games) {
        this.games = games;
    }

    public Game getLastGame() {
        return games.get(games.size()-1);
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
