package info.kgeorgiy.ja.churakova.hello;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.StandardCharsets;

import static info.kgeorgiy.ja.churakova.hello.Utilits.*;

public class HelloUDPNonblockingClient extends AbstractClient implements SupportConnect {

    /**
     * Default constructor
     */
    public HelloUDPNonblockingClient() {
    }

    @Override
    protected void runImpl(InetSocketAddress address, String prefix, int threads, int requests) {
        try {
            Selector selector = Selector.open();

            for (int i = 1; i <= threads; i++) {
                DatagramChannel channel = DatagramChannel.open();
                channel.configureBlocking(false);
                channel.connect(address);
                channel.register(selector, SelectionKey.OP_WRITE, new int[]{i, 1, requests + 1});
            }

            ByteBuffer buffer = ByteBuffer.allocate(DEFAULT_BUF_SIZE);

            while (!Thread.interrupted() && !selector.keys().isEmpty()) {
                selector.select(TIMEOUT);
                if (selector.selectedKeys().isEmpty()) {
                    selector.keys().forEach(key -> key.interestOps(SelectionKey.OP_WRITE));
                } else {
                    handleSelectedKeys(selector, buffer, address, prefix, this);
                }
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }


    /**
     * Writes and sends request to server
     *
     * @param key     selected key
     * @param address socket addres
     * @param prefix  string prefix for request
     */
    public void write(SelectionKey key, InetSocketAddress address, String prefix) {
        final DatagramChannel sender = (DatagramChannel) key.channel();
        int[] attachment = (int[]) key.attachment();
        byte[] request = getRequestString(prefix, attachment[0], attachment[1]).getBytes(StandardCharsets.UTF_8);
        ByteBuffer buffer = ByteBuffer.allocate(request.length);
        buffer.put(request);
        buffer.flip();
        try {
            if (sender.send(buffer, address) == request.length) {
                key.interestOps(SelectionKey.OP_READ);
            }
        } catch (IOException e) {
            System.err.printf("IOException! Can't send request%n%s%n", e.getMessage());
        }

    }

    /**
     * Receives server response
     *
     * @param key    selected key
     * @param buffer buffer for response
     * @param prefix string prefix for response and request
     */
    public void receive(SelectionKey key, ByteBuffer buffer, String prefix) {

        final DatagramChannel receiver = (DatagramChannel) key.channel();
        int[] attachment = (int[]) key.attachment();
        try {
            SocketAddress socketAddress = receiver.receive(buffer.clear());
            if (socketAddress != null) {
                String response = bytesToString(buffer);
                if (response.contains(getRequestString(prefix, attachment[0], attachment[1]))) {
                    System.out.println(response);
                    if (++attachment[1] == attachment[2]) {
                        receiver.close();
                        key.cancel();
                        return;
                    }
                }
            }
        } catch (IOException e) {
            System.err.printf("IOException! Can't receive response%n%s%n", e.getMessage());
        }

        key.interestOps(SelectionKey.OP_WRITE);
    }


    /**
     * An entry point runs Hello client with <host> <prefix> <port> <number of threads> <number of requests>
     */

    public static void main(String[] args) {
        AbstractClient.init(new HelloUDPNonblockingClient(), args);
    }
}
