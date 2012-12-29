package org.openyou;

import lombok.Data;
import org.openyou.Packet.Sensor;

import javax.persistence.*;
import java.util.Date;
import java.util.UUID;

/**
 * A Java Persistence API (JPA) {@link @Entity} that is appropriate
 * for storage in a traditional RDBMS.
 *
 * @author Sam Halliday
 */
@Entity
@Data
public class EmotivDatum {

    /**
     * @param packet
     * @return a datum, which is not yet assigned to a session.
     */
    public static EmotivDatum fromPacket(Packet packet) {
        // Verbosity alert! Gotta love Java...
        EmotivDatum datum = new EmotivDatum();

        datum.setTimestamp(packet.getDate());
        datum.setBattery(packet.getBatteryLevel());
        datum.setGyroX(packet.getGyroX());
        datum.setGyroY(packet.getGyroY());

        datum.setF3(packet.getSensor(Sensor.F3));
        datum.setF3_QUALITY(packet.getQuality(Sensor.F3));
        datum.setFC5(packet.getSensor(Sensor.FC5));
        datum.setFC5_QUALITY(packet.getQuality(Sensor.FC5));
        datum.setAF3(packet.getSensor(Sensor.AF3));
        datum.setAF3_QUALITY(packet.getQuality(Sensor.AF3));
        datum.setF7(packet.getSensor(Sensor.F7));
        datum.setF7_QUALITY(packet.getQuality(Sensor.F7));
        datum.setT7(packet.getSensor(Sensor.T7));
        datum.setT7_QUALITY(packet.getQuality(Sensor.T7));
        datum.setP7(packet.getSensor(Sensor.P7));
        datum.setP7_QUALITY(packet.getQuality(Sensor.P7));
        datum.setO1(packet.getSensor(Sensor.O1));
        datum.setO1_QUALITY(packet.getQuality(Sensor.O1));
        datum.setO2(packet.getSensor(Sensor.O2));
        datum.setO2_QUALITY(packet.getQuality(Sensor.O2));
        datum.setP8(packet.getSensor(Sensor.P8));
        datum.setP8_QUALITY(packet.getQuality(Sensor.P8));
        datum.setT8(packet.getSensor(Sensor.T8));
        datum.setT8_QUALITY(packet.getQuality(Sensor.T8));
        datum.setF8(packet.getSensor(Sensor.F8));
        datum.setF8_QUALITY(packet.getQuality(Sensor.F8));
        datum.setAF4(packet.getSensor(Sensor.AF4));
        datum.setAF4_QUALITY(packet.getQuality(Sensor.AF4));
        datum.setFC6(packet.getSensor(Sensor.FC6));
        datum.setFC6_QUALITY(packet.getQuality(Sensor.FC6));
        datum.setF4(packet.getSensor(Sensor.F4));
        datum.setF4_QUALITY(packet.getQuality(Sensor.F4));

        return datum;
    }


    @Id
    private UUID id = UUID.randomUUID();

    @ManyToOne
    private EmotivSession session;

    @Column
    @Temporal(TemporalType.TIMESTAMP)
    private Date timestamp;

    @Column
    private Integer battery;

    @Column
    private Integer gyroX, gyroY;

    @Column
    private Integer F3, FC5, AF3, F7, T7, P7, O1, O2, P8, T8, F8, AF4, FC6, F4;

    @Column
    private Integer F3_QUALITY, FC5_QUALITY, AF3_QUALITY, F7_QUALITY, T7_QUALITY,
            P7_QUALITY, O1_QUALITY, O2_QUALITY, P8_QUALITY, T8_QUALITY, F8_QUALITY, AF4_QUALITY,
            FC6_QUALITY, F4_QUALITY;

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof EmotivDatum) || id == null) {
            return false;
        }
        EmotivDatum other = (EmotivDatum) obj;
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        if (id == null)
            throw new NullPointerException("id must be set before @Entity.hashCode can be called");
        return id.hashCode();
    }

}
