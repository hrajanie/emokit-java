/* Copyright Samuel Halliday 2012 */
package org.openyou;

import fommil.utils.ProducerConsumer;
import lombok.extern.java.Log;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.Closeable;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

/**
 * Unencrypted access to an Emotiv EEG.
 * <p/>
 * The device is constantly polled in a background thread,
 * filling up a buffer (which could cause the application
 * to OutOfMemory if not evacuated).
 *
 * @author Sam Halliday
 */
@Log
public final class Emotiv implements Iterable<Packet>, Closeable {

    public static void main(String[] args) throws Exception {
        Emotiv emotiv = new Emotiv();
        for (Packet packet : emotiv) {
            Emotiv.log.info(packet.toString());
        }
    }

    private final EmotivHid raw;
    private final AtomicBoolean accessed = new AtomicBoolean();
    private final Cipher cipher;
    private final Map<Packet.Sensor, Integer> quality = new EnumMap<Packet.Sensor, Integer>(Packet.Sensor.class);

    private volatile int battery;

    /**
     * @throws IOException if there was a problem discovering the device.
     */
    public Emotiv() throws IOException {
        raw = new EmotivHid();
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
                    byte[] bytes = new byte[EmotivHid.BUFSIZE];
                    byte lastCounter = -1;
                    while (!iterator.stopped()) {

                        raw.poll(bytes);

                        long start = System.currentTimeMillis();
                        byte[] decrypted = cipher.doFinal(bytes);

                        // the counter is used to mixin battery and quality levels
                        byte counter = decrypted[0];
                        if (counter != lastCounter + 1 && lastCounter != 127)
                            Emotiv.log.config("missed a packet");

                        if (counter < 0) {
                            lastCounter = -1;
                            battery = 0xFF & counter;
                        } else {
                            lastCounter = counter;
                        }

                        Packet.Sensor channel = getQualityChannel(counter);
                        if (channel != null) {
                            int reading = Packet.Sensor.QUALITY.apply(decrypted);
                            quality.put(channel, reading);
                        }

                        Packet packet = new Packet(start, battery, decrypted, new EnumMap<Packet.Sensor, Integer>(quality));
                        iterator.produce(packet);

                        long end = System.currentTimeMillis();
                        if ((end - start) > 7) {
                            Emotiv.log.severe("Decryption is unsustainable on your platform: " + (end - start));
                        } else if ((end - start) > 4) {
                            Emotiv.log.info("Decryption took a worryingly long time: " + (end - start));
                        }
                    }
                } catch (Exception e) {
                    Emotiv.log.logp(Level.SEVERE, Emotiv.class.getName(), "iterator", "Problem when polling", e);
                    try {
                        close();
                    } catch (IOException ignored) {
                    }
                }
            }
        };

        Thread thread = new Thread(runnable, "Emotiv polling and decryption");
        thread.setDaemon(true);
        thread.start();
        return iterator;
    }

    private Packet.Sensor getQualityChannel(byte counter) {
        if (64 <= counter && counter <= 75) {
            counter = (byte) (counter - 64);
        } else if (76 <= counter) {
            // https://github.com/openyou/emokit/issues/56
            counter = (byte) ((counter - 76) % 4 + 15);
        }
        switch (counter) {
            case 0:
                return Packet.Sensor.F3;
            case 1:
                return Packet.Sensor.FC5;
            case 2:
                return Packet.Sensor.AF3;
            case 3:
                return Packet.Sensor.F7;
            case 4:
                return Packet.Sensor.T7;
            case 5:
                return Packet.Sensor.P7;
            case 6:
                return Packet.Sensor.O1;
            case 7:
                return Packet.Sensor.O2;
            case 8:
                return Packet.Sensor.P8;
            case 9:
                return Packet.Sensor.T8;
            case 10:
                return Packet.Sensor.F8;
            case 11:
                return Packet.Sensor.AF4;
            case 12:
                return Packet.Sensor.FC6;
            case 13:
                return Packet.Sensor.F4;
            case 14:
                return Packet.Sensor.F8;
            case 15:
                return Packet.Sensor.AF4;
            default:
                return null;
        }
    }

    @Override
    public void close() throws IOException {
        raw.close();
    }
}
