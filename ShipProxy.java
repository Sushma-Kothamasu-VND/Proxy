import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class ShipProxy {
    private static final int LISTEN_PORT = 8080;
    private static final String SERVER_HOST = "localhost"; // offshore server
    private static final int SERVER_PORT = 9090;

    public static void main(String[] args) throws Exception {
        ServerSocket serverSocket = new ServerSocket(LISTEN_PORT);
        System.out.println("Ship Proxy started on port " + LISTEN_PORT);

        // Queue to handle requests one by one
        BlockingQueue<Socket> requestQueue = new LinkedBlockingQueue<>();

        // Persistent TCP connection to offshore server
        Socket offshoreSocket = new Socket(SERVER_HOST, SERVER_PORT);
        DataOutputStream out = new DataOutputStream(offshoreSocket.getOutputStream());
        DataInputStream in = new DataInputStream(offshoreSocket.getInputStream());

        // Worker thread to process requests sequentially
        new Thread(() -> {
            try {
                while (true) {
                    Socket client = requestQueue.take();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
                    PrintWriter writer = new PrintWriter(client.getOutputStream(), true);

                    // Read HTTP request
                    StringBuilder httpRequest = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null && !line.isEmpty()) {
                        httpRequest.append(line).append("\r\n");
                    }
                    httpRequest.append("\r\n");

                    // Send request to offshore server
                    out.writeUTF(httpRequest.toString());
                    out.flush();

                    // Receive response from offshore server
                    String response = in.readUTF();
                    writer.write(response);
                    writer.flush();

                    client.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        // Accept clients and add to queue
        while (true) {
            Socket client = serverSocket.accept();
            requestQueue.add(client);
        }
    }
}
