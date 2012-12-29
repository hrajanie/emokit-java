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
            //log.info(packet.toString());
            log.info(String.format("BATTERY %s%%", emotive.getBatteryLevel()));
        }
    }

    private final EmotiveHid raw;
    private final AtomicBoolean accessed = new AtomicBoolean();
    private final Cipher cipher;

    private volatile int battery;

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
                    byte lastCounter = -1;
                    while (!iterator.stopped()) {
                        raw.poll(bytes);
                        long start = System.currentTimeMillis();
                        byte[] decrypted = decrypt(bytes);
                        byte counter = decrypted[0];
                        if (counter != lastCounter + 1 && lastCounter != 127)
                            log.config("missed a packet");
                        if (counter < 0) {
                            lastCounter = -1;
                            battery = 0xFF & counter;
                        } else {
                            lastCounter = counter;
                        }
                        Packet packet = new Packet(start, decrypted);
                        iterator.produce(packet);
                        long end = System.currentTimeMillis();
                        log.info("@bschumacher: " + (end - start));
                        if ((end - start) > 7) {
                            log.severe("Decryption is unsustainable on your platform: " + (end - start));
                        } else if ((end - start) > 5) {
                            log.info("Decryption took a worryingly long time: " + (end - start));
                        }
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

    /**
     * @return [0, 100] the percentage level of the battery.
     */
    public int getBatteryLevel() {
        int snapshot = battery;
        if (snapshot >= 248) return 100;
        switch (snapshot) {
            case 247:
                return 99;
            case 246:
                return 97;
            case 245:
                return 93;
            case 244:
                return 89;
            case 243:
                return 85;
            case 242:
                return 82;
            case 241:
                return 77;
            case 240:
                return 72;
            case 239:
                return 66;
            case 238:
                return 62;
            case 237:
                return 55;
            case 236:
                return 46;
            case 235:
                return 32;
            case 234:
                return 20;
            case 233:
                return 12;
            case 232:
                return 6;
            case 231:
                return 4;
            case 230:
                return 3;
            case 229:
                return 2;
            case 228:
            case 227:
            case 226:
                return 1;
            default:
                return 0;
        }
    }

    @Override
    public void close() throws IOException {
        raw.close();
    }

    protected byte[] decrypt(byte[] packet) {
        try {
            return cipher.doFinal(packet);
        } catch (Exception e) {
            throw new IllegalArgumentException(Arrays.toString(packet), e);
        }
    }
}
