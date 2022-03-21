package com.company;

import java.io.Serializable;

public class GameDescription implements Serializable {
    private String game;
    private String result;
    private String player;
    private String id;


    public GameDescription(String game, String result, String player, String id) {
        this.game = new String(game);
        this.result = new String(result);
        this.player = new String(player);
        this.id = new String(id);
    }

    public String getGame() {
        return game;
    }

    public String gameProperty() {
        return game;
    }

    public void setGame(String game) {
        this.game = game;
    }

    public String getResult() {
        return result;
    }

    public String resultProperty() {
        return result;
    }

    public String getPlayer() {
        return player;
    }

    public String playerProperty() {
        return player;
    }

    public void setPlayer(String player) {
        this.player = player;
    }

    public String getId() {
        return id;
    }

    public String idProperty() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setResult(String result) {
        this.result = result;
    }
}
