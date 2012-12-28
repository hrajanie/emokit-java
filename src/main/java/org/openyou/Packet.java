/* Copyright Samuel Halliday 2012 */
package org.openyou;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @see <a href="https://github.com/openyou/emokit/blob/master/doc/emotiv_protocol.asciidoc">Emotive Protocol</a>
 * @author Sam Halliday
 */
@RequiredArgsConstructor
public final class Packet {

//    @Getter
//    private final int gyroX, gyroY;

    @Getter
    private final int counter;

    protected enum Mask {
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

        private Mask(int... bits) {
            this.bits = bits;
        }
    }

    protected static Packet fromBytes(byte[] raw) {
        int gyroX = 0xFF & raw[29] - 102;
        int gyroY = 0xFF & raw[30] - 104;
        int counter = 0xFF & raw[0];

        return new Packet(counter);
    }

    @Override
    public String toString() {
//        return gyroX + " " + gyroY;
        return Integer.toString(counter);
    }
}
