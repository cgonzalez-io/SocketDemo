import jakarta.xml.bind.DatatypeConverter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A class to demonstrate a simple client-server connection using sockets.
 * Robust error handling and logging have been added so that unexpected issues
 * (such as connection problems or improper requests) do not crash the server.
 */
public class SockServer {
    private static final Logger logger = LoggerFactory.getLogger(SockServer.class);
    private static final byte[] JAVA_MAGIC_HEADER = {(byte) 0xAC, (byte) 0xED, 0x00, 0x05};
    // A simple rate limiter per IP (you can swap this with a more advanced implementation)
// Note: This is a simple example and may require refinement for production use.
    private static final Map<String, Integer> connectionAttempts = new ConcurrentHashMap<>();
    private static final int MAX_CONNECTIONS_PER_MINUTE = 4;
    static ArrayList<Question> quizQuestions = new ArrayList<>();

    // Static initializer for quiz questions.
    static {
        quizQuestions.add(new Question("What is 2+2?", "4"));
        quizQuestions.add(new Question("What is the capital of France?", "Paris"));
    }

    /**
     * Checks if the specified IP address has exceeded the allowed number of connection attempts
     * within a defined time period.
     *
     * @param ip the IP address to check for rate limiting
     * @return true if the IP address is rate-limited, false otherwise
     */
    private static boolean isRateLimited(String ip) {
        int count = connectionAttempts.getOrDefault(ip, 0);
        if (count > MAX_CONNECTIONS_PER_MINUTE) {
            return true;
        }
        connectionAttempts.put(ip, count + 1);
        return false;
    }

    public static void main(String[] args) {

        if (args.length < 1) {
            throw new IllegalArgumentException("Expected port as argument: <port>");
        }

        int port = 8888;
        try {
            port = Integer.parseInt(args[0]);
        } catch (NumberFormatException nfe) {
            logger.error("Port argument must be an integer. Provided: {}", args[0]);
            System.exit(2);
        }

        try (ServerSocket serv = new ServerSocket(port)) {
            logger.info("Server started on port {}", port);
            System.out.println("Server started on port " + port);

            // The main loop now catches exceptions and logs them without crashing.
            while (true) {
                try {
                    logger.info("Waiting for client connection...");
                    Socket sock = serv.accept(); // blocking wait
                    logger.info("Client connected: {}", sock.getRemoteSocketAddress());
                    handleClient(sock);
                } catch (Exception e) {
                    // Log exception details and continue waiting for the next client.
                    logger.error("Error accepting or handling client connection: {}", e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            logger.error("Critical error starting server: {}", e.getMessage(), e);
        }
    }

    /**
     * Processes a single client connection in a try-with-resources block.
     */
    private static void handleClient(Socket clientSocket) {
        String clientIP = clientSocket.getInetAddress().getHostAddress();

        // Rate limiting check.
        if (isRateLimited(clientIP)) {
            logger.warn("Rate limit exceeded for client IP: {}", clientIP);
            try {
                clientSocket.close();
            } catch (Exception ignore) {
            }
            return;
        }

        try (
                // Wrap the raw input stream so we can check the magic header first.
                BufferedInputStream bis = new BufferedInputStream(clientSocket.getInputStream());
                DataOutputStream os = new DataOutputStream(clientSocket.getOutputStream())
        ) {
            bis.mark(8);  // mark the stream so we can reset it after reading the first 4 bytes.
            byte[] header = new byte[4];
            int readCount = bis.read(header);
            if (readCount != 4 || !Arrays.equals(header, JAVA_MAGIC_HEADER)) {
                // Log the received header as a hex string for diagnostic purposes.
                String hexHeader = DatatypeConverter.printHexBinary(header);
                logger.warn("[{}] Received invalid magic header: {}. Connection will be closed.", clientSocket.getRemoteSocketAddress(), hexHeader);
                clientSocket.close();
                return;
            }
            // Reset the stream so that ObjectInputStream can read the entire serialized stream.
            bis.reset();
            ObjectInputStream in = new ObjectInputStream(bis);

            // Process the connection using your protocol as before.
            final Question[] currentQuizQuestionHolder = new Question[1];
            boolean connected = true;
            while (connected) {
                String input = "";
                try {
                    input = (String) in.readObject();
                    logger.info("[{}] Received request: {}", clientSocket.getRemoteSocketAddress(), input);
                } catch (EOFException eof) {
                    logger.warn("[{}] Client disconnected unexpectedly: {}", clientSocket.getRemoteSocketAddress(), eof.getMessage());
                    break;
                } catch (StreamCorruptedException sce) {
                    logger.warn("[{}] Stream corrupted: {}", clientSocket.getRemoteSocketAddress(), sce.getMessage());
                    break;
                } catch (Exception e) {
                    logger.warn("[{}] Exception reading from client: {}. Possibly bad protocol data.", clientSocket.getRemoteSocketAddress(), e.getMessage());
                    break;
                }

                // Validate that the input is valid JSON. If not, send an error response.
                JSONObject res = isValid(input);
                if (res.has("ok") && !res.getBoolean("ok")) {
                    writeOut(os, res);
                    continue;
                }

                // Process the request.
                JSONObject req;
                try {
                    req = new JSONObject(input);
                } catch (JSONException je) {
                    logger.error("[{}] Failed to parse JSON request: {}", clientSocket.getRemoteSocketAddress(), je.getMessage());
                    JSONObject errorRes = new JSONObject();
                    errorRes.put("ok", false);
                    errorRes.put("message", "Invalid JSON format.");
                    writeOut(os, errorRes);
                    continue;
                }

                res = testField(req, "type");
                if (!res.getBoolean("ok")) { // no "type" header provided
                    res = noType(req);
                    writeOut(os, res);
                    continue;
                }

                try {
                    // Use switch or if-else to process request by type.
                    String reqType = req.getString("type");
                    switch (reqType) {
                        case "echo":
                            res = echo(req);
                            break;
                        case "add":
                            res = add(req);
                            break;
                        case "addmany":
                            res = addmany(req);
                            break;
                        case "stringconcatenation":
                            // If the request contains the "strings" field (an array),
                            // then call the helper that concatenates arrays.
                            if (req.has("strings"))
                                res = concatenation(req);
                            else
                                res = stringConcatenation(req);
                            break;
                        case "quizgame":
                            // If the request contains the "options" field,
                            // then it’s the multiple‐choice version; call the helper method for that.
                            if (req.has("options"))
                                res = quiz(req);
                            else
                                res = quizGame(req, currentQuizQuestionHolder);
                            break;
                        default:
                            res = wrongType(req);
                            break;
                    }
                } catch (Exception e) {
                    logger.error("[{}] Exception processing request: {}", clientSocket.getRemoteSocketAddress(), e.getMessage(), e);
                    res = new JSONObject();
                    res.put("ok", false);
                    res.put("message", "Internal server error while processing request.");
                }
                writeOut(os, res);
            }
        } catch (Exception e) {
            logger.error("[{}] Exception handling client: {}", clientSocket.getRemoteSocketAddress(), e.getMessage(), e);
        } finally {
            try {
                if (!clientSocket.isClosed()) {
                    clientSocket.close();
                }
                logger.info("Closed connection to client {}", clientSocket.getRemoteSocketAddress());
            } catch (Exception e) {
                logger.error("Error closing client socket: {}", e.getMessage(), e);
            }
        }
    }

    /**
     * Sends the response using the provided DataOutputStream.
     */
    static void writeOut(DataOutputStream os, JSONObject res) {
        try {
            os.writeUTF(res.toString());
            os.flush();
            logger.info("Sent response: {}", res);
        } catch (Exception e) {
            logger.error("Error writing response: {}", e.getMessage(), e);
        }
    }

    /**
     * Validates if the provided string is a valid JSON object or array.
     */
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

    /**
     * Checks if a specific field exists in the request.
     */
    static JSONObject testField(JSONObject req, String key) {
        JSONObject res = new JSONObject();
        if (!req.has(key)) {
            res.put("ok", false);
            res.put("message", "Field " + key + " does not exist in request");
            return res;
        }
        return res.put("ok", true);
    }

    // Echo service handler
    static JSONObject echo(JSONObject req) {
        logger.info("Processing echo request: {}", req);
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

    // Add service handler
    static JSONObject add(JSONObject req) {
        logger.info("Processing add request: {}", req);
        JSONObject res1 = testField(req, "num1");
        if (!res1.getBoolean("ok")) return res1;
        JSONObject res2 = testField(req, "num2");
        if (!res2.getBoolean("ok")) return res2;

        JSONObject res = new JSONObject();
        res.put("ok", true);
        res.put("type", "add");
        try {
            res.put("result", req.getInt("num1") + req.getInt("num2"));
        } catch (JSONException e) {
            res.put("ok", false);
            res.put("message", "Field num1/num2 needs to be of type: int");
        }
        return res;
    }

    // Add many service handler
    static JSONObject addmany(JSONObject req) {
        logger.info("Processing addmany request: {}", req);
        JSONObject res = testField(req, "nums");
        if (!res.getBoolean("ok")) return res;

        int result = 0;
        JSONArray array = req.getJSONArray("nums");
        for (int i = 0; i < array.length(); i++) {
            try {
                result += array.getInt(i);
            } catch (JSONException e) {
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

    // String concatenation service handler
    static JSONObject stringConcatenation(JSONObject req) {
        logger.info("Processing stringconcatenation request: {}", req);
        JSONObject res = testField(req, "string1");
        if (!res.getBoolean("ok")) return res;
        res = testField(req, "string2");
        if (!res.getBoolean("ok")) return res;

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
        String concatenated = req.getString("string1") + req.getString("string2");
        JSONObject response = new JSONObject();
        response.put("type", "stringconcatenation");
        response.put("ok", true);
        response.put("result", concatenated);
        return response;
    }

    // Quiz game service handler
    static JSONObject quizGame(JSONObject req, Question[] currentQuizQuestionHolder) {
        logger.info("Processing quizgame request: {}", req);
        JSONObject response = new JSONObject();
        response.put("type", "quizgame");

        if (req.has("addQuestion")) {
            boolean addQuestion = req.getBoolean("addQuestion");
            if (addQuestion) {
                JSONObject resTest = testField(req, "question");
                if (!resTest.getBoolean("ok")) return resTest;
                resTest = testField(req, "answer");
                if (!resTest.getBoolean("ok")) return resTest;

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
                String questionText = req.getString("question");
                String answer = req.getString("answer");
                quizQuestions.add(new Question(questionText, answer));

                response.put("ok", true);
                return response;
            } else {
                if (quizQuestions.isEmpty()) {
                    response.put("ok", false);
                    response.put("message", "No quiz questions available.");
                    return response;
                }
                int index = (int) (Math.random() * quizQuestions.size());
                Question selected = quizQuestions.get(index);
                currentQuizQuestionHolder[0] = selected;
                response.put("ok", true);
                response.put("question", selected.questionText);
                return response;
            }
        } else if (req.has("answer")) {
            if (currentQuizQuestionHolder[0] == null) {
                response.put("ok", false);
                response.put("message", "No active quiz question. Please request a new question first.");
                return response;
            }
            if (!req.get("answer").getClass().getName().equals("java.lang.String")) {
                response.put("ok", false);
                response.put("message", "Field answer needs to be of type: String");
                return response;
            }
            String clientAnswer = req.getString("answer").trim();
            String correctAnswer = currentQuizQuestionHolder[0].answer.trim();
            boolean result = clientAnswer.equalsIgnoreCase(correctAnswer);
            response.put("ok", true);
            response.put("result", result);
            if (!result) {
                response.put("question", currentQuizQuestionHolder[0].questionText);
                logger.info("Quiz answer incorrect. Client answer: {}", clientAnswer);
            } else {
                currentQuizQuestionHolder[0] = null;
                logger.info("Quiz answer correct: {}", clientAnswer);
            }
            return response;
        } else {
            response.put("ok", false);
            response.put("message", "Invalid quizgame request. Must include 'addQuestion' or 'answer'.");
            return response;
        }
    }

    // Handler for unknown type requests.
    static JSONObject wrongType(JSONObject req) {
        logger.warn("Wrong type request received: {}", req);
        JSONObject res = new JSONObject();
        res.put("ok", false);
        res.put("message", "Type " + req.getString("type") + " is not supported.");
        return res;
    }

    // Handler for requests with no type.
    static JSONObject noType(JSONObject req) {
        logger.warn("No type in request: {}", req);
        JSONObject res = new JSONObject();
        res.put("ok", false);
        res.put("message", "No request type was given.");
        return res;
    }

    static JSONObject quiz(JSONObject req) {
        logger.info("Processing quiz request: {}", req);
        JSONObject response = new JSONObject();
        response.put("type", "quizgame");
        try {
            // Ensure that all required fields exist.
            JSONObject resTest = testField(req, "question");
            if (!resTest.getBoolean("ok")) return resTest;
            resTest = testField(req, "options");
            if (!resTest.getBoolean("ok")) return resTest;
            resTest = testField(req, "answer");
            if (!resTest.getBoolean("ok")) return resTest;

            String question = req.getString("question");
            JSONArray options = req.getJSONArray("options");
            int answer = req.getInt("answer");

            // Check that answer index is within the valid range.
            if (answer < 0 || answer >= options.length()) {
                response.put("ok", false);
                response.put("message", "Answer is not in range of options");
                return response;
            }
            // For simplicity, assume that the provided answer is accepted as correct,
            // and simply return it. (You can extend this logic if needed.)
            response.put("ok", true);
            response.put("result", answer);
            return response;
        } catch (Exception e) {
            logger.error("Error processing quiz request: {}", e.getMessage(), e);
            response.put("ok", false);
            response.put("message", "Error processing quiz request: " + e.getMessage());
            return response;
        }
    }

    //Helper methods to handle combined requests

    static JSONObject concatenation(JSONObject req) {
        logger.info("Processing concatenation request: {}", req);
        JSONObject res = testField(req, "strings");
        if (!res.getBoolean("ok")) {
            return res;
        }
        JSONArray arr;
        try {
            arr = req.getJSONArray("strings");
        } catch (JSONException e) {
            res.put("ok", false);
            res.put("message", "Field strings needs to be of type: JSON Array");
            return res;
        }
        StringBuilder concatenated = new StringBuilder();
        for (int i = 0; i < arr.length(); i++) {
            try {
                String s = arr.getString(i);
                concatenated.append(s);
            } catch (JSONException e) {
                res.put("ok", false);
                res.put("message", "All elements in strings must be of type: String");
                return res;
            }
        }
        JSONObject response = new JSONObject();
        response.put("type", "stringconcatenation");
        response.put("ok", true);
        response.put("result", concatenated.toString());
        return response;
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
