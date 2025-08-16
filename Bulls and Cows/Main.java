package bullscows;
import java.util.Random;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.println("Input the length of the secret code:");

        String lengthInput = sc.nextLine();
        // Check if length is a valid number
        if (!lengthInput.matches("\\d+")) {
            System.out.println("Error: \"" + lengthInput + "\" isn't a valid number.");
            return;
        }
        int length = Integer.parseInt(lengthInput);

        System.out.println("Input the number of possible symbols in the code:");

        String symbolsInput = sc.nextLine();
        // Check if symbols is a valid number
        if (!symbolsInput.matches("\\d+")) {
            System.out.println("Error: \"" + symbolsInput + "\" isn't a valid number.");
            return;
        }
        int symbols = Integer.parseInt(symbolsInput);

        // Error: length > symbols
        if (symbols < length) {
            System.out.printf("Error: it's not possible to generate a code with a length of %d with %d unique symbols.\n", length, symbols);
            return;
        }

        // Error: symbols > 36
        if (symbols > 36) {
            System.out.println("Error: maximum number of possible symbols in the code is 36 (0-9, a-z).");
            return;
        }

        if (length <= 0) {
            System.out.println("Error: length must be greater than 0.");
            return;
        }

        printRoses(length,symbols);

        String secretCode = generateSecretCodeWide(length,symbols);
        System.out.println("Okay, let's start a game!");
        countBullsCows(secretCode);
        sc.close();
    }

    public static void countBullsCows(String code) {
        int count = 1;

        while (true) {
            Scanner sc = new Scanner(System.in);
            System.out.println("Turn " + count + ":");
            String guess = sc.next();
            count++;

            if (guess.equals(code)) {
                System.out.println("Grade: " + code.length() + " bull(s)");
                System.out.println("Congratulations! You guessed the secret code.");
                break;
            }

            int bulls = 0;
            int cows = 0;
            for (int i = 0; i < code.length(); i++) {
                if (guess.charAt(i) == code.charAt(i)) {
                    bulls++;
                } else if (guess.contains(String.valueOf(code.charAt(i)))) {
                    cows++;
                }
            }
            // Print grade result
            if (bulls == 0 && cows == 0) {
                System.out.println("Grade: None.");
            } else {
                StringBuilder result = new StringBuilder("Grade: ");
                if (bulls > 0) result.append(bulls).append(" bull(s)");
                if (bulls > 0 && cows > 0) result.append(" and ");
                if (cows > 0) result.append(cows).append(" cow(s)");
                System.out.println(result);
            }

        }
    }

    public static String generateSecretCodeNano(int length) { //using System.nano
        while (true) {
            String pseudoRandomNumber = Long.toString(System.nanoTime());
            StringBuilder code = new StringBuilder();

            for (int i = pseudoRandomNumber.length() - 1; i >= 0; i--) {
                char ch = pseudoRandomNumber.charAt(i);
                if (ch >= '1' && ch <= '9') {
                    code.append(ch);
                    break;
                }
            }

            for (int i = pseudoRandomNumber.length() - 1; i >= 0 && code.length() < length; i--) {
                char ch = pseudoRandomNumber.charAt(i);
                if (code.indexOf(String.valueOf(ch)) == -1) {
                    code.append(ch);
                }
            }

            if (code.length() == length) {
                return code.toString();
            }
        }
    }

    public static String generateSecretCodeRandom(int length) { //using random class

        StringBuilder code = new StringBuilder();
        Random rand = new Random();

        //first digit: between 1 and 9
        int firstDigit = rand.nextInt(1, 10);
        code.append(firstDigit);

        //remaining digits between 0 to 9 and skip already used
        while (code.length() < length) {
            int digit = rand.nextInt(0, 10);
            if (code.indexOf(String.valueOf(digit)) == -1) {
                code.append(digit);
            }
        }

        return code.toString();
    }

    public static String generateSecretCodeWide(int length, int symbols) { //includes alphabets
        StringBuilder code = new StringBuilder();
        Random rand = new Random();

        String allSymbols = "0123456789abcdefghijklmnopqrstuvwxyz".substring(0, symbols);

        while (code.length() < length) {
            int index = rand.nextInt(allSymbols.length());
            char ch = allSymbols.charAt(index);
            if (code.indexOf(String.valueOf(ch)) == -1) {
                code.append(ch);
            }
        }
        return code.toString();
    }

    public static void printRoses(int length, int symbols) {
        StringBuilder stars = new StringBuilder();
        for (int i = 0; i < length; i++) {
            stars.append('*');
        }

        StringBuilder symbolRange = new StringBuilder();
        if (symbols <= 10) {
            symbolRange.append("0-").append((char) ('0' + symbols - 1));
        } else {
            symbolRange.append("0-9, a-")
                    .append((char) ('a' + (symbols - 11)));  // 11 because 'a' is the 11th symbol (index 10)
        }

        System.out.println("The secret is prepared: " + stars + " (" + symbolRange + ").");
    }
}



