/* Copyright Samuel Halliday 2012 */
package org.openyou;

import com.codeminders.hidapi.*;
import lombok.extern.java.Log;

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
    static final int BUFSIZE = 32;
    static final int TIMEOUT = 1000;

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
