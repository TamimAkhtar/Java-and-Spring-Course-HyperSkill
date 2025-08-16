package machine;
import java.util.Scanner;

public class CoffeeMachine { //Main is only for user interaction
    public static Scanner sc = new Scanner(System.in);

    public static void main(String[] args) {
        while (true) {
            System.out.println("Write action (buy, fill, take, clean, remaining, exit):");
            String action = sc.next();

            if (action.equals("exit")) {
                break;
            } else if (action.equals("buy")) {
                Worker.buy();
            } else if (action.equals("fill")) {
                Worker.fill();
            } else if (action.equals("take")) {
                Worker.take();
            } else if (action.equals("clean")) {
                Worker.clean();
            } else if (action.equals("remaining")) {
                Worker.printState();
            }
        }
    }
}

enum CoffeeType {
    ESPRESSO(250,0,16,4),
    LATTE(350,75,20,7),
    CAPPUCCINO(200,100,12,6);

    int reqWater;
    int reqMilk;
    int reqBeans;
    int unitPrice;

    CoffeeType (int reqWater, int reqMilk, int reqBeans, int unitPrice) {
        this.reqWater = reqWater;
        this.reqMilk = reqMilk;
        this.reqBeans = reqBeans;
        this.unitPrice = unitPrice;
    }
}

class MachineState {
    static int water = 400;
    static int milk = 540;
    static int beans = 120;
    static int cups = 9;
    static int money = 550;
    static int cupsMade = 0;
}

class Worker {

    private static String canMakeCoffee(CoffeeType coffee) {
        if (MachineState.water < coffee.reqWater) {
            return "Sorry, not enough water!";
        }

        if (MachineState.milk < coffee.reqMilk) {
            return "Sorry, not enough milk!";
        }

        if (MachineState.beans < coffee.reqBeans) {
            return "Sorry, not enough beans!";
        }

        if (MachineState.cups < 1) {
            return "Sorry, not enough cups!";
        } else {
            return "I have enough resources, making you a coffee!";
        }
    }

    public static void buy() {

        if (MachineState.cupsMade >= 10) {
            System.out.println("I need cleaning!");
            return;
        }

        System.out.println("What do you want to buy? 1 - espresso, 2 - latte, 3 - cappuccino, back - to main menu:");
        String choice = CoffeeMachine.sc.next();

        if (choice.equals("back")) {
            return;
        }

        CoffeeType coffee = null;

        switch (choice) {
            case "1":
                coffee = CoffeeType.ESPRESSO;
                break;
            case "2":
                coffee = CoffeeType.LATTE;
                break;
            case "3":
                coffee =  CoffeeType.CAPPUCCINO;
                break;
        }

        String msg = canMakeCoffee(coffee);
        System.out.println(msg);

        if (msg.startsWith("I have enough resources")) {
            // Update machine state after successful coffee preparation
            MachineState.water -= coffee.reqWater;
            MachineState.milk -= coffee.reqMilk;
            MachineState.beans -= coffee.reqBeans;
            MachineState.cups -= 1;
            MachineState.money += coffee.unitPrice;
            MachineState.cupsMade++;
        }
    }

    public static void fill() {
        System.out.println("Write how many ml of water you want to add:");
        MachineState.water += CoffeeMachine.sc.nextInt();
        System.out.println("Write how many ml of milk you want to add:");
        MachineState.milk += CoffeeMachine.sc.nextInt();
        System.out.println("Write how many grams of coffee beans you want to add:");
        MachineState.beans += CoffeeMachine.sc.nextInt();
        System.out.println("Write how many disposable cups of coffee you want to add:");
        MachineState.cups += CoffeeMachine.sc.nextInt();
    }

    public static void take() {
        System.out.println("I gave you $" + MachineState.money);
        MachineState.money = 0;
    }

    public static void printState() {
        System.out.println("The coffee machine has:");
        System.out.println(MachineState.water + " ml of water");
        System.out.println(MachineState.milk + " ml of milk");
        System.out.println(MachineState.beans + " g of coffee beans");
        System.out.println(MachineState.cups + " disposable cups");
        System.out.println("$" + MachineState.money + " of money");
    }

    public static void clean() {
        MachineState.cupsMade  = 0;
        System.out.println("I have been cleaned!");
    }
}
