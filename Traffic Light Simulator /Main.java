package traffic;
import java.io.IOException;
import java.util.Scanner;

public class Main {
  public static void main(String[] args) {

    Scanner sc = new Scanner(System.in);
    String[] menuOptions = {"1. Add", "2. Delete", "3. System", "0. Quit"};

    System.out.println("Welcome to the traffic management system!");

    System.out.println("Input the number of roads:"); //use the errorHandler to keep asking for an acceptable value for numOfRoads before moving on
    int errorHandlerReturn = -1;
    while (errorHandlerReturn <= 0) {
      String numOfRoads = sc.nextLine();
      errorHandlerReturn = errorHandler(numOfRoads);
    }

    System.out.println("Input the interval:"); //use the errorHandler to keep asking for an acceptable value for interval before moving on
    int errorHandlerReturn2 = -1;
    while (errorHandlerReturn2 <= 0) {
      String interval = sc.nextLine();
      errorHandlerReturn2 = errorHandler(interval);
    }

    CircularQueue queue = new CircularQueue(errorHandlerReturn);
    RoadManager manager = new RoadManager(queue);

    TimerWorkerThread worker = new TimerWorkerThread(errorHandlerReturn, errorHandlerReturn2, queue, manager);
    worker.setName("QueueThread");
    worker.start();



    while (true) { //keep the menu options until the user enters 0 and exits
      System.out.println("Menu:");
      for (String i : menuOptions) {
        System.out.println(i);
      }
      String selectedOption = sc.nextLine();

      if (selectedOption.matches("\\d+")) {
        int selectedOptionInt = Integer.parseInt(selectedOption);

        if (selectedOptionInt == 3) {
          worker.enterSystemState();
          sc.nextLine();
          worker.exitSystemState();
          continue;
        }

        switch (selectedOptionInt) {
          case 1 -> {
            System.out.println("Input road name: > ");
            String roadName = sc.nextLine();
            queue.addElement(roadName);
            manager.updateCurrentOnFirstAdd();
          }
          case 2 -> {
            queue.deleteElement();
            manager.updateCurrentOnDeleteAfterFrontRemoval();
          }
          case 0 -> {
            worker.shutdown();
            System.out.println("Bye!");
            return;
          }
          default -> System.out.println("Incorrect option");
        }
      } else {
        System.out.println("Incorrect option");
      }
      //sc.nextLine();
      sc.nextLine();
      clearConsole();
    }
  }

  public static int errorHandler(String value) {
    try {
      int valueInt = Integer.parseInt(value);
      if (valueInt > 0) {
        return valueInt;
      }
    } catch (NumberFormatException ignored) {
    }
    System.out.println("Error! Incorrect Input. Try again: ");
    return -1;
  }

  public static void clearConsole() {
    try {
      var clearCommand = System.getProperty("os.name").contains("Windows")
              ? new ProcessBuilder("cmd", "/c", "cls")
              : new ProcessBuilder("clear");
      clearCommand.inheritIO().start().waitFor();
    } catch (IOException | InterruptedException ignored) {}
  }
}

class TimerWorkerThread extends Thread {
  private int numRoads;
  private int interval;
  private CircularQueue queue;
  private RoadManager manager;

  private int seconds = 0;
  private boolean inSystemState = false;
  private volatile boolean running = true;

    public TimerWorkerThread(int numRoads, int interval, CircularQueue queue, RoadManager manager) {
    this.numRoads = numRoads;
    this.interval = interval;
    this.queue = queue;
    this.manager = manager;
  }

  public void enterSystemState() {
    inSystemState = true;
  }

  public void exitSystemState() {
    inSystemState = false;
  }

  public void shutdown() {
    running = false;
  }

  @Override
  public void run() {
      int intervalCountdown = interval;
    while (running) {
      try {
        Thread.sleep(1000);
        seconds++;

        if (inSystemState) {
          System.out.println("! " + seconds + "s. have passed since system startup !");
          System.out.println("! Number of roads: " + numRoads + " !");
          System.out.println("! Interval: " + interval + " !");

          if (queue.getSize() >= 1) {
            manager.printStates(interval, intervalCountdown);

            intervalCountdown--;
            if (intervalCountdown == 0) {
              manager.updateCurrentOnTimer();
              intervalCountdown = interval;
            }
          }

          System.out.println("! Press \"Enter\" to open menu !");
        }
      } catch (InterruptedException e) { //if thread is interrupted, ignore it and continue loop
      }
    }
  }
}

class CircularQueue {
  private String[] queue;
  private int capacity;

  private int front = 0;
  private int rear = 0;
  private int size = 0;

  public CircularQueue(int numRoads){
    this.capacity = numRoads;
    this.queue = new String[capacity];
  }

  public String[] getQueue() {
    return queue;
  }

  public int getFront() {
    return front;
  }

  public int getSize() {
    return size;
  }

  public int getCapacity() {
    return capacity;
  }

  public boolean isFull() {
    return size == capacity;
  }

  public boolean isEmpty() {
    return size == 0;
  }

  public void addElement (String road) {
    if (isFull()) {
      System.out.println("Queue is full");
    } else {
      queue[rear] = road;
      rear = (rear + 1) % capacity;
      size++;
      System.out.println(road + " Added!");
    }
  }

  public void deleteElement () {
    if (isEmpty()) {
      System.out.println("Queue is empty");
    } else {
      String removed = queue[front];
      queue[front] = null;
      front = (front + 1) % capacity;
      size--;
      System.out.println(removed + " deleted!");
    }
  }

  public void showElements() {
    if (isEmpty()) {
      return;
    }
    for (int i = 0; i < size; i++) {
      int index = (front + i) % capacity;
      System.out.println(queue[index]);
    }
  }

  public String getElementAt(int index) {
    if (index < 0 || index >= size) return null;
    return queue[(front + index) % capacity];
  }
}

class RoadManager {

  private CircularQueue queue;

  private int current = -1; //current is logical index and is always at physical index (front+current)%capacity
  public static final String GREEN = "\u001B[32m";
  public static final String RED = "\u001B[31m";
  public static final String RESET = "\u001B[0m";

  public RoadManager(CircularQueue queue) {
    this.queue = queue;
  }

  public void updateCurrentOnFirstAdd() {
    if (queue.getSize() == 1) { //first road added
      current = 0;
    }
  }

  public void updateCurrentOnTimer() {
    int size = queue.getSize();
    if (size > 0) {
      current = (current + 1) % queue.getSize();
    }
  }

  public void updateCurrentOnDelete() {
    int size = queue.getSize();
    if (size==0) {
      current = -1; //no roads left
    } else if (current >= size) { //if current is out of bounds (e.g. deleted last road)
      current = 0; //reset to first road logically
    }
    //else current remains same logical index
  }

  public String getCurrentRoad() { //so current is basically relative position wrt to front
    if (current == -1) return null; // no roads
    int physicalIndex = (queue.getFront() + current) % queue.getCapacity();
    return queue.getQueue()[physicalIndex];
  }

  public void printStates(int interval, int intervalCountdown) {
    int size = queue.getSize();
    if (size == 0 || current == -1) return;

    String[] q = queue.getQueue();
    int front = queue.getFront();
    int capacity = queue.getCapacity();

    for (int i = 0; i < size; i++) {
      String road = queue.getElementAt(i);
      if (road == null) continue;

      if (i == current) {
        System.out.println(GREEN + road + " will be open for " + intervalCountdown + "s." + RESET);
      } else {
        int stepsAhead = (i - current + size) % size;
        int timeUntilOpen = intervalCountdown + interval * (stepsAhead - 1);
        System.out.println(GREEN + road + " will be closed for " + timeUntilOpen + "s." + RESET);
      }
    }
  }

  public void updateCurrentOnDeleteAfterFrontRemoval() {
    int size = queue.getSize();
    if (size == 0) {
      current = -1;
    } else if (current == 0) {
      // current stays 0, because next road is now at index 0 after front removed
    } else if (current > 0) {
      current--;
    }
  }


}


