package info.kgeorgiy.ja.churakova.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static info.kgeorgiy.ja.churakova.hello.Utilits.*;

public abstract class AbstractServer implements HelloServer {
    /**
     * Default constructor
     */
    public AbstractServer() {}

    protected ExecutorService executors;
    protected List<Closeable> closeableSources = new ArrayList<>();

    protected static void init(HelloServer server, String[] args) {
        if (!verifyArgs(args, 2)) {
            return;
        }

        int[] arguments = requirePositive(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
        Objects.requireNonNull(arguments);
        server.start(arguments[0], arguments[1]);
        while (!Thread.currentThread().isInterrupted()) ;
        server.close();
    }

    protected void addInheritorElements(Closeable... sources) {
        closeableSources.addAll(Arrays.asList(sources));
    }


    /**
     * Starts a new Hello server.
     * This method should return immediately.
     *
     * @param port       server port.
     * @param thread_cnt number of working threads.
     */
    @Override
    public void start(int port, int thread_cnt) {
        int threads = initResources(port, thread_cnt);
        if (threads != -1) {
            for (int i = 0; i < threads; i++) {
                final int cur_thread = i;
                executors.submit(() -> serveExecutorsTask(cur_thread));
            }
        }
    }

    protected abstract void serveExecutorsTask(int cur_thread);

    protected abstract int initResources(int port, int thread_cnt);

    @Override
    public void close() {
        shutdown();
        closeCloseable();
    }

    private void closeCloseable() {
        for (Closeable source : closeableSources) {
            try {
                source.close();
            } catch (IOException e) {
                System.err.printf("Unable to close resources%n%s%n", e.getMessage());
            }
        }
    }

    private void shutdown() {
        executors.shutdown();
        try {
            if (!executors.awaitTermination(TIMEOUT, TimeUnit.MILLISECONDS)) {
                executors.shutdownNow();
                if (!executors.awaitTermination(2 * TIMEOUT, TimeUnit.MILLISECONDS)) {
                    System.err.println("Can't shutdown executors");
                }
            }
        } catch (InterruptedException e) {
            executors.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    protected byte[] getResponseBytes(String response) {
        return ("Hello, " + response).getBytes(StandardCharsets.UTF_8);
    }
}
