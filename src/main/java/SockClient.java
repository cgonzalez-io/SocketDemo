import org.json.JSONArray;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Scanner;


/**
 * The SockClient class is a simple TCP client that communicates with a server using JSON-based messages.
 * It provides users with several interactive service options, such as echoing messages, performing arithmetic calculations,
 * string concatenation, and engaging in a quiz game. The client connects to a server specified by the host and port
 * provided as command-line arguments.
 * The services include:
 * 1. Echo: Sends a string to the server and receives the same string as a response.
 * 2. Add: Sends two numbers to the server and receives their sum as a response.
 * 3. AddMany: Sends an array of numbers to the server and receives their sum as a response.
 * 4. String Concatenation: Sends two strings to the server and receives their concatenation as a response.
 * 5. Quiz Game Service: Allows the user to either add a new question-answer pair, request a quiz question, or answer a current question.
 * The communication is handled using JSON objects for requests and responses. This class demonstrates basic input/output
 * handling, server communication, and JSON parsing in a Java application.
 * Notes:
 * - The program requires the host (as a String) and port (as an integer) to be passed as command-line arguments.
 * - Input validation is minimal and assumes correct user input where required.
 * - All services communicate with the server through sockets using ObjectOutputStream for outgoing messages
 * and DataInputStream for incoming messages.
 * Static Methods:
 * - main(String[] args): The entry point of the application. Initializes client-server communication, prompts the user for input,
 * and handles responses for the selected services.
 * - overandout(): Closes all input/output streams and the socket connection.
 * - connect(String host, int port): Establishes a connection to the server using the specified host and port.
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
                // ... after receiving the response from server into res ...
                System.out.println("Got response: " + res);
                if (res.getBoolean("ok")) {
                    String type = res.getString("type");
                    switch (type) {
                        case "echo":
                            // For the echo service, print the echoed string.
                            System.out.println(res.getString("echo"));
                            break;
                        case "add":
                        case "addmany":
                            // For addition services, the result is an integer.
                            System.out.println(res.getInt("result"));
                            break;
                        case "stringconcatenation":
                            // For string concatenation, the result is a string.
                            System.out.println(res.getString("result"));
                            break;
                        case "quizgame":
                            // Handle the quiz game response based on your protocol.
                            // For example, if it has a "question" field then print that, etc.
                            if (res.has("question")) {
                                System.out.println("Question: " + res.getString("question"));
                            } else if (res.has("result")) {
                                boolean correct = res.getBoolean("result");
                                System.out.println("Your answer is " + (correct ? "correct" : "incorrect"));
                            }
                            break;
                        default:
                            // If the response type is unrecognized
                            System.out.println("Unrecognized response type: " + type);
                            break;
                    }
                } else {
                    // If the response indicates an error, print the error message.
                    System.out.println(res.getString("message"));
                }

            }
            // want to keep requesting services so don't close connection
            //overandout();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Closes the resources used for communication, including the input stream,
     * output stream, and socket connection.
     *
     * @throws IOException if an I/O error occurs while attempting to close any of the resources
     */
    private static void overandout() throws IOException {
        //closing things, could
        in.close();
        os.close();
        sock.close(); // close socked after sending
    }

    /**
     * Establishes a connection to the specified host and port.
     * Initializes the socket, output stream, object output stream, and input stream
     * required for communication.
     *
     * @param host the hostname or IP address of the server to connect to
     * @param port the specific port number on the server to connect to
     * @throws IOException if an I/O error occurs when creating the socket or streams
     */
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