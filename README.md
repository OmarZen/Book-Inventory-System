# Distributed Systems: Online Bookstore Project

### Assignment 1

## Objective:
This project is an implementation of an online bookstore application using socket programming and multithreading in Java SE. The bookstore operates with a client-server architecture, allowing users to browse, search, borrow, and lend books. The server manages book inventory and user requests, while clients interact with the bookstore and other users.

## Features:
1. **Server-Client Communication**: 
   - Uses Java SE sockets for communication between the server and multiple clients.
   
2. **Book Inventory Management**:
   - Server maintains a database of books with details like title, author, genre, price, and quantity.
   
3. **User Authentication**:
   - Users can register and log in securely.
   - Supports login, registration, and error handling for incorrect credentials.
   
4. **Browse and Search Books**:
   - Users can browse and search for books by title, author, genre, etc.

5. **Add and Remove Books**:
   - Users can add books they wish to lend or remove books from their collection.

6. **Borrowing Requests**:
   - Borrowers can submit borrowing requests to lenders.
   - Lenders can accept or reject requests, after which they can communicate with the borrower.

7. **Request History**:
   - Users can view their borrowing request history and track request statuses (accepted, rejected, pending).

8. **Library Statistics**:
   - Admin users can view statistics on borrowed books, available books, and requests.

9. **Error Handling**:
   - Handles scenarios such as invalid user inputs, disconnections, and other runtime exceptions.

## Additional Features (For Teams of 3):
1. **Book Review and Rating**:
   - Users can review books, and the server calculates overall book ratings.
   
2. **Personalized Recommendations**:
   - Recommendations based on reviews or user preferences.

## Prerequisites:
- Java SE (JDK 8 or higher)
- MySQL 8.0.36
- MySQL JDBC Connector (Jar file included)

## Documentation:
### Team:
- **Omar Waleed Zenhom** - ID: 20206130
- **Mohamed Alaa El-Din** - ID: 20206068

### Decisions and Assumptions:
1. **User Authentication**:
   - Registered users' details are stored in the `users` table of the MySQL database.
   
2. **Book Management**:
   - Books are associated with the user who added them in the `books` table.
   
3. **Request Submission**:
   - Requests are stored in the `requests` table with statuses ("Pending", "Accepted", or "Rejected").
   
4. **Admin Access**:
   - Only the admin can view overall library statistics.

### Setting Up the Database:
1. Install MySQL and import the provided SQL files to set up the following tables:
   - `users`: User authentication details.
   - `books`: Book inventory data.
   - `requests`: Borrowing requests and statuses.
   
2. SQL schema is included in the project files.

### Running the Project:

#### 1. **Setting Up the Java Environment**:
   - Ensure Java is installed.
   
#### 2. **Running the Server**:
   - Run the `Server.java` class.
   - The server listens for client connections on port `12345` and handles requests using threads.

#### 3. **Running the Client**:
   - Run the `Client.java` class.
   - A list of available commands is displayed to interact with the server.
   
   **Client Commands**:
   ```
   1. LOGIN <username> <password>
   2. REGISTER <username> <password>
   3. BROWSE
   4. SEARCH
   5. ADD <username> <title> <author> <genre> <price> <quantity>
   6. REMOVE <username> <bookName>
   7. GET_MY_BOOKS <username>
   8. REQUEST <borrowerUsername> <lenderUsername> <bookId> <message>
   9. ACCEPT <lenderName> <requestId>
   10. REJECT <lenderName> <requestId>
   11. REQUEST_HISTORY <username>
   12. LIBRARY_STATS <adminUsername>
   13. LOGOUT <username>
   14. Get_OnlineUser
   ```

### Testing:
- The project has been tested with multiple clients connected simultaneously, demonstrating all the required functionalities.

## How to Run:
1. **Setup MySQL Database**: 
   - Import the provided SQL files to create the necessary tables.
   
2. **Run Server**: 
   - Execute `Server.java` on the command line.

3. **Run Clients**: 
   - Execute multiple instances of `Client.java` to simulate different users interacting with the server.

4. **Commands**: 
   - Follow the client instructions to browse books, add books, submit requests, and perform other actions.

## Notes:
- Ensure the MySQL database is set up correctly, and the jar files are included in the classpath.
- Handle errors such as invalid inputs and client disconnections.

## License:
Distributed under the MIT License. See `LICENSE` for more information.
