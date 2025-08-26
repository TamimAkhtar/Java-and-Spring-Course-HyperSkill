package client;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.gson.Gson;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;

class Main {
    @Parameter(names = {"-type", "-t"} , description = "Type of Request")
    private String type;
    @Parameter(names = {"-fileName", "-in"} , description = "Read JSON from this file")
    private String fileName;
    @Parameter(names = {"-key", "-k"} , description = "Property key")
    private String key;
    @Parameter(names = {"-value", "-v"} , description = "Property value")
    private String value;

    private static final int SERVER_PORT = 15000;
    private static final String SERVER_ADDRESS = "127.0.0.1";

    public static void main(String[] args) {
        Main main = new Main();
        JCommander.newBuilder().addObject(main).build().parse(args);
        main.run();
    }

    public void run() {
        try (
                //create socket object and input and output data object
                Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT); //create socket object for communication
                DataInputStream input = new DataInputStream(socket.getInputStream());
                DataOutputStream output = new DataOutputStream(socket.getOutputStream())
        ) {
            System.out.println("Client started!");

            JsonPreparationStrategy strategy = JsonStrategyFactory.selectStrategy(type, fileName, key, value);
            JsonMaker maker = new JsonMaker();
            maker.setStrategy(strategy);
            String jsonString = maker.prepare();

            output.writeUTF(jsonString);
            System.out.println("Sent: " +jsonString);

            ReceivedResponse receivedResponse = gson.fromJson(input.readUTF(), ReceivedResponse.class);
            System.out.println("Received: " + gson.toJson(receivedResponse));

        } catch (IOException e) {
            System.err.println("I/O Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

class SendRequest {
    private String type;
    private String key;
    private String value;

    public SendRequest(String type, String key, String value) {
        this.type = type;
        this.key = key;
        this.value = value;
    }

    protected void setType(String type) {
        this.type = type;
    }
    protected void setKey(String key) {
        this.key = key;
    }
    protected void setValue(String value) {
        this.value = value;
    }
}

class ReceivedResponse {
    private final String response;
    private final String value;
    private final String reason;

    public ReceivedResponse(String response, String value, String reason) {
        this.response = response;
        this.value = value;
        this.reason = reason;
    }
}

interface JsonPreparationStrategy { //how to prepare JSON
    String prepareJson();
}

class PrepareFromFile implements JsonPreparationStrategy {
    private final String fileName;
    String jsonString;

    public PrepareFromFile(String fileName) {
        this.fileName = fileName;
    }

    @Override
    public String prepareJson()  {
        String filePath = System.getProperty("user.dir") + "/src/client/data/" + fileName;

        try {
            jsonString = Files.readString(Path.of(filePath));
            return jsonString;
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file with filepath: " + filePath + " | " + e.getMessage(), e);
        }
    }
}

class PrepareFromCommandLine implements JsonPreparationStrategy {
    String type;
    String key;
    String value;
    String jsonString;
    private static final Gson gson = new Gson();

    public PrepareFromCommandLine(String type, String key, String value) {
        this.type = type;
        this.key = key;
        this.value = value;
    }

    @Override
    public String prepareJson(){
        SendRequest sendRequest = new SendRequest(type,key,value);
        jsonString = gson.toJson(sendRequest);
        return jsonString;
    }
}

class JsonMaker {
    private JsonPreparationStrategy strategy;

    public void setStrategy(JsonPreparationStrategy strategy) {
        this.strategy = strategy;
    }

    public String prepare() {
        return this.strategy.prepareJson();
    }
}

enum RequestType {
    GET, SET, DELETE;

    public static boolean isValid(String value) {
        if (value == null) return false;
        try {
            RequestType.valueOf(value.toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}

class JsonStrategyFactory {

    public static JsonPreparationStrategy selectStrategy(String type, String key, String value, String fileName) {

        boolean useFile = fileName != null && !fileName.isBlank();
        boolean useCmd = type != null && !type.isBlank();

        // Validation first

        if ((type == null || type.isBlank()) && (fileName == null || fileName.isBlank())) {
            System.out.println("Error: Please specify either a command type (-t) or an input file (-in).");
            System.exit(1);
        }

        if (useFile == useCmd) {
            System.err.println(
                    "Error: Please specify either:\n" +
                            "  -in <fileName> to read from file\n" +
                            "  OR\n" +
                            "  -t <type> -k <key> [-v <value>] to build from command line\n" +
                            "But not both."
            );
            System.exit(1);
        }

        if (useFile) {
            PrepareFromFile prepareFromFile = new PrepareFromFile(fileName);
            try {
                // Trigger JSON preparation to check file exists and is readable
                prepareFromFile.prepareJson();
            } catch (RuntimeException e) {
                System.err.println("Error: Could not read file '" + fileName + "'. " + e.getMessage());
                System.exit(1);
            }
            return prepareFromFile;
        }

        if (!RequestType.isValid(type)) {
            System.err.println(
                    "Error: Invalid type '" + type +
                            "'. Allowed values: GET, SET, DELETE."
            );
            System.exit(1);
        }

        RequestType reqType = RequestType.valueOf(type.toUpperCase());

        if ((reqType == RequestType.SET || reqType == RequestType.DELETE)
                && (key == null || key.isBlank())) {
            System.err.println("Error: '-k <key>' must be provided for type '" + reqType + "'.");
            System.exit(1);
        }

        return new PrepareFromCommandLine(type, key, value);
    }
}

