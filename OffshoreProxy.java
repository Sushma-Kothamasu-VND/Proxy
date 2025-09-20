import java.io.*;
import java.net.*;

public class OffshoreProxy {
    private static final int LISTEN_PORT = 9090;

    public static void main(String[] args) throws Exception {
        ServerSocket serverSocket = new ServerSocket(LISTEN_PORT);
        System.out.println("Offshore Proxy started on port " + LISTEN_PORT);

        Socket shipSocket = serverSocket.accept();
        DataInputStream in = new DataInputStream(shipSocket.getInputStream());
        DataOutputStream out = new DataOutputStream(shipSocket.getOutputStream());

        while (true) {
            String request = in.readUTF();
            System.out.println("Received request:\n" + request);

            // Parse destination host
            String host = extractHost(request);

            // Forward request to actual server
            Socket target = new Socket(host, 80);
            PrintWriter targetOut = new PrintWriter(target.getOutputStream(), true);
            BufferedReader targetIn = new BufferedReader(new InputStreamReader(target.getInputStream()));

            targetOut.write(request);
            targetOut.flush();

            // Collect response
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = targetIn.readLine()) != null) {
                response.append(line).append("\r\n");
            }

            // Send back to ship proxy
            out.writeUTF(response.toString());
            out.flush();

            target.close();
        }
    }

    private static String extractHost(String request) {
        for (String line : request.split("\r\n")) {
            if (line.startsWith("Host:")) {
                return line.split(" ")[1];
            }
        }
        return "httpforever.com"; // fallback
    }
}
