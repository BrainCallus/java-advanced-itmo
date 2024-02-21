package info.kgeorgiy.ja.churakova.hello;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

public class Utilits {
    protected static final int DEFAULT_BUF_SIZE = 1024;
    protected static final int TIMEOUT = 100;

    protected static String getPortOutOfRangeMessage(int port, String message) {
        return String.format("Given port %d is out of range: %s%n", port, message);
    }

    protected static String getUnreachablePortMessage(int port, String message) {
        return String.format("Port %d unreachable now: %s%n", port, message);
    }

    protected static String getSocketExceptionMessage(String message) {
        return String.format("SocketException occur during creation DatagramSocket: %s%n", message);
    }

    protected static DatagramPacket getDatagramPack(DatagramSocket socket) throws SocketException {
        return new DatagramPacket(new byte[socket.getReceiveBufferSize()], socket.getReceiveBufferSize());
    }

    protected static boolean verifyArgs(String[] args, int expected) {
        if (args == null || args.length != expected) {
            System.err.printf("Expected %d arguments %n", expected);
            return false;
        }
        for (String arg : args) {
            if (arg == null) {
                System.err.println("All arguments mustn't be null");
                return false;
            }
        }
        return true;
    }

    protected static int[] requirePositive(int... args) {
        for (int arg : args) {
            if (arg <= 0) {
                return null;
            }
        }
        return args;
    }

    protected static String generateResponse(DatagramSocket datagram, DatagramPacket responsePack) throws IOException {
        datagram.receive(responsePack);
        return new String(responsePack.getData(), responsePack.getOffset(),
                responsePack.getLength(), StandardCharsets.UTF_8);
    }

    protected static void sendData(DatagramSocket socket, DatagramPacket data, int cur_thread, boolean server) {
        try {
            socket.send(data);
        } catch (PortUnreachableException p) {
            System.err.print(getUnreachablePortMessage(socket.getPort(), p.getMessage()));
        } catch (IOException e) {
            String from = server ? "Server" : "Client";
            System.err.printf("Thread %d from Hello%s can't send data: %s%n", cur_thread, from, e.getMessage());
        }
    }

    protected static String bytesToString(ByteBuffer buffer) {
        byte[] bytes = new byte[buffer.flip().remaining()];
        buffer.get(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    protected static void handleSelectedKeys(Selector selector, ByteBuffer buffer,
                                             InetSocketAddress address, String prefix, SupportConnect connector) {
        for (final Iterator<SelectionKey> i = selector.selectedKeys().iterator(); i.hasNext(); ) {
            final SelectionKey key = i.next();
            if (key.isValid()) {
                if (key.isReadable()) {
                    connector.receive(key, buffer, prefix);
                }

                if (key.isValid() && key.isWritable()) {
                    connector.write(key, address, prefix);
                }
            }
            i.remove();
        }
    }
}
