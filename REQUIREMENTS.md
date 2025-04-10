
## Activity 1: Simple Server/Client (30 points)

- [ ] **Directory Structure and Starter Code**
    - [ ] Copy both starter codes into the correct directories (e.g., `Assignment3/Assign3-1` and `Assignment3/Assign3-2`).
    - [ ] Use the provided starter code as a guideline.

- [ ] **Protocol Implementation**
    - [ ] Follow the exact JSON protocol as described in the README.
    - [ ] Do not change the protocol; ensure any client can communicate with any server.

- [ ] **Implemented Services**
    - [ ] **Echo Service** (Already implemented)
        - [ ] Server returns the echoed string if valid.
        - [ ] Error handling if `"data"` field is missing or wrong type.
    - [ ] **Add Service** (Already implemented)
        - [ ] Server returns the sum of two numbers if valid.
        - [ ] Validate that both numbers exist and are integers.
    - [ ] **Add Many Service** (Already implemented)
        - [ ] Server sums the numbers in an array.
        - [ ] Error handling if any values are not valid integers.
    - [ ] **String Concatenation Service**
        - [ ] Validate that `"string1"` and `"string2"` exist and are strings.
        - [ ] Concatenate the two strings and return the result.
        - [ ] Return clear error messages for missing or incorrect field types.
    - [ ] **Quiz Game Service**
        - [ ] **Adding Questions:** Allow the client to add new questions by providing `"question"` and `"answer"`.
        - [ ] **Requesting a Question:** When the client requests to play, return a random question.
        - [ ] **Answering a Question:** Check the client’s answer against the current question.
        - [ ] Store the current quiz question per client session (e.g., using a mutable holder).

- [ ] **Error Handling and Unit Testing**
    - [ ] Implement robust error handling on the server side (e.g., missing fields, incorrect types).
    - [ ] Provide comprehensive unit tests covering:
        - Valid and invalid requests.
        - Each service (echo, add, addmany, string concatenation, and quizgame).
    - [ ] Ensure the server does not crash on unexpected input.

- [ ] **Deployment and Peer Interaction**
    - [ ] Host your server on AWS using an open port.
    - [ ] Post your server’s public IP and the exact Gradle command in the designated Slack channel.
    - [ ] Comment on at least two peers’ servers.

