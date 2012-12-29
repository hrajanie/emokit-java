// Copyright Samuel Halliday 2012
package org.openyou.jpa;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import lombok.Data;

import javax.persistence.*;
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
    private Collection<EmotivDatum> data = Lists.newArrayList();

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
        Preconditions.checkNotNull(id, "id must be set before @Entity.hashCode can be called");
        return id.hashCode();
    }

}
