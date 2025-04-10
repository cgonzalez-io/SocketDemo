import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

/**
 * A class to demonstrate a simple client-server connection using sockets.
 */
public class SockServer {
    static Socket sock;
    static DataOutputStream os;
    static ObjectInputStream in;

    static int port = 8888;
    // Static list to store all quiz questions
    static ArrayList<Question> quizQuestions = new ArrayList<>();

    // A simple static initializer to add default questions:
    static {
        quizQuestions.add(new Question("What is 2+2?", "4"));
        quizQuestions.add(new Question("What is the capital of France?", "Paris"));
    }

    public static void main(String[] args) {

        if (args.length < 2) {
            throw new IllegalArgumentException("Expected host and port as arguments: <host> <port>");
        }

        String host = args[0];
        int port = Integer.parseInt(args[1]);

        try {
            port = Integer.parseInt(args[1]);
        } catch (NumberFormatException nfe) {
            System.out.println("[Port|sleepDelay] must be an integer");
            System.exit(2);
        }

        try {
            //open socket
            ServerSocket serv = new ServerSocket(port,50, java.net.InetAddress.getByName(host));
            System.out.println("Server started on " + host + ":" + port);

            while (true) {
                System.out.println("Server waiting for a connection");
                sock = serv.accept(); // blocking wait
                System.out.println("Client connected");

                // setup the object reading channel
                in = new ObjectInputStream(sock.getInputStream());

                // get output channel
                OutputStream out = sock.getOutputStream();

                // create an object output writer (Java only)
                os = new DataOutputStream(out);

                // Add the current quiz question holder specific to this client session.
                // This array will hold the current quiz question, if one is active.
                final Question[] currentQuizQuestionHolder = new Question[1]; // index 0 will hold the current question


                boolean connected = true;
                while (connected) {
                    //track current question
                    // For tracking the current quiz question for this client session
                    String s = "";
                    try {
                        s = (String) in.readObject(); // attempt to read string in from client
                    } catch (Exception e) { // catch rough disconnect
                        System.out.println("Client disconnect");
                        connected = false;
                        continue;
                    }

                    JSONObject res = isValid(s);

                    if (res.has("ok")) {
                        writeOut(res);
                        continue;
                    }

                    JSONObject req = new JSONObject(s);

                    res = testField(req, "type");
                    if (!res.getBoolean("ok")) { // no "type" header provided
                        res = noType(req);
                        writeOut(res);
                        continue;
                    }
                    // check which request it is (could also be a switch statement)
                    if (req.getString("type").equals("echo")) {
                        res = echo(req);
                    } else if (req.getString("type").equals("add")) {
                        res = add(req);
                    } else if (req.getString("type").equals("addmany")) {
                        res = addmany(req);
                    } else if (req.getString("type").equals("stringconcatenation")) {
                        res = stringConcatenation(req);
                    } else if (req.getString("type").equals("quizgame")) {
                        res = quizGame(req, currentQuizQuestionHolder); // see note below
                    } else {
                        res = wrongType(req);
                    }
                    writeOut(res);
                }
                // if we are here - client has disconnected so close connection to socket
                overandout();
            }
        } catch (Exception e) {
            e.printStackTrace();
            overandout(); // close connection to socket upon error
        }
    }

    /**
     * Checks if a specific field exists
     */
    static JSONObject testField(JSONObject req, String key) {
        JSONObject res = new JSONObject();

        // field does not exist
        if (!req.has(key)) {
            res.put("ok", false);
            res.put("message", "Field " + key + " does not exist in request");
            return res;
        }
        return res.put("ok", true);
    }

    // handles the simple echo request
    static JSONObject echo(JSONObject req) {
        System.out.println("Echo request: " + req.toString());
        JSONObject res = testField(req, "data");
        if (res.getBoolean("ok")) {
            if (!req.get("data").getClass().getName().equals("java.lang.String")) {
                res.put("ok", false);
                res.put("message", "Field data needs to be of type: String");
                return res;
            }

            res.put("type", "echo");
            res.put("echo", "Here is your echo: " + req.getString("data"));
        }
        return res;
    }

    // handles the simple add request with two numbers
    static JSONObject add(JSONObject req) {
        System.out.println("Add request: " + req.toString());
        JSONObject res1 = testField(req, "num1");
        if (!res1.getBoolean("ok")) {
            return res1;
        }

        JSONObject res2 = testField(req, "num2");
        if (!res2.getBoolean("ok")) {
            return res2;
        }

        JSONObject res = new JSONObject();
        res.put("ok", true);
        res.put("type", "add");
        try {
            res.put("result", req.getInt("num1") + req.getInt("num2"));
        } catch (org.json.JSONException e) {
            res.put("ok", false);
            res.put("message", "Field num1/num2 needs to be of type: int");
        }
        return res;
    }

    static JSONObject quizGame(JSONObject req, Question[] currentQuizQuestionHolder) {
        JSONObject response = new JSONObject();
        response.put("type", "quizgame");

        // Case 1: Client wants to add a new question.
        if (req.has("addQuestion")) {
            boolean addQuestion = req.getBoolean("addQuestion");
            if (addQuestion) {
                // Check for required fields "question" and "answer"
                JSONObject resTest = testField(req, "question");
                if (!resTest.getBoolean("ok")) return resTest;
                resTest = testField(req, "answer");
                if (!resTest.getBoolean("ok")) return resTest;

                // Verify they are strings
                if (!req.get("question").getClass().getName().equals("java.lang.String")) {
                    response.put("ok", false);
                    response.put("message", "Field question needs to be of type: String");
                    return response;
                }
                if (!req.get("answer").getClass().getName().equals("java.lang.String")) {
                    response.put("ok", false);
                    response.put("message", "Field answer needs to be of type: String");
                    return response;
                }

                // Add the new question
                String questionText = req.getString("question");
                String answer = req.getString("answer");
                quizQuestions.add(new Question(questionText, answer));

                response.put("ok", true);
                return response;
            } else {
                // Case 2: Client wants to play the game (i.e., request a new question)
                if (quizQuestions.isEmpty()) {
                    response.put("ok", false);
                    response.put("message", "No quiz questions available.");
                    return response;
                }
                // Randomly select a question
                int index = (int) (Math.random() * quizQuestions.size());
                Question selected = quizQuestions.get(index);
                currentQuizQuestionHolder[0] = selected; // Save this as the current active question for this session

                response.put("ok", true);
                response.put("question", selected.questionText);
                return response;
            }
        }
        // Case 3: Client is answering a question.
        else if (req.has("answer")) {
            // Verify that there is an active question
            if (currentQuizQuestionHolder[0] == null) {
                response.put("ok", false);
                response.put("message", "No active quiz question. Please request a new question first.");
                return response;
            }
            // Verify that the answer is provided as a string
            if (!req.get("answer").getClass().getName().equals("java.lang.String")) {
                response.put("ok", false);
                response.put("message", "Field answer needs to be of type: String");
                return response;
            }
            String clientAnswer = req.getString("answer").trim();
            String correctAnswer = currentQuizQuestionHolder[0].answer.trim();

            // Check answer case-insensitively
            boolean result = clientAnswer.equalsIgnoreCase(correctAnswer);
            response.put("ok", true);
            response.put("result", result);

            // If the answer is incorrect, send the question again to remind the client
            if (!result) {
                response.put("question", currentQuizQuestionHolder[0].questionText);
            } else {
                // Optionally clear the current question so a new one is requested next time.
                currentQuizQuestionHolder[0] = null;
            }
            return response;
        }
        // If the request doesn't contain addQuestion or answer fields, it's invalid.
        else {
            response.put("ok", false);
            response.put("message", "Invalid quizgame request. Must include 'addQuestion' or 'answer'.");
            return response;
        }
    }


    static JSONObject stringConcatenation(JSONObject req) {
        // Check that "string1" exists
        JSONObject res = testField(req, "string1");
        if (!res.getBoolean("ok")) {
            return res;  // return error response if missing
        }

        // Check that "string2" exists
        res = testField(req, "string2");
        if (!res.getBoolean("ok")) {
            return res;
        }

        // Check that both fields are Strings
        if (!req.get("string1").getClass().getName().equals("java.lang.String")) {
            JSONObject error = new JSONObject();
            error.put("ok", false);
            error.put("message", "Field string1 needs to be of type: String");
            return error;
        }
        if (!req.get("string2").getClass().getName().equals("java.lang.String")) {
            JSONObject error = new JSONObject();
            error.put("ok", false);
            error.put("message", "Field string2 needs to be of type: String");
            return error;
        }

        // Perform concatenation
        String concatenated = req.getString("string1") + req.getString("string2");

        // Build the success response
        JSONObject response = new JSONObject();
        response.put("type", "stringconcatenation");
        response.put("ok", true);
        response.put("result", concatenated);
        return response;
    }

    // handles the simple addmany request
    static JSONObject addmany(JSONObject req) {
        System.out.println("Add many request: " + req.toString());
        JSONObject res = testField(req, "nums");
        if (!res.getBoolean("ok")) {
            return res;
        }

        int result = 0;
        JSONArray array = req.getJSONArray("nums");
        for (int i = 0; i < array.length(); i++) {
            try {
                result += array.getInt(i);
            } catch (org.json.JSONException e) {
                res.put("ok", false);
                res.put("message", "Values in array need to be ints");
                return res;
            }
        }

        res.put("ok", true);
        res.put("type", "addmany");
        res.put("result", result);
        return res;
    }

    // creates the error message for wrong type
    static JSONObject wrongType(JSONObject req) {
        System.out.println("Wrong type request: " + req.toString());
        JSONObject res = new JSONObject();
        res.put("ok", false);
        res.put("message", "Type " + req.getString("type") + " is not supported.");
        return res;
    }

    // creates the error message for no given type
    static JSONObject noType(JSONObject req) {
        System.out.println("No type request: " + req.toString());
        JSONObject res = new JSONObject();
        res.put("ok", false);
        res.put("message", "No request type was given.");
        return res;
    }

    // From: https://www.baeldung.com/java-validate-json-string
    public static JSONObject isValid(String json) {
        try {
            new JSONObject(json);
        } catch (JSONException e) {
            try {
                new JSONArray(json);
            } catch (JSONException ne) {
                JSONObject res = new JSONObject();
                res.put("ok", false);
                res.put("message", "req not JSON");
                return res;
            }
        }
        return new JSONObject();
    }

    // sends the response and closes the connection between client and server.
    static void overandout() {
        try {
            os.close();
            in.close();
            sock.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    // sends the response and closes the connection between client and server.
    static void writeOut(JSONObject res) {
        try {
            os.writeUTF(res.toString());
            // make sure it wrote and doesn't get cached in a buffer
            os.flush();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    // A simple class to encapsulate a quiz question and its answer
    static class Question {
        String questionText;
        String answer;

        Question(String questionText, String answer) {
            this.questionText = questionText;
            this.answer = answer;
        }
    }

}
