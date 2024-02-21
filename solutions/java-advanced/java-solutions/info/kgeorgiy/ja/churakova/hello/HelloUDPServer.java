package info.kgeorgiy.ja.churakova.hello;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.PortUnreachableException;
import java.net.SocketException;
import java.util.concurrent.Executors;

import static info.kgeorgiy.ja.churakova.hello.Utilits.*;


public class HelloUDPServer extends AbstractServer {
    private DatagramSocket datagramSocket;

    /**
     * Default constructor
     */
    public HelloUDPServer() {
    }

    @Override
    protected void serveExecutorsTask(int cur_thread) {
        try {
            DatagramPacket responsePack = getDatagramPack(datagramSocket);

            while (!datagramSocket.isClosed()) {
                try {
                    String response = generateResponse(datagramSocket, responsePack);
                    responsePack.setData(getResponseBytes(response));
                    sendData(datagramSocket, responsePack, cur_thread, true);
                } catch (PortUnreachableException p) {
                    System.err.print(getUnreachablePortMessage(datagramSocket.getPort(), p.getMessage()));
                } catch (IOException e) {
                    if (!datagramSocket.isClosed()) {
                        System.err.println(e.getMessage());
                    }
                }
            }
        } catch (SocketException sock) {
            System.err.print(getSocketExceptionMessage(sock.getMessage()));
        }
    }

    @Override
    protected int initResources(int port, int thread_cnt) {
        try {
            datagramSocket = new DatagramSocket(port);
            addInheritorElements(datagramSocket);
            executors = Executors.newFixedThreadPool(thread_cnt);
            return thread_cnt;
        } catch (SocketException sock) {
            System.err.print(getSocketExceptionMessage(sock.getMessage()));
        } catch (IllegalArgumentException il) {
            System.err.print(getPortOutOfRangeMessage(port, il.getMessage()));
        }
        return -1;
    }


    /**
     * An entry point starts Hello server with <port> <threads>
     */
    public static void main(String[] args) {
        AbstractServer.init(new HelloUDPServer(), args);
    }
}
