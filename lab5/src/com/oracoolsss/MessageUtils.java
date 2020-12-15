package com.oracoolsss;

import org.apache.commons.lang3.ArrayUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class MessageUtils {

    private static byte OK = 0x00;
    private static byte ERROR = 0x01;
    private static int BUF_SIZE = 8192;
    private static byte NO_AUTH = 0x00;
    private static byte SOCKS5 = 0x05;
    private static byte TCPIP = 0x01;
    private static byte IPV4 = 0x01;
    private static byte DNS = 0x03;
    private static byte RESERVED = 0x00;
    private static byte[] LOCALHOST = new byte[]{0x7F, 0x00, 0x00, 0x01};

    public static SecondParseResult MESSAGE_ERROR = new SecondParseResult();

    public static boolean getFirstMessage(SocketChannel from) {
        ByteBuffer buffer = ByteBuffer.allocate(BUF_SIZE);
        try {
            if (from.read(buffer) == -1) {
                return false;
            } else {
                buffer.flip();
                byte socksVersion = buffer.get();
                byte methodAmount = buffer.get();
                byte[] methods = new byte[methodAmount];
                buffer.get(methods);
                return socksVersion == SOCKS5 && ArrayUtils.contains(methods, NO_AUTH);
            }
        } catch (IOException e) {
            return false;
        }
    }

    public static SecondParseResult getSecondMessage(SocketChannel from) {
        ByteBuffer buffer = ByteBuffer.allocate(BUF_SIZE);
        try {
            if (from.read(buffer) == -1) {
                return MESSAGE_ERROR;
            } else {
                buffer.flip();
                byte socksVersion = buffer.get();
                byte command = buffer.get();
                byte reserved = buffer.get();
                if (socksVersion != SOCKS5 || command != TCPIP || reserved != RESERVED) {
                    return MESSAGE_ERROR;
                }

                byte addressType = buffer.get();
                byte[] address;
                if (addressType == IPV4) {
                    address = new byte[4];
                    buffer.get(address, 0, 4);
                } else if (addressType == DNS) {
                    int nameLength = buffer.get();
                    address = new byte[nameLength];
                    buffer.get(address, 0, nameLength);
                } else {
                    return MESSAGE_ERROR;
                }
                short port = buffer.getShort();
                return new SecondParseResult(addressType == DNS, true, address, port);
            }
        } catch (IOException e) {
            return MESSAGE_ERROR;
        }
    }

    public static void sendFirstConfirmation(SocketChannel to) throws IOException {
        ByteBuffer message = ByteBuffer.allocate(2);
        message.put(SOCKS5);
        message.put(NO_AUTH);
        to.write(ByteBuffer.wrap(message.array(), 0, 2));
    }

    public static void sendSecondConfirmationMessage(SocketChannel to, short port, boolean isNotError) throws IOException {
        //ByteBuffer message = ByteBuffer.allocate(10);
        byte[] resultMessage = null;
        if (isNotError) {
            resultMessage = ArrayUtils.addAll(new byte[] {SOCKS5, OK, RESERVED, IPV4}, LOCALHOST);
        } else {
            resultMessage = ArrayUtils.addAll(new byte[] {SOCKS5, ERROR, RESERVED, IPV4}, LOCALHOST);
        }
        resultMessage = ArrayUtils.addAll(resultMessage, (byte) ((port >> 8) & 0xFF), (byte) (port & 0xFF));
        //message.put(resultMessage);
        to.write(ByteBuffer.wrap(resultMessage, 0, 10));
    }
}