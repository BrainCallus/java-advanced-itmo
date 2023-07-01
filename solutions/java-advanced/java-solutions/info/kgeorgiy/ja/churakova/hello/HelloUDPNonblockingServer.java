package info.kgeorgiy.ja.churakova.hello;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.concurrent.Executors;

import static info.kgeorgiy.ja.churakova.hello.Utilits.*;

public class HelloUDPNonblockingServer extends AbstractServer implements SupportConnect {
    private Selector selector;

    private static class Attachment {
        SocketAddress address;
        ByteBuffer buffer;
    }

    @Override
    protected void serveExecutorsTask(int cur_thread) {
        try {
            ByteBuffer buffer = ByteBuffer.allocate(DEFAULT_BUF_SIZE);
            while (!Thread.interrupted()) {
                selector.select(TIMEOUT);
                handleSelectedKeys(selector, buffer, null, null, this);
            }
        } catch (IOException e) {
            System.err.printf("IOException occur during selecting keys%n%s%n", e.getMessage());
        }
    }

    @Override
    protected int initResources(int port, int thread_cnt) {
        try {
            selector = Selector.open();
            DatagramChannel channel = DatagramChannel.open();
            addInheritorElements(selector, channel);
            channel.configureBlocking(false);
            channel.bind(new InetSocketAddress(port));
            channel.register(selector, SelectionKey.OP_READ, new Attachment());
            executors = Executors.newSingleThreadExecutor();
            return 1;
        } catch (IOException e) {
            System.err.printf("IOException occur during selecting keys%n%s%n", e.getMessage());
        }
        return -1;
    }

    /**
     * Receives client's request
     *
     * @param key    selected key
     * @param buffer buffer to receive request
     * @param prefix string prefix, in that case null
     */
    @Override
    public void receive(SelectionKey key, ByteBuffer buffer, String prefix) {
        final DatagramChannel receiver = (DatagramChannel) key.channel();
        buffer.clear();
        Attachment attachment = (Attachment) key.attachment();
        try {
            attachment.address = receiver.receive(buffer);

            if (attachment.address != null) {
                byte[] responseBytes = getResponseBytes(bytesToString(buffer));
                attachment.buffer = ByteBuffer.allocate(responseBytes.length);
                attachment.buffer.put(responseBytes);

                key.interestOps(SelectionKey.OP_WRITE);
            }
        } catch (IOException e) {
            System.err.printf("IOException! Can't get receive request%n%s%n", e.getMessage());
        }
    }

    /**
     * Writes and send response to client
     *
     * @param key     selected key
     * @param address socket address, not used here
     * @param prefix  request prefix, not used here
     */
    @Override
    public void write(SelectionKey key, InetSocketAddress address, String prefix) {
        final DatagramChannel sender = (DatagramChannel) key.channel();
        Attachment attachment = (Attachment) key.attachment();
        attachment.buffer.flip();
        try {
            sender.send(attachment.buffer, attachment.address);
            key.interestOps(SelectionKey.OP_READ);
        } catch (IOException e) {
            System.err.printf("IOException! Can't send response%n%s%n", e.getMessage());
        }
    }


    /**
     * An entry point starts Hello server with <port> <threads>
     */
    public static void main(String[] args) {
        AbstractServer.init(new HelloUDPNonblockingServer(), args);
    }
}
