/* Copyright Samuel Halliday 2012 */
package org.openyou;

import com.codeminders.hidapi.ClassPathLibraryLoader;
import com.codeminders.hidapi.HIDDevice;
import com.codeminders.hidapi.HIDDeviceInfo;
import com.codeminders.hidapi.HIDManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author Sam Halliday
 */
class EmotiveHid {
    private static final List<byte[]> supported = new ArrayList<byte[]>();
    private static final Logger log = Logger.getLogger(EmotiveHid.class.getName());

    static {
        try {
//            System.loadLibrary("hidapi-jni");
            ClassPathLibraryLoader.loadNativeHIDLibrary();
            supported.add(new byte[]{33, -1, 31, -1, 30, 0, 0, 0});
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static final int VENDOR_ID = 8609;
    private static final int PRODUCT_ID = 1;
    private static final int BUFSIZE = 32;

    public static void main(String[] args) throws IOException {

        EmotiveHid tester = new EmotiveHid();

        HIDDevice emotive = tester.findEmotive();

        byte[] buf = new byte[BUFSIZE];
        int n = 0;
        emotive.enableBlocking();
        while (n != -1) {
            n = emotive.read(buf);
            for (int i = 0; i < n; i++) {
                int v = buf[i] & 0xFF;
                String hs = Integer.toHexString(v);
                System.out.print(hs + " ");
            }
            System.err.println("");
        }

        System.exit(0);
    }

    public List<HIDDeviceInfo> findDevices(int vendor, int product) throws IOException {
        HIDManager manager = HIDManager.getInstance();
        HIDDeviceInfo[] infos = manager.listDevices();
        List<HIDDeviceInfo> devs = new ArrayList<HIDDeviceInfo>(infos.length);
        for (HIDDeviceInfo info : infos) {
            if (info.getVendor_id() == vendor && info.getProduct_id() == product)
                devs.add(info);
        }
        return devs;
    }

    public HIDDevice findEmotive() throws IOException {
        List<HIDDeviceInfo> infos = findDevices(VENDOR_ID, PRODUCT_ID);
        for (HIDDeviceInfo info : infos) {
            HIDDevice dev = info.open();
            try {
                byte[] report = new byte[9];
                int size = dev.getFeatureReport(report);
                byte[] result = Arrays.copyOf(report, size);
                log.info("Found " + dev.getProductString() + " with report: " + Arrays.toString(result));
                for (byte[] check : supported) {
                    if (Arrays.equals(check, result))
                        return dev;
                }
                dev.close();
            } catch (Exception e) {
                dev.close();
            }
        }
        throw new UnsupportedOperationException("Create a support request with your 'report' info (above) on https://github.com/openyou/emokit/issues");
    }


}
