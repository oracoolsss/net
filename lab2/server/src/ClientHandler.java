import java.io.*;
import java.net.Socket;
import java.text.DecimalFormat;

public class ClientHandler implements Runnable {

    private static final String RECEIVE_FILES_PATH = "./uploads/";
    private static final long DELTA = 3000;
    private static final int BUFFER_SIZE = 1024;

    private static Socket client;
    private static String fileName;
    private static long fileSize = -1;

    private static DataOutputStream out;
    private static DataInputStream in;

    public ClientHandler(Socket client) {
        ClientHandler.client = client;
        try {
            out = new DataOutputStream(client.getOutputStream());
            in = new DataInputStream(client.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            receiveFileInfo();
            receiveFile(RECEIVE_FILES_PATH + fileName);
            checkReceivedFile();

            in.close();
            out.close();
            client.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void checkReceivedFile() {
        String textToSend;
        if (fileSize == (new File(RECEIVE_FILES_PATH + fileName)).length()) {
            textToSend = "File was received successfully";
        } else {
            textToSend = "Error, file wasn't received correctly";
        }
        try {
            out.writeUTF(textToSend);
            out.flush();
            System.out.println(textToSend);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void receiveFileInfo() {
        try {
            fileName = in.readUTF();
            System.out.println("FileName: " + fileName);
            fileSize = in.readLong();
            System.out.println("FileSize: " + fileSize + " B");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void receiveFile(String outputFilePath) {
        try {
            File output = new File(outputFilePath);
            FileOutputStream fos = new FileOutputStream(output);
            byte[] buffer = new byte[BUFFER_SIZE];
            int data = 0;

            double endTime, startTime = System.currentTimeMillis();
            double lastTime = startTime, currentTime = startTime;
            double bytesCount = 0, bytesCountPrev = 0;
            double speed, avgSpeed;
            DecimalFormat dec = new DecimalFormat("#0.00");
            while (true) {
                if (in.available() > 0) data = in.read(buffer);
                if (data != -1) {
                    bytesCount += data;
                    currentTime = System.currentTimeMillis();
                    fos.write(buffer, 0, data);
                    data = -1;
                    if (currentTime - DELTA >= lastTime) {
                        lastTime = currentTime;
                        speed = (bytesCount - bytesCountPrev) / (DELTA / 1000);
                        avgSpeed = bytesCount / ((currentTime - startTime) / 1000);
                        System.out.println("Current speed: " + dec.format(speed / 1024) + "KB/s");
                        System.out.println("Average speed: " + dec.format(avgSpeed / 1024) + "KB/s\n");
                        bytesCountPrev = bytesCount;
                    }
                } else {
                    endTime = System.currentTimeMillis();
                    avgSpeed = bytesCount / ((endTime - startTime) / 1000);
                    System.out.println("Average speed: " + dec.format(avgSpeed / 1024) + "KB/s\n");
                    break;
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}