package org.openyou;

import lombok.Data;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

/**
 * Gathers data for a single sitting (one headset wearer, continuous timeseries data).
 *
 * @author Sam Halliday
 */
@Entity
@Data
public class EmotivSession {

    @Id
    private UUID id = UUID.randomUUID();

    @Column
    private String name;

    @Lob
    @Column(length = 8192)
    private String notes;

    @OneToMany(mappedBy = "session")
    private Collection<EmotivDatum> data = new ArrayList<EmotivDatum>();

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof EmotivSession) || id == null) {
            return false;
        }
        EmotivSession other = (EmotivSession) obj;
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        if (id == null)
            throw new NullPointerException("id must be set before @Entity.hashCode can be called");
        return id.hashCode();
    }

}
