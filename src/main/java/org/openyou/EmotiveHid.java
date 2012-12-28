/* Copyright Samuel Halliday 2012 */
package org.openyou;

import com.codeminders.hidapi.*;
import lombok.extern.java.Log;

import javax.crypto.spec.SecretKeySpec;
import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeoutException;

import static java.lang.String.format;

/**
 * Wrapper for the low level HIDAPI to access an Emotive EEG.
 * <p/>
 * Supported devices are discovered on construction and a
 * poll is provided to obtain raw packets.
 *
 * @author Sam Halliday
 */
@Log
final class EmotiveHid implements Closeable {
    static final int VENDOR_ID = 8609;
    static final int PRODUCT_ID = 1;
    static final int BUFSIZE = 32; // at 128hz
    static final int TIMEOUT = 1000;

    static final boolean research = false;

    private static final List<byte[]> supported = new ArrayList<byte[]>();

    static {
        try {
            ClassPathLibraryLoader.loadNativeHIDLibrary();
            supported.add(new byte[]{33, -1, 31, -1, 30, 0, 0, 0});
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private final HIDDevice emotive;

    public EmotiveHid() throws IOException {
        emotive = findEmotive();
        emotive.enableBlocking();
    }

    @Override
    public void close() throws IOException {
        emotive.close();
    }

    @Override
    public void finalize() throws Throwable {
        synchronized (this) {
            close();
            super.finalize();
        }
    }

    /**
     * Forwards to {@link #poll(byte[])} with a newly allocated buffer.
     */
    public byte[] poll() throws TimeoutException, IOException {
        return poll(new byte[BUFSIZE]);
    }

    /**
     * @param buf use the supplied buffer.
     * @throws java.io.IOException if there was no response from the Emotive.
     * @throws TimeoutException    which may indicate that the Emotive is not connected.
     */
    public byte[] poll(byte[] buf) throws TimeoutException, IOException {
        assert buf.length == BUFSIZE;
        int n = emotive.readTimeout(buf, TIMEOUT);
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
        String serial = emotive.getSerialNumberString();
        if (!serial.startsWith("SN") || serial.length() != 16)
            throw new IOException("Bad serial: " + serial);

        byte[] raw = serial.getBytes();
        assert raw.length == 16;
        byte[] bytes = new byte[16];
//        bytes[0] = raw[15];
//        bytes[1] = 0;
//        bytes[2] = raw[14];
//        bytes[3] = research ? (byte) 54 : 48;
//        bytes[4] = raw[13];
//        bytes[5] = research ? (byte) 10 : 0;
//        bytes[6] = raw[12];
//        bytes[7] = research ? (byte) 42 : 54;
//        bytes[8] = raw[15];
//        bytes[9] = research ? (byte) 0 : 10;
//        bytes[10] = raw[14];
//        bytes[11] = research ? (byte) 48 : 42;
//        bytes[12] = raw[13];
//        bytes[13] = 0;
//        bytes[14] = raw[12];
//        bytes[15] = 50;

        bytes[0] = raw[15];
        bytes[1] = 0;
        bytes[2] = raw[14];
        bytes[3] = research ? 'H' : 'T';
        bytes[4] = research ? raw[15]: raw[13];
        bytes[5] = research ? 0 : 10;
        bytes[6] = research ? raw[14]: raw[12];
        bytes[7] = research ? 'T' : 'B';
        bytes[8] = research ? raw[13]: raw[15];
        bytes[9] = research ? 10 : 0;
        bytes[10] = research ? raw[12]: raw[14];
        bytes[11] = research ? 'B' : 'H';
        bytes[12] = raw[13];
        bytes[13] = 0;
        bytes[14] = raw[12];
        bytes[15] = 'P';

        return new SecretKeySpec(bytes, "AES");
    }

    // workaround http://code.google.com/p/javahidapi/issues/detail?id=40
    private List<HIDDeviceInfo> findDevices(int vendor, int product) throws IOException {
        HIDManager manager = HIDManager.getInstance();
        HIDDeviceInfo[] infos = manager.listDevices();
        List<HIDDeviceInfo> devs = new ArrayList<HIDDeviceInfo>(infos.length);
        for (HIDDeviceInfo info : infos) {
            if (info.getVendor_id() == vendor && info.getProduct_id() == product)
                devs.add(info);
        }
        return devs;
    }

    private HIDDevice findEmotive() throws IOException {
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
                for (byte[] check : supported) {
                    if (Arrays.equals(check, result))
                        return dev;
                }
                dev.close();
            } catch (Exception e) {
                dev.close();
            }
        }
        throw new HIDDeviceNotFoundException("Send all this information to https://github.com/openyou/emokit/issues");
    }

}
