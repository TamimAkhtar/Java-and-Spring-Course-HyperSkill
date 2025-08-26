package server;
import com.google.gson.Gson;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class Main {

    static Map<String, String> jsonDatabase = new HashMap<>();
    public static final int LISTENING_ON_PORT = 15000;

    public static void main(String[] args) {

        try (
                ServerSocket server = new ServerSocket(LISTENING_ON_PORT);
        ) {
            System.out.println("Server started!");
            Gson gson = new Gson();

            while (true) {
                try (
                        Socket socket = server.accept();
                        DataInputStream input = new DataInputStream(socket.getInputStream());
                        DataOutputStream output = new DataOutputStream(socket.getOutputStream());
                ) {

                    ReceivedRequest receivedMessage = gson.fromJson(input.readUTF(), ReceivedRequest.class);
                    String type = receivedMessage.getType();

                    if (type.equals("exit")) {
                        output.writeUTF(gson.toJson(new SendResponse("OK" , null , null)));
                        return; // Exit the program
                    }

                    if (receivedMessage.getKey() == null) {
                        System.out.println("Please Input a property key");
                        continue;
                    }

                    String key = receivedMessage.getKey();
                    String value = receivedMessage.getValue();

                    //do operations on received message accordingly
                    SendResponse serverResponse = switch (type) {
                        case "set" -> set(key,value);
                        case "get" -> get(key);
                        case "delete" -> delete(key);
                        default -> new SendResponse("ERROR" , null , null);
                    };

                    String jsonSendResponse = gson.toJson(serverResponse);

                    //send back response to client
                    output.writeUTF(jsonSendResponse);
                    System.out.println("Sent: " + jsonSendResponse);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static SendResponse set(String key , String value) {
        jsonDatabase.put(key, value);
        return new SendResponse("OK" , null , null);
    }

    static SendResponse get(String key) {
        String returnedValue = jsonDatabase.get(key); //returns value for this key

        if (returnedValue == null) {
            return new SendResponse("ERROR" , null , "No such key");
        } else {
            return new SendResponse("OK" , returnedValue , null);
        }
    }

    static SendResponse delete(String key) {
        if (jsonDatabase.containsKey(key)) {
            jsonDatabase.remove(key);
            return new SendResponse("OK" , null , null);
        } else {
            return new SendResponse("ERROR" , null , "No such key");
        }
    }
}

class ReceivedRequest {
    String type;
    String key;
    String value;

    public ReceivedRequest(String type, String key, String value) {
        this.type = type;
        this.key = key;
        this.value = value;
    }

    protected String getType() {
        return this.type;
    }

    protected String getKey() {
        return this.key;
    }

    protected String getValue() {
        return this.value;
    }
}

class SendResponse{
    String response;
    String value;
    String reason;

    public SendResponse(String response, String value, String reason) {
        this.response = response;
        this.value = value;
        this.reason = reason;
    }
}

