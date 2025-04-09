##### Author: Instructor team SE, ASU Polytechnic, CIDSE, SE


##### Purpose
This program shows a very simple client server implementation. The server
has 3 services, echo, add, addmany. Basic error handling on the server side
is implemented. Client does not have error handling and only has hard coded
calls to the server.

* Please run `gradle Server` and `gradle Client` together.
* Program runs on localhost
* Port is hard coded

## Protocol: ##

### Echo: ###

Request: 

    {
        "type" : "echo", -- type of request
        "data" : <String>  -- String to be echoed 
    }

General response:

    {
        "type" : "echo", -- echoes the initial response
        "ok" : <bool>, -- true or false depending on request
        "echo" : <String>,  -- echoed String if ok true
        "message" : <String>,  -- error message if ok false
    }

Success response:

    {
        "type" : "echo",
        "ok" : true,
        "echo" : <String> -- the echoed string
    }

Error response:

    {
        "type" : "echo",
        "ok" : false,
        "message" : <String> -- what went wrong
    }

### Add: ### 
Request:

    {
        "type" : "add",
        "num1" : <String>, -- first number -- String needs to be an int number e.g. "3"
        "num2" : <String> -- second number -- String needs to be an int number e.g. "4" 
    }

General response

    {
        "type" : "add", -- echoes the initial request
        "ok" : <bool>, -- true or false depending on request
        "result" : <int>,  -- result if ok true
        "message" : <String>,  -- error message if ok false
    }

Success response:

    {
        "type" : "add",
        "ok" : true,
        "result" : <int> -- the result of add
    }

Error response:

    {
        "type" : "add",
        "ok" : false,
        "message" : <String> - error message about what went wrong
    }


### StringConcatenation: ###
This service will concatenate two strings provided by the client. The client will send a request to the server with two strings to be concatenated. The server will concatenate the strings and send back the result to the client.

Request:

    {
        "type" : "stringconcatenation",
        "string1" : <String>, -- first string
        "string2" : <String> -- second string
    }

General response:

    {
        "type" : "stringconcatenation",
        "ok" : <bool>, -- true or false depending on request
        "result" : <String>,  -- concatenated string if ok true
        "message" : <String>  -- error message if ok false
    }

Success response:

    {
        "type" : "stringconcatenation",
        "ok" : true,
        "result" : <String> -- concatenated string
    }

Error response:

    {
        "type" : "stringconcatenation",
        "ok" : false,
        "message" : <String> -- error message about what went wrong
    }


### QuizGame: ###
This service will allow the client to play a quiz game. The server will store a set of questions and their corresponding answers. The client can choose to either add new questions or play the game. If the client chooses to add new questions, they can send a request to the server with the new question and answer. If the client chooses to play the game, the server will randomly select a question from the existing set and send it to the client. The client will respond with the answer. The server will check if the answer is correct and send the result back to the client. The game will continue until a certain number of questions have been answered or a certain time limit has been reached. The questions do not have to persist if the server shuts off, it is nice if they do but they do not have to

Request to add a new question:

    {
        "type" : "quizgame",
        "addQuestion" : true,  -- true if adding questions, false if playing game
        "question" : <String>, -- new question only if addQuestion = true
        "answer" : <String>    -- answer to the new question only if addQuestion = true
    }

Success response:

    {
        "type" : "quizgame",
        "ok" : true
    }

Error response:

    {
        "type" : "quizgame",
        "ok" : false,
        "message" : <String> -- error message about what went wrong
    }


Request to play the game:

    {
        "type" : "quizgame",
        "addQuestion" : false
    }

Success response:

    {
        "type" : "quizgame",
        "ok" : true,
        "question" : <String>,  -- question to be answered
    }

Error response:

    {
        "type" : "quizgame",
        "ok" : false,
        "message" : <String> -- error message about what went wrong
    }


Request to answer a question:

    {
        "type" : "quizgame",
        "answer" : <String> -- client's answer to the question
    }

Success response:

    {
        "type" : "quizgame",
        "ok" : true,
        "question" : <String>, -- question only if result is false
        "result" : <bool> -- result of the answer (true if correct, false if incorrect)
    }

Error response:

    {
        "type" : "quizgame",
        "ok" : false,
        "message" : <String> -- error message about what went wrong
    }


### General error responses: ###
These are used for all requests.

Error response: When a required field "key" is not in request

    {
        "ok" : false
        "message" : "Field <key> does not exist in request" 
    }

Error response: When a required field "key" is not of correct "type"

    {
        "ok" : false
        "message" : "Field <key> needs to be of type: <type>"
    }

Error response: When the "type" is not supported, so an unsupported request

    {
        "ok" : false
        "message" : "Type <type> is not supported."
    }


Error response: When the "type" is not supported, so an unsupported request

    {
        "ok" : false
        "message" : "req not JSON"
    }