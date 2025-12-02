import java.io.*;
import java.net.*;
import java.util.*;

public class TicTacToeServer {

    private ServerSocket serverSocket;
    private final List<ClientHandler> clients =
            Collections.synchronizedList(new ArrayList<>());

    private char[][] board = new char[3][3];
    private int currentTurn = 1;         // 1 = Player 1 (X), 2 = Player 2 (O)
    private boolean[] restartVotes = new boolean[2];

    public static void main(String[] args) throws IOException {
        new TicTacToeServer().start();
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(12345);
        System.out.println("Server started on port 12345.");

        resetBoard();

        // Accept exactly 2 clients
        while (clients.size() < 2) {
            Socket socket = serverSocket.accept();
            ClientHandler handler = new ClientHandler(socket, clients.size() + 1, this);

            clients.add(handler);
            handler.start();

            System.out.println("Player " + handler.getPlayerID() + " connected.");
        }

        broadcast("START");
        sendTurn();
    }

    // ========== GAME LOGIC ==========

    public synchronized void handleMove(int pid, int r, int c) {
        if (pid != currentTurn) return;
        if (board[r][c] != ' ') return;

        char mark = (pid == 1) ? 'X' : 'O';
        board[r][c] = mark;

        broadcast("MOVE " + r + " " + c + " " + mark);

        if (checkWin(mark)) {
            broadcast("RESULT " + (pid == 1 ? "1" : "2"));
            Arrays.fill(restartVotes, false);
            return;
        }

        if (boardFull()) {
            broadcast("RESULT DRAW");
            Arrays.fill(restartVotes, false);
            return;
        }

        currentTurn = (currentTurn == 1 ? 2 : 1);
        sendTurn();
    }

    public synchronized void voteRestart(int pid) {
        restartVotes[pid - 1] = true;
        if (restartVotes[0] && restartVotes[1]) {
            resetBoard();
            Arrays.fill(restartVotes, false);
            currentTurn = 1;
            broadcast("START");
            sendTurn();
        }
    }

public synchronized void playerDisconnected(int pid) {
    System.out.println("DEBUG: playerDisconnected called for player " + pid);

    ClientHandler leavingClient = null;

    // Find and remove the leaving client
    Iterator<ClientHandler> it = clients.iterator();
    while (it.hasNext()) {
        ClientHandler c = it.next();
        if (c.getPlayerID() == pid) {
            leavingClient = c;
            it.remove();  // remove safely during iteration
            System.out.println("DEBUG: Removed player " + pid + " from client list");
            break;
        }
    }

    if (leavingClient != null) {
        try {
            leavingClient.closeConnection(); // close socket
            System.out.println("DEBUG: Closed socket for player " + pid);
        } catch (Exception e) {
            e.printStackTrace();
        }
    } else {
        System.out.println("DEBUG: Leaving client not found!");
    }

    // Broadcast safely to remaining clients
    synchronized (clients) {
        System.out.println("DEBUG: Broadcasting OPP_LEFT to remaining clients...");
        for (ClientHandler c : clients) {
            try {
                c.send("OPP_LEFT");
                System.out.println("DEBUG: Sent OPP_LEFT to player " + c.getPlayerID());
            } catch (Exception e) {
                System.out.println("DEBUG: Failed to send OPP_LEFT to player " + c.getPlayerID());
                e.printStackTrace();
            }
        }
    }
}




    // ========== UTILITIES ==========

    public void broadcast(String msg) {
        synchronized (clients) {
            for (ClientHandler c : clients) {
                c.send(msg);
            }
        }
    }

    private void sendTurn() {
        broadcast("TURN " + currentTurn);
    }

    private void resetBoard() {
        for (int i = 0; i < 3; i++) {
            Arrays.fill(board[i], ' ');
        }
    }

    private boolean boardFull() {
        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 3; j++)
                if (board[i][j] == ' ') return false;
        return true;
    }

    private boolean checkWin(char m) {
        for (int i = 0; i < 3; i++) {
            if (board[i][0] == m && board[i][1] == m && board[i][2] == m) return true;
            if (board[0][i] == m && board[1][i] == m && board[2][i] == m) return true;
        }
        if (board[0][0] == m && board[1][1] == m && board[2][2] == m) return true;
        if (board[0][2] == m && board[1][1] == m && board[2][0] == m) return true;
        return false;
    }
}
