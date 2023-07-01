package info.kgeorgiy.ja.churakova.hello;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;

public interface SupportConnect {
    void receive(SelectionKey key, ByteBuffer buffer, String prefix);

    void write(SelectionKey key, InetSocketAddress address, String prefix);
}
