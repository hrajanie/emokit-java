/* Copyright Samuel Halliday 2012 */
package org.openyou;

import fommil.utils.ProducerConsumer;
import lombok.extern.java.Log;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

/**
 * Unencrypted access to an Emotive EEG.
 * <p>
 * The device is constantly polled in a background thread,
 * filling up a buffer (which could cause the application
 * to OutOfMemory if not evacuated).
 *
 * @author Sam Halliday
 */
@Log
public final class Emotive implements Iterable<Emotive.Packet>, Closeable {

    public static void main(String[] args) throws Exception {
        Emotive emotive = new Emotive();
        for (Packet packet : emotive) {
            log.info(packet.toString());
        }
    }


    public static final class Packet {

        protected static Packet fromBytes(byte[] raw) {
            return new Packet();
        }

        @Override
        public String toString() {
            return "dummy";
        }
    }

    private final EmotiveHid raw;
    private final AtomicBoolean accessed = new AtomicBoolean();

    /**
     * @throws IOException if there was a problem discovering the device.
     */
    public Emotive() throws IOException {
        raw = new EmotiveHid();
    }

    /**
     * Can only be called once.
     *
     * @return a one-shot iterator.
     */
    public ProducerConsumer<Packet> iterator() {
        if (accessed.getAndSet(true))
            throw new IllegalStateException("Cannot be called more than once.");

        final ProducerConsumer<Packet> iterator = new ProducerConsumer<Packet>();

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    while (!iterator.stopped()) {
                        byte[] bytes = raw.poll(); // TODO: reuse the buffer
                        byte[] decrypted = decrypt(bytes);
                        Packet packet = Packet.fromBytes(decrypted);
                        iterator.produce(packet);
                    }
                } catch (Exception e) {
                    log.logp(Level.SEVERE, "Emotive.class.getName()", "iterator", "Problem when polling", e);
                    try {
                        close();
                    } catch (IOException ignored) {
                    }
                }
            }
        };

        Thread thread = new Thread(runnable, "Emotive polling and decryption");
        thread.setDaemon(true);
        thread.start();
        return iterator;
    }

    @Override
    public void close() throws IOException {
        raw.close();
    }

    protected byte[] decrypt(byte[] packet) {
        return packet;
    }
}
