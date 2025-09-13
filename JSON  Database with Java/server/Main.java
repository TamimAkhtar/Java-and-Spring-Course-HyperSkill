package server;
import com.google.gson.*;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;


public class Main {

    public static final int LISTENING_ON_PORT = 15000;
    static final String filePath = System.getProperty("user.dir") + "/src/server/data/db.json";
    static final File file = new File (filePath);
    private static JsonObject db = new JsonObject();
    static final Gson gson = new Gson();
    private static final ReadWriteLock lock = new ReentrantReadWriteLock();
    private static volatile boolean running = true;


    //static block to make sure that we always start with a valid json object
    static {
        try {
            if (!file.exists()) {
                file.createNewFile(); //create empty file
                Files.writeString(file.toPath(), "{}"); //initialize with empty Json object
            }
            String content = Files.readString(file.toPath()).trim(); //read file
            db = content.isBlank() //if db.Json is blank after trim,
                    ? new JsonObject() //start again with db = new Json object in memory
                    : JsonParser.parseString(content).getAsJsonObject(); //otherwise parse content as json and convert it
            // to json object, assigning the result to static db
        } catch (IOException e) {
            db = new JsonObject(); // fallback
        }
    }

    public static void main(String[] args) {

        ExecutorService executor = Executors.newFixedThreadPool(5);

        try (
                ServerSocket server = new ServerSocket(LISTENING_ON_PORT);
        ) {
            System.out.println("Server started!");

            while (running) {
                Socket socket = server.accept(); //accept new connection

                executor.submit(() -> handleClient(server, socket)); //every thread is passed a different socket object but the same server object
            }

        } catch (IOException e) {
            System.err.println("ServerSocket Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            executor.shutdown();
        }
    }

    //here the logic is that whenever an error is thrown in opening a ServerSeocket server or in the body of while,
    //an IOException is thrown and executor is closed. so that means that if handleClient also throws any error,
    // the catch block will be run and any exceptions in handleClient method will be caught here

    private static void handleClient(ServerSocket server, Socket socket) {
        //we want each thread to close its own socket and streams, thats why we are not closing it
        //from the main method because main can close socket (if its in main's try-with-resources block
        //while the thread is running

        try (
                Socket client = socket;
                DataInputStream input = new DataInputStream(client.getInputStream());
                DataOutputStream output = new DataOutputStream(client.getOutputStream());
        ) {

            ReceivedRequest receivedMessage = gson.fromJson(input.readUTF(), ReceivedRequest.class);
            String type = receivedMessage.getType();

            if (type.equals("exit")) {
                output.writeUTF(gson.toJson(new SendResponse("OK", null, null)));

                // Stop the server
                running = false;

                // Close ServerSocket to unblock accept()
                try {
                    server.close();
                } catch (IOException ignored) { }

                return; // Exit the program
            }

            if (receivedMessage.getKey()==null) {
                output.writeUTF(gson.toJson(new SendResponse("ERROR", null, "Key is required")));
                return; //Exit the program
            }

            if ("set".equals(type) && receivedMessage.getValue() == null) {
                output.writeUTF(gson.toJson(new SendResponse("ERROR", null, "Value is required for set")));
                return;
            }

            JsonElement key = receivedMessage.getKey();
            JsonElement value = receivedMessage.getValue();

            SendResponse serverResponse = switch (type) {
                case "set" -> setToFile(key, value);
                case "get" -> getFromFile(key);
                case "delete" -> deleteFromFile(key);
                default -> new SendResponse("ERROR", null, null);
            };


            String jsonSendResponse = gson.toJson(serverResponse);

            //send back response to client
            output.writeUTF(jsonSendResponse);

        } catch (Exception e) {
            System.err.print("Error in Data Parsing" + e.getMessage());
            e.printStackTrace();
        }
    }

    private static JsonElement traverse(JsonElement current, JsonArray keys) {
        JsonElement level = current;
        for (JsonElement key : keys) {
            if (!level.isJsonObject()) {
                return null; // dead end
            }
            JsonObject obj = level.getAsJsonObject();
            if (!obj.has(key.getAsString())) {
                return null;
            }
            level = obj.get(key.getAsString());
        }
        return level;
    }

    private static void persist() throws IOException {
        Files.writeString(file.toPath(), gson.toJson(db));
    }

    static SendResponse getFromFile(JsonElement keyElement) {

        Lock readLock = lock.readLock();
        readLock.lock(); //lock file for reading but multiple readers can read file simultaneously

        try {
            JsonElement result;

            if (keyElement.isJsonPrimitive()) { //key is a simple string
                String key = keyElement.getAsString();
                if (!db.has(key)) {
                    return new SendResponse("ERROR", null, "No such key");
                }
                result = db.get(key);

            } else { // key is an array
                JsonArray keys = keyElement.getAsJsonArray();
                result = traverse(db, keys);
                if (result == null) {
                    return new SendResponse("ERROR", null, "No such key");
                }
            }

            return new SendResponse("OK", result, null);

        } catch (Exception e) {
            return new SendResponse("ERROR", null, "Database read Error");
        } finally {
            readLock.unlock();
        }
    }

    static SendResponse setToFile(JsonElement keyElement, JsonElement valueElement) {
        Lock writeLock = lock.writeLock();
        writeLock.lock(); // Wait until all readers finish, block other writers

        try {
            if (keyElement.isJsonPrimitive()) {
                db.add(keyElement.getAsString(), valueElement);

            } else {
                JsonArray keys = keyElement.getAsJsonArray();
                JsonObject obj = db;

                // Traverse until last-1 key
                for (int i = 0; i < keys.size() - 1; i++) {
                    String k = keys.get(i).getAsString();
                    if (!obj.has(k) || !obj.get(k).isJsonObject()) {
                        obj.add(k, new JsonObject());
                    }
                    obj = obj.getAsJsonObject(k);
                }

                String lastKey = keys.get(keys.size() - 1).getAsString();
                obj.add(lastKey, valueElement);
            }

            persist();
            return new SendResponse("OK", null, null);

        } catch (Exception e) {
            e.printStackTrace();
            return new SendResponse("ERROR", null, "Database write Error");
        } finally {
            writeLock.unlock();
        }
    }


    static SendResponse deleteFromFile(JsonElement keyElement) {

        Lock writeLock = lock.writeLock();
        writeLock.lock(); // Wait until all readers finish, block other writers

        try {
            if (keyElement.isJsonPrimitive()) {
                String k = keyElement.getAsString();
                if (!db.has(k)) {
                    return new SendResponse("ERROR", null, "No such key");
                }
                db.remove(k);

            } else {
                JsonArray keys = keyElement.getAsJsonArray();
                JsonObject obj = db;

                for (int i = 0; i < keys.size() - 1; i++) {
                    String k = keys.get(i).getAsString();
                    if (!obj.has(k) || !obj.get(k).isJsonObject()) {
                        return new SendResponse("ERROR", null, "No such key");
                    }
                    obj = obj.getAsJsonObject(k);
                }

                String lastKey = keys.get(keys.size() - 1).getAsString();
                if (!obj.has(lastKey)) {
                    return new SendResponse("ERROR", null, "No such key");
                }
                obj.remove(lastKey);
            }

            persist();
            return new SendResponse("OK", null, null);

        } catch (Exception e) {
            return new SendResponse("ERROR", null, "Database delete error");
        } finally {
            writeLock.unlock();
        }
    }
}

class ReceivedRequest { //to deserialize json to ReceivedRequest object
    String type;
    JsonElement key;
    JsonElement value;

    public ReceivedRequest(String type, JsonElement key, JsonElement value) {
        this.type = type;
        this.key = key;
        this.value = value;
    }

    protected String getType() {
        return this.type;
    }

    protected JsonElement getKey() {
        return this.key;
    }

    protected JsonElement getValue() {
        return this.value;
    }
}

class SendResponse{ //serialize SendResponse object to json and send
    String response;
    JsonElement value;
    String reason;

    public SendResponse(String response, JsonElement value, String reason) {
        this.response = response;
        this.value = value;
        this.reason = reason;
    }
}





