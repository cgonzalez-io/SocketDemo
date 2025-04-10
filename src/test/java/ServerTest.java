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

    @org.junit.After
    public void close() throws Exception {
        if (out != null) out.close();
        if (sock != null) sock.close();
    }

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