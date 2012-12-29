// Copyright Samuel Halliday 2012
package org.openyou.gui;

import fommil.persistence.CrudDao;
import lombok.extern.java.Log;
import org.openyou.Emotiv;
import org.openyou.Packet;
import org.openyou.jpa.EmotivDatum;
import org.openyou.jpa.EmotivDatumCrud;
import org.openyou.jpa.EmotivSession;
import org.openyou.jpa.EmotivSessionCrud;

import javax.persistence.EntityManagerFactory;
import javax.swing.*;
import java.awt.*;

/**
 * Zoku is a Swing GUI application for the acquisition of
 * EEG data from an Emotiv EPOC headset. Although this
 * runs as a standalone application for data acquistition,
 * its primary function is to showcase the various pluggable
 * Swing components in this package.
 *
 * @author Sam Halliday
 * @see <a href="http://en.wikipedia.org/wiki/Glossary_of_terms_in_The_Quantum_Thief#The_zoku">Zoku</a>
 */
@Log
public class Zoku {

    public static void main(String[] args) throws Exception {
        Emotiv emotive = new Emotiv();

        JFrame frame = new JFrame("Zoku");
        frame.setLayout(new BorderLayout());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1200, 800);
        JPanel sidebar = new JPanel();
        sidebar.setBackground(Color.WHITE);
        frame.add(sidebar, BorderLayout.EAST);

        SensorQualityView quality = new SensorQualityView();
        frame.add(quality, BorderLayout.CENTER);

        BatteryView battery = new BatteryView();
        sidebar.add(new JLabel("Battery"));
        sidebar.add(battery);

        frame.setVisible(true);

        EntityManagerFactory emf = CrudDao.createEntityManagerFactory("ZokuPU");
        EmotivDatumCrud datumCrud = new EmotivDatumCrud(emf);
        EmotivSessionCrud sessionCrud = new EmotivSessionCrud(emf);

        EmotivSession session = new EmotivSession();
        session.setName("Test");
        session.setNotes(emotive.getSerial());

        sessionCrud.create(session);

        log.info("Persisted session");

        // refactor to have an asynchronous runner
        for (Packet packet : emotive) {
            quality.receivePacket(packet);
            battery.receivePacket(packet);

            long start = System.currentTimeMillis();
            EmotivDatum datum = EmotivDatum.fromPacket(packet);
            datum.setSession(session);

            datumCrud.create(datum); // taking about 6 millis
            long end = System.currentTimeMillis();
            log.config("Persistence took " + (end - start));
        }
    }

}
