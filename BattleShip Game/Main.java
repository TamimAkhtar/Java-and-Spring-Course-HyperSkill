package org.tamim.HyperSkill.BattleShipGame;
import java.util.Scanner;
import java.util.ArrayList;
import java.util.List;


public class Main {
    static Scanner sc = new Scanner(System.in);

    public static void main(String[] args) {

        Player[] players = {new Player("Player 1"), new Player("Player 2")};

        for (Player p : players) {
            p.getGame().createBattleFields();

            System.out.printf("%s, place your ships on the game field", p.getName());
            placeAllShips(p.getGame());

            System.out.println("Press Enter and pass the move to another player");
            sc.nextLine();
        }

        Player currentPlayer = players[0];
        Player opponentPlayer = players[1];

        while (true) {
            currentPlayer.getGame().printPvPFields(opponentPlayer, currentPlayer);
            System.out.printf("%s, it's your turn:", currentPlayer.getName());
            String targetCell = sc.nextLine().trim();
            currentPlayer.enteredCells.add(targetCell);

            int hitOrMiss = opponentPlayer.getGame().shotHitOrMiss(targetCell);

            switch (hitOrMiss) {
                case 0 ->
                        System.out.println("Error! You entered the wrong coordinates! Try again:"); //wrong coordinates
                case 1 -> {

                    if (opponentPlayer.getGame().isShipSunk(currentPlayer.enteredCells, opponentPlayer.getGame().allShipParts)) {
                        if (opponentPlayer.getGame().allShipsSunk()) {
                            System.out.println("You sank the last ship. You won. Congratulations!");
                            return; // Game over
                        }
                        System.out.println("You sank a ship!");
                    } else {
                        System.out.println("You hit a ship!");
                    }
                }
                case 2 -> System.out.println("You missed!");
            }

            if (currentPlayer == players[0]) {
                currentPlayer = players[1];
                opponentPlayer = players[0];
            } else {
                currentPlayer = players[0];
                opponentPlayer = players[1];
            }

            System.out.println("Press Enter and pass the move to another player");
            sc.nextLine();
        }
    }

    static void placeAllShips(BattleShip player) {

        String[] shipType = {"Aircraft Carrier" , "Battleship" , "Submarine" , "Cruiser" , "Destroyer"};
        int currentShipIndex = 0;

        player.printField(player.realFieldArray);

        while (currentShipIndex< shipType.length) {
            String currentShip = shipType[currentShipIndex];
            System.out.printf("Enter the coordinates of the %s (%d cells):" , currentShip , BattleShip.shipLengthByType(currentShip));//entered in the form A2 B5

            while (true) {
                String coordinates = sc.nextLine().trim(); //in the form of A2 A7

                if (coordinates.isEmpty()) {
                    System.out.println("Error, you entered an empty line. Please try again!");
                    continue;
                }

                player.configureShipCoordinates(coordinates); //configure ship coordinates

                List<String> shipParts = player.returnShipParts(); //return ship parts

                //check for correct length
                if (BattleShip.shipLengthByType(currentShip) != player.returnShipLength()) {
                    System.out.printf("Error! Wrong length of the %s! Try again:" , currentShip);
                    continue;
                }

                if (player.isShipDiagonal() || player.isShipOutOfBounds()) {
                    System.out.println("Error! Wrong ship location! Try again:");
                    continue;
                }

                if (player.isTooCloseToOtherShips(shipParts)) {
                    System.out.println("Error! You placed it too close to another one. Try again:");
                    continue;
                }

                player.allShipParts.add(new ArrayList<>(shipParts));

                player.placeShipOnRealField();
                player.printField(player.realFieldArray);
                currentShipIndex++;
                break;
            }
        }
    }
}

class BattleShip {

    //class variables
    String[][] realFieldArray = new String[11][11]; //stores the current 2d array field
    String[][] fogFieldArray = new String[11][11];

    char startRow = ' '; //if user enters A2 B5, start row is A
    char endRow = ' '; //if user enters A2 B5, end row is B
    int startColumn = 0; //if user enters A2 B5, start column is 2
    int endColumn = 0; //if user enters A2 B5, end column is 5

    List<String> adjacentCells = new ArrayList<>();

    List<List<String>> allShipParts = new ArrayList<>();

    // getter methods -------------------------------------------------------------------------------------------
    char getStartRow() {
        return startRow;
    }

    char getEndRow() {
        return endRow;
    }

    int getStartColumn() {
        return startColumn;
    }

    int getEndColumn() {
        return endColumn;
    }

    List<String> getAdjacentCells() {
        return adjacentCells;
    }
    //----------------------------------------------------------------------------------------------------------

    protected void createBattleFields() {
        initField(realFieldArray);
        initField(fogFieldArray);
    }

    protected void initField(String[][] fieldArray) { //method to create an empty field and label all rows and columns by updating fieldArray

        String initColumnNumber = "1";
        int initHexValueChar = 65;
        String initRowNumber = String.valueOf((char) initHexValueChar); //returns "A"

        fieldArray[0][0] = " ";  //first element is empty

        for (int j = 1; j < fieldArray.length; j++) { //populating first row with numbers
            fieldArray[0][j] = initColumnNumber;
            initColumnNumber = String.valueOf(Integer.parseInt(initColumnNumber) + 1);
        }

        for (int i = 1; i < fieldArray.length; i++) { //populating first column with alphabets
            fieldArray[i][0] = initRowNumber;
            initRowNumber = String.valueOf((char) ++initHexValueChar);
        }

        for (int i = 1; i < fieldArray.length; i++) { //populating the field with ~
            for (int j = 1; j < fieldArray[i].length; j++) {
                fieldArray[i][j] = "~";
            }
        }
    }

    protected void configureShipCoordinates(String coordinates) { //takes user input for ship coordinates like A2 B5 and returns each of them separately

        String[] coordinatesArray = coordinates.trim().split(" ");

        String startCoordinates = coordinatesArray[0].toUpperCase();
        String endCoordinates = coordinatesArray[1].toUpperCase();

        startRow = startCoordinates.charAt(0);
        endRow = endCoordinates.charAt(0);
        startColumn = Integer.parseInt(startCoordinates.substring(1));
        endColumn = Integer.parseInt(endCoordinates.substring(1));
    }

    protected int returnShipLength() {
        int length = 0;

        boolean isShipHorizontal = isShipHorizontal();
        boolean isShipVertical = isShipVertical();
        boolean isShipDiagonal = isShipDiagonal();

        if (isShipDiagonal) return 0; //

        if (isShipHorizontal) {
            length = Math.abs(startColumn - endColumn) + 1;
        }

        if (isShipVertical) {
            length = Math.abs(startRow - endRow) + 1;
        }

        return length;
    }

    protected List<String> returnShipParts() { //if user enters A2 A5 as ship coordinates, returns all cells occupied by ship as A2 A3 A4 A5
        List<String> shipParts = new ArrayList<>();

        boolean isShipHorizontal = isShipHorizontal();
        boolean isShipVertical = isShipVertical();

        if (isShipHorizontal) { //populate parts if ship is horizontal
            if (startColumn <= endColumn) {
                for (int i = startColumn; i <= endColumn; i++) {
                    shipParts.add("" + startRow + i);
                }
            } else {
                for (int i = startColumn; i >= endColumn; i--) {
                    shipParts.add("" + startRow + i);
                }
            }
        }

        if (isShipVertical) { //populate parts if ship is vertical
            if (startRow <= endRow) {
                for (char i = startRow; i <= endRow; i++) {
                    shipParts.add("" + i + startColumn);
                }
            } else {
                for (char i = startRow; i >= endRow; i--) {
                    shipParts.add("" + i + startColumn);
                }
            }
        }

        return shipParts;
    }

    protected boolean isShipHorizontal() { //using ship coordinates entered before, checks if ship is horizontal
        return startRow == endRow;
    }

    protected boolean isShipVertical() { //using ship coordinates entered before, checks if ship is horizontal
        return startColumn == endColumn;
    }

    protected boolean isShipDiagonal() { //using ship coordinates entered before, checks if ship is diagonal (not allowed)
        return ((startRow != endRow) && (startColumn != endColumn));
    }

    protected boolean isShipOutOfBounds() { //using ship coordinates entered before, checks if ship is outside field array (not allowed)
        return (startRow < 'A' || startRow > 'J' ||
                endRow < 'A' || endRow > 'J' ||
                startColumn < 1 || startColumn > 10 ||
                endColumn < 1 || endColumn > 10);
    }

    protected static int shipLengthByType(String shipType) {
        return ShipTypes.valueOf(shipType.toUpperCase().replace(" ", "_")).getLength();
    }

    protected boolean isTooCloseToOtherShips(List<String> shipParts) {
        //for each cell in shipParts, check its 8 neighbours and return false if any of these
        // neighbours contain 'O' ie there's already a ship on an adjacent cell

        int[][] offsets = {
                {-1, 0}, {1, 0}, {0, -1}, {0, 1}, // one row up, one row down, one column left, one column right
                {-1, -1}, {-1, 1}, {1, -1}, {1, 1} //also taking diagonals in account
        };

        for (String i : shipParts) {
            char row = i.charAt(0); //extract row letter
            int col = Integer.parseInt(i.substring(1)); //extract column number
            int rowNum = rowToIndex(row);

            for (int[] j : offsets) {
                int adjRow = rowNum + j[0];
                int adjCol = col + j[1];

                if (adjRow >= 1 && adjRow <= 10 &&
                        adjCol >= 1 && adjCol <= 10) {
                    if (realFieldArray[adjRow][adjCol].equals("O")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    protected void placeShipOnRealField() {
        List<String> shipParts = returnShipParts();

        for (String i : shipParts) {
            char row = i.charAt(0);
            int col = Integer.parseInt(i.substring(1));

            int rowNumber = rowToIndex(row);
            realFieldArray[rowNumber][col] = "O";
        }
    }

    protected int rowToIndex(char row) {
        return row - 'A' + 1;
    }

    protected void printField(String[][] fieldArray) {
        for (int i = 0; i < fieldArray.length; i++) {
            for (int j = 0; j < fieldArray[i].length; j++)
                System.out.print(fieldArray[i][j] + " ");
            System.out.println();
        }
    }

    protected int shotHitOrMiss(String targetCell) {
        if (targetCell.length() < 2) return 0;

        char row = Character.toUpperCase(targetCell.charAt(0));
        int rowIndex = rowToIndex(row);

        int col;
        try {
            col = Integer.parseInt(targetCell.substring(1));
        } catch (NumberFormatException e) {
            return 0;
        }

        boolean cellOutOfBounds = cellOutOfBounds(rowIndex, col);
        if (cellOutOfBounds) return 0;

        String cell = realFieldArray[rowIndex][col];

        // If the cell is already hit ("X") or is an alive ship ("O") -> treat as hit
        if ("X".equals(cell) || "O".equals(cell)) {
            realFieldArray[rowIndex][col] = "X";
            fogFieldArray[rowIndex][col] = "X";
            return 1;
        } else { // already miss or empty
            realFieldArray[rowIndex][col] = "M";
            fogFieldArray[rowIndex][col] = "M";
            return 2;
        }
    }

    protected boolean cellOutOfBounds(int rowIndex, int col) {
        return (rowIndex < 1 || rowIndex > 10 ||
                col < 1 || col > 10);
    }

    protected boolean allShipsSunk() {
        for (int i = 1; i < realFieldArray.length; i++) { //populating the field with ~
            for (int j = 1; j < realFieldArray[i].length; j++) {
                if (realFieldArray[i][j].equals("O")) {
                    return false;
                }
            }
        }
        return true;
    }

    protected boolean isShipSunk(List<String> enteredCells, List<List<String>> allShipParts) {
        for (int i = 0; i < allShipParts.size(); i++) {
            List<String> ship = allShipParts.get(i);
            if (enteredCells.containsAll(ship)) {
                allShipParts.remove(i); // remove this ship from tracking
                return true;
            }
        }
        return false;
    }

    protected void printPvPFields(Player opponent , Player current) {
        printField(opponent.game.fogFieldArray);
        System.out.println("-".repeat(21));
        printField(current.game.realFieldArray);
    }
}

class Player {
    private String name;
    BattleShip game;//game here represents my object of BattleShip class
    List<String> enteredCells;

    public Player(String name) {
        this.name = name;
        this.game = new BattleShip(); //also create an object/instance for every new player
        this.enteredCells = new ArrayList<>();
    }

    public String getName() {return name;}
    public BattleShip getGame() {return game;} //returns the object/instance of BattleSHip class
    public List<String> getEnteredCells() {return enteredCells;}
}

enum ShipTypes {
    AIRCRAFT_CARRIER (5),
    BATTLESHIP(4),
    SUBMARINE(3),
    CRUISER(3),
    DESTROYER(2);

    private final int length;

    ShipTypes(int length) {
        this.length = length;
    }

    public int getLength() {
        return length;
    }
}