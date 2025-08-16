package banking;

import java.sql.*;
import java.util.Random;
import java.util.Scanner;

public class Main {

    static boolean loginMenuState = false;
    static int newCardCount = 0;
    static String enteredCardNo = "";

    public static void main(String[] args) throws SQLException {
        String dbFileName = "default.s3db";

        for (int i = 0; i < args.length - 1; i++) {
            if ("-filename".equals(args[i])) {
                dbFileName = args[i + 1];
                break;
            }
        }

        Scanner sc = new Scanner(System.in);
        creditCardDatabase.createDatabase(dbFileName);
        creditCardDatabase.createTable();

        while (true) {
            if (!loginMenuState) {
                optionsMenu();
                int menuSelected = sc.nextInt();
                sc.nextLine();
                switch (menuSelected) {
                    case 1 -> {
                        long cardNo = generateCardNo();
                        int pin = generatePin();
                        newCardCount += 1;

                        System.out.println("Your card has been created");
                        System.out.println("Your card number:");
                        System.out.println(cardNo);
                        System.out.println("Your card PIN");
                        System.out.println(pin);

                        creditCardDatabase.createNewCard(newCardCount,String.valueOf(cardNo), String.valueOf(pin) , 0);
                    }
                    case 2 -> {
                        System.out.println("Enter your card number:");
                        enteredCardNo = sc.nextLine();
                        System.out.println("Enter your PIN");
                        String enteredPIN = sc.nextLine();

                        boolean cardFound = false;

                        if (creditCardDatabase.verifyCardInfo(enteredCardNo,enteredPIN)) {
                            cardFound = true;
                            loginMenuState = true;
                            break;
                        }

                        if (!cardFound) {
                            System.out.println("Wrong card number or PIN!");
                        }
                    }
                    case 0 -> {
                        System.out.println("Bye!");
                        System.exit(0);
                    }
                    default -> throw new IllegalStateException("Invalid Choice " + menuSelected);
                }
            } else {
                loginMenu();
                int selectedOption = sc.nextInt();
                sc.nextLine();

                switch (selectedOption) {
                    case 1 -> System.out.println("Balance: " + creditCardDatabase.checkBalance(enteredCardNo));

                    case 2 -> {
                        System.out.println("Enter income:");
                        int balancetoAdd = sc.nextInt();
                        creditCardDatabase.addBalanceToAccount(enteredCardNo , balancetoAdd);
                        System.out.println("Income was added!");
                    }
                    case 3 -> {
                        int currentBalance = creditCardDatabase.checkBalance(enteredCardNo);

                        System.out.println("System");
                        System.out.println("Enter card number:");
                        String cardNoForTransfer = sc.nextLine();

                        if (cardNoForTransfer.equals(enteredCardNo)) {
                            System.out.println("You can't transfer money to the same account!");
                            break;
                        }

                        if (creditCardDatabase.verifyCardNo(cardNoForTransfer) == 1) {
                            System.out.println("Probably you made a mistake in the card number. Please try again!");
                            break;
                        }

                        if (creditCardDatabase.verifyCardNo(cardNoForTransfer) == 2) {
                            System.out.println("Such a card does not exist.");
                            break;
                        }

                        if (creditCardDatabase.verifyCardNo(cardNoForTransfer) == 0) {
                            System.out.println("Enter how much money you want to transfer:");
                            int moneyToTransfer = sc.nextInt();

                            if (moneyToTransfer > currentBalance) {
                                System.out.println("Not enough money!");
                            } else {
                                creditCardDatabase.addBalanceToAccount(cardNoForTransfer , moneyToTransfer);
                                creditCardDatabase.subtractBalanceFromAccount(enteredCardNo, moneyToTransfer);
                                System.out.println("Success!");
                            }
                        }
                    }
                    case 4 -> {
                        creditCardDatabase.deleteAccount(enteredCardNo);
                    }
                    case 5 -> {
                        System.out.println("You have successfully logged out!");
                        loginMenuState = false;
                    }
                    case 0 -> {
                        System.out.println("Bye");
                        System.exit(0);
                    }
                    default -> throw new IllegalStateException("Invalid Choice " + selectedOption);
                }
            }

        }
    }

    public static void optionsMenu() {
        String createAccount = "1. Create an account";
        String loginToAccount = "2. Log into account";
        String exitAccount = "0. Exit";

        System.out.println(createAccount);
        System.out.println(loginToAccount);
        System.out.println(exitAccount);
    }

    public static void loginMenu() {
        String balance = "1. Balance";
        String addIncome = "2. Add income";
        String doTransfer = "3. Do Transfer";
        String closeAccount = "4. Close Account";
        String logOut = "5. Log out";
        String exitLogin = "0. Exit";

        System.out.println(balance);
        System.out.println(addIncome);
        System.out.println(doTransfer);
        System.out.println(closeAccount);
        System.out.println(logOut);
        System.out.println(exitLogin);
    }

    public static long generateCardNo() {
        final String BIN = "400000"; // Bank Identification Number (6 digits)
        Random random = new Random();

        // Generate a 9-digit account identifier (so total = 6 + 9 = 15 digits before checksum)
        int accountIdentifier = random.nextInt(100_000_000, 1_000_000_000); // 9-digit number

        // Concatenate BIN and account identifier
        String cardNumberWithoutChecksum = BIN + accountIdentifier; // 15 digits

        // Convert to int array for Luhn calculation
        int[] digits = new int[15];
        for (int i = 0; i < 15; i++) {
            digits[i] = Character.getNumericValue(cardNumberWithoutChecksum.charAt(i));
        }

        // Apply Luhn's algorithm
        for (int i = 0; i < 15; i += 2) {
            digits[i] *= 2;
            if (digits[i] > 9) {
                digits[i] -= 9;
            }
        }

        // Calculate checksum digit
        int sum = 0;
        for (int digit : digits) {
            sum += digit;
        }
        int checksum = (10 - (sum % 10)) % 10;

        // Append checksum to the original 15-digit string
        String fullCardNumber = cardNumberWithoutChecksum + checksum;

        return Long.parseLong(fullCardNumber);
    }

    public static int generatePin() {
        return 6743;
    }
}

class creditCardDatabase {
    private static Connection conn;

    public static void createDatabase(String filename) {
        String url = "jdbc:sqlite:" + filename;

        try {
            conn = DriverManager.getConnection(url);
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }

    public static void closeDatabaseConnection() {
        try {
            if (conn != null) conn.close();
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }

    public static void createTable() {
        var sql = "CREATE TABLE IF NOT EXISTS card ("
                + "     id INTEGER,"
                + "     number TEXT,"
                + "     pin TEXT,"
                + "     balance INTEGER"
                + ");";

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql); // create a new table
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public static void createNewCard(int id, String cardNo, String pin, int balance) {
        String sql = "INSERT INTO card(id, number, pin, balance) VALUES(? , ? , ? , ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.setString(2, cardNo);
            stmt.setString(3, pin);
            stmt.setInt(4, balance);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }

    public static boolean verifyCardInfo(String cardNo, String pin) {
        var sql = "SELECT * FROM card WHERE number = ? AND pin = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, cardNo);
            pstmt.setString(2, pin);

            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next(); //returns true if atleast one matching row is found
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
            return false;
        }

    }

    public static void addBalanceToAccount(String cardNo, int balance) {
        var sql = "UPDATE card SET balance = balance + ? WHERE number = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, balance);
            pstmt.setString(2, cardNo);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }

    public static void subtractBalanceFromAccount(String cardNo, int balance) {
        var sql = "UPDATE card SET balance = balance - ? WHERE number = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, balance);
            pstmt.setString(2, cardNo);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }

    public static int checkBalance(String cardNo) {
        var sql = "SELECT balance FROM card WHERE number = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, cardNo);
            var rs = pstmt.executeQuery();

            while (rs.next()) {
                return (rs.getInt("balance"));
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
        return 0;
    }

    public static int verifyCardNo(String cardNo) {
        var sqlExact = "SELECT number FROM card WHERE number = ?";
        var sqlLike = "SELECT number FROM card where number LIKE ?";

        try (PreparedStatement pstmtExact = conn.prepareStatement(sqlExact);
             PreparedStatement pstmtLike = conn.prepareStatement(sqlLike)) {

            //check for exact match
            pstmtExact.setString(1, cardNo);
            var rsExact = pstmtExact.executeQuery();

            if (rsExact.next()) {
                return 0; //exact match found
            }

            //check for partial match starting with card number
            pstmtLike.setString(1, cardNo + "%");
            var rsLike = pstmtLike.executeQuery();

            if (rsLike.next()) {
                return 1; // Starts with match found
            } else {
                return 2; // No match at all
            }
        } catch (SQLException e) {
            System.err.println("DB Error: " + e.getMessage());
        }
        return 3; // if there's a problem in connection
    }

    public static void deleteAccount(String cardNo) {
        var sql = "DELETE FROM card WHERE number = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, cardNo);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }
}


