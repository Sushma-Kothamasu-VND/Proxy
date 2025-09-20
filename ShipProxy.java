// import java.io.*;
// import java.net.*;
// import java.util.concurrent.*;

// public class ShipProxy {
//     private static final int LISTEN_PORT = 8080;
//     private static final String SERVER_HOST = "localhost"; // offshore server
//     private static final int SERVER_PORT = 9090;

//     public static void main(String[] args) throws Exception {
//         ServerSocket serverSocket = new ServerSocket(LISTEN_PORT);
//         System.out.println("Ship Proxy started on port " + LISTEN_PORT);

//         // Queue to handle requests one by one
//         BlockingQueue<Socket> requestQueue = new LinkedBlockingQueue<>();

//         // Persistent TCP connection to offshore server
//         Socket offshoreSocket = new Socket(SERVER_HOST, SERVER_PORT);
//         DataOutputStream out = new DataOutputStream(offshoreSocket.getOutputStream());
//         DataInputStream in = new DataInputStream(offshoreSocket.getInputStream());

//         // Worker thread to process requests sequentially
//         new Thread(() -> {
//             try {
//                 while (true) {
//                     Socket client = requestQueue.take();
//                     BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
//                     PrintWriter writer = new PrintWriter(client.getOutputStream(), true);

//                     // Read HTTP request
//                     StringBuilder httpRequest = new StringBuilder();
//                     String line;
//                     while ((line = reader.readLine()) != null && !line.isEmpty()) {
//                         httpRequest.append(line).append("\r\n");
//                     }
//                     httpRequest.append("\r\n");

//                     // Send request to offshore server
//                     out.writeUTF(httpRequest.toString());
//                     out.flush();

//                     // Receive response from offshore server
//                     String response = in.readUTF();
//                     writer.write(response);
//                     writer.flush();

//                     client.close();
//                 }
//             } catch (Exception e) {
//                 e.printStackTrace();
//             }
//         }).start();

//         // Accept clients and add to queue
//         while (true) {
//             Socket client = serverSocket.accept();
//             requestQueue.add(client);
//         }
//     }
// }
import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class ShipProxy {
    private static final int LISTEN_PORT = 8080;
    private static final String SERVER_HOST = "localhost"; // offshore server
    private static final int SERVER_PORT = 9090;

    private static Socket offshoreSocket;
    private static DataOutputStream out;
    private static DataInputStream in;

    public static void main(String[] args) throws Exception {
        ServerSocket serverSocket = new ServerSocket(LISTEN_PORT);
        log("Ship Proxy started on port " + LISTEN_PORT);

        BlockingQueue<Socket> requestQueue = new LinkedBlockingQueue<>();

        // establish persistent connection initially
        connectToOffshore();

        // Worker thread
        new Thread(() -> {
            while (true) {
                try {
                    Socket client = requestQueue.take();
                    handleClient(client);
                } catch (Exception e) {
                    logError("Worker thread error: " + e.getMessage());
                }
            }
        }).start();

        // Accept clients and add to queue
        while (true) {
            Socket client = serverSocket.accept();
            requestQueue.add(client);

            if (requestQueue.size() > 10) {
                log("⚠ Warning: request queue size is " + requestQueue.size() +
                    " (performance bottleneck possible since sequential).");
            }
        }
    }

    private static void handleClient(Socket client) {
        try (
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(client.getInputStream(), "UTF-8"));
            OutputStream clientOut = client.getOutputStream()
        ) {
            String firstLine = reader.readLine();
            if (firstLine == null) {
                client.close();
                return;
            }

            StringBuilder requestBuilder = new StringBuilder(firstLine).append("\r\n");
            String line;
            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                requestBuilder.append(line).append("\r\n");
            }
            requestBuilder.append("\r\n");

            String httpRequest = requestBuilder.toString();

            // Send request to OffshoreProxy
            try {
                out.writeUTF(httpRequest);
                out.flush();

                String response = in.readUTF();
                clientOut.write(response.getBytes("UTF-8"));
                clientOut.flush();
            } catch (IOException e) {
                logError("Lost connection to Offshore, reconnecting...");
                connectToOffshore(); // reconnect attempt
                clientOut.write("HTTP/1.1 502 Bad Gateway\r\n\r\n".getBytes());
            }

        } catch (Exception e) {
            logError("Client handling failed: " + e.getMessage());
        } finally {
            try { client.close(); } catch (IOException ignored) {}
        }
    }

    // Establish or re-establish Offshore connection
    private static void connectToOffshore() {
        while (true) {
            try {
                offshoreSocket = new Socket(SERVER_HOST, SERVER_PORT);
                offshoreSocket.setKeepAlive(true);
                out = new DataOutputStream(offshoreSocket.getOutputStream());
                in = new DataInputStream(offshoreSocket.getInputStream());
                log("Connected to Offshore Proxy at " + SERVER_HOST + ":" + SERVER_PORT);
                break;
            } catch (IOException e) {
                logError("Retrying Offshore connection in 3s...");
                try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
            }
        }
    }

    private static void log(String msg) {
        System.out.println("[ShipProxy] " + msg);
    }

    private static void logError(String msg) {
        System.err.println("[ShipProxy][ERROR] " + msg);
    }
}
