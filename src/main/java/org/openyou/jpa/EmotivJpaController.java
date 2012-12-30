// Copyright Samuel Halliday 2012

package org.openyou.jpa;

import com.google.common.base.Preconditions;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;
import org.openyou.Emotiv;
import org.openyou.Packet;

import javax.persistence.EntityManagerFactory;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Abstracts the lower level CRUD operations for recording
 * data one session at a time. Data recording is off by
 * default.
 * <p/>
 * Needs to be told about changes to the session to be able
 * to persist (one of the failings of JPA is that it isn't
 * fully compatible with PropertyChangeListener support).
 * <p/>
 * Persistence is performed in background worker threads.
 * <p/>
 * The session object must be re-obtained from the database
 * layer in order to see all associated data.
 *
 * @author Sam Halliday
 */
@Log
public class EmotivJpaController implements Emotiv.PacketListener {

    private final EmotivDatumCrud datumCrud;
    private final EmotivSessionCrud sessionCrud;
    private final Executor executor;
    @Getter
    private volatile EmotivSession session;
    @Getter @Setter
    private volatile boolean recording;

    public EmotivJpaController(EntityManagerFactory emf) {
        datumCrud = new EmotivDatumCrud(emf);
        sessionCrud = new EmotivSessionCrud(emf);

        Config config = ConfigFactory.load().getConfig("org.openyou.jpa.controller");
        int threads = config.getInt("threads");
        executor = Executors.newFixedThreadPool(threads);
    }

    public void setSession(EmotivSession session) {
        this.session = session;
        sessionCrud.create(session);
    }

    public void updateSession(EmotivSession session) {
        Preconditions.checkNotNull(session);
        Preconditions.checkArgument(session.equals(this.session));
        sessionCrud.update(session);
    }

    @Override
    public void receivePacket(final Packet packet) {
        if (!recording)
            return;
        final EmotivSession session = this.session;
        final long start = System.currentTimeMillis();
        Runnable runnable = new Runnable() {
            @Override
            public void run () {
                EmotivDatum datum = EmotivDatum.fromPacket(packet);
                datum.setSession(session);

                datumCrud.create(datum); // taking about 6 millis
                long end = System.currentTimeMillis();
                log.config("Persistence took " + (end - start));
            }
        };
        executor.execute(runnable);
    }
}
