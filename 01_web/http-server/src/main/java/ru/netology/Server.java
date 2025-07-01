package ru.netology;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class Server {
    private final int port;
    private final List<String> validPaths;
    private final ExecutorService threadPool;

    public Server(int port, List<String> validPaths) {
        this.port = port;
        this.validPaths = validPaths;
        this.threadPool = Executors.newFixedThreadPool(64);
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server started on port " + port);
            while (true) {
                Socket socket = serverSocket.accept();
                threadPool.submit(() -> handleConnection(socket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            shutdown();
        }
    }

    private void handleConnection(Socket socket) {
        try (
                final var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                final var out = new BufferedOutputStream(socket.getOutputStream());
        ) {

            String requestLine = in.readLine();
            if (requestLine == null || requestLine.isEmpty()) {
                socket.close();
                return;
            }

            String[] parts = requestLine.split(" ");
            if (parts.length != 3) {
                socket.close();
                return;
            }

            String method = parts[0];
            String path = parts[1];

            if (!method.equals("GET")) {
                sendResponse(out, "HTTP/1.1 405 Method Not Allowed\r\n\r\n");
                return;
            }

            if (!validPaths.contains(path)) {
                sendResponse(out, "HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\nConnection: close\r\n\r\n");
                return;
            }

            Path filePath = Path.of(".", "public", path);
            String mimeType = Files.probeContentType(filePath);

            if (path.equals("/classic.html")) {
                handleClassicHtml(out, filePath, mimeType);
                return;
            }

            long length = Files.size(filePath);
            String headers = "HTTP/1.1 200 OK\r\n" +
                             "Content-Type: " + mimeType + "\r\n" +
                             "Content-Length: " + length + "\r\n" +
                             "Connection: close\r\n\r\n";

            out.write(headers.getBytes());
            Files.copy(filePath, out);
            out.flush();

        } catch (IOException e) {

        } finally {
            try {
                socket.close();
            } catch (IOException e) {

            }
        }
    }
    private void handleClassicHtml(BufferedOutputStream out, Path filePath, String mimeType) throws IOException {
        String template = Files.readString(filePath);
        String contentStr = template.replace("{time}", LocalDateTime.now().toString());
        byte[] contentBytes = contentStr.getBytes();

        String headers = "HTTP/1.1 200 OK\r\n" +
                         "Content-Type: " + mimeType + "\r\n" +
                         "Content-Length: " + contentBytes.length + "\r\n" +
                         "Connection: close\r\n\r\n";

        out.write(headers.getBytes());
        out.write(contentBytes);
        out.flush();
    }
    private void sendResponse(BufferedOutputStream out, String response) throws IOException {
        out.write(response.getBytes());
        out.flush();
    }
    public void shutdown() {
        threadPool.shutdown();
    }
}