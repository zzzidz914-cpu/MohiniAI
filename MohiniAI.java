// ------------------- WELCOME TO MOHINI AI -------------------
import java.io.*;
import java.net.*;        													//enviornment valrible
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.json.*;

public class MohiniAI {

    private static final String API_KEY = "PAST_YOUR_API_KEY";
    private static final String API_URL = "https://openrouter.ai/api/v1/chat/completions";
    private static final String MODEL = "gpt-4o-mini"; // you can change if needed
    private static final String HISTORY_FILE = "mohini_history.json"; // memory file

    // in-memory chat history
    private static final List<Map<String, String>> conversationHistory = new ArrayList<>();

    public static void main(String[] args) {
        System.out.println("------------------- WELCOME TO MOHINI AI -------------------");
        System.out.println("Type 'clear' to forget all, or 'exit' to quit.\n");

        // Load previous memory from file
        loadConversationHistory();

        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print("You: ");
            String userMessage = scanner.nextLine().trim();

            if (userMessage.equalsIgnoreCase("exit")) {
                System.out.println("Goodbye, Idz ");
                break;
            } else if (userMessage.equalsIgnoreCase("clear")) {
                conversationHistory.clear();
                saveConversationHistory(); // save the cleared file
                System.out.println("Memory cleared ");
                continue;
            }

            addMessage("user", userMessage);

            try {
                String aiResponse = getAIResponse();
                System.out.println("Mohini: " + aiResponse);
                addMessage("assistant", aiResponse);

                // Save conversation every turn
                saveConversationHistory();

            } catch (Exception e) {
                System.out.println( "Something went wrong: " + e.getMessage());
            }
        }

        scanner.close();
    }

    private static void addMessage(String role, String content) {
        Map<String, String> message = new HashMap<>();
        message.put("role", role);
        message.put("content", content);
        conversationHistory.add(message);
    }

    private static String getAIResponse() throws IOException {
        if (API_KEY == null || API_KEY.isEmpty()) {
            throw new IOException("API key missing. Set OPENROUTER_API_KEY as an environment variable.");
        }

        JSONObject requestBody = new JSONObject();
        requestBody.put("model", MODEL);
        requestBody.put("messages", new JSONArray(conversationHistory));

        HttpURLConnection conn = (HttpURLConnection) new URL(API_URL).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
        conn.setRequestProperty("HTTP-Referer", "http://localhost");
        conn.setRequestProperty("X-Title", "Mohini AI");
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = requestBody.toString().getBytes(StandardCharsets.UTF_8);
            os.write(input);
        }

        if (conn.getResponseCode() != 200) {
            throw new IOException("Server returned " + conn.getResponseCode() + ": " + conn.getResponseMessage());
        }

        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line.trim());
            }
        }

        JSONObject jsonResponse = new JSONObject(response.toString());
        return jsonResponse.getJSONArray("choices")
                           .getJSONObject(0)
                           .getJSONObject("message")
                           .getString("content");
    }

    // ------------------- MEMORY HANDLING -------------------

    private static void saveConversationHistory() {
        try (FileWriter file = new FileWriter(HISTORY_FILE)) {
            JSONArray jsonArray = new JSONArray(conversationHistory);
            file.write(jsonArray.toString(2)); // pretty print
        } catch (IOException e) {
            System.out.println(" Could not save memory: " + e.getMessage());
        }
    }

    private static void loadConversationHistory() {
        File file = new File(HISTORY_FILE);
        if (!file.exists()) return;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line);
            }

            JSONArray jsonArray = new JSONArray(content.toString());
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                Map<String, String> message = new HashMap<>();
                message.put("role", obj.getString("role"));
                message.put("content", obj.getString("content"));
                conversationHistory.add(message);
            }

            System.out.println(" Loaded previous chat with " + conversationHistory.size() + " messages.\n");
        } catch (Exception e) {
            System.out.println(" Could not load memory: " + e.getMessage());
        }
    }
}
