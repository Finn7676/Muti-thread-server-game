import java.io.*;
import java.net.*;

public class ClientHandler extends Thread {

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    private int playerID;
    private TicTacToeServer server;

    public ClientHandler(Socket socket, int id, TicTacToeServer server) {
        this.socket = socket;
        this.playerID = id;
        this.server = server;

        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            out.println("PLAYERID " + id);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getPlayerID() {
        return playerID;
    }
    public void closeConnection() {
    try {
        if (in != null) in.close();
        if (out != null) out.close();
        if (socket != null) socket.close();
    } catch (IOException e) {
        e.printStackTrace();
    }
}

public void send(String msg) {
    try {
        out.println(msg);
        System.out.println("DEBUG: send() -> " + msg + " to player " + playerID);
    } catch (Exception e) {
        System.out.println("DEBUG: Exception sending message to player " + playerID);
        e.printStackTrace();
    }
}


    @Override
    public void run() {
        try {
            String msg;

            while ((msg = in.readLine()) != null) {
                String[] p = msg.split(" ");

                switch (p[0]) {
                    case "NAME":
                        out.println("NAMEOK");
                    
                        break;

                    case "MOVE":
                        int r = Integer.parseInt(p[1]);
                        int c = Integer.parseInt(p[2]);
                        server.handleMove(playerID, r, c);
                        break;

                    case "RESTART":
                        server.voteRestart(playerID);
                        break;

                    case "EXIT":
                        server.playerDisconnected(playerID);
                        return;
                }
            }

        } catch (Exception e) {
            server.playerDisconnected(playerID);
        }
      finally{
        System.out.println("DEBUG: Finally block player" + playerID);
        server.playerDisconnected(playerID);
      }
    }
}
