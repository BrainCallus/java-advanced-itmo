package info.kgeorgiy.ja.churakova.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Phaser;

import static info.kgeorgiy.ja.churakova.hello.Utilits.*;


public class HelloUDPClient extends AbstractClient implements HelloClient {


    /**
     * Default constructor;
     */
    public HelloUDPClient() {

    }

    @Override
    protected void runImpl(InetSocketAddress address, String prefix, int thread_cnt, int requests) {
        ExecutorService executors = Executors.newFixedThreadPool(thread_cnt);
        Phaser phaser = new Phaser(1);
        for (int i = 1; i < thread_cnt + 1; i++) {
            final int cur_thread = i;
            phaser.register();
            executors.submit(() -> clientExecutorTask(address, phaser, prefix, requests, cur_thread));
        }
        phaser.arriveAndAwaitAdvance();
        executors.shutdown();
    }


    private void clientExecutorTask(SocketAddress adress, Phaser phaser, String prefix, int requests, int cur_thread) {
        try (DatagramSocket datagramSocket = new DatagramSocket()) {
            DatagramPacket responsePack = getDatagramPack(datagramSocket);
            for (int cur_req = 1; cur_req < requests + 1; cur_req++) {
                String request = getRequestString(prefix, cur_thread, cur_req);
                DatagramPacket requestPack = new DatagramPacket(request.getBytes(StandardCharsets.UTF_8), request.length(), adress);
                datagramSocket.setSoTimeout(TIMEOUT);
                int attempts = 1;
                while (!datagramSocket.isClosed()) {
                    try {
                        sendData(datagramSocket, requestPack, cur_thread, false);
                        String response = generateResponse(datagramSocket, responsePack);
                        if (response.contains(request)) {
                            System.out.println(response);
                            attempts = 1;
                            break;
                        }

                    } catch (SocketTimeoutException time) {
                        System.err.printf("Thread %d did not get response in %d ms: %s%n", cur_thread, datagramSocket.getSoTimeout() * attempts, time.getMessage());
                        attempts++;
                    } catch (PortUnreachableException p) {
                        System.err.print(getUnreachablePortMessage(datagramSocket.getPort(), p.getMessage()));
                    } catch (IOException e) {
                        System.err.println(e.getMessage());
                    }
                }
            }
        } catch (SocketException sock) {
            System.err.print(getSocketExceptionMessage(sock.getMessage()));
        } finally {
            phaser.arriveAndDeregister();
        }
    }


    /**
     * An entry point runs Hello client with <host> <prefix> <port> <number of threads> <number of requests>
     */
    public static void main(String[] args) {
        AbstractClient.init(new HelloUDPClient(), args);
    }
}
