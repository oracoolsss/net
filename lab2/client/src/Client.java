import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;

public class Client {
    private static final int PORT_ARGUMENT = 2;
    private static final int IP_ADDR_ARGUMENT = 1;
    private static final int FILE_PATH_ARGUMENT = 0;

    private static final int BUFFER_SIZE = 1024;

    private static Socket socket;
    private static String inputFilePath;
    private static String ipAddr;
    private static int port;

    private static DataOutputStream out;
    private static DataInputStream in;

    public static void main(String[] args) {
        if (args.length >= 3) {
            inputFilePath = args[FILE_PATH_ARGUMENT];
            ipAddr = args[IP_ADDR_ARGUMENT];
            port = Integer.parseInt(args[PORT_ARGUMENT]);
        } else {
            System.out.println("Expected arguments: [file path] [ip address] [port]");
            System.exit(0);
        }

        try {
            socket = new Socket(ipAddr, port);
            out = new DataOutputStream(socket.getOutputStream());
            in = new DataInputStream(socket.getInputStream());

            sendFileInfo();
            sendFile();
            getFeedBack();

            in.close();
            out.close();
            socket.close();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void sendFileInfo() {
        try {
            out.writeUTF(getFileNameFromFilePath(inputFilePath));
            out.writeLong((new File(inputFilePath)).length());
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String getFileNameFromFilePath(String filePath) {
        String res = "";
        for (int i = filePath.length() - 1; i >= 0; i--) {
            if (filePath.charAt(i) == '\\' || filePath.charAt(i) == '/') {
                break;
            }
            res += filePath.charAt(i);
        }
        StringBuffer fileName = new StringBuffer(res);
        fileName.reverse();
        return String.valueOf(fileName);
    }

    private static void sendFile() {
        try {
            File input = new File(inputFilePath);
            FileInputStream fis = new FileInputStream(input);
            byte[] buffer = new byte[BUFFER_SIZE];
            int data;
            while (true) {
                data = fis.read(buffer);
                if (data != -1) {
                    out.write(buffer, 0, data);
                } else {
                    break;
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void getFeedBack() {
        String result;
        try {
            result = in.readUTF();
            System.out.println("Server: " + result);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}