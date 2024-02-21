package info.kgeorgiy.ja.churakova.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Objects;

import static info.kgeorgiy.ja.churakova.hello.Utilits.*;

public abstract class AbstractClient implements HelloClient {
    /**
     * Default constructor
     */
    public AbstractClient() {

    }

    protected static void init(HelloClient client, String[] args) {
        if (!verifyArgs(args, 5)) {
            return;
        }
        try {
            String nameIP = args[0];
            String prefix = args[2];
            int[] arguments = requirePositive(Integer.parseInt(args[1]),
                    Integer.parseInt(args[3]), Integer.parseInt(args[4]));
            Objects.requireNonNull(arguments);
            client.run(nameIP, arguments[0], prefix, arguments[1], arguments[2]);
        } catch (NumberFormatException num) {
            System.err.println("Can't parse arguments");
        }
    }

    /**
     * Runs Hello client.
     * This method should return when all requests are completed.
     *
     * @param host       server host
     * @param port       server port
     * @param prefix     request prefix
     * @param thread_cnt number of request threads
     * @param requests   number of requests per thread.
     */
    @Override
    public void run(String host, int port, String prefix, int thread_cnt, int requests) {
        try {
            runImpl(new InetSocketAddress(InetAddress.getByName(host), port), prefix, thread_cnt, requests);
        } catch (UnknownHostException h) {
            System.err.printf("Host %s not found: %s%n", host, h.getMessage());
        } catch (IllegalArgumentException il) {
            System.err.print(getPortOutOfRangeMessage(port, il.getMessage()));
        }
    }

    protected String getRequestString(String prefix, int threadId, int request) {
        return String.format("%s%d_%d", prefix, threadId, request);
    }

    protected abstract void runImpl(InetSocketAddress address, String prefix, int threads_cnt, int requests);
}
