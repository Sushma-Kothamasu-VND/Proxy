// import java.io.*;
// import java.net.*;

// public class OffshoreProxy {
//     private static final int LISTEN_PORT = 9090;

//     public static void main(String[] args) throws Exception {
//         ServerSocket serverSocket = new ServerSocket(LISTEN_PORT);
//         System.out.println("Offshore Proxy started on port " + LISTEN_PORT);

//         Socket shipSocket = serverSocket.accept();
//         DataInputStream in = new DataInputStream(shipSocket.getInputStream());
//         DataOutputStream out = new DataOutputStream(shipSocket.getOutputStream());

//         while (true) {
//             String request = in.readUTF();
//             System.out.println("Received request:\n" + request);

//             // Parse destination host
//             String host = extractHost(request);

//             // Forward request to actual server
//             Socket target = new Socket(host, 80);
//             PrintWriter targetOut = new PrintWriter(target.getOutputStream(), true);
//             BufferedReader targetIn = new BufferedReader(new InputStreamReader(target.getInputStream()));

//             targetOut.write(request);
//             targetOut.flush();

//             // Collect response
//             StringBuilder response = new StringBuilder();
//             String line;
//             while ((line = targetIn.readLine()) != null) {
//                 response.append(line).append("\r\n");
//             }

//             // Send back to ship proxy
//             out.writeUTF(response.toString());
//             out.flush();

//             target.close();
//         }
//     }

//     private static String extractHost(String request) {
//         for (String line : request.split("\r\n")) {
//             if (line.startsWith("Host:")) {
//                 return line.split(" ")[1];
//             }
//         }
//         return "httpforever.com"; // fallback
//     }
// }
// import java.io.*;
// import java.net.*;

// public class OffshoreProxy {
//     private static final int LISTEN_PORT = 9090;

//     public static void main(String[] args) {
//         try (ServerSocket serverSocket = new ServerSocket(LISTEN_PORT)) {
//             log("Offshore Proxy started on port " + LISTEN_PORT);

//             Socket shipSocket = serverSocket.accept();
//             DataInputStream in = new DataInputStream(shipSocket.getInputStream());
//             DataOutputStream out = new DataOutputStream(shipSocket.getOutputStream());

//             while (true) {
//                 try {
//                     String request = in.readUTF();
//                     log("Received request:\n" + request);

//                     String host = extractHost(request);

//                     try (Socket target = new Socket(host, 80);
//                          PrintWriter targetOut = new PrintWriter(target.getOutputStream(), true);
//                          BufferedReader targetIn = new BufferedReader(
//                              new InputStreamReader(target.getInputStream(), "UTF-8"))
//                     ) {
//                         targetOut.write(request);
//                         targetOut.flush();

//                         StringBuilder response = new StringBuilder();
//                         String line;
//                         while ((line = targetIn.readLine()) != null) {
//                             response.append(line).append("\r\n");
//                         }

//                         out.writeUTF(response.toString());
//                         out.flush();

//                     } catch (UnknownHostException e) {
//                         logError("Unknown host: " + host);
//                         out.writeUTF("HTTP/1.1 502 Bad Gateway\r\n\r\n");
//                     } catch (IOException e) {
//                         logError("I/O error contacting host " + host + ": " + e.getMessage());
//                         out.writeUTF("HTTP/1.1 504 Gateway Timeout\r\n\r\n");
//                     }
//                 } catch (EOFException eof) {
//                     logError("Ship connection closed. Waiting for reconnect...");
//                     shipSocket = serverSocket.accept(); // wait for new ship connection
//                     in = new DataInputStream(shipSocket.getInputStream());
//                     out = new DataOutputStream(shipSocket.getOutputStream());
//                 } catch (Exception e) {
//                     logError("Unhandled error: " + e.getMessage());
//                 }
//             }
//         } catch (IOException e) {
//             logError("Fatal error: " + e.getMessage());
//         }
//     }

//     private static String extractHost(String request) {
//         for (String line : request.split("\r\n")) {
//             if (line.toLowerCase().startsWith("host:")) {
//                 return line.split(" ")[1].trim();
//             }
//         }
//         throw new IllegalArgumentException("No Host header found in request");
//     }

//     private static void log(String msg) {
//         System.out.println("[OffshoreProxy] " + msg);
//     }

//     private static void logError(String msg) {
//         System.err.println("[OffshoreProxy][ERROR] " + msg);
//     }
// }
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class OffshoreProxy {
    private static final int LISTEN_PORT = 9090;

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(LISTEN_PORT)) {
            log("Offshore Proxy started on port " + LISTEN_PORT);

            Socket shipSocket = serverSocket.accept();
            DataInputStream in = new DataInputStream(shipSocket.getInputStream());
            DataOutputStream out = new DataOutputStream(shipSocket.getOutputStream());

            while (true) {
                try {
                    // Receive request from ShipProxy
                    String request = in.readUTF();  // headers + maybe body
                    log("Received request:\n" + request);

                    // Parse host
                    String host = extractHost(request);

                    // Forward to target server
                    try (Socket target = new Socket(host, 80)) {
                        InputStream targetIn = target.getInputStream();
                        OutputStream targetOut = target.getOutputStream();

                        // Send request as bytes
                        targetOut.write(request.getBytes(StandardCharsets.UTF_8));
                        targetOut.flush();

                        // Capture response as raw bytes
                        ByteArrayOutputStream responseBuffer = new ByteArrayOutputStream();
                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        while ((bytesRead = targetIn.read(buffer)) != -1) {
                            responseBuffer.write(buffer, 0, bytesRead);
                        }

                        // Send full response back to ShipProxy
                        byte[] responseBytes = responseBuffer.toByteArray();
                        out.writeUTF(new String(responseBytes, StandardCharsets.UTF_8));
                        out.flush();

                    } catch (UnknownHostException e) {
                        logError("Unknown host: " + host);
                        out.writeUTF("HTTP/1.1 502 Bad Gateway\r\n\r\n");
                    } catch (IOException e) {
                        logError("I/O error contacting " + host + ": " + e.getMessage());
                        out.writeUTF("HTTP/1.1 504 Gateway Timeout\r\n\r\n");
                    }

                } catch (EOFException eof) {
                    logError("Ship connection closed. Waiting for reconnect...");
                    shipSocket = serverSocket.accept(); // wait for new ship connection
                    in = new DataInputStream(shipSocket.getInputStream());
                    out = new DataOutputStream(shipSocket.getOutputStream());
                } catch (Exception e) {
                    logError("Unhandled error: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            logError("Fatal error: " + e.getMessage());
        }
    }

    // Extract host from headers
    private static String extractHost(String request) {
        for (String line : request.split("\r\n")) {
            if (line.toLowerCase().startsWith("host:")) {
                return line.split(" ")[1].trim();
            }
        }
        throw new IllegalArgumentException("No Host header found in request");
    }

    private static void log(String msg) {
        System.out.println("[OffshoreProxy] " + msg);
    }

    private static void logError(String msg) {
        System.err.println("[OffshoreProxy][ERROR] " + msg);
    }
}
