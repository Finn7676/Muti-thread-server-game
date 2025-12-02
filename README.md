
How the System Works
1. Server Startup
javac TicTacToeServer.java
java TicTacToeServer
Waits for 2 clients to connect
Assigns Player 1 and Player 2
Broadcasts START
Sends TURN 1 to begin the game
2. Client Startup
Start two terminals:
javac GameClient.java
java GameClient
Then enter a name in each window.
Buttons stay disabled until name submission + TURN message.
