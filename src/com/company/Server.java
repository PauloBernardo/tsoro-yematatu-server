package com.company;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.Semaphore;

public class Server {
    private final ArrayList<Client> clients;
    private final ArrayList<Client> waitingClients;
    private final Semaphore gameSemaphore;

    public Server() {
        clients = new ArrayList<>();
        waitingClients = new ArrayList<>();
        gameSemaphore = new Semaphore(1, true);
    }

    public void start(int serverPort) {
        try {
            ServerSocket server = new ServerSocket(serverPort);
            System.out.println("Server started on port " + serverPort);

            while (true) {
                Socket client = server.accept();
                clients.add(new Client(client));
                new Thread(new ListenToClient(client, this)).start();
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    private void sendResponse(Socket client, String message) throws IOException {
        DataOutputStream out = new DataOutputStream(client.getOutputStream());
        out.writeUTF(message);
        out.flush();
    }

    public void handleClientClose(Socket socketClient) throws IOException {
        Client client = getClientFromSocket(socketClient);
        waitingClients.remove(client);

        Game game = client.getLastGame();
        if (game == null) return;

        Client[] players = game.getPlayers();
        if (game.isFinished() && game.getPlayer1() != null && game.getPlayer2() != null ) {
            game.finish();
            if (game.getPlayer1() == client) {
                game.setWinner(game.getPlayer2());
                sendResponse(game.getPlayer2().getId(), "endGame:OK,winner");
            }
            else {
                game.setWinner(game.getPlayer1());
                sendResponse(game.getPlayer1().getId(), "endGame:OK,winner");
            }
        } else if (game.isFinished()) {
            game.finish();
            if (players[0] == client) {
                game.setWinner(players[1]);
                sendResponse(players[1].getId(), "endGame:OK,your opponent left");
            }
            else {
                game.setWinner(players[0]);
                sendResponse(players[0].getId(), "endGame:OK,your opponent left");
            }
        }
        clients.remove(client);
    }

    private Client getClientFromSocket(Socket socketClient) {
        Client c = null;
        for (Client client : clients) {
            if (client.getId() == socketClient) {
                c = client;
                break;
            }
        }
        return c;
    }

    public void handleInputStream(Socket socketClient, String stream) throws IOException, InterruptedException {
        Client client = this.getClientFromSocket(socketClient);
        if (client == null) return;

        if (stream.startsWith("setName:")) {
            client.setName(stream.substring(8));
            sendResponse(socketClient, "setName:OK");
        }

        if (stream.startsWith("getName:")) {
            sendResponse(socketClient, "getName:OK," + client.getName());
        }

        if (stream.startsWith("startNewMatch:")) {
            waitingClients.add(client);
            sendResponse(socketClient, "startNewMatch:OK");
        }

        if (stream.startsWith("startRandomMatch:")) {
            if (waitingClients.size() != 0) {
                Client clientWaitingGame = waitingClients.remove(0);
                Game game = new Game(clientWaitingGame, client);
                clientWaitingGame.addGame(game);
                client.addGame(game);
                sendResponse(clientWaitingGame.getId(), "startRandomMatch:OK,start");
                sendResponse(socketClient, "startRandomMatch:OK,start");
            } else {
                waitingClients.add(client);
                sendResponse(socketClient, "startRandomMatch:OK,wait");
            }
        }

        if (stream.startsWith("startChooseMatch:")) {
            for(Client waitingClient: waitingClients) {
                String waitingClientId = waitingClient.getName() + "#" + waitingClient.getTimeId();
                String chooseId = stream.substring(17);

                if (waitingClientId.equals(chooseId)) {
                    Game game = new Game(waitingClient, client);
                    waitingClient.addGame(game);
                    client.addGame(game);
                    waitingClients.remove(waitingClient);
                    sendResponse(waitingClient.getId(), "startChooseMatch:OK,start");
                    sendResponse(socketClient, "startChooseMatch:OK,start");
                    return;
                }
            }
            sendResponse(socketClient, "startChooseMatch:ERROR");
        }

        if (stream.startsWith("getChooseMatch:")) {
            DataOutputStream out = new DataOutputStream(socketClient.getOutputStream());
            StringBuilder clientIds = new StringBuilder("getChooseMatch:OK");
            for (Client waitingClient : waitingClients) {
                clientIds.append(",").append(waitingClient.getName()).append("#").append(waitingClient.getTimeId());
            }
            out.writeUTF(clientIds.toString());
            out.flush();
        }

        if (stream.startsWith("getHistory:")) {
            DataOutputStream out = new DataOutputStream(socketClient.getOutputStream());
            StringBuilder clientIds = new StringBuilder("getHistory:OK");
            int gameNumber = 1;
            for (Game game : client.getGames()) {
                clientIds.append(",").append("Game: ").append(gameNumber).append(" -----> ");
                if (game.getWinner() == client) {
                    clientIds.append("Winner");
                } else if (game.isWasADraw()) {
                    clientIds.append("Draw");
                } else {
                    clientIds.append("Loser");
                }
                gameNumber++;
            }
            out.writeUTF(clientIds.toString());
            out.flush();
        }

        if (stream.startsWith("cancelGame:")) {
            Game game = client.getLastGame();
            waitingClients.remove(client);
            sendResponse(socketClient, "cancelGame:OK");
            if (game != null && game.isFinished()) {
                Client[] players = game.getPlayers();
                game.finish();
                if (players[0] == client) {
                    game.setWinner(players[1]);
                    sendResponse(players[1].getId(), "endGame:OK,your opponent left");
                }
                else {
                    game.setWinner(players[0]);
                    sendResponse(players[0].getId(), "endGame:OK,your opponent left");
                }
            }
        }

        if (stream.startsWith("endGame:")) {
            Game game = client.getLastGame();
            game.finish();
            sendResponse(socketClient, "endGame:OK,giveUp");
            if (client == game.getPlayer1()) {
                game.setWinner(game.getPlayer2());
                sendResponse(game.getPlayer2().getId(), "endGame:OK,winner");
            } else {
                game.setWinner(game.getPlayer1());
                sendResponse(game.getPlayer1().getId(), "endGame:OK,winner");
            }
        }

        if (stream.startsWith("chooseColor:")) {
            gameSemaphore.acquire();

            Game game = client.getLastGame();
            for (Client c : clients) {
                if (c.getLastGame() == game && c != client) {
                    if (c.getColor().equals(stream.substring(12))) {
                        sendResponse(socketClient, "chooseColor:ERROR");
                    } else {
                        client.setColor(stream.substring(12));
                        sendResponse(socketClient, "chooseColor:OK,yourself," + stream.substring(12));
                        sendResponse(c.getId(), "chooseColor:OK,another," + stream.substring(12));
                    }
                }
            }

            gameSemaphore.release();
        }

        if (stream.startsWith("move:")) {
            Game game = client.getLastGame();
            if (game == null) return;
            int older = Integer.parseInt(stream.split(":")[1].split(",")[0]);
            int newer = Integer.parseInt(stream.split(":")[1].split(",")[1]);
            if (
                    (client == game.getPlayer1() && game.getCurrentPlayer() == 1)
                            || (client == game.getPlayer2() && game.getCurrentPlayer() == 2)
            ) {
                try {
                    game.playerMove(older, newer);
                    if (client == game.getPlayer1()) {
                        sendResponse(socketClient, "move:OK,player1," + stream.substring(5));
                        sendResponse(game.getPlayer2().getId(), "move:OK,player1," + stream.substring(5));
                        sendResponse(socketClient, "turn:OK,player2");
                        sendResponse(game.getPlayer2().getId(), "turn:OK,player2");
                    } else {
                        sendResponse(socketClient, "move:OK,player2," + stream.substring(5));
                        sendResponse(game.getPlayer1().getId(), "move:OK,player2," + stream.substring(5));
                        sendResponse(socketClient, "turn:OK,player1");
                        sendResponse(game.getPlayer1().getId(), "turn:OK,player1");
                    }
                    char winner = game.checkIfHasAWinner();
                    if (winner == '1') {
                        sendResponse(game.getPlayer1().getId(), "endGame:OK,winner");
                        sendResponse(game.getPlayer2().getId(), "endGame:OK,loser");
                    } else if (winner == '2') {
                        sendResponse(game.getPlayer1().getId(), "endGame:OK,loser");
                        sendResponse(game.getPlayer2().getId(), "endGame:OK,winner");
                    }
                } catch (Exception e) {
                    sendResponse(socketClient, "move:ERROR," + e.getMessage());
                }
            } else {
                sendResponse(socketClient, "move:ERROR,invalidPlayer");
            }
        }

        if (stream.startsWith("choosePlayer:")) {
            gameSemaphore.acquire();

            Game game = client.getLastGame();
            if (game == null) return;
            for (Client c : clients) {
                if (c.getLastGame() == game && c != client) {
                    if (game.getPlayer1() == c && stream.substring(13).equals("player1")) {
                        sendResponse(socketClient, "choosePlayer:ERROR,player2");
                        game.setPlayer2(client);
                        sendResponse(c.getId(), "begin:OK");
                        sendResponse(socketClient, "begin:OK");
                        sendResponse(c.getId(), "turn:OK,player1");
                        sendResponse(socketClient, "turn:OK,player1");
                    } else if (game.getPlayer2() == c && stream.substring(13).equals("player2")) {
                        sendResponse(socketClient, "choosePlayer:ERROR,player1");
                        game.setPlayer1(client);
                        sendResponse(c.getId(), "begin:OK");
                        sendResponse(socketClient, "begin:OK");
                        sendResponse(c.getId(), "turn:OK,player1");
                        sendResponse(socketClient, "turn:OK,player1");
                    } else if (game.getPlayer1() != c && game.getPlayer2() != c) {
                        if (stream.substring(13).equals("player1")) {
                            game.setPlayer1(client);
                            sendResponse(socketClient, "choosePlayer:OK,player1");
                        } else {
                            game.setPlayer2(client);
                            sendResponse(socketClient, "choosePlayer:OK,player2");
                        }
                    } else {
                        System.out.println(game.getPlayer1());
                        System.out.println(game.getPlayer2());
                        System.out.println(c);
                        if (stream.substring(13).equals("player1")) {
                            game.setPlayer1(client);
                            sendResponse(socketClient, "choosePlayer:OK,player1");
                        } else {
                            game.setPlayer2(client);
                            sendResponse(socketClient, "choosePlayer:OK,player2");
                        }
                        sendResponse(c.getId(), "begin:OK");
                        sendResponse(socketClient, "begin:OK");
                        sendResponse(c.getId(), "turn:OK,player1");
                        sendResponse(socketClient, "turn:OK,player1");
                    }
                }
            }

            gameSemaphore.release();
        }

        if (stream.startsWith("drawGame:")) {
            gameSemaphore.acquire();

            Game game = client.getLastGame();
            if (game == null) return;
            String resp = stream.substring(9);
            if (game.getAwaitingForDraw() != client && (game.getAwaitingForDraw() == game.getPlayer1() || game.getAwaitingForDraw() == game.getPlayer2())) {
                if (resp.equals("YES")) {
                    game.setWasADraw(true);
                    game.finish();
                    sendResponse(game.getPlayer2().getId(), "drawGame:OK,draw");
                    sendResponse(game.getPlayer1().getId(), "drawGame:OK,draw");
                } else if (resp.equals("NO")) {
                    game.askForDraw(null);
                    if (game.getPlayer1() == client) {
                        sendResponse(game.getPlayer2().getId(), "drawGame:OK,refused");
                    } else {
                        sendResponse(game.getPlayer1().getId(), "drawGame:OK,refused");
                    }
                }
            } else if (game.getAwaitingForDraw() == null && resp.equals("YES")) {
                game.askForDraw(client);
                sendResponse(client.getId(), "drawGame:OK,wait");
                if (game.getPlayer1() == client)
                    sendResponse(game.getPlayer2().getId(), "drawGame:OK,ask");
                else
                    sendResponse(game.getPlayer1().getId(), "drawGame:OK,ask");
            }

            gameSemaphore.release();
        }

        if (stream.startsWith("chatMessage:")) {
            Game game = client.getLastGame();
            if (game == null) return;

            if (game.getPlayer1() == client)
                sendResponse(game.getPlayer2().getId(), "chatMessage:OK," + client.getName() + "," + stream.substring(12));
            else
                sendResponse(game.getPlayer1().getId(), "chatMessage:OK," + client.getName() + "," + stream.substring(12));
        }
    }
}
