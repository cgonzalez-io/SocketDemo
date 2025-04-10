import org.json.JSONArray;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Scanner;

/**
 *
 */
public class SockClient {
    static Socket sock = null;
    static String host = "localhost";
    static int port = 8888;
    static OutputStream out;
    // Using and Object Stream here and a Data Stream as return. Could both be the same type I just wanted
    // to show the difference. Do not change these types.
    static ObjectOutputStream os;
    static DataInputStream in;

    public static void main(String[] args) {

        if (args.length != 2) {
            System.out.println("Expected arguments: <host(String)> <port(int)>");
            System.exit(1);
        }

        try {
            host = args[0];
            port = Integer.parseInt(args[1]);
        } catch (NumberFormatException nfe) {
            System.out.println("[Port|sleepDelay] must be an integer");
            System.exit(2);
        }

        try {
            connect(host, port); // connecting to server
            System.out.println("Client connected to server.");
            boolean requesting = true;
            while (requesting) {
                System.out.println("What would you like to do: 1 - echo, 2 - add, 3 - addmany, 4 - string concatenation, 5 - quizz (0 to quit)");
                Scanner scanner = new Scanner(System.in);
                int choice = Integer.parseInt(scanner.nextLine());
                // You can assume the user put in a correct input, you do not need to handle errors here
                // You can assume the user inputs a String when asked and an int when asked. So you do not have to handle user input checking
                JSONObject json = new JSONObject(); // request object
                switch (choice) {
                    case 0:
                        System.out.println("Choose quit. Thank you for using our services. Goodbye!");
                        requesting = false;
                        break;
                    case 1:
                        System.out.println("Choose echo, which String do you want to send?");
                        String message = scanner.nextLine();
                        json.put("type", "echo");
                        json.put("data", message);
                        break;
                    case 2:
                        System.out.println("Choose add, enter first number:");
                        String num1 = scanner.nextLine();
                        json.put("type", "add");
                        json.put("num1", num1);

                        System.out.println("Enter second number:");
                        String num2 = scanner.nextLine();
                        json.put("num2", num2);
                        break;
                    case 3:
                        System.out.println("Choose addmany, enter as many numbers as you like, when done choose 0:");
                        JSONArray array = new JSONArray();
                        String num = "1";
                        while (!num.equals("0")) {
                            num = scanner.nextLine();
                            array.put(num);
                            System.out.println("Got your " + num);
                        }
                        json.put("type", "addmany");
                        json.put("nums", array);
                        break;
                    case 4:
                        System.out.println("Choose string concatenation. Enter first string:");
                        String str1 = scanner.nextLine();
                        System.out.println("Enter second string:");
                        String str2 = scanner.nextLine();
                        json.put("type", "stringconcatenation");
                        json.put("string1", str1);
                        json.put("string2", str2);
                        break;
                    case 5:
                        System.out.println("Quiz Game Service. Choose an option:");
                        System.out.println("1: Add a new question");
                        System.out.println("2: Request a new question");
                        System.out.println("3: Answer the current question");
                        int quizChoice = Integer.parseInt(scanner.nextLine());
                        JSONObject quizJson = new JSONObject();
                        quizJson.put("type", "quizgame");

                        if (quizChoice == 1) {
                            // Add a question
                            System.out.println("Enter the new question:");
                            String newQuestion = scanner.nextLine();
                            System.out.println("Enter the answer for the new question:");
                            String newAnswer = scanner.nextLine();
                            quizJson.put("addQuestion", true);
                            quizJson.put("question", newQuestion);
                            quizJson.put("answer", newAnswer);
                        } else if (quizChoice == 2) {
                            // Request a new question (playing the game)
                            quizJson.put("addQuestion", false);
                        } else if (quizChoice == 3) {
                            // Answer the current question
                            System.out.println("Enter your answer:");
                            String userAnswer = scanner.nextLine();
                            quizJson.put("answer", userAnswer);
                        } else {
                            System.out.println("Invalid quiz option.");
                            continue; // back to the main menu
                        }
                        // Use quizJson as your request:
                        json = quizJson;
                        break;


                }
                if (!requesting) {
                    continue;
                }

                // write the whole message
                os.writeObject(json.toString());
                // make sure it wrote and doesn't get cached in a buffer
                os.flush();

                // handle the response
                // - not doing anything other than printing payload
                // !! you will most likely need to parse the response for the other 2 services!
                String i = in.readUTF();
                JSONObject res = new JSONObject(i);
                System.out.println("Got response: " + res);
                if (res.getBoolean("ok")) {
                    if (res.getString("type").equals("echo")) {
                        System.out.println(res.getString("echo"));
                    } else {
                        System.out.println(res.getInt("result"));
                    }
                } else {
                    System.out.println(res.getString("message"));
                }
            }
            // want to keep requesting services so don't close connection
            //overandout();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void overandout() throws IOException {
        //closing things, could
        in.close();
        os.close();
        sock.close(); // close socked after sending
    }

    public static void connect(String host, int port) throws IOException {
        // open the connection
        sock = new Socket(host, port); // connect to host and socket on port 8888

        // get output channel
        out = sock.getOutputStream();

        // create an object output writer (Java only)
        os = new ObjectOutputStream(out);

        in = new DataInputStream(sock.getInputStream());
    }
}