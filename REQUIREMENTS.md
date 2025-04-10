
## Activity 1: Simple Server/Client (30 points)

- [ ] **Directory Structure and Starter Code**
    - [ ] Copy both starter codes into the correct directories (e.g., `Assignment3/Assign3-1` and `Assignment3/Assign3-2`).
    - [ ] Use the provided starter code as a guideline.

- [ ] **Protocol Implementation**
    - [X] Follow the exact JSON protocol as described in the README.
    - [X] Do not change the protocol; ensure any client can communicate with any server.

- [ ] **Implemented Services**
    - [X] **Echo Service** (Already implemented)
        - [X] Server returns the echoed string if valid.
        - [X] Error handling if `"data"` field is missing or wrong type.
    - [X] **Add Service** (Already implemented)
        - [X] Server returns the sum of two numbers if valid.
        - [X] Validate that both numbers exist and are integers.
    - [X] **Add Many Service** (Already implemented)
        - [X] Server sums the numbers in an array.
        - [X] Error handling if any values are not valid integers.
    - [X] **String Concatenation Service**
        - [X] Validate that `"string1"` and `"string2"` exist and are strings.
        - [X] Concatenate the two strings and return the result.
        - [X] Return clear error messages for missing or incorrect field types.
    - [X] **Quiz Game Service**
        - [X] **Adding Questions:** Allow the client to add new questions by providing `"question"` and `"answer"`.
        - [X] **Requesting a Question:** When the client requests to play, return a random question.
        - [X] **Answering a Question:** Check the client’s answer against the current question.
        - [X] Store the current quiz question per client session (e.g., using a mutable holder).

- [X] **Error Handling and Unit Testing**
    - [X] Implement robust error handling on the server side (e.g., missing fields, incorrect types).
    - [ ] Provide comprehensive unit tests covering:
        - Valid and invalid requests.
        - Each service (echo, add, addmany, string concatenation, and quizgame).
    - [X] Ensure the server does not crash on unexpected input.

- [X] **Deployment and Peer Interaction**
    - [X] Host your server on AWS using an open port.
    - [X] Post your server’s public IP and the exact Gradle command in the designated Slack channel.
    - [X] Comment on at least two peers’ servers.

