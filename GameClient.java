import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

public class GameClient {

    Socket socket;
    BufferedReader in;
    PrintWriter out;

    int playerID;
    boolean myTurn = false;
    private boolean nameSubmitted = false;

    JFrame frame;
    JTextField field;
    JLabel Top;
    JLabel timelabel;
    JButton[][] buttons;
    char[][] board;
    String playername;

    int p1Wins = 0;
    int p2Wins = 0;
    int draws = 0;

    JLabel p1Label;
    JLabel p2Label;
    JLabel drawLabel;

    public static void main(String[] args) {
        new GameClient().start();
    }

    public void start() {
        setupNetworking();
        setupGUI();
        listenToServer();
    }

    private void setupNetworking() {
        try {
            socket = new Socket("127.0.0.1", 12345);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Unable to connect to server.");
            System.exit(0);
        }
    }

    private void setupGUI() {
        frame = new JFrame("Tic Tac Toe");
        frame.setSize(500, 600);
        frame.setLayout(new BorderLayout());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        Top = new JLabel("Waiting for server...", JLabel.CENTER);
        frame.add(Top, BorderLayout.NORTH);

        createMenu();
        createBoard();
        createBottom();
        createScorePanel();

        frame.setVisible(true);

        Timer timer = new Timer(1000, e -> updateTime());
        timer.start();
    }

    private void createMenu() {
        JMenuBar bar = new JMenuBar();

        JMenu control = new JMenu("Control");
        JMenuItem exit = new JMenuItem("Exit");
        exit.addActionListener(e -> {
            out.println("EXIT");
            System.exit(0);
        });
        control.add(exit);

        JMenu help = new JMenu("Help");
        JMenuItem ins = new JMenuItem("Instructions");
        ins.addActionListener(e -> showInstructions());
        help.add(ins);

        bar.add(control);
        bar.add(help);

        frame.setJMenuBar(bar);
    }

    private void createBoard() {
        buttons = new JButton[3][3];
        board = new char[3][3];

        JPanel grid = new JPanel(new GridLayout(3, 3, 5, 5));
        grid.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                board[i][j] = ' ';
                final int r = i, c = j;

                JButton b = new JButton();
                b.setEnabled(false);
                b.setFont(new Font("Arial", Font.BOLD, 40));
                b.addActionListener(e -> attemptMove(r, c));

                buttons[i][j] = b;
                grid.add(b);
            }
        }

        frame.add(grid, BorderLayout.CENTER);
    }

    private void createBottom() {
        JPanel bottom = new JPanel(new BorderLayout());

        timelabel = new JLabel();
        timelabel.setHorizontalAlignment(SwingConstants.CENTER);

        field = new JTextField("");
        JButton submit = new JButton("Submit");

        submit.addActionListener(e -> {
            playername = field.getText().trim();
            if (playername.length() > 0) {
                out.println("NAME " + playername);
                field.setEnabled(false);
                submit.setEnabled(false);
                nameSubmitted=true;

                if(myTurn){
                    enableEmptyCells(true);
                }
            }
        });

        bottom.add(timelabel, BorderLayout.NORTH);
        bottom.add(new JLabel("Enter your name:"), BorderLayout.WEST);
        bottom.add(field, BorderLayout.CENTER);
        bottom.add(submit, BorderLayout.EAST);

        frame.add(bottom, BorderLayout.SOUTH);
    }

    private void createScorePanel() {
        JPanel scorePanel = new JPanel();
        scorePanel.setLayout(new BoxLayout(scorePanel, BoxLayout.Y_AXIS));

        JLabel label = new JLabel("Score");
        label.setFont(new Font("Arial", Font.BOLD, 16));

        p1Label = new JLabel("Player 1 Wins: 0");
        p2Label = new JLabel("Player 2 Wins: 0");
        drawLabel = new JLabel("Draws: 0");

        scorePanel.add(label);
        scorePanel.add(Box.createRigidArea(new Dimension(0, 5)));
        scorePanel.add(p1Label);
        scorePanel.add(p2Label);
        scorePanel.add(drawLabel);

        frame.add(scorePanel, BorderLayout.EAST);
    }

    private void attemptMove(int r, int c) {
        if (myTurn && board[r][c] == ' ') {
            out.println("MOVE " + r + " " + c);
        }
    }

    private void listenToServer() {
        new Thread(() -> {
            try {
                String msg;
                

                while ((msg = in.readLine()) != null) {
                    System.out.println(msg);
                    String[] p = msg.split(" ");
                    
                    switch (p[0]) {

                        case "PLAYERID":
                            playerID = Integer.parseInt(p[1]);
                            break;

                        case "START":
                            Top.setText("WELCOME " + playername);
                            resetBoard();
                            break;

                        case "NAMEOK":
                            Top.setText("WELCOME " + playername);
                            break;

                        case "TURN":
                            int turn = Integer.parseInt(p[1]);
                            handleTurn(turn);
                            break;

                        case "MOVE":
                            int r = Integer.parseInt(p[1]);
                            int c = Integer.parseInt(p[2]);
                            char mark = p[3].charAt(0);
                            applyMove(r, c, mark);
                            break;

                        case "RESULT":
                            handleResult(p[1]);
                            break;

                        case "OPP_LEFT":
                            JOptionPane.showMessageDialog(frame,
                                    "Game Ends. One of the players left.");
                            System.exit(0);
                            break;
                    }
                }

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame,
                        "Disconnected from server.");
                System.exit(0);
            }
        }).start();
    }

    private void handleTurn(int t) {
        
        
     myTurn=(t==playerID);
        if(!nameSubmitted){
            Top.setText("Please set your name first");
           
            return;
        }
        

        if (myTurn) {
            Top.setText("Your turn.");
            enableEmptyCells(true);
        } else {
            Top.setText("Waiting for opponent...");
            enableEmptyCells(false);
        }
    }

    private void applyMove(int r, int c, char mark) {
        board[r][c] = mark;
        buttons[r][c].setText(String.valueOf(mark));
        buttons[r][c].setEnabled(false);
    }

    private void handleResult(String res) {
        String msg;
        msg="";
if(res.equalsIgnoreCase("Draw")){
    msg="Draw!";
    draws++;
    drawLabel.setText("Draws: "+draws);}
    else{
        if (playerID==Integer.parseInt(res)) {
            msg = "Congratulations you win!";
            p1Wins++;
            p1Label.setText("Player 1 Wins: " + p1Wins);
        } else if (Math.abs(playerID-Integer.parseInt(res))==1) {
            msg = "You lose!";
            p2Wins++;
            p2Label.setText("Player 2 Wins: " + p2Wins);
        } }
    
    
        int choice = JOptionPane.showConfirmDialog(frame,
                msg + "\nRestart?",
                "Game Over",
                JOptionPane.YES_NO_OPTION);

        if (choice == JOptionPane.YES_OPTION) {
            out.println("RESTART");
        } else {
            out.println("EXIT");
            System.exit(0);
        }
    }

    private void enableEmptyCells(boolean enable) {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (board[i][j] == ' ') {
                    buttons[i][j].setEnabled(enable);
                }
            }
        }
    }

    private void resetBoard() {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                board[i][j] = ' ';
                buttons[i][j].setText("");
                buttons[i][j].setEnabled(false);
            }
        }
    }

    private void updateTime() {
        SimpleDateFormat f = new SimpleDateFormat("HH:mm:ss");
        timelabel.setText("Current Time: " + f.format(new Date()));
    }

    private void showInstructions() {
        JOptionPane.showMessageDialog(frame,
                "Instructions:\n"
                        + "- You must enter your name first.\n"
                        + "- Player 1 uses X, Player 2 uses O.\n"
                        + "- Wait for your turn.\n"
                        + "- Winner gets a point.\n"
                        + "- Draws are counted.\n"
                        + "- Restart if both players agree.",
                "Instructions",
                JOptionPane.INFORMATION_MESSAGE);
    }
}

