package client;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

class Main {
    @Parameter(names = {"-type", "-t"} , description = "Type of Request")
    private String type;
    @Parameter(names = {"-fileName", "-in"} , description = "Read JSON from this file")
    private String fileName;
    @Parameter(names = {"-key", "-k"} , converter = TypeConverter.class , description = "Property key")
    private JsonElement key;
    @Parameter(names = {"-value", "-v"} , converter = TypeConverter.class , description = "Property value")
    private JsonElement value;

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

            // Handle exit before parsing type
           if ("exit".equalsIgnoreCase(type)) {
               String exitJson = "{\"type\":\"exit\"}";
                output.writeUTF(exitJson);
                System.out.println("Sent: " + exitJson);

                String response = input.readUTF();
                System.out.println("Received: " + response);
                return; // exit after sending
           }

            JsonPreparationStrategy strategy = JsonStrategyFactory.selectStrategy(type, key, value, fileName);
            JsonMaker maker = new JsonMaker();
            maker.setStrategy(strategy);
            String jsonString = maker.prepare();

            output.writeUTF(jsonString);
            System.out.println("Sent: " +jsonString);

            String receivedString = input.readUTF();
            System.out.println("Received: " + receivedString);

        } catch (ClientInputException e) {
            System.err.println("Input Error: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("I/O Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

class SendRequest {
    private String type;
    private JsonElement key;
    private JsonElement value;

    public SendRequest(String type, JsonElement key, JsonElement value) {
        this.type = type;
        this.key = key;
        this.value = value;
    }

    protected void setType(String type) {
        this.type = type;
    }
    protected void setKey(JsonElement key) {
        this.key = key;
    }
    protected void setValue(JsonElement value) {
        this.value = value;
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

        // Folder path only (without the file)
        Path path = Paths.get(System.getProperty("user.dir") + "/src/client/data/" + fileName);

        try {
            if (!Files.exists(path.getParent())) {
                Files.createDirectories(path.getParent());
            }

            // Ensure file exists
            if (!Files.exists(path)) {
                Files.writeString(path, "{}"); // initialize empty JSON
            }

            jsonString = Files.readString(path);
            return jsonString;
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file with filepath: " + path + " | " + e.getMessage(), e);
        }
    }
}

class PrepareFromCommandLine implements JsonPreparationStrategy {
    String type;
    JsonElement key;
    JsonElement value;
    String jsonString;
    private static final Gson gson = new Gson();

    public PrepareFromCommandLine(String type, JsonElement key, JsonElement value) {
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
    GET, SET, DELETE , EXIT;

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

    public static JsonPreparationStrategy selectStrategy(String type, JsonElement key, JsonElement value, String fileName) throws ClientInputException {

        boolean useFile = fileName != null && !fileName.isBlank();
        boolean useCmd = type != null && !type.isBlank();

        // Validation first

        if ((type == null || type.isBlank()) && (fileName == null || fileName.isBlank())) {
            throw new ClientInputException("Please specify either a command type (-t) or an input file (-in).");
        }

        if (useFile == useCmd) {
            throw new ClientInputException(
                    "Please specify either:\n" +
                            "  -in <fileName> to read from file\n" +
                            "  OR\n" +
                            "  -t <type> -k <key> [-v <value>] to build from command line\n" +
                            "But not both."
            );
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
            System.err.println("Warning: Unknown type '" + type + "'. Proceeding anyway — server may reject.");
        }

        RequestType reqType = RequestType.valueOf(type.toUpperCase());

        if ((reqType == RequestType.SET || reqType == RequestType.DELETE)
                && (key.isJsonNull())) {
            throw new ClientInputException("'-k <key>' must be provided for type '" + type + "'.");
        }

        return new PrepareFromCommandLine(type, key, value);
    }
}

class ClientInputException extends Exception {
    public ClientInputException(String message) {
        super(message);
    }
}

class TypeConverter implements IStringConverter<JsonElement> {
    @Override
    public JsonElement convert(String value) {
        try {
            // Try to parse as JSON (works for numbers, arrays, objects, quoted strings)
            return JsonParser.parseString(value);
        } catch (Exception e) {
            // Fallback: treat as plain string
            return new JsonPrimitive(value);
        }
    }
}

