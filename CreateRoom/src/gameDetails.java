import java.io.PrintWriter;
import java.util.ArrayList;

/**
 * Created by csclass on 12/2/16.
 */
public class gameDetails {


    public ArrayList<PrintWriter> printWriters = new ArrayList<>();

    public ArrayList<String> players = new ArrayList<>();
    int questionCount = 0;

    public PrintWriter toLeaderClient;
    int wordCount = 0;

    String question;
    String answer;
    String suggestion1;
    String suggestion2;

    String choice1;
    String choice2;

    String[] user1;
    String[] user2;

    int countSuggestions = 0;
    int countChoices = 0;


}
