// Copyright Samuel Halliday 2012
package org.openyou;

import com.codeminders.hidapi.*;
import com.google.common.collect.Lists;
import lombok.extern.java.Log;

import javax.annotation.concurrent.NotThreadSafe;
import javax.crypto.spec.SecretKeySpec;
import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeoutException;

import static java.lang.String.format;

/**
 * Wrapper for the low level HIDAPI to access an Emotiv EEG.
 * <p/>
 * Supported devices are discovered on construction and a
 * poll is provided to obtain raw packets.
 *
 * @author Sam Halliday
 */
@Log
@NotThreadSafe
final class EmotivHid implements Closeable {
    static final int VENDOR_ID = 8609;
    static final int PRODUCT_ID = 1;
    static final int BUFSIZE = 32; // at 128hz
    static final int TIMEOUT = 1000;

    private static final List<byte[]> supportedResearch = Lists.newArrayList();
    private static final List<byte[]> supportedConsumer = Lists.newArrayList();

    static {
        try {
            ClassPathLibraryLoader.loadNativeHIDLibrary();
            supportedConsumer.add(new byte[]{33, -1, 31, -1, 30, 0, 0, 0});
            supportedConsumer.add(new byte[]{32, -1, 31, -1, 30, 0, 0, 0});
            supportedConsumer.add(new byte[]{-32, -1, 31, -1, 0, 0, 0, 0}); // unconfirmed
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private volatile boolean research = false;
    private final HIDDevice device;

    public EmotivHid() throws IOException {
        device = findEmotiv();
        device.enableBlocking();
    }

    @Override
    public void close() throws IOException {
        device.close();
    }

    @Override
    public void finalize() throws Throwable {
        synchronized (this) {
            close();
            super.finalize();
        }
    }

    /**
     * @param buf use the supplied buffer.
     * @throws java.io.IOException if there was no response from the Emotiv.
     * @throws TimeoutException    which may indicate that the Emotiv is not connected.
     */
    public byte[] poll(byte[] buf) throws TimeoutException, IOException {
        assert buf.length == BUFSIZE;
        int n = device.readTimeout(buf, TIMEOUT);
        if (n == 0)
            throw new TimeoutException("No response.");
        if (n != BUFSIZE)
            throw new IOException(format("Bad Packet: (%s) %s", n, Arrays.toString(buf)));
        return buf;
    }

    /**
     * @return the crypto key for this device.
     * @throws IOException
     */
    public SecretKeySpec getKey() throws IOException {
        String serial = getSerial();

        byte[] raw = serial.getBytes();
        assert raw.length == 16;
        byte[] bytes = new byte[16];

        bytes[0] = raw[15];
        bytes[1] = 0;
        bytes[2] = raw[14];
        bytes[3] = research ? (byte)'H' : (byte)'T';
        bytes[4] = research ? raw[15] : raw[13];
        bytes[5] = research ? (byte)0 : 16;
        bytes[6] = research ? raw[14] : raw[12];
        bytes[7] = research ? (byte)'T' : (byte)'B';
        bytes[8] = research ? raw[13] : raw[15];
        bytes[9] = research ? (byte)16 : 0;
        bytes[10] = research ? raw[12] : raw[14];
        bytes[11] = research ? (byte)'B' : (byte)'H';
        bytes[12] = raw[13];
        bytes[13] = 0;
        bytes[14] = raw[12];
        bytes[15] = 'P';

        return new SecretKeySpec(bytes, "AES");
    }

    /**
     * @return
     */
    public String getSerial() throws IOException {
        String serial = device.getSerialNumberString();
        if (!serial.startsWith("SN") || serial.length() != 16)
            throw new IOException("Bad serial: " + serial);
        return serial;
    }

    // workaround http://code.google.com/p/javahidapi/issues/detail?id=40
    private List<HIDDeviceInfo> findDevices(int vendor, int product) throws IOException {
        HIDManager manager = HIDManager.getInstance();
        HIDDeviceInfo[] infos = manager.listDevices();
        List<HIDDeviceInfo> devs = Lists.newArrayList();
        for (HIDDeviceInfo info : infos) {
            if (info.getVendor_id() == vendor && info.getProduct_id() == product)
                devs.add(info);
        }
        return devs;
    }

    private HIDDevice findEmotiv() throws IOException {
        List<HIDDeviceInfo> infos = findDevices(VENDOR_ID, PRODUCT_ID);
        for (HIDDeviceInfo info : infos) {
            HIDDevice dev = info.open();
            try {
                byte[] report = new byte[9];
                int size = dev.getFeatureReport(report);
                byte[] result = Arrays.copyOf(report, size);
                log.info(format("Found (%s) %s [%s] with report: %s",
                        dev.getManufacturerString(),
                        dev.getProductString(),
                        dev.getSerialNumberString(),
                        Arrays.toString(result)));
                for (byte[] check : supportedConsumer) {
                    if (Arrays.equals(check, result)) {
                        return dev;
                    }
                }
                for (byte[] check : supportedResearch) {
                    if (Arrays.equals(check, result)) {
                        research = true;
                        return dev;
                    }
                }
                dev.close();
            } catch (Exception e) {
                dev.close();
            }
        }
        throw new HIDDeviceNotFoundException("Send all this information to https://github.com/fommil/emokit-java/issues and let us know if you have the 'research' or 'consumer' product.");
    }

}
