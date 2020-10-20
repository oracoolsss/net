import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {

    static ExecutorService executorService = Executors.newFixedThreadPool(8);
    public static final int SERVER_PORT = 7777;
    public static final int PORT_ARGUMENT = 0;
    public static final String UPLOADS_FOLDER_NAME = "uploads";

    public static void main(String[] args) {
        createFolder(UPLOADS_FOLDER_NAME);

        int serverPort = SERVER_PORT;
        if (args.length >= 1) {
            serverPort = Integer.parseInt(args[PORT_ARGUMENT]);
        }

        try (ServerSocket server = new ServerSocket(serverPort)) {
            while (!server.isClosed()) {
                Socket client = server.accept();
                executorService.execute(new ClientHandler(client));
            }
            executorService.shutdown();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void createFolder(String folderName) {
        File file = new File(folderName);
        file.mkdirs();
    }
}