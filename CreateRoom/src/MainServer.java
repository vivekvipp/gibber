import java.io.*;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

/**
 * Created by csclass on 12/2/16.
 */
public class MainServer {

    public static ArrayList<String> usersLoggedIn = new ArrayList<>();
    public static ArrayList<String> usersPlayingGames = new ArrayList<>();

    public static HashMap<String, gameDetails> gameDetails = new HashMap<>();

    public static ArrayList<String> gameKeyArray = new ArrayList<>();
    public static ArrayList<String> userTokenArray = new ArrayList<>();
    //Array list of all the game Keys created

    static ArrayList<Socket> clientSockets = new ArrayList<>(); //Collection of Sockets
    public static int socketCount = 0;

    protected static File fileUsers = new File("/Users/csclass/Desktop/UserDatabase");
    protected static FileOutputStream outStreamUsers;
    protected static PrintWriter writeToFileUsers;

    //Data members to read from UserDatabase File
    protected static Scanner scanUsers;

    //Data members to read from WordleDeck File
    protected static File fileWords = new File("/Users/csclass/Desktop/WordleDeck");
    protected static Scanner scanWords;

    protected static ArrayList<String> arrayUsers;
    protected static ArrayList<String> arrayWords;


    public static void main(String[] args) {
        int portNumber = 9999;

        try {
            serveClients(portNumber);
        } catch (IOException e) {
            System.err.println("Unexpected error: " + e.getMessage());
        }
    }

    private static void serveClients(int portNumber) throws IOException {
        ServerSocket serverSocket = null;

        try {
            System.out.println("FoilMaker Game - Creating Socket");
            serverSocket = new ServerSocket(portNumber);

            System.out.println("Listening for Players");


            //READING DETAILS FROM FILE
            scanUsers = new Scanner(fileUsers);
            scanWords = new Scanner(fileWords);


            //CREATING AN ARRAY OF DATA RECEIVED FROM TWO FILES

            //DATA of USERS
            //Creating an array of the contents
            System.out.println("LIST OF USERS--------------------");
            arrayUsers = new ArrayList<>();
            while (scanUsers.hasNext()) {
                String text1 = scanUsers.nextLine();
                arrayUsers.add(text1);
                System.out.println(text1);
            }


            //DATA of WORDS
            //Creating an array of the contents
            System.out.println("LIST OF QUESTIONS--------------------");
            arrayWords = new ArrayList<>();
            while (scanWords.hasNext()) {
                String text = scanWords.nextLine();
                arrayWords.add(text);
                System.out.println(text);
            }

            while (true) {
                clientSockets.add(serverSocket.accept());
                System.out.println("Got a request from a new Player: " + clientSockets.get(socketCount).getPort());
                interactClient(clientSockets.get(socketCount), socketCount);
                socketCount++;
            }


        } catch (MalformedURLException e) {
            System.err.println("Unable to connect:\n" + e.getMessage());
            System.exit(3);
        } finally {
            if (serverSocket != null)
                serverSocket.close();
            for (int i = 0; i < clientSockets.size(); i++) {
                if (clientSockets.get(i) != null && !clientSockets.get(i).isClosed())
                    clientSockets.get(i).close();
            }
        }
    }

    public static void interactClient(Socket clientSocket, int socketId) {

        new Thread() {

            public void run() {
                //Data Members to send info to CLIENT
                PrintWriter outToClient = null;


                //TO PRINT USE - PRINTLN
                //TO READ USE - READLN

                //Data members to read from the client
                BufferedReader inFromClient = null;



                String[] user1Details = {"", "", "", "", "", ""};

                //ORDER - username,password,cumulative score, no of times fooled others, no of times fooled by others, game token


                try {

                    //TRYING TO GET ALL THE CONNECTIONS IN THE BEGINNING
                    outToClient = new PrintWriter(clientSocket.getOutputStream(), true);
                    inFromClient = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));


                    //NOW THE ACTUAL GAME STARTS!!!!!
                    boolean start = true;
                    String clientMessage = null;
                    String userToken = "";
                    String gameKey = "";

                    while (start) {


                        clientMessage = null;

                        while (clientMessage == null)
                            clientMessage = inFromClient.readLine();

                        if (clientMessage.contains("CREATENEWUSER"))
                            createUser(clientMessage, outToClient);

                        if (clientMessage.contains("LOGIN--"))
                            userToken = loginUser(clientMessage, outToClient, user1Details);

                        if (clientMessage.contains("STARTNEWGAME--"))
                            gameKey = startNewGame(clientMessage, outToClient, user1Details, socketId, inFromClient,
                                    clientSocket);

                        if (clientMessage.contains("JOINGAME--"))
                            gameKey = joinGame(clientMessage, outToClient, user1Details, socketId, inFromClient,
                                    clientSocket);

                        if (clientMessage.contains("ALLPARTICIPANTSHAVEJOINED--"))
                            launchGame(clientMessage, user1Details, outToClient);

                        if(clientMessage.contains("PLAYERSUGGESTION--")) {
                            if(gameDetails.get(gameKey).questionCount == 0 || gameDetails.get(gameKey).questionCount
                                    == 1) {
                                collectPlayersSuggestions(clientMessage, outToClient);
                                gameDetails.get(gameKey).questionCount++;
                            } else {
                                outToClient.println("RESPONSE--PLAYERSUGGESTION--UNEXPECTEDMESSAGETYPE");
                                System.out.println("Sent to Client : " +
                                        "RESPONSE--PLAYERSUGGESTION--UNEXPECTEDMESSAGETYPE");
                            }
                        }

                        if(clientMessage.contains("PLAYERCHOICE--")) {
                            if(gameDetails.get(gameKey).questionCount == 2) {
                                collectPlayersChoices(clientMessage, outToClient);
                                gameDetails.get(gameKey).questionCount++;
                            } else if (gameDetails.get(gameKey).questionCount == 3){
                                collectPlayersChoices(clientMessage, outToClient);
                                gameDetails.get(gameKey).questionCount = 0;
                            } else {
                                outToClient.println("RESPONSE--PLAYERCHOICE--UNEXPECTEDMESSAGETYPE");
                                System.out.println("Sent to Client : " +
                                        "RESPONSE--PLAYERCHOICE--UNEXPECTEDMESSAGETYPE");
                            }
                        }

                        if(clientMessage.contains("LOGOUT--")){
                            logout(user1Details,clientMessage,outToClient, userToken, gameKey, inFromClient);
                            start = false;
                        }


                    }


                } catch (IOException e) {
                    System.err.println("Error with IO with Client\n" + clientSocket + e.getMessage());

                    e.printStackTrace();
                } finally {
                    if (writeToFileUsers != null)
                        writeToFileUsers.close();
                }

            }
        }.start();
    }

    public static void logout(String[] user1Details, String clientMessage,PrintWriter outToClient, String userToken,
                              String gameKey, BufferedReader inFromClient) {

        int countUser = 0;
        for (int i = 0; i < userTokenArray.size(); i++) {
            if (userTokenArray.get(i).equals(userToken))
                countUser++;
        }

        if (countUser == 0) {
            outToClient.println("RESPONSE--LOGOUT--USERNOTLOGGEDIN");
            System.out.println("Sent to Client : RESPONSE--LOGOUT--USERNOTLOGGEDIN");
        } else {
            outToClient.println("RESPONSE--LOGOUT--SUCCESS");
            System.out.println("Sent to Client : RESPONSE--LOGOUT--SUCCESS");

            usersLoggedIn.remove(user1Details[0]);
            usersPlayingGames.remove(user1Details[0]);
            userTokenArray.remove(userToken);
            gameDetails.get(gameKey).printWriters.remove(outToClient);
            gameDetails.get(gameKey).players.remove(user1Details[0]);
            gameDetails.get(gameKey).wordCount = 0;

            //UPDATING USER SCORES
            for (int i = 0; i < arrayUsers.size(); i++) {
                if (arrayUsers.get(i).contains(user1Details[0])) {
                    if (gameDetails.get(gameKey).user1[0].equals(user1Details[0])) {
                        arrayUsers.set(i, gameDetails.get(gameKey).user1[0] + ":" + gameDetails.get(gameKey)
                                .user1[1] + ":" + gameDetails.get(gameKey).user1[2] + ":" +
                                gameDetails.get(gameKey).user1[3] + ":" +
                                gameDetails.get(gameKey).user1[4]);

                        System.out.println(gameDetails.get(gameKey).user1[0] + ":" + gameDetails.get(gameKey)
                                .user1[1] + ":" + gameDetails.get(gameKey).user1[2] + ":" +
                                gameDetails.get(gameKey).user1[3] + ":" +
                                gameDetails.get(gameKey).user1[4]);
                    } else {
                        arrayUsers.set(i, gameDetails.get(gameKey).user2[0] + ":" + gameDetails.get(gameKey)
                                .user2[1] + ":" + gameDetails.get(gameKey).user2[2] + ":" +
                                gameDetails.get(gameKey).user2[3] + ":" +
                                gameDetails.get(gameKey).user2[4]);

                        System.out.println(gameDetails.get(gameKey).user2[0] + ":" + gameDetails.get(gameKey)
                                .user2[1] + ":" + gameDetails.get(gameKey).user2[2] + ":" +
                                gameDetails.get(gameKey).user2[3] + ":" +
                                gameDetails.get(gameKey).user2[4]);
                    }

                    try {
                        outStreamUsers = new FileOutputStream(fileUsers);
                    } catch (IOException e) {

                    }
                    writeToFileUsers = new PrintWriter(outStreamUsers, true);
                    //Writing back to the file
                    for (int j = 0; j < arrayUsers.size(); j++) {
                        writeToFileUsers.println(arrayUsers.get(j));
                    }
                    break;
                }
            }


            try {
                outToClient.close();
                inFromClient.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }



    public static void createUser(String msg, PrintWriter outToClient) {

        System.out.println("Create User Command - " + msg);


        String message = msg;
        message = message.replace("--", "-");

        //Checking for Invalid Message Format
        int indexfirst = message.indexOf("-");
        if (message.substring(indexfirst, indexfirst + 1).equals(message.substring(indexfirst + 1, indexfirst + 2))) {
            outToClient.println("RESPONSE--CREATENEWUSER--INVALIDMESSAGEFORMAT");
            System.out.println("SENT TO CLIENT - RESPONSE--CREATENEWUSER--INVALIDMESSAGEFORMAT");
        }
        if (message.substring(message.length() - 1).equals("-")) {
            outToClient.println("RESPONSE--CREATENEWUSER--INVALIDMESSAGEFORMAT");
            System.out.println("SENT TO CLIENT - RESPONSE--CREATENEWUSER--INVALIDMESSAGEFORMAT");
        }


        message = message.replace("CREATENEWUSER-", "");
        int index = message.indexOf("-");
        String username = message.substring(0, index);
        String password = message.substring(index + 1);


        int count = 0;
        for (int i = 0; i < arrayUsers.size(); i++) {
            String arrayName = arrayUsers.get(i).substring(0,arrayUsers.get(i).indexOf(":"));
            if (arrayName.equals(username)) {
                outToClient.println("RESPONSE--CREATENEWUSER--USERALREADYEXISTS");
                System.out.println("SENT TO CLIENT - RESPONSE--CREATENEWUSER--USERALREADYEXISTS");

                count++;
            }
        }

        if (count == 0) {
            if (username == null || username == "" || username.length() >= 10 || !username.matches("^[a-zA-Z0-9_]*$")) {
                outToClient.println("RESPONSE--CREATENEWUSER--INVALIDUSERNAME");
                System.out.println("SENT TO CLIENT - RESPONSE--CREATENEWUSER--INVALIDUSERNAME");
            }
            else if (password == null || password == "" || password.length() >= 10 || !password.matches
                    ("^[a-zA-Z0-9#&$*]*$") || checkPassword(password) == false ) {
                outToClient.println("RESPONSE--CREATENEWUSER--INVALIDPASSWORD");
                System.out.println("SENT TO CLIENT - RESPONSE--CREATENEWUSER--INVALIDPASSWORD");
            } else {
                arrayUsers.add(username + ":" + password + ":" + "0:0:0");
                try {
                    outStreamUsers = new FileOutputStream(fileUsers);
                } catch (IOException e) {

                }
                writeToFileUsers = new PrintWriter(outStreamUsers, true);
                for (int i = 0; i < arrayUsers.size(); i++) {
                    writeToFileUsers.println(arrayUsers.get(i));
                }

                outToClient.println("RESPONSE--CREATENEWUSER--SUCCESS");
                System.out.println("SENT TO CLIENT - RESPONSE--CREATENEWUSER--SUCCESS");

            }
        }
    }

    public static boolean checkPassword(String password) {

        int numCount = 0;

        boolean hasUppercase = !password.equals(password.toLowerCase());
        if (!hasUppercase)
            return false;

        for (int i = 0; i < password.length(); i++) {
            char c = password.charAt(i);
            if (Character.isDigit(c))
                numCount++;
        }

        if (numCount < 1)
            return false;

        return true;
    }

    public static String loginUser(String msg, PrintWriter outToClient, String[] user1Details) {
        System.out.println("Login User Command - " + msg);


        String message = msg;
        message = message.replace("--", "-");

        //Checking for Invalid Message Format
        int indexfirst = message.indexOf("-");
        if (message.substring(indexfirst, indexfirst + 1).equals(message.substring(indexfirst + 1, indexfirst + 2))) {
            outToClient.println("RESPONSE--LOGIN--INVALIDMESSAGEFORMAT");
            System.out.println("SENT TO CLIENT - RESPONSE--LOGIN--INVALIDMESSAGEFORMAT");
        }
        if (message.substring(message.length() - 1).equals("-")) {
            outToClient.println("RESPONSE--LOGIN--INVALIDMESSAGEFORMAT");
            System.out.println("SENT TO CLIENT - RESPONSE--LOGIN--INVALIDMESSAGEFORMAT");
        }


        message = message.replace("LOGIN-", "");
        int index = message.indexOf("-");
        String username = message.substring(0, index);
        String password = message.substring(index + 1);


        //Checking for User and Password
        String passwordInFile = "";
        String entry = "";

        int count = 0;
        for (int i = 0; i < arrayUsers.size(); i++) {
            String arrayName = arrayUsers.get(i).substring(0,arrayUsers.get(i).indexOf(":"));

            if (arrayName.equals(username)) {
                entry = arrayUsers.get(i);
                count++;
            }
        }

        if (count == 0) {
            outToClient.println("RESPONSE--LOGIN--UNKNOWNUSER");
            System.out.println("SENT TO CLIENT - RESPONSE--LOGIN--UNKNOWNUSER");

        } else {

            entry = entry.replace(username + ":", "");
            int indexColon = entry.indexOf(":");
            passwordInFile = entry.substring(0, indexColon);

            if (!password.equals(passwordInFile)) {
                outToClient.println("RESPONSE--LOGIN--INVALIDUSERPASSWORD");
                System.out.println("SENT TO CLIENT - RESPONSE--LOGIN--INVALIDUSERPASSWORD");
            } else {

                int userCount = 0;
                for (int i = 0; i < usersLoggedIn.size(); i++) {
                    if (usersLoggedIn.get(i).contains(username))
                        userCount++;

                }

                if (userCount != 0) {
                    outToClient.println("RESPONSE--LOGIN--USERALREADYLOGGEDIN");
                    System.out.println("SENT TO CLIENT - RESPONSE--LOGIN--USERALREADYLOGGEDIN");
                } else {


                    String[] arrayLetters = {"A","B","C","D","E","F","G","H","I","J","K","L","M","N","O","P","Q","R",
                            "S","T","U","V","W","X","Y","Z","a","b","c","d","e","f","g","h","i","j","k","l","m","n",
                            "o","p","q","r","s","t","u","v","w","x","y","z"};
                    Random random = new Random();
                    String gameToken = arrayLetters[random.nextInt(52)] + arrayLetters[random.nextInt(52)] + arrayLetters[random.nextInt(52)] + arrayLetters[random.nextInt(52)] +arrayLetters[random.nextInt(52)] + arrayLetters[random.nextInt(52)] +arrayLetters[random.nextInt(52)] + arrayLetters[random.nextInt(52)] +arrayLetters[random.nextInt(52)] + arrayLetters[random.nextInt(52)];
                    userTokenArray.add((String.valueOf(gameToken)));
                    System.out.println("Game Token Created - " + gameToken);
                    user1Details[5] = String.valueOf(gameToken);

                    outToClient.println("RESPONSE--LOGIN--SUCCESS--" + gameToken);
                    System.out.println("SENT TO CLIENT - RESPONSE--LOGIN--SUCCESS--" + gameToken);


                    user1Details[0] = username;
                    usersLoggedIn.add(username);
                    user1Details[1] = password;

                    entry = entry.substring(indexColon + 1);
                    int index2 = entry.indexOf(":");
                    String cummulativeScore = (entry.substring(0, index2));
                    user1Details[2] = cummulativeScore;
                    entry = entry.substring(index2 + 1);

                    int indexFinal = entry.indexOf(":");
                    String fool1 = entry.substring(0, indexFinal);
                    user1Details[3] = fool1;
                    user1Details[4] = entry.substring(indexFinal + 1);

                    System.out.println("-----------------------------------------");
                    System.out.println("USER LOGGED IN  - " + user1Details[0]);
                    System.out.println("Cummulative Score  - " + user1Details[2]);
                    System.out.println("Number of times fooled by others  - " + user1Details[3]);
                    System.out.println("Number of users fooled  - " + user1Details[4]);
                    System.out.println("Game Token  - " + user1Details[5]);
                    System.out.println("-----------------------------------------");

                    return String.valueOf(gameToken);


                }
            }
        }

        return "";
    }

    public static String startNewGame(String msg, PrintWriter outToClient, String[] user1Details, int socketId,
                                      BufferedReader inFromClient, Socket socket) {


        //Creating Game Key
        Random random = new Random();
        String[] letters = {"a","b","c","d","e","f","g","h","i","j","k","l","m","n",
                "o","p","q","r","s","t","u","v","w","x","y","z"};
        String gamekey = letters[random.nextInt(25)] + letters[random.nextInt(25)] + letters[random.nextInt(25)];

        msg = msg.replace("STARTNEWGAME--", "");


        if ((!msg.equals(user1Details[5]))) {
            outToClient.println("RESPONSE--STARTNEWGAME--USERNOTLOGGEDIN");
            System.out.println("Sent to Client - RESPONSE--STARTNEWGAME--USERNOTLOGGEDIN");
        } else {


            int count = 0;
            for (int i = 0; i < usersPlayingGames.size(); i++) {
                if (usersPlayingGames.get(i).equals(user1Details[0]))
                    count++;
            }

            if (count != 0) {
                outToClient.println("RESPONSE--STARTNEWGAME--FAILURE");
                System.out.println("Sent to Client - RESPONSE--STARTNEWGAME--FAILURE");
            } else {
                if (msg.equals(user1Details[5])) {
                    outToClient.println("RESPONSE--STARTNEWGAME--SUCCESS--" + gamekey);
                    System.out.println("Sent to Client - RESPONSE--STARTNEWGAME--SUCCESS--" + gamekey);

                    gameKeyArray.add(gamekey);
                    gameDetails.put(gamekey, new gameDetails());
                    gameDetails.get(gamekey).toLeaderClient = outToClient;
                    gameDetails.get(gamekey).printWriters.add(outToClient);
                    gameDetails.get(gamekey).players.add(user1Details[0]);

                    gameDetails.get(gamekey).user1 = user1Details;


                    System.out.println("Created a New game with the Game Key - " + gamekey);
                    System.out.println("Added Player " + user1Details[0] + " to game with the key " + gamekey);
                    return gamekey;
                } else {
                    outToClient.println("RESPONSE--STARTNEWGAME--FAILURE");
                    System.out.println("Sent to Client - RESPONSE--STARTNEWGAME--FAILURE");
                }
            }
        }
        return "";
    }

    public static String joinGame(String msg, PrintWriter outToClient, String[] user1Details, int socketId,
                                  BufferedReader inFromClient, Socket socket) {

        String message = msg;

        message = message.replace("JOINGAME--", "");
        message = message.replace("--", "*");
        int index = message.indexOf("*");

        String userToken = message.substring(0, index);
        String gameToken = message.substring(index + 1);

        if (!userToken.equals(user1Details[5])) {
            outToClient.println("RESPONSE--JOINGAME--USERNOTLOGGEDIN--" + gameToken);
            System.out.println("Sent to Client - RESPONSE--JOINGAME--USERNOTLOGGEDIN--" + gameToken);
        } else {

            int count = 0;
            for (int i = 0; i < gameKeyArray.size(); i++) {
                if (gameToken.equals(gameKeyArray.get(i)))
                    count++;
            }
            if (count == 0) {
                outToClient.println("RESPONSE--JOINGAME--GAMEKEYNOTFOUND--" + gameToken);
                System.out.println("Sent to Client - RESPONSE--JOINGAME--GAMEKEYNOTFOUND--" + gameToken);
            } else {

                int countPlayers = 0;
                for (int i = 0; i < gameDetails.get(gameToken).players.size(); i++) {
                    if (gameDetails.get(gameToken).players.get(i).equals(user1Details[0]))
                        countPlayers++;
                }

                if (countPlayers != 0) {
                    outToClient.println("RESPONSE--JOINGAME--FAILURE--" + gameToken);
                    System.out.println("Sent to Client - RESPONSE--JOINGAME--FAILURE--" + gameToken);
                } else {

                    outToClient.println("RESPONSE--JOINGAME--SUCCESS--" + gameToken);
                    System.out.println("Sent to Client - RESPONSE--JOINGAME--SUCCESS--" + gameToken);

                    gameDetails.get(gameToken).printWriters.add(outToClient);
                    gameDetails.get(gameToken).players.add(user1Details[0]);
                    gameDetails.get(gameToken).user2 = user1Details;


                    System.out.println("Added " + user1Details[0] + " to the game " + gameToken);


                    gameDetails.get(gameToken).toLeaderClient.println("NEWPARTICIPANT--" +
                            user1Details[0] + "--" + user1Details[2]);
                    System.out.println("Sent to Client Leader - NEWPARTICIPANT--" +
                            user1Details[0] + "--" + user1Details[2]);

                    return gameToken;


                }
            }
        }
        return "";
    }

    public static void launchGame(String message, String[] user1Details, PrintWriter outToClient) {
        message = message.replace("ALLPARTICIPANTSHAVEJOINED--", "");
        message = message.replace("--", "*");

        int index = message.indexOf("*");

        String gameToken = message.substring(index + 1);
        String userToken = message.substring(0, index);

        if (!user1Details[5].equals(userToken)) {
            outToClient.println("RESPONSE--ALLPARTICIPANTSHAVEJOINED--USERNOTLOGGEDIN");
            System.out.println("Sent to Client - RESPONSE--ALLPARTICIPANTSHAVEJOINED--USERNOTLOGGEDIN");
        } else {

            int count = 0;
            for (int i = 0; i < gameKeyArray.size(); i++) {
                if (gameKeyArray.get(i).equals(gameToken))
                    count++;
            }

            if (count == 0) {
                outToClient.println("RESPONSE--ALLPARTICIPANTSHAVEJOINED--INVALIDGAMETOKEN");
                System.out.println("Sent to Client - RESPONSE--ALLPARTICIPANTSHAVEJOINED--INVALIDGAMETOKEN");

            } else {

                int playercount = 0;
                for (int i = 0; i < usersPlayingGames.size(); i++) {
                    if (user1Details[0].equals(usersPlayingGames.get(i)))
                        playercount++;
                }

                if (playercount != 0) {
                    outToClient.println("RESPONSE--ALLPARTICIPANTSHAVEJOINED--USERNOTGAMELEADER");
                    System.out.println("Sent to Client - RESPONSE--ALLPARTICIPANTSHAVEJOINED--USERNOTGAMELEADER");
                } else {


                    String qaa = arrayWords.get(gameDetails.get(gameToken).wordCount);
                    gameDetails.get(gameToken).wordCount++;
                    int sc = qaa.indexOf(":");
                    gameDetails.get(gameToken).question = qaa.substring(0, sc - 1);
                    gameDetails.get(gameToken).answer = qaa.substring(sc + 2, qaa.length());


                    //Sending the words to all Players
                    for (int i = 0; i < gameDetails.get(gameToken).printWriters.size(); i++) {
                        gameDetails.get(gameToken).printWriters.get(i).println(("NEWGAMEWORD--" + gameDetails.get
                                (gameToken).question + "--" + gameDetails.get(gameToken).answer));
                        System.out.println("Sent to Client " + gameDetails.get(gameToken).players.get(i) + " : " +
                                "NEWGAMEWORD--" + gameDetails.get
                                (gameToken)
                                .question + "--" + gameDetails.get(gameToken).answer);
                    }


                }


            }
        }
    }

    public static void collectPlayersSuggestions(String message, PrintWriter outToClient) {

        String msg = message;
        msg = msg.replace("--", "-");

        //Checking for Invalid Message Format
        int indexfirst = msg.indexOf("-");

        if (msg.substring(indexfirst, indexfirst + 1).equals(msg.substring(indexfirst + 1, indexfirst + 2))) {
            outToClient.println("RESPONSE--PLAYERSUGGESTION--INVALIDMESSAGEFORMAT");
            System.out.println("SENT TO CLIENT - RESPONSE--PLAYERSUGGESTION--INVALIDMESSAGEFORMAT");
        }
        if (msg.substring(msg.length() - 1).equals("-")) {
            outToClient.println("RESPONSE--PLAYERSUGGESTION--INVALIDMESSAGEFORMAT");
            System.out.println("SENT TO CLIENT - RESPONSE--PLAYERSUGGESTION--INVALIDMESSAGEFORMAT");
        }

        message = message.replace("PLAYERSUGGESTION--", "");
        message = message.replace("--", "*");

        int index = message.indexOf("*");

        String userToken = message.substring(0, index);

        message = message.substring(index + 1, message.length());
        index = message.indexOf("*");

        String gameToken = message.substring(0, index);

        message = message.substring(index + 1);

        int count = 0;
        for (int i = 0; i <gameKeyArray.size(); i++) {
            if (gameKeyArray.get(i).equals(gameToken))
                count++;
        }

        if (count == 0) {
            outToClient.println("RESPONSE--PLAYERSUGGESTION--INVALIDGAMETOKEN");
            System.out.println("Sent to Client - RESPONSE--PLAYERSUGGESTION--INVALIDGAMETOKEN");
        }
        else {
            int countToken = 0;
            for(int i = 0; i<userTokenArray.size(); i++){
                if(userTokenArray.get(i).equals(userToken))
                    countToken++;
            }

            if(countToken == 0){
                outToClient.println("RESPONSE--PLAYERSUGGESTION--USERNOTLOGGEDIN");
                System.out.println("Sent to Client - RESPONSE--PLAYERSUGGESTION--USERNOTLOGGEDIN");
            } else {
                System.out.println("Received Suggestion Successfully : " + message );

                if(outToClient == gameDetails.get(gameToken).toLeaderClient)
                    gameDetails.get(gameToken).suggestion1 = message;
                else gameDetails.get(gameToken).suggestion2 = message;


                gameDetails.get(gameToken).countSuggestions++;

                if(gameDetails.get(gameToken).countSuggestions >= 2){

                    Random r = new Random();
                    int n = r.nextInt(6);

                    for(int i = 0; i<gameDetails.get(gameToken).players.size(); i++) {

                        if(n == 0) {
                            gameDetails.get(gameToken).printWriters.get(i).println
                                    ("ROUNDOPTIONS--" + gameDetails.get(gameToken).suggestion1 + "--" + gameDetails.get(gameToken).suggestion2 + "--" +
                                            gameDetails.get(gameToken).answer);
                            System.out.println("Sent to Client : ROUNDOPTIONS--" + gameDetails.get(gameToken)
                                    .suggestion1 + "--" + gameDetails.get(gameToken).suggestion2 + "--" +
                                    gameDetails.get(gameToken).answer);
                        }
                        if(n == 1) {
                            gameDetails.get(gameToken).printWriters.get(i).println
                                    ("ROUNDOPTIONS--" + gameDetails.get(gameToken).suggestion1 + "--" + gameDetails.get(gameToken).answer + "--" +
                                            gameDetails.get(gameToken).suggestion2);
                            System.out.println("Sent to Client : ROUNDOPTIONS--" + gameDetails.get(gameToken).suggestion1 + "--" + gameDetails.get(gameToken).answer + "--" +
                                    gameDetails.get(gameToken).suggestion2);
                        }
                        if(n == 2) {
                            gameDetails.get(gameToken).printWriters.get(i).println
                                    ("ROUNDOPTIONS--" + gameDetails.get(gameToken).suggestion2 + "--" + gameDetails.get(gameToken).suggestion1 + "--" +
                                            gameDetails.get(gameToken).answer);
                            System.out.println("Sent to Client : ROUNDOPTIONS--" + gameDetails.get(gameToken)
                                    .suggestion2 + "--" + gameDetails.get(gameToken).suggestion1 + "--" +
                                    gameDetails.get(gameToken).answer);
                        }
                        if(n == 3) {
                            gameDetails.get(gameToken).printWriters.get(i).println
                                    ("ROUNDOPTIONS--" + gameDetails.get(gameToken).suggestion2 + "--" + gameDetails.get(gameToken).answer + "--" +
                                            gameDetails.get(gameToken).suggestion1);
                            System.out.println("Sent to Client : ROUNDOPTIONS--" + gameDetails.get(gameToken)
                                    .suggestion2 + "--" + gameDetails.get(gameToken).suggestion1 + "--" +
                                    gameDetails.get(gameToken).answer);
                        }
                        if(n == 4) {
                            gameDetails.get(gameToken).printWriters.get(i).println
                                    ("ROUNDOPTIONS--" + gameDetails.get(gameToken).answer + "--" + gameDetails.get(gameToken).suggestion1 + "--" +
                                            gameDetails.get(gameToken).suggestion2);
                            System.out.println("Sent to Client : ROUNDOPTIONS--" + gameDetails.get(gameToken)
                                    .answer + "--" + gameDetails.get(gameToken).suggestion1 + "--" +
                                    gameDetails.get(gameToken).suggestion2);
                        }
                        if(n == 5) {
                            gameDetails.get(gameToken).printWriters.get(i).println
                                    ("ROUNDOPTIONS--" + gameDetails.get(gameToken).answer + "--" + gameDetails.get(gameToken).suggestion2 + "--" +
                                            gameDetails.get(gameToken).suggestion1);
                            System.out.println("Sent to Client : ROUNDOPTIONS--" + gameDetails.get(gameToken)
                                    .answer + "--" + gameDetails.get(gameToken).suggestion2 + "--" +
                                    gameDetails.get(gameToken).suggestion1);
                        }
                    }
                }
            }
        }

    }

    public static void collectPlayersChoices(String message, PrintWriter outToClient) {

        String msg = message;
        msg = msg.replace("--", "-");

        //Checking for Invalid Message Format
        int indexfirst = msg.indexOf("-");
        if (msg.substring(indexfirst, indexfirst + 1).equals(msg.substring(indexfirst + 1, indexfirst + 2))) {
            outToClient.println("RESPONSE--PLAYERCHOICE--INVALIDMESSAGEFORMAT");
            System.out.println("SENT TO CLIENT - RESPONSE--PLAYERCHOICE--INVALIDMESSAGEFORMAT");
        }
        if (msg.substring(msg.length() - 1).equals("-")) {
            outToClient.println("RESPONSE--PLAYERCHOICE--INVALIDMESSAGEFORMAT");
            System.out.println("SENT TO CLIENT - RESPONSE--PLAYERCHOICE--INVALIDMESSAGEFORMAT");
        }
        else {

            message = message.replace("PLAYERCHOICE--", "");
            message = message.replace("--", "*");

            int index = message.indexOf("*");

            String userToken = message.substring(0, index);

            message = message.substring(index + 1, message.length());
            index = message.indexOf("*");

            String gameToken = message.substring(0, index);

            message = message.substring(index + 1);


            int count = 0;
            for (int i = 0; i < gameKeyArray.size(); i++) {
                if (gameKeyArray.get(i).equals(gameToken))
                    count++;
            }

            if (count == 0) {
                outToClient.println("RESPONSE--PLAYERCHOICE--INVALIDGAMETOKEN");
                System.out.println("Sent to Client - RESPONSE--PLAYERCHOICE--INVALIDGAMETOKEN");
            } else {
                int countToken = 0;
                for(int i = 0; i<userTokenArray.size(); i++){
                    if(userTokenArray.get(i).equals(userToken))
                        countToken++;
                }

                if(countToken == 0){
                    outToClient.println("RESPONSE--PLAYERSUGGESTION--USERNOTLOGGEDIN");
                    System.out.println("Sent to Client - RESPONSE--PLAYERSUGGESTION--USERNOTLOGGEDIN");
                } else {
                    if(outToClient == gameDetails.get(gameToken).toLeaderClient)
                        gameDetails.get(gameToken).choice1 = (message);
                    else gameDetails.get(gameToken).choice2 = (message);

                    gameDetails.get(gameToken).countChoices++;
                    System.out.println("Received answer from player : "+  message);

                    if(gameDetails.get(gameToken).countChoices == 2){
                        gameLogicSendResults(gameToken);

                        if(gameDetails.get(gameToken).wordCount< arrayWords.size()){
                            String qaa = arrayWords.get(gameDetails.get(gameToken).wordCount);
                            gameDetails.get(gameToken).wordCount++;
                            int sc = qaa.indexOf(":");
                            gameDetails.get(gameToken).question = qaa.substring(0, sc - 1);
                            gameDetails.get(gameToken).answer = qaa.substring(sc + 2, qaa.length());


                            //Sending the words to all Players
                            for (int i = 0; i < gameDetails.get(gameToken).printWriters.size(); i++) {
                                gameDetails.get(gameToken).printWriters.get(i).println(("NEWGAMEWORD--" + gameDetails.get
                                        (gameToken).question + "--" + gameDetails.get(gameToken).answer));
                                System.out.println("Sent to Client " + gameDetails.get(gameToken).players.get(i) + " : " +
                                        "NEWGAMEWORD--" + gameDetails.get
                                        (gameToken)
                                        .question + "--" + gameDetails.get(gameToken).answer);
                            }


                            gameDetails.get(gameToken).countChoices = 0;
                            gameDetails.get(gameToken).countSuggestions = 0;
                            gameDetails.get(gameToken).choice1 = null;
                            gameDetails.get(gameToken).choice2 = null;
                            gameDetails.get(gameToken).suggestion1 = null;
                            gameDetails.get(gameToken).suggestion2 = null;


                        }
                        else {
                            //Sending Game Over
                            for (int i = 0; i < gameDetails.get(gameToken).printWriters.size(); i++) {
                                gameDetails.get(gameToken).printWriters.get(i).println("GAMEOVER");
                                System.out.println("Sent to client: GAMEOVER");
                            }
                        }
                    }
                }


            }
        }
    }

    public static void gameLogicSendResults(String gameToken) {

        String message1 = "";
        String message2 = "";

        if(gameDetails.get(gameToken).choice1.equals(gameDetails.get(gameToken).answer)) {
            message1 = "You got it right!";
            gameDetails.get(gameToken).user1[2] = Integer.toString(Integer.parseInt(gameDetails.get(gameToken)
                    .user1[2]) + 10);
        }
        if(gameDetails.get(gameToken).choice2.equals(gameDetails.get(gameToken).answer)) {
            message2 = "You got it right!";
            gameDetails.get(gameToken).user2[2] = Integer.toString(Integer.parseInt(gameDetails.get(gameToken)
                    .user2[2]) + 10);
        }
        if(gameDetails.get(gameToken).choice1.equals(gameDetails.get(gameToken).suggestion2)) {
            message1 = message1+"You were fooled by " + gameDetails.get(gameToken).user2[0];
            gameDetails.get(gameToken).user1[4] = Integer.toString(Integer.parseInt(gameDetails.get(gameToken)
                    .user1[4]) + 1);

            message2 = message2+"You fooled " + gameDetails.get(gameToken).user1[0];
            gameDetails.get(gameToken).user2[3] = Integer.toString(Integer.parseInt(gameDetails.get(gameToken)
                    .user2[3]) + 1);
            gameDetails.get(gameToken).user2[2] = Integer.toString(Integer.parseInt(gameDetails.get(gameToken)
                    .user2[2]) + 5);
        }
        if(gameDetails.get(gameToken).choice2.equals(gameDetails.get(gameToken).suggestion1)) {
            message2 = message2+"You were fooled by " + gameDetails.get(gameToken).user1[0];
            gameDetails.get(gameToken).user2[4] = Integer.toString(Integer.parseInt(gameDetails.get(gameToken)
                    .user2[4]) + 1);

            message1 = message1+"You fooled " + gameDetails.get(gameToken).user2[0];
            gameDetails.get(gameToken).user1[3] = Integer.toString(Integer.parseInt(gameDetails.get(gameToken)
                    .user1[3]) + 1);
            gameDetails.get(gameToken).user1[2] = Integer.toString(Integer.parseInt(gameDetails.get(gameToken)
                    .user1[2]) + 5);
        }

        gameDetails.get(gameToken).printWriters.get(0).println("ROUNDRESULT--"+gameDetails.get(gameToken)
                .user1[0]+"--"+message1+"--"+gameDetails.get(gameToken).user1[2]+"--"
                +gameDetails.get(gameToken).user1[3]+"--"+gameDetails.get(gameToken)
                .user1[4]+"--"+gameDetails.get(gameToken).user2[0]+"--"+message2+"--"+gameDetails.get(gameToken)
                .user2[2]+"--"
                +gameDetails.get(gameToken).user2[3]+"--"+gameDetails.get(gameToken).user2[4]);

        gameDetails.get(gameToken).printWriters.get(1).println
                ("ROUNDRESULT--"+gameDetails.get(gameToken).user1[0]+"--"+message1+"--"+gameDetails.get(gameToken)
                        .user1[2]+"--"
                        +gameDetails.get(gameToken).user1[3]+"--"+gameDetails.get(gameToken)
                        .user1[4]+"--"+gameDetails.get(gameToken).user2[0]+"--"+message2+"--"+gameDetails.get
                        (gameToken).user2[2]+"--"
                        +gameDetails.get(gameToken).user2[3]+"--"+gameDetails.get(gameToken).user2[4]);
    }


}
