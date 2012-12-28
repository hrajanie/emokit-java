/* Copyright Samuel Halliday 2012 */
package org.openyou;

import fommil.utils.ProducerConsumer;
import lombok.extern.java.Log;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

/**
 * Unencrypted access to an Emotive EEG.
 * <p/>
 * The device is constantly polled in a background thread,
 * filling up a buffer (which could cause the application
 * to OutOfMemory if not evacuated).
 *
 * @author Sam Halliday
 */
@Log
public final class Emotive implements Iterable<Packet>, Closeable {

    public static void main(String[] args) throws Exception {
        Emotive emotive = new Emotive();
        for (Packet packet : emotive) {
            log.info(packet.toString());
        }
    }

    private final EmotiveHid raw;
    private final AtomicBoolean accessed = new AtomicBoolean();
    private final Cipher cipher;

    /**
     * @throws IOException if there was a problem discovering the device.
     */
    public Emotive() throws IOException {
        raw = new EmotiveHid();
        try {
            cipher = Cipher.getInstance("AES/ECB/NoPadding");
            SecretKeySpec key = raw.getKey();
            cipher.init(Cipher.DECRYPT_MODE, key);
        } catch (Exception e) {
            throw new IllegalStateException("no javax.crypto support");
        }
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
                    byte[] bytes = new byte[EmotiveHid.BUFSIZE];
                    while (!iterator.stopped()) {
                        raw.poll(bytes);
                        long start = System.currentTimeMillis();
                        byte[] decrypted = decrypt(bytes);
                        assert decrypted != bytes;
                        Packet packet = Packet.fromBytes(decrypted);
                        iterator.produce(packet);
                        long end = System.currentTimeMillis();
                        // TODO: analysis of crypto vs Hz of device
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

    // 128-bit AES in ECB mode, block size of 16 bytes.
    protected byte[] decrypt(byte[] packet) {
        try {
            return cipher.doFinal(packet);
        } catch (Exception e) {
            throw new IllegalArgumentException(Arrays.toString(packet), e);
        }
    }
}
