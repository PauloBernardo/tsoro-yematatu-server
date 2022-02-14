package com.company;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.Semaphore;

public class Server {
    private ArrayList<Socket> socketClients;
    private ArrayList<Client> clients;
    private ArrayList<Game> games;
    private ArrayList<Client> waitingClients;
    private ServerSocket server;
    private Semaphore gameSemaphore;

    public Server() {
        socketClients = new ArrayList<>();
        clients = new ArrayList<>();
        games = new ArrayList<>();
        waitingClients = new ArrayList<>();
        gameSemaphore = new Semaphore(1, true);
    }

    public void start() {
        try {
            ServerSocket server = new ServerSocket(3322);
            this.server = server;
            System.out.println("Servidor iniciado na porta 3322");

            while (true) {
                Socket client = server.accept();
                socketClients.add(client);
                clients.add(new Client(client));
                new Thread(new ListenToClient(server, client, this)).start();
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    public void removeClient(Socket client) {
        Client removeClient = null;
        for (Client c : clients) {
            if (c.getId() == client) {
                removeClient = c;
                break;
            }
        }
        clients.remove(removeClient);
        socketClients.remove(client);
    }

    private void sendResponse(Socket client, String message) throws IOException {
        DataOutputStream out = new DataOutputStream(client.getOutputStream());
        out.writeUTF(message);
        out.flush();
    }

    public void handleInputStream(Socket socketClient, String stream) throws IOException, InterruptedException {
        if (stream.startsWith("setName:")) {
            for (Client client : clients) {
                if (client.getId() == socketClient) {
                    client.setName(stream.substring(8));
                    sendResponse(socketClient, "setName:OK");
                    break;
                }
            }
        }
        if (stream.startsWith("getName:")) {
            for (Client client : clients) {
                if (client.getId() == socketClient) {
                    sendResponse(socketClient, "getName:OK," + client.getName());
                }
            }
        }
        if (stream.startsWith("startNewMatch:")) {
            for (Client client : clients) {
                if (client.getId() == socketClient) {
                    waitingClients.add(client);
                    sendResponse(socketClient, "startNewMatch:OK");
                }
            }
        }
        if (stream.startsWith("startRandomMatch:")) {
            for (Client client : clients) {
                if (client.getId() == socketClient) {
                    if (waitingClients.size() != 0) {
                        Client clientWaitingGame = waitingClients.remove(0);
                        Game game = new Game();
                        games.add(game);
                        clientWaitingGame.addGame(game);
                        client.addGame(game);
                        sendResponse(clientWaitingGame.getId(), "startRandomMatch:OK,start");
                        sendResponse(socketClient, "startRandomMatch:OK,start");
                    } else {
                        waitingClients.add(client);
                        sendResponse(socketClient, "startRandomMatch:OK,wait");
                    }
                }
            }
        }
        if (stream.startsWith("getChooseMatch:")) {
            for (Client client : clients) {
                if (client.getId() == socketClient) {
                    DataOutputStream out = new DataOutputStream(socketClient.getOutputStream());
                    StringBuilder clientIds = new StringBuilder("getChooseMatch:OK");
                    for (Client waitingClient : waitingClients) {
                        clientIds.append(",").append(waitingClient.getName()).append("#").append(waitingClient.getTimeId());
                    }
                    out.writeUTF(clientIds.toString());
                    out.flush();
                }
            }
        }

        if (stream.startsWith("cancelGame:")) {
            for (Client client : clients) {
                if (client.getId() == socketClient) {
                    waitingClients.remove(client);
                    sendResponse(socketClient, "cancelGame:OK");
                }
            }
        }

        if (stream.startsWith("endGame:")) {
            for (Client client : clients) {
                if (client.getId() == socketClient) {
                    Game game = client.getGames().remove(client.getGames().size() - 1);
                    game.finish();
                    if (client == game.getPlayer1()) {
                        game.setWinner(game.getPlayer2());
                        sendResponse(socketClient, "endGame:OK,loser");
                    } else {
                        game.setWinner(game.getPlayer1());
                        sendResponse(socketClient, "endGame:OK,winner");
                    }
                }
            }
        }

        if (stream.startsWith("chooseColor:")) {
            for (Client client : clients) {
                if (client.getId() == socketClient) {
                    gameSemaphore.acquire();
                    System.out.println("Entrei aqui");
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
                    System.out.println("Saindo");
                    gameSemaphore.release();
                }
            }
        }

        if (stream.startsWith("move:")) {
            for (Client client : clients) {
                if (client.getId() == socketClient) {
                    Game game = client.getLastGame();
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
                                sendResponse(game.getPlayer1().getId(), "endGame:OK,winner");
                                sendResponse(game.getPlayer2().getId(), "endGame:OK,loser");
                            }
                        } catch (Exception e) {
                            sendResponse(socketClient, "move:ERROR," + e.getMessage());
                        }
                    } else {
                        sendResponse(socketClient, "move:ERROR,invalidPlayer");
                    }
                }
            }
        }

        if (stream.startsWith("choosePlayer:")) {
            for (Client client : clients) {
                if (client.getId() == socketClient) {
                    gameSemaphore.acquire();
                    System.out.println("Entrei aqui 2");
                    Game game = client.getLastGame();
                    for (Client c : clients) {
                        if (c.getLastGame() == game && c != client) {
                            if (game.getPlayer1() == c && stream.substring(13).equals("player1")) {
                                sendResponse(socketClient, "choosePlayer:ERROR,player2");
                                sendResponse(c.getId(), "begin:OK");
                                sendResponse(socketClient, "begin:OK");
                                sendResponse(c.getId(), "turn:OK,player1");
                                sendResponse(socketClient, "turn:OK,player1");
                            } else if (game.getPlayer2() == c && stream.substring(13).equals("player2")) {
                                sendResponse(socketClient, "choosePlayer:ERROR,player1");
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
                    System.out.println("Saindo 2");
                    gameSemaphore.release();
                }
            }
        }
    }
}
