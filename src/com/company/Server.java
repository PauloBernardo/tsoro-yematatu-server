package com.company;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.Semaphore;

public class Server extends UnicastRemoteObject implements TsoroYematatuServerInterface {
    private final ArrayList<Client> clients;
    private final ArrayList<Client> waitingClients;
    private final Semaphore gameSemaphore;

    public Server() throws RemoteException {
        super();
        clients = new ArrayList<>();
        waitingClients = new ArrayList<>();
        gameSemaphore = new Semaphore(1, true);
    }

    private Client getClientFromRemote(String id) throws Exception {
        Client c = null;
        for (Client client : clients) {
            if (Objects.equals(client.getId(), id)) {
                c = client;
                break;
            }
        }
        if (c == null) throw new Exception("Client not found!");
        return c;
    }

    @Override
    public boolean registry(String id, String path) throws MalformedURLException, NotBoundException, RemoteException {
        TsoroYematatuClient client = (TsoroYematatuClient) Naming.lookup(path);
        clients.add(new Client(id, client));
        return false;
    }

    @Override
    public boolean unregister(String id) throws Exception {
        Client client = getClientFromRemote(id);
        waitingClients.remove(client);

        Game game = client.getLastGame();
        if (game == null) return true;

        Client[] players = game.getPlayers();
        if (game.isFinished() && game.getPlayer1() != null && game.getPlayer2() != null ) {
            game.finish();
            if (game.getPlayer1() == client) {
                game.setWinner(game.getPlayer2());
                game.getPlayer2().getClientRemote().endGame("winner");
            }
            else {
                game.setWinner(game.getPlayer1());
                game.getPlayer1().getClientRemote().endGame("winner");
            }
        } else if (game.isFinished()) {
            game.finish();
            if (players[0] == client) {
                game.setWinner(players[1]);
                players[1].getClientRemote().endGame("your opponent left");
            }
            else {
                game.setWinner(players[0]);
                players[0].getClientRemote().endGame("your opponent left");
            }
        }
        clients.remove(client);
        return false;
    }

    @Override
    public String setName(String id, String name) throws Exception {
        System.out.println("Setting name: " + name);
        Client client = this.getClientFromRemote(id);
        client.setName(name);

        System.out.println(client);
        return name;
    }

    @Override
    public String getName(String id) throws Exception {
        Client client = this.getClientFromRemote(id);

        return client.getName();
    }

    @Override
    public boolean startNewMatch(String id) throws Exception {
        Client client = this.getClientFromRemote(id);
        waitingClients.add(client);
        return true;
    }

    @Override
    public String startRandomMatch(String id) throws Exception {
        Client client = this.getClientFromRemote(id);
        if (waitingClients.size() != 0) {
            Client clientWaitingGame = waitingClients.remove(0);
            Game game = new Game(clientWaitingGame, client);
            clientWaitingGame.addGame(game);
            client.addGame(game);
            clientWaitingGame.getClientRemote().startRandomMatch();
            return "start";
        } else {
            waitingClients.add(client);
            return "wait";
        }
    }

    @Override
    public boolean startChooseMatch(String id, String chooseId) throws Exception {
        Client client = this.getClientFromRemote(id);
        for(Client waitingClient: waitingClients) {
            if (waitingClient.getId().equals(chooseId)) {
                Game game = new Game(waitingClient, client);
                waitingClient.addGame(game);
                client.addGame(game);
                waitingClients.remove(waitingClient);
                waitingClient.getClientRemote().startChooseMatch();
                return true;
            }
        }
        return false;
    }

    @Override
    public ArrayList<GameDescription> getChooseMatch(String id) {
        ArrayList<GameDescription> games = new ArrayList<>();
        for (Client waitingClient : waitingClients) {
            games.add(new GameDescription("", "", waitingClient.getName(), waitingClient.getId()));
        }
        return games;
    }

    @Override
    public ArrayList<GameDescription> getHistory(String id) throws Exception {
        Client client = this.getClientFromRemote(id);
        ArrayList<GameDescription> games = new ArrayList<>();
        int gameNumber = 1;
        for (Game game : client.getGames()) {
            if (game.getWinner() == client) {
                games.add(new GameDescription("" + gameNumber,"winner", "", ""));
            } else if (game.isWasADraw()) {
                games.add(new GameDescription("" + gameNumber,"draw", "", ""));
            } else {
                games.add(new GameDescription("" + gameNumber,"loser", "", ""));
            }
            gameNumber++;
        }
        return games;
    }

    @Override
    public boolean cancelGame(String id) throws Exception {
        Client client = this.getClientFromRemote(id);
        Game game = client.getLastGame();
        waitingClients.remove(client);
        if (game != null && game.isFinished()) {
            Client[] players = game.getPlayers();
            game.finish();
            if (players[0] == client) {
                game.setWinner(players[1]);
                players[1].getClientRemote().endGame("your opponent left");
            }
            else {
                game.setWinner(players[0]);
                players[0].getClientRemote().endGame("your opponent left");
            }
        }
        return true;
    }

    @Override
    public boolean endGame(String id) throws Exception {
        Client client = this.getClientFromRemote(id);
        Game game = client.getLastGame();
        game.finish();
        if (client == game.getPlayer1()) {
            game.setWinner(game.getPlayer2());
            game.getPlayer2().getClientRemote().endGame("winner");
        } else {
            game.setWinner(game.getPlayer1());
            game.getPlayer1().getClientRemote().endGame("winner");
        }
        return true;
    }

    @Override
    public boolean chooseColor(String id, String color) throws Exception {
        Client client = this.getClientFromRemote(id);
        gameSemaphore.acquire();

        try {
            Game game = client.getLastGame();
            for (Client c : clients) {
                if (c.getLastGame() == game && c != client) {
                    if (c.getColor().equals(color)) {
                        throw new Exception("chooseColor:ERROR");
                    } else {
                        client.setColor(color);
                        c.getClientRemote().chooseColor(color);
                    }
                }
            }
        } catch (Exception e) {
            gameSemaphore.release();
            throw e;
        }

        gameSemaphore.release();
        return true;
    }

    @Override
    public boolean move(String id, int older, int newer) throws Exception {
        Client client = this.getClientFromRemote(id);
        Game game = client.getLastGame();
        if (game == null) return false;
        if (
                (client == game.getPlayer1() && game.getCurrentPlayer() == 1)
                        ||
                (client == game.getPlayer2() && game.getCurrentPlayer() == 2)
        ) {
            try {
                game.playerMove(older, newer);
                if (client == game.getPlayer1()) {
                    game.getPlayer2().getClientRemote().move(older, newer);
                    game.getPlayer2().getClientRemote().turn();
                } else {
                    game.getPlayer1().getClientRemote().move(older, newer);
                    game.getPlayer1().getClientRemote().turn();
                }
                char winner = game.checkIfHasAWinner();
                if (winner == '1') {
                    game.getPlayer1().getClientRemote().endGame("winner");
                    game.getPlayer2().getClientRemote().endGame("loser");
                } else if (winner == '2') {
                    game.getPlayer1().getClientRemote().endGame("loser");
                    game.getPlayer2().getClientRemote().endGame("winner");
                }
            } catch (Exception e) {
                throw new Exception("move:ERROR," + e.getMessage());
            }
        } else {
            throw new Exception("move:ERROR,invalidPlayer");
        }
        return true;
    }

    @Override
    public boolean choosePlayer(String id, String player) throws Exception {
        Client client = this.getClientFromRemote(id);
        gameSemaphore.acquire();

        try {
            Game game = client.getLastGame();
            if (game == null) {
                gameSemaphore.release();
                return false;
            }
            for (Client c : clients) {
                if (c.getLastGame() == game && c != client) {
                    if (game.getPlayer1() == c && player.equals("player1")) {
                        game.setPlayer2(client);
                        c.getClientRemote().begin("player1");
                        client.getClientRemote().begin("player2");
                        gameSemaphore.release();
                        return false;
                    } else if (game.getPlayer2() == c && player.equals("player2")) {
                        game.setPlayer1(client);
                        c.getClientRemote().begin("player2");
                        client.getClientRemote().begin("player1");
                        gameSemaphore.release();
                        return false;
                    } else if (game.getPlayer1() != c && game.getPlayer2() != c) {
                        if (player.equals("player1")) {
                            game.setPlayer1(client);
                        } else {
                            game.setPlayer2(client);
                        }
                    } else {
                        System.out.println(game.getPlayer1());
                        System.out.println(game.getPlayer2());
                        System.out.println(c);
                        if (player.equals("player1")) {
                            game.setPlayer1(client);
                            c.getClientRemote().begin("player2");
                            client.getClientRemote().begin("player1");
                        } else {
                            game.setPlayer2(client);
                            c.getClientRemote().begin("player1");
                            client.getClientRemote().begin("player2");
                        }
                    }
                }
            }
        } catch (Exception e) {
            gameSemaphore.release();
            throw e;
        }

        gameSemaphore.release();
        return true;
    }

    @Override
    public boolean drawGame(String id, String response) throws Exception {
        Client client = this.getClientFromRemote(id);
        gameSemaphore.acquire();

        try {
            Game game = client.getLastGame();
            if (game == null) {
                gameSemaphore.release();
                return false;
            }
            if (game.getAwaitingForDraw() != client && (game.getAwaitingForDraw() == game.getPlayer1() || game.getAwaitingForDraw() == game.getPlayer2())) {
                if (response.equals("YES")) {
                    game.setWasADraw(true);
                    game.finish();
                    game.getPlayer2().getClientRemote().drawGame("draw");
                    game.getPlayer1().getClientRemote().drawGame("draw");
                } else if (response.equals("NO")) {
                    game.askForDraw(null);
                    if (game.getPlayer1() == client) {
                        game.getPlayer2().getClientRemote().drawGame("refused");
                    } else {
                        game.getPlayer1().getClientRemote().drawGame("refused");
                    }
                }
            } else if (game.getAwaitingForDraw() == null && response.equals("YES")) {
                game.askForDraw(client);
                client.getClientRemote().drawGame("wait");
                if (game.getPlayer1() == client)
                    game.getPlayer2().getClientRemote().drawGame("ask");
                else
                    game.getPlayer1().getClientRemote().drawGame("ask");
            }
        } catch (Exception e) {
            gameSemaphore.release();
            throw e;
        }

        gameSemaphore.release();
        return false;
    }

    @Override
    public String chatMessage(String id, String message) throws Exception {
        Client client = this.getClientFromRemote(id);
        Game game = client.getLastGame();
        if (game == null) return null;

        if (game.getPlayer1() == client)
            game.getPlayer2().getClientRemote().chatMessage(client.getName(), message);
        else
            game.getPlayer1().getClientRemote().chatMessage(client.getName(), message);

        return message;
    }
}
