import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;


public class ServerTest {

    Socket sock;
    OutputStream out;
    ObjectOutputStream os;

    DataInputStream in;


    /**
     * Sets up the required resources and establishes a connection to the server
     * before running the tests. This method is annotated with {@code @Before} and
     * will execute before each test in the {@code ServerTest} class.
     * <p>
     * Responsibilities of this method include:
     * - Connecting to the server running on localhost with port 8888.
     * - Initializing the socket for communication between the test client and server.
     * - Setting up the output stream for sending data to the server.
     * - Setting up the input stream for receiving data from the server.
     *
     * @throws Exception if an error occurs while setting up the connection or streams.
     */
    // Establishing a connection to the server, make sure you start the server on localhost and 8888
    @org.junit.Before
    public void setUp() throws Exception {
        // Establish connection to server and create in/out streams
        sock = new Socket("localhost", 8888); // connect to host and socket

        // get output channel
        out = sock.getOutputStream();

        // create an object output writer (Java only)
        os = new ObjectOutputStream(out);

        // setup input stream
        in = new DataInputStream(sock.getInputStream());
    }

    /**
     * Closes the resources associated with the socket connection and the output stream.
     * <p>
     * This method performs cleanup by safely closing any open streams and sockets initialized during the test execution.
     * Ensures that the `out` and `sock` fields are not left open after the test runs, potentially avoiding resource leaks and
     * ensuring proper termination of network or I/O resources.
     *
     * @throws Exception if an I/O error occurs while closing the resources.
     */
    @org.junit.After
    public void close() throws Exception {
        if (out != null) out.close();
        if (sock != null) sock.close();
    }

    /**
     * Tests the behavior of the server's "add" request functionality.
     * This method sends multiple requests to the server, each testing different scenarios
     * for the "add" operation. The server is expected to handle valid requests correctly
     * and return appropriate error messages for invalid or malformed requests.
     * The following scenarios are tested:
     * 1. A correct request with valid "num1" and "num2" fields.
     * 2. A request missing the "num2" field.
     * 3. A request missing the "num1" field.
     * 4. A request missing both "num1" and "num2" fields.
     * 5. A request with non-integer values for "num1" and/or "num2".
     * Each scenario asserts the correctness of the server's response by:
     * - Validating the "ok" status in the response.
     * - Checking the "type" field in valid responses.
     * - Ensuring the "result" field in valid responses matches the expected addition.
     * - Verifying error messages for invalid requests.
     *
     * @throws IOException if an I/O error occurs during communication with the server
     */
    @Test
    public void addRequest() throws IOException {
        // create a correct req for server
        JSONObject req = new JSONObject();
        req.put("type", "add");
        req.put("num1", "1");
        req.put("num2", "2");

        // write the whole message
        os.writeObject(req.toString());
        // make sure it wrote and doesn't get cached in a buffer
        os.flush();

        String i = in.readUTF();
        // assuming I get correct JSON back
        JSONObject res = new JSONObject(i);

        // test response
        assertTrue(res.getBoolean("ok"));
        assertEquals("add", res.getString("type"));
        assertEquals(3, res.getInt("result"));

        // Wrong request to server num2 missing
        JSONObject req2 = new JSONObject();
        req2.put("type", "add");
        req2.put("num1", "1");
        // write the whole message
        os.writeObject(req2.toString());
        // make sure it wrote and doesn't get cached in a buffer
        os.flush();

        i = in.readUTF();
        // assuming I get correct JSON back
        res = new JSONObject(i);

        System.out.println(res);

        // test response
        assertFalse(res.getBoolean("ok"));
        assertEquals("Field num2 does not exist in request", res.getString("message"));

        // Wrong request to server num1 missing
        JSONObject req3 = new JSONObject();
        req3.put("type", "add");
        req3.put("num2", "1");
        // write the whole message
        os.writeObject(req3.toString());
        // make sure it wrote and doesn't get cached in a buffer
        os.flush();

        i = in.readUTF();
        // assuming I get correct JSON back
        res = new JSONObject(i);

        System.out.println(res);

        // test response
        assertFalse(res.getBoolean("ok"));
        assertEquals("Field num1 does not exist in request", res.getString("message"));

        // Wrong request to server num1 num2 missing
        JSONObject req4 = new JSONObject();
        req4.put("type", "add");
        // write the whole message
        os.writeObject(req4.toString());
        // make sure it wrote and doesn't get cached in a buffer
        os.flush();

        i = in.readUTF();
        // assuming I get correct JSON back
        res = new JSONObject(i);

        System.out.println(res);

        // test response
        assertFalse(res.getBoolean("ok"));
        assertEquals("Field num1 does not exist in request", res.getString("message"));

        // Wrong request to server num2 missing
        JSONObject req5 = new JSONObject();
        req5.put("type", "add");
        req5.put("num1", "hello");
        req5.put("num2", "2");
        // write the whole message
        os.writeObject(req5.toString());
        // make sure it wrote and doesn't get cached in a buffer
        os.flush();

        i = in.readUTF();
        // assuming I get correct JSON back
        res = new JSONObject(i);

        System.out.println(res);

        // test response
        assertFalse(res.getBoolean("ok"));
        assertEquals("Field num1/num2 needs to be of type: int", res.getString("message"));
    }

    /**
     * Tests the functionality of the "echo" type request to the server.
     * Executes the following scenarios:
     * - Sends a valid echo request with data and verifies that the server responds correctly.
     * - Sends an invalid echo request without the "data" field and verifies the server error response.
     * The method evaluates the following:
     * - For a valid request, it ensures that:
     * - The returned response has an "ok" field set to true.
     * - The returned response has a "type" field with the value "echo".
     * - The returned response contains an "echo" field correctly echoing the provided data.
     * - For an invalid request (missing "data"), it ensures that:
     * - The returned response has an "ok" field set to false.
     * - The returned response contains an appropriate error message indicating the missing "data" field.
     *
     * @throws IOException If an I/O error occurs during communication with the server.
     */
    @Test
    public void echoRequest() throws IOException {
        // valid request with data
        JSONObject req1 = new JSONObject();
        req1.put("type", "echo");
        req1.put("data", "gimme this back!");
        // write the whole message
        os.writeObject(req1.toString());
        // make sure it wrote and doesn't get cached in a buffer
        os.flush();
        String i = in.readUTF();
        // assuming I get correct JSON back
        JSONObject res = new JSONObject(i);
        // test response
        assertTrue(res.getBoolean("ok"));
        assertEquals("echo", res.getString("type"));
        assertEquals("Here is your echo: gimme this back!", res.getString("echo"));

        // Invalid request - no data sent
        JSONObject req2 = new JSONObject();
        req2.put("type", "echo");
        // write the whole message
        os.writeObject(req2.toString());
        // make sure it wrote and doesn't get cached in a buffer
        os.flush();
        i = in.readUTF();
        // assuming I get correct JSON back
        res = new JSONObject(i);
        System.out.println(res);
        // test response
        assertFalse(res.getBoolean("ok"));
        assertEquals("Field data does not exist in request", res.getString("message"));
    }

    /**
     * Tests the server's "addmany" request functionality, ensuring both valid and invalid cases
     * are handled correctly.
     * The method simulates two client requests to the server:
     * 1. A valid "addmany" request with a list of integers, expecting a successful response
     * containing the sum of the integers.
     * 2. An invalid "addmany" request with a list containing non-integer values, expecting
     * an error response indicating the issue.
     * The server's responses are validated for correctness using assertions, checking
     * for expected values such as response type, success status, result of operations,
     * and error messages.
     *
     * @throws IOException if there is an I/O issue during communication with the server
     */
    @Test
    public void addManyRequest() throws IOException {
        // create a correct req for server
        JSONObject req = new JSONObject();
        req.put("type", "addmany");
        List<String> myList = Arrays.asList(
                "12",
                "15",
                "111",
                "42"
        );
        req.put("nums", myList);
        // write the whole message
        os.writeObject(req.toString());
        // make sure it wrote and doesn't get cached in a buffer
        os.flush();
        String i = in.readUTF();
        // assuming I get correct JSON back
        JSONObject res = new JSONObject(i);
        // test response
        assertTrue(res.getBoolean("ok"));
        assertEquals("addmany", res.getString("type"));
        assertEquals(180, res.getInt("result"));

        // Invalid request to server
        JSONObject req2 = new JSONObject();
        req2.put("type", "addmany");
        myList = Arrays.asList(
                "two",
                "15",
                "111",
                "42"
        );
        req2.put("nums", myList);
        // write the whole message
        os.writeObject(req2.toString());
        // make sure it wrote and doesn't get cached in a buffer
        os.flush();
        i = in.readUTF();
        // assuming I get correct JSON back
        res = new JSONObject(i);
        System.out.println(res);
        // test response
        assertFalse(res.getBoolean("ok"));
        assertEquals("Values in array need to be ints", res.getString("message"));
    }

    /**
     * Tests the behavior of the server when a non-JSON request is sent.
     * This method writes a non-JSON string to the server, expects a JSON response,
     * and verifies that the server correctly identifies the request as invalid.
     * The response is checked for the following:
     * - `ok` flag is set to false.
     * - The `message` field contains the text "req not JSON".
     * After verifying the response, the method calls the `addRequest` test
     * to ensure the server continues functioning as expected and correctly handles
     * subsequent requests after the invalid request.
     *
     * @throws IOException if an I/O error occurs during interaction with the server.
     */
    @Test
    public void notJSON() throws IOException {
        // create a correct req for server
        os.writeObject("a");

        String i = in.readUTF();
        // assuming I get correct JSON back
        JSONObject res = new JSONObject(i);

        // test response
        assertFalse(res.getBoolean("ok"));
        assertEquals("req not JSON", res.getString("message"));

        // calling the other test to make sure server continues to work and the "continue" does what it is supposed to do
        addRequest();
    }

    /**
     * Tests the quiz request functionality of the server.
     * This method creates a JSON request object for the "quizgame" type, which contains
     * a quiz question, multiple choice options, and the correct answer index.
     * The request is sent to the server over an output stream. The server's response
     * is read and validated to ensure it matches the expected format and content.
     * The test checks the following:
     * - The server responds with an "ok" status set to true.
     * - The type of the response matches the expected "quizgame" type.
     * - The result field in the response contains the expected answer index.
     * This test ensures that the server can handle quiz requests correctly
     * and provides the appropriate response based on the request.
     *
     * @throws IOException if there is an error in communication with the server.
     */
    @Test
    public void quizRequest() throws IOException {
        // create a correct req for server
        JSONObject req = new JSONObject();
        req.put("type", "quizgame");
        req.put("question", "What is the capital of France?");
        List<String> myList = Arrays.asList(
                "Paris",
                "London",
                "Berlin",
                "Madrid"
        );
        req.put("options", myList);
        req.put("answer", 0);
        // write the whole message
        os.writeObject(req.toString());
        // make sure it wrote and doesn't get cached in a buffer
        os.flush();
        String i = in.readUTF();
        // assuming I get correct JSON back
        JSONObject res = new JSONObject(i);
        // test response
        assertTrue(res.getBoolean("ok"));
        assertEquals("quizgame", res.getString("type"));
        assertEquals(0, res.getInt("result"));

    }

    /**
     * Tests the server's handling of an incorrectly formatted quiz request where the correct answer
     * specified is not within the range of provided options.
     * This method sends a JSON request to the server with the following issues:
     * - The "type" field is correctly set to "quizgame".
     * - The question field and options list are properly included.
     * - The answer field is set to a value (5) that is out of range for the provided options (indices 0-3).
     * The server is expected to return a response indicating failure with:
     * - `"ok": false` to signify an error.
     * - An error message stating that the answer is not within the valid option range.
     * Assertions are performed to verify the server's response against the expected failure message.
     *
     * @throws IOException If an I/O error occurs during communication with the server.
     */
    @Test
    public void quizRequestWrong() throws IOException {
        // create a correct req for server
        JSONObject req = new JSONObject();
        req.put("type", "quizgame");
        req.put("question", "What is the capital of France?");
        List<String> myList = Arrays.asList(
                "Paris",
                "London",
                "Berlin",
                "Madrid"
        );
        req.put("options", myList);
        req.put("answer", 5);
        // write the whole message
        os.writeObject(req.toString());
        // make sure it wrote and doesn't get cached in a buffer
        os.flush();
        String i = in.readUTF();
        // assuming I get correct JSON back
        JSONObject res = new JSONObject(i);
        // test response
        assertFalse(res.getBoolean("ok"));
        assertEquals("Answer is not in range of options", res.getString("message"));

    }

    /**
     * Tests the functionality of the server to handle a string concatenation request.
     * The method verifies the following:
     * - Sends a properly formatted JSON request to the server with a concatenation operation type.
     * - Asserts that the server responds with a valid JSON object.
     * - Checks the correctness of individual fields in the response ("ok", "type", and "result").
     * - Validates the result of the string concatenation performed by the server.
     *
     * @throws IOException if an I/O error occurs during communication with the server.
     */
    @Test
    public void concatenationRequest() throws IOException {
        // create a correct req for server
        JSONObject req = new JSONObject();
        req.put("type", "stringconcatenation");
        List<String> myList = Arrays.asList(
                "hello",
                "world",
                "!"
        );
        req.put("strings", myList);
        // write the whole message
        os.writeObject(req.toString());
        // make sure it wrote and doesn't get cached in a buffer
        os.flush();
        String i = in.readUTF();
        // assuming I get correct JSON back
        JSONObject res = new JSONObject(i);
        // test response
        assertTrue(res.getBoolean("ok"));
        assertEquals("stringconcatenation", res.getString("type"));
        assertEquals("helloworld!", res.getString("result"));

    }

    /**
     * Tests the successful concatenation of two strings using the stringConcatenation method.
     * This test ensures that:
     * - The response contains a success status indicated by the "ok" field set to true.
     * - The "type" field in the response matches the input type, which is "stringconcatenation".
     * - The "result" field in the response contains the correct concatenated value of the input strings.
     * Preconditions:
     * - A valid JSONObject input must be provided, containing the keys "type", "string1", and "string2".
     * Test steps:
     * 1. Construct a JSON input object with valid "type", "string1", and "string2" fields.
     * 2. Call the stringConcatenation method with the constructed input.
     * 3. Verify that the method returns the expected output through assertions on the "ok", "type", and "result" fields.
     */
    @Test
    public void testStringConcatenationSuccess() {
        // Setup
        JSONObject input = new JSONObject();
        input.put("type", "stringconcatenation");
        input.put("string1", "Hello");
        input.put("string2", "World");

        // Execute
        JSONObject result = SockServer.stringConcatenation(input);

        // Assert
        Assert.assertTrue(result.getBoolean("ok"));
        Assert.assertEquals("stringconcatenation", result.getString("type"));
        Assert.assertEquals("HelloWorld", result.getString("result"));
    }

    /**
     * Tests the behavior of the `SockServer.stringConcatenation` method when the "string1" field
     * is missing from the input JSON object.
     * This test verifies that the method properly identifies the absence of the "string1" field
     * and returns an appropriate error response.
     * The method executes the following steps:
     * 1. Prepares an input JSON object that specifies a string concatenation request but omits
     * the "string1" field.
     * 2. Sends the input object to the `SockServer.stringConcatenation` method for processing.
     * 3. Asserts that the result JSON object returned by the method:
     * - Contains an "ok" field set to false.
     * - Includes a "message" field with the value "Field string1 does not exist in request".
     */
    @Test
    public void testStringConcatenationMissingString1() {
        // Setup
        JSONObject input = new JSONObject();
        input.put("type", "stringconcatenation");
        input.put("string2", "World");

        // Execute
        JSONObject result = SockServer.stringConcatenation(input);

        // Assert
        Assert.assertFalse(result.getBoolean("ok"));
        Assert.assertEquals("Field string1 does not exist in request", result.getString("message"));
    }

    /**
     * Tests the behavior of the `stringConcatenation` method in the scenario
     * where the input JSON object is missing the "string2" field.
     * This test verifies that the method correctly identifies the absence of the
     * "string2" field in the input and responds with an appropriate error message
     * and status.
     * Test process:
     * 1. Sets up an input JSON object where "string1" is provided, but "string2" is missing.
     * 2. Invokes the `SockServer.stringConcatenation` method with the input.
     * 3. Asserts that the result contains:
     * - An "ok" field set to `false` to indicate the failure.
     * - A "message" field set to "Field string2 does not exist in request".
     */
    @Test
    public void testStringConcatenationMissingString2() {
        // Setup
        JSONObject input = new JSONObject();
        input.put("type", "stringconcatenation");
        input.put("string1", "Hello");

        // Execute
        JSONObject result = SockServer.stringConcatenation(input);

        // Assert
        Assert.assertFalse(result.getBoolean("ok"));
        Assert.assertEquals("Field string2 does not exist in request", result.getString("message"));
    }

    /**
     * Tests the behavior of the stringConcatenation method when the "string1" field is not of type String.
     * This method sets up a JSON input object that simulates a request with an invalid type
     * for the "string1" field (set to an integer instead of a string). It then executes
     * the string concatenation operation and verifies the response to ensure the application
     * correctly identifies and handles the type mismatch error.
     * The expected behavior is that the operation fails, returning a response with:
     * - "ok" set to false.
     * - An appropriate error message indicating that "string1" must be of type String.
     */
    @Test
    public void testStringConcatenationInvalidString1Type() {
        // Setup
        JSONObject input = new JSONObject();
        input.put("type", "stringconcatenation");
        input.put("string1", 123);
        input.put("string2", "World");

        // Execute
        JSONObject result = SockServer.stringConcatenation(input);

        // Assert
        Assert.assertFalse(result.getBoolean("ok"));
        Assert.assertEquals("Field string1 needs to be of type: String", result.getString("message"));
    }

    /**
     * Tests the behavior of the stringConcatenation method when provided with an invalid data type
     * for the "string2" field. Specifically, this test verifies that the method handles the case
     * where the "string2" field is not of type String, ensuring correct validation and error
     * message generation.
     * The input JSON contains:
     * - type: "stringconcatenation"
     * - string1: A valid string ("Hello")
     * - string2: An invalid type (Integer: 456)
     * This test checks the following expectations:
     * - The "ok" field in the result is set to false, indicating the operation was unsuccessful.
     * - The "message" field in the result contains the error message stating that "string2" must be of type String.
     */
    @Test
    public void testStringConcatenationInvalidString2Type() {
        // Setup
        JSONObject input = new JSONObject();
        input.put("type", "stringconcatenation");
        input.put("string1", "Hello");
        input.put("string2", 456);

        // Execute
        JSONObject result = SockServer.stringConcatenation(input);

        // Assert
        Assert.assertFalse(result.getBoolean("ok"));
        Assert.assertEquals("Field string2 needs to be of type: String", result.getString("message"));
    }
}