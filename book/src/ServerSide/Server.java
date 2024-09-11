package ServerSide;

import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.*;

import Database.Connection;

import static java.lang.System.out;

public class Server {
    private static final int PORT = 12345;
    private static Connection dbConnection;
    private static Map<String, User> loggedInUsers = new HashMap<>();

    public static void main(String[] args) {
        dbConnection = new Connection("jdbc:mysql://localhost:3306/mysql", "root", "12345678");
        dbConnection.connect();
        //jdbc:mysql://localhost:3306/mysql

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            out.println("Server started. Waiting for clients...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                out.println("Client connected: " + clientSocket);

                ClientHandler clientHandler = new ClientHandler(clientSocket);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                dbConnection.disconnect();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    // getOnlineUsers method Finished
    public static List<String> getOnlineUsers() {
        List<String> onlineUsers = new ArrayList<>();
        for (Map.Entry<String, User> entry : loggedInUsers.entrySet()) {
            onlineUsers.add(entry.getKey());
        }
        return onlineUsers;
    }

    private static class ClientHandler implements Runnable {
        private Socket clientSocket;
        private PrintWriter out;
        private BufferedReader in;
        private static Set<String> loggedInUsernames = new HashSet<>();

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
            try {
                out = new PrintWriter(clientSocket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
//                sendCommandList();// menue for the client commands
                String input;
                while ((input = in.readLine()) != null) {
                    String[] tokens = input.split(" ");
                    String command = tokens[0];
                    switch (command) {
                        case "LOGIN":
                            handleLogin(tokens);
                            break;
                        case "REGISTER":
                            handleRegistration(tokens);
                            break;
                        case "BROWSE":
                            handleBrowse();
                            break;
                        case "SEARCH":
                            handleSearch();
                            break;
                        case "ADD":
                            addBookFromUser(tokens);
                            break;
                        case "REMOVE":
                            removeBookFromUser(tokens);
                            break;
                        case "GET_MY_BOOKS":
                            GetMyBooks(tokens[1]);
                            break;
                        case "REQUEST":
                            submitRequest(tokens);
                            break;
                        case "ACCEPT":
                            acceptRequest(tokens);
                            break;
                        case "REJECT":
                            rejectRequest(tokens);
                            break;
                        case "REQUEST_HISTORY":
                            getRequestHistory(tokens[1]);
                            break;
                        case "LIBRARY_STATS":
                            sendLibraryStats(tokens[1]);
                            break;
                        case "LOGOUT":
                            logout(tokens[1]);
                            break;
                        case "Get_OnlineUsers":
                            out.println(getOnlineUsers());
                            break;
                        default:
                            out.println("Invalid command entered. Please try again:");
                            break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // handleLogin method Finished
        private void handleLogin(String[] tokens) {
            String username = tokens[1];
            String password = tokens[2];

            try {
                if (loggedInUsers.containsKey(username)) {
                    out.println("USER_ALREADY_LOGGED_IN");
                    return;
                }
                PreparedStatement statement = dbConnection.getConnection().prepareStatement("SELECT * FROM mysql.users WHERE username = ?");
                statement.setString(1, username);
                ResultSet resultSet = statement.executeQuery();

                if (resultSet.next()) {
                    // Username exists, check password
                    String dbPassword = resultSet.getString("password");
                    if (password.equals(dbPassword)) {
                        // Correct password
                        User user = new User(username, password);
                        if (username.equalsIgnoreCase("Admin")) {
                            user.setRole("Admin");
                        }
                        loggedInUsers.put(username, user);
                        out.println("LOGIN_SUCCESS");
                    } else {
                        // Wrong password
                        out.println("401 Wrong password");
                    }
                } else {
                    // Username not found
                    out.println("404 Username not found");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        // handleRegistration method Finishes
        private void handleRegistration(String[] tokens) {
            String username = tokens[1];
            String password = tokens[2];

            try {

                // Check if username already exists
                PreparedStatement checkStatement = dbConnection.getConnection().prepareStatement("SELECT * FROM mysql.users WHERE username = ?");
                checkStatement.setString(1, username);
                ResultSet resultSet = checkStatement.executeQuery();

                if (resultSet.next()) {
                    out.println("409 Username already exists");
                    return;
                }

                PreparedStatement insertStatement = dbConnection.getConnection().prepareStatement("INSERT INTO mysql.users (username, password) VALUES (?, ?)");
                insertStatement.setString(1, username);
                insertStatement.setString(2, password);
                int rowsInserted = insertStatement.executeUpdate();

                if (rowsInserted > 0) {
                    out.println("REGISTER_SUCCESS");
                } else {
                    out.println("500 Internal Server Error");
                }
            } catch (SQLException e) {
                e.printStackTrace();
                out.println("500 Internal Server Error");
            }
        }

        // handleBrowse method =======> Finished
        private void handleBrowse() {
            StringBuilder response = new StringBuilder();
            try {
                PreparedStatement statement = dbConnection.getConnection().prepareStatement("SELECT * FROM mysql.books");
                ResultSet resultSet = statement.executeQuery();

                List<String> books = new ArrayList<>();
                while (resultSet.next()) {
                    int bookId = resultSet.getInt("book_id");
                    String title = resultSet.getString("title");
                    String author = resultSet.getString("author");
                    String genre = resultSet.getString("genre");
                    double price = resultSet.getDouble("price");
                    int quantity = resultSet.getInt("quantity");
                    int userid = resultSet.getInt("user_id");
                    String user_name = getUsernameById(userid);
                    String bookInfo = String.format("BookID: %s,Title: %s, Author: %s, Genre: %s, Price: %.2f, Quantity: %d, User_id: %s, User_name: %s",
                            bookId, title, author, genre, price, quantity, userid, user_name);
                    books.add(bookInfo);
                }

                if (!books.isEmpty()) {
                    response.append("BROWSE_SUCCESS\n");
                    for (String book : books) {
                        response.append(book).append("\n");
                    }
                    out.println(response);

                } else {
                    out.println("No books available");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

        }

        // handleSearch method =======> Finished
        private void handleSearch() {
            try {
                // Prompt the user to choose the search criteria
                out.println("Search for a book by:");
                out.println("1 - Title");
                out.println("2 - Author");
                out.println("3 - Genre");
                out.println("Choose the number:");

                // Read the user's choice
                String choice = null;
                try {
                    choice = in.readLine();
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }

                // Perform the search based on the user's choice
                switch (choice) {
                    case "1":
                        out.println("Enter the title:");
                        String title = null;
                        try {
                            title = in.readLine();
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                        try {
                            PreparedStatement statementtitle = dbConnection.getConnection().prepareStatement("SELECT * FROM mysql.books WHERE title = ?");
                            statementtitle.setString(1, title);
                            ResultSet resultSet = statementtitle.executeQuery();
                            List<String> books = new ArrayList<>();
                            while (resultSet.next()) {
                                String author = resultSet.getString("author");
                                String genre = resultSet.getString("genre");
                                double price = resultSet.getDouble("price");
                                int quantity = resultSet.getInt("quantity");
                                String bookInfo = String.format("Author: %s, Genre: %s, Price: %.2f, Quantity: %d",
                                        author, genre, price, quantity);
                                books.add(bookInfo);
                            }
                            if (!books.isEmpty()) {
                                out.println("SEARCH_SUCCESS");
                                for (String bookInfo : books) {
                                    out.println(bookInfo);
                                }
                            } else {
                                out.println("No books found with the title: " + title);
                            }
                        } catch (SQLException ex) {
                            throw new RuntimeException(ex);
                        }
                        break;
                    case "2":
                        out.println("Enter the author:");
                        String author = null;
                        try {
                            author = in.readLine();
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                        PreparedStatement statementAuthor = dbConnection.getConnection().prepareStatement("SELECT * FROM mysql.books WHERE author = ?");
                        statementAuthor.setString(1, author);
                        ResultSet resultSet = statementAuthor.executeQuery();
                        List<String> books = new ArrayList<>();
                        while (resultSet.next()) {
                            String titlebook = resultSet.getString("title");
                            String genre = resultSet.getString("genre");
                            double price = resultSet.getDouble("price");
                            int quantity = resultSet.getInt("quantity");
                            String bookInfo = String.format("Title: %s, Genre: %s, Price: %.2f, Quantity: %d",
                                    titlebook, genre, price, quantity);
                            books.add(bookInfo);
                        }
                        if (!books.isEmpty()) {
                            out.println("SEARCH_SUCCESS");
                            for (String bookInfo : books) {
                                out.println(bookInfo);
                            }
                        } else {
                            out.println("No books found with the author: " + author);
                        }
                        break;
                    case "3":
                        out.println("Enter the genre:");
                        String genre = null;
                        try {
                            genre = in.readLine();
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                        PreparedStatement statementGenre = dbConnection.getConnection().prepareStatement("SELECT * FROM mysql.books WHERE genre = ?");
                        statementGenre.setString(1, genre);
                        ResultSet resultSetGenre = statementGenre.executeQuery();
                        List<String> booksGenre = new ArrayList<>();
                        while (resultSetGenre.next()) {
                            String titleGenre = resultSetGenre.getString("title");
                            String authorbook = resultSetGenre.getString("author");
                            double price = resultSetGenre.getDouble("price");
                            int quantity = resultSetGenre.getInt("quantity");
                            String bookInfo = String.format("Title: %s, Author: %s, Price: %.2f, Quantity: %d",
                                    titleGenre, authorbook, price, quantity);
                            booksGenre.add(bookInfo);
                        }
                        if (!booksGenre.isEmpty()) {
                            out.println("SEARCH_SUCCESS");
                            for (String bookInfo : booksGenre) {
                                out.println(bookInfo);
                            }
                        } else {
                            out.println("No books found with the genre: " + genre);
                        }
                        break;
                    default:
                        out.println("Invalid choice.");
                        break;
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        // addBook method =======> Finished
        private void addBookFromUser(String[] tokens) {
            String username = tokens[1];
            if (!loggedInUsers.containsKey(username)) {// tokens[1] is the username of the client
                out.println("403 Forbidden: You must be logged in to add a book");
                return;
            }

            String title = tokens[2];
            String author = tokens[3];
            String genre = tokens[4];
            double price = Double.parseDouble(tokens[5]);
            int quantity = Integer.parseInt(tokens[6]);

            try {
                PreparedStatement statementUserId = dbConnection.getConnection().prepareStatement("SELECT id FROM mysql.users WHERE username = ?");
                statementUserId.setString(1, username);
                ResultSet resultSetUserID = statementUserId.executeQuery();
                resultSetUserID.next();
                int userId = resultSetUserID.getInt("id");

                PreparedStatement statement = dbConnection.getConnection().prepareStatement("INSERT INTO mysql.books (title, author, genre , price , quantity , user_id) VALUES (?, ?, ?, ?, ?, ?)");
                statement.setString(1, title);
                statement.setString(2, author);
                statement.setString(3, genre);
                statement.setDouble(4, price);
                statement.setInt(5, quantity);
                statement.setInt(6, userId);

                int rowsInserted = statement.executeUpdate();

                if (rowsInserted > 0) {
                    out.println("BOOK_ADDED");
                } else {
                    out.println("BOOK_ADD_FAILED");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        // removeBook method =======> Finished
        private void removeBookFromUser(String[] tokens) {
            String username = tokens[1];
            if (!loggedInUsers.containsKey(username)) {
                out.println("403 Forbidden: You must be logged in to remove a book");
                return;
            }

            String title = tokens[2];
            try {
                PreparedStatement statementUserId = dbConnection.getConnection().prepareStatement("SELECT id FROM mysql.users WHERE username = ?");
                statementUserId.setString(1, username);
                ResultSet resultSetUserID = statementUserId.executeQuery();
                resultSetUserID.next();
                int userId = resultSetUserID.getInt("id");

                PreparedStatement statementDelete = dbConnection.getConnection().prepareStatement("DELETE FROM mysql.books WHERE title = ? AND user_id = ?");
                statementDelete.setString(1, title);
                statementDelete.setInt(2, userId);

                int rowsDeleted = statementDelete.executeUpdate();
                if (rowsDeleted > 0) {
                    out.println("BOOK_REMOVED");
                } else {
                    out.println("BOOK_REMOVE_FAILED");
                }

            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        // GetMyBooks method =======> Finished
        private void GetMyBooks(String username) {
            if (!loggedInUsers.containsKey(username)) {
                out.println("403 Forbidden: You must be logged in to get your books");
                return;
            }

            try {
                // Get the user ID of the logged-in user
                PreparedStatement getUserIdStatement = dbConnection.getConnection().prepareStatement("SELECT id FROM mysql.users WHERE username = ?");
                getUserIdStatement.setString(1, username);
                ResultSet userIdResultSet = getUserIdStatement.executeQuery();

                if (userIdResultSet.next()) {
                    int userId = userIdResultSet.getInt("id");

                    // Query the books table for books added by the user
                    PreparedStatement getBooksStatement = dbConnection.getConnection().prepareStatement("SELECT * FROM mysql.books WHERE user_id = ?");
                    getBooksStatement.setInt(1, userId);
                    ResultSet resultSet = getBooksStatement.executeQuery();

                    List<String> userBooks = new ArrayList<>();
                    while (resultSet.next()) {
                        String title = resultSet.getString("title");
                        String author = resultSet.getString("author");
                        String genre = resultSet.getString("genre");
                        double price = resultSet.getDouble("price");
                        int quantity = resultSet.getInt("quantity");

                        String bookInfo = String.format("title: %s, author: %s, genre: %s, price: %s, quantity: %s",
                                title, author, genre, price, quantity);
                        userBooks.add(bookInfo);
                    }

                    if (!userBooks.isEmpty()) {
                        out.println("MY_BOOKS");
                        for (String bookInfo : userBooks) {
                            out.println(bookInfo);
                        }
                    } else {
                        out.println("You have not added any books yet.");
                    }
                } else {
                    out.println("404 User not found");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        // submitRequest method ============> Finished
        private void submitRequest(String[] tokens) {
            String borrowerUsername = tokens[1];
            String lenderUsername = tokens[2];
            int bookId = Integer.parseInt(tokens[3]);
            String message = tokens[4];  // Optional message from borrower

            try {
                // Get borrower and lender IDs
                int borrowerId = getUserIdByUsername(borrowerUsername);
                int lenderId = getUserIdByUsername(lenderUsername);

                PreparedStatement insertStatement = dbConnection.getConnection().prepareStatement("INSERT INTO mysql.requests (borrower_id, lender_id, book_id, status ,message) VALUES (?, ?, ? , ?, ?)");
                insertStatement.setInt(1, borrowerId);
                insertStatement.setInt(2, lenderId);
                insertStatement.setInt(3, bookId);
                insertStatement.setString(4, "Pending");
                insertStatement.setString(5, message);

                int rowsInserted = insertStatement.executeUpdate();

                if (rowsInserted > 0) {
                    out.println("REQUEST_SUBMITTED");
                } else {
                    out.println("REQUEST_SUBMIT_FAILED");
                }
            } catch (SQLException e) {
                e.printStackTrace();
                out.println("REQUEST_SUBMIT_FAILED");
            }
        }

        // getRequestHistory method =========> Finished
        private void getRequestHistory(String username) {
            try {
                int userId = getUserIdByUsername(username);
                if (userId == -1) {
                    out.println("404 User not found");
                    return;
                }
                if (!loggedInUsers.containsKey(username)) {
                    out.println("403 Forbidden: You must be logged in to view your request history");
                    return;
                }
                PreparedStatement getHistoryStatement = dbConnection.getConnection().prepareStatement("SELECT * FROM mysql.requests WHERE borrower_id = ? OR lender_id = ?");
                getHistoryStatement.setInt(1, userId);
                getHistoryStatement.setInt(2, userId);

                ResultSet resultSet = getHistoryStatement.executeQuery();
                List<String> requestHistory = new ArrayList<>();

                while (resultSet.next()) {
                    int requestId = resultSet.getInt("id");
                    String borrower = getUsernameById(resultSet.getInt("borrower_id"));
                    String lender = getUsernameById(resultSet.getInt("lender_id"));
                    String bookTitle = getBookTitleById(resultSet.getInt("book_id"));
                    String status = resultSet.getString("status");
                    String message = resultSet.getString("message");

                    String requestInfo = String.format("Request ID: %d, Borrower: %s, Lender: %s, Book Title: %s, Status: %s , Message: %s",
                            requestId, borrower, lender, bookTitle, status, message);
                    requestHistory.add(requestInfo);
                }

                if (!requestHistory.isEmpty()) {
                    out.println("REQUEST_HISTORY");
                    for (String info : requestHistory) {
                        out.println(info);
                    }
                } else {
                    out.println("No request history found.");
                }
            } catch (SQLException e) {
                e.printStackTrace();
                out.println("REQUEST_HISTORY_FAILED");
            }
        }

        // acceptRequest method =======> Finished (Review)
        private void acceptRequest(String[] tokens) {
            String lenderUsername = tokens[1];
            String requestId = tokens[2];
            try {
                if (!loggedInUsers.containsKey(lenderUsername)) {
                    out.println("403 Forbidden: You must be logged in as the lender to accept the request.");
                    return;
                }

                int lenderId = getUserIdByUsername(lenderUsername);
                PreparedStatement statmentGetLendername = dbConnection.getConnection().prepareStatement("SELECT lender_id FROM mysql.requests WHERE id = ?");
                statmentGetLendername.setInt(1, Integer.parseInt(requestId));
                ResultSet resultSet = statmentGetLendername.executeQuery();
                resultSet.next();
                int lenderIdFromDB = resultSet.getInt("lender_id");
                if (lenderId != lenderIdFromDB) {
                    out.println("403 Forbidden: You must be the lender of the request to accept it.");
                    return;
                }

                PreparedStatement updateStatement = dbConnection.getConnection().prepareStatement("UPDATE mysql.requests SET status = ? WHERE id = ?");
                updateStatement.setString(1, "Accepted");
                updateStatement.setInt(2, Integer.parseInt(requestId));

                int rowsUpdated = updateStatement.executeUpdate();

                if (rowsUpdated > 0) {
                    out.println("REQUEST_ACCEPTED");
                } else {
                    out.println("REQUEST_ACCEPT_FAILED");
                }

                PreparedStatement updateBookOwnerID = dbConnection.getConnection().prepareStatement("UPDATE mysql.books SET user_id = ? WHERE book_id = (SELECT book_id FROM mysql.requests WHERE id = ?)");
                updateBookOwnerID.setInt(1, lenderId);
                updateBookOwnerID.setInt(2, Integer.parseInt(requestId));
                int rowsUpdatedBookOwnerID = updateBookOwnerID.executeUpdate();

                if (rowsUpdatedBookOwnerID > 0) {
                    out.println("BOOK_OWNER_UPDATED");
                } else {
                    out.println("BOOK_OWNER_UPDATE_FAILED");
                }

            } catch (SQLException e) {
                e.printStackTrace();
                out.println("REQUEST_ACCEPT_FAILED");
            }
        }

        // rejectRequest method =======> Finished (Review)
        private void rejectRequest(String[] tokens) {
            String lenderUsername = tokens[1];
            String requestId = tokens[2];
            try {
                if (!loggedInUsers.containsKey(lenderUsername)) {
                    out.println("403 Forbidden: You must be logged in as the lender to reject the request.");
                    return;
                }

                int lenderId = getUserIdByUsername(lenderUsername);
                PreparedStatement statmentGetLendername = dbConnection.getConnection().prepareStatement("SELECT lender_id FROM mysql.requests WHERE id = ?");
                statmentGetLendername.setInt(1, Integer.parseInt(requestId));
                ResultSet resultSet = statmentGetLendername.executeQuery();
                resultSet.next();
                int lenderIdFromDB = resultSet.getInt("lender_id");
                if (lenderId != lenderIdFromDB) {
                    out.println("403 Forbidden: You must be the lender of the request to reject it.");
                    return;
                }

                PreparedStatement updateStatement = dbConnection.getConnection().prepareStatement("UPDATE mysql.requests SET status = ? WHERE id = ?");
                updateStatement.setString(1, "Rejected");
                updateStatement.setInt(2, Integer.parseInt(requestId));

                int rowsUpdated = updateStatement.executeUpdate();

                if (rowsUpdated > 0) {
                    out.println("REQUEST_REJECTED");
                } else {
                    out.println("REQUEST_REJECT_FAILED");
                }
            } catch (SQLException e) {
                e.printStackTrace();
                out.println("REQUEST_REJECT_FAILED");
            }
        }

        // sendLibraryStats method =======> Finished
        private void sendLibraryStats(String username) {
            try {
                // check if this user is and Admin or not
                if (!loggedInUsers.containsKey(username)) {
                    out.println("403 Forbidden: You must be logged in to view library stats. (AS Administrator)");
                    return;
                }
                User user = loggedInUsers.get(username);
                if (!user.getRole().equalsIgnoreCase("Admin")) {
                    out.println("403 Forbidden: You must be logged in as an Admin to view library stats.");
                    return;
                }
                PreparedStatement statement = dbConnection.getConnection().prepareStatement("SELECT COUNT(*) AS totalBooks, SUM(quantity) AS totalQuantity FROM mysql.books");
                ResultSet resultSet = statement.executeQuery();
                resultSet.next();
                int totalBooks = resultSet.getInt("totalBooks");
                int totalQuantity = resultSet.getInt("totalQuantity");

                // current borrowed books and accepted/rejected/pending requests so far.
                PreparedStatement statementAccRejPen = dbConnection.getConnection().prepareStatement("SELECT COUNT(*) AS totalAccepted, COUNT(*) AS totalRejected, COUNT(*) AS totalPending FROM mysql.requests WHERE status = ? OR status = ? OR status = ?");
                statementAccRejPen.setString(1, "Accepted");
                statementAccRejPen.setString(2, "Rejected");
                statementAccRejPen.setString(3, "Pending");
                ResultSet resultSetAccRejPen = statementAccRejPen.executeQuery();
                resultSetAccRejPen.next();
                int totalAccepted = resultSetAccRejPen.getInt("totalAccepted");
                int totalRejected = resultSetAccRejPen.getInt("totalRejected");
                int totalPending = resultSetAccRejPen.getInt("totalPending");

                out.println("LIBRARY_STATS");
                out.println("Total Books: " + totalBooks);
                out.println("Total Quantity: " + totalQuantity);
                out.println("Total Accepted Requests: " + totalAccepted);
                out.println("Total Rejected Requests: " + totalRejected);
                out.println("Total Pending Requests: " + totalPending);
                out.println("LIBRARY_STATS_SUCCESS");


            } catch (SQLException e) {
                e.printStackTrace();
                out.println("LIBRARY_STATS_FAILED");
            }
        }

        // getBookTitleById method =======> Finished
        private int getUserIdByUsername(String username) throws SQLException {
            PreparedStatement getUserIdStatement = dbConnection.getConnection().prepareStatement("SELECT id FROM mysql.users WHERE username = ?");
            getUserIdStatement.setString(1, username);
            ResultSet resultSet = getUserIdStatement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getInt("id");
            }
            return -1;  // User not found
        }

        // getUsernameById method =======> Finished
        private String getUsernameById(int userId) throws SQLException {
            PreparedStatement getUsernameStatement = dbConnection.getConnection().prepareStatement("SELECT username FROM mysql.users WHERE id = ?");
            getUsernameStatement.setInt(1, userId);
            ResultSet resultSet = getUsernameStatement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getString("username");
            }
            return null;  // Username not found
        }

        // getBookTitleById method =======> Finished
        private String getBookTitleById(int bookId) throws SQLException {
            PreparedStatement getBookTitleStatement = dbConnection.getConnection().prepareStatement("SELECT title FROM mysql.books WHERE book_id = ?");
            getBookTitleStatement.setInt(1, bookId);
            ResultSet resultSet = getBookTitleStatement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getString("title");
            }
            return null;  // Book title not found
        }

        // logout method Finished
        private void logout(String username) {
            loggedInUsers.remove(username);
            out.println("LOGOUT_SUCCESS");
        }
    }
}