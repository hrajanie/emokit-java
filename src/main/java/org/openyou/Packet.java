/* Copyright Samuel Halliday 2012 */
package org.openyou;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;

import javax.annotation.concurrent.Immutable;
import java.util.Date;
import java.util.Map;

/**
 * Contains the sensor data from a single packet of data.
 * <p/>
 * This is not designed for easy persistence: clients are
 * advised to use their own persistent format or convert
 * to {@link EmotivDatum}.
 *
 * @author Sam Halliday
 * @see <a href="https://github.com/openyou/emokit/blob/master/doc/emotiv_protocol.asciidoc">Emotiv Protocol</a>
 */
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@Log
@Immutable
@EqualsAndHashCode
public final class Packet {

    private final long timestamp;
    private final int battery;
    private final byte[] frame;
    private final Map<Sensor, Integer> quality;

    public Date getDate() {
        return new Date(timestamp);
    }

    public Integer getGyroX() {
        return 0xFF & frame[29];// - 102;
    }

    public Integer getGyroY() {
        return 0xFF & frame[30];// - 104;
    }

    public Integer getSensor(Sensor sensor) {
        if (sensor == Sensor.QUALITY)
            throw new IllegalArgumentException();
        return sensor.apply(frame);
    }

    /**
     * @param sensor
     * @return the quality of the sensor.
     */
    public Integer getQuality(Packet.Sensor sensor) {
        if (sensor == null)
            throw new NullPointerException();
        if (sensor == Packet.Sensor.QUALITY)
            throw new IllegalArgumentException();
        return quality.get(sensor);
    }

    /**
     * @return [0, 100] the percentage level of the battery, zero if no data available.
     */
    public int getBatteryLevel() {
        if (battery >= 248) return 100;
        switch (battery) {
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
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(String.format("%tF %<tT.%<tL", getDate()));
        builder.append(",");
        builder.append(getBatteryLevel());
        builder.append(",");
        builder.append(getGyroX());
        builder.append(",");
        builder.append(getGyroY());
        for (Sensor sensor : Sensor.values()) {
            if (sensor == Sensor.QUALITY) continue;
            builder.append(",");
            builder.append(getSensor(sensor));
            builder.append(" (");
            builder.append(getQuality(sensor));
            builder.append(")");
        }
        return builder.toString();
    }

    public enum Sensor {
        QUALITY(99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112),
        F3(10, 11, 12, 13, 14, 15, 0, 1, 2, 3, 4, 5, 6, 7),
        FC5(28, 29, 30, 31, 16, 17, 18, 19, 20, 21, 22, 23, 8, 9),
        AF3(46, 47, 32, 33, 34, 35, 36, 37, 38, 39, 24, 25, 26, 27),
        F7(48, 49, 50, 51, 52, 53, 54, 55, 40, 41, 42, 43, 44, 45),
        T7(66, 67, 68, 69, 70, 71, 56, 57, 58, 59, 60, 61, 62, 63),
        P7(84, 85, 86, 87, 72, 73, 74, 75, 76, 77, 78, 79, 64, 65),
        O1(102, 103, 88, 89, 90, 91, 92, 93, 94, 95, 80, 81, 82, 83),
        O2(140, 141, 142, 143, 128, 129, 130, 131, 132, 133, 134, 135, 120, 121),
        P8(158, 159, 144, 145, 146, 147, 148, 149, 150, 151, 136, 137, 138, 139),
        T8(160, 161, 162, 163, 164, 165, 166, 167, 152, 153, 154, 155, 156, 157),
        F8(178, 179, 180, 181, 182, 183, 168, 169, 170, 171, 172, 173, 174, 175),
        AF4(196, 197, 198, 199, 184, 185, 186, 187, 188, 189, 190, 191, 176, 177),
        FC6(214, 215, 200, 201, 202, 203, 204, 205, 206, 207, 192, 193, 194, 195),
        F4(216, 217, 218, 219, 220, 221, 222, 223, 208, 209, 210, 211, 212, 213);

        private final int[] bits;

        private Sensor(int... bits) {
            this.bits = bits;
        }

        protected int apply(byte[] frame) {
            int level = 0;
            for (int i = bits.length - 1; i >= 0; --i) {
                level <<= 1;
                int b = (bits[i] >> 3) + 1;
                int o = bits[i] % 8;
                level |= ((0xFF & frame[b]) >>> o) & 1;
            }
            return level;
        }
    }
}
