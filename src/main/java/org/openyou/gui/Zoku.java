// Copyright Samuel Halliday 2012
package org.openyou.gui;

import com.google.common.base.Preconditions;
import fommil.persistence.CrudDao;
import lombok.extern.java.Log;
import org.openyou.Emotiv;
import org.openyou.Packet;
import org.openyou.jpa.EmotivJpaController;

import javax.persistence.EntityManagerFactory;
import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.logging.Level;

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

    public static void main(String[] args) {
        EntityManagerFactory emf = CrudDao.createEntityManagerFactory("ZokuPU");
        EmotivJpaController database = new EmotivJpaController(emf);

        JFrame frame = new JFrame("Zoku");
        enableOSXFullscreen(frame);
        frame.setLayout(new BorderLayout());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1200, 800);
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(Color.WHITE);
        frame.add(sidebar, BorderLayout.EAST);

        SensorQualityView quality = new SensorQualityView();
        frame.add(quality, BorderLayout.CENTER);

        BatteryView battery = new BatteryView();
        sidebar.add(battery, BorderLayout.NORTH);

        GyroView gyro = new GyroView();
        sidebar.add(gyro, BorderLayout.SOUTH);

        SessionEditor editor = new SessionEditor();
        editor.setController(database);
        sidebar.add(editor, BorderLayout.CENTER);

        SensorView sensors = new SensorView();
        frame.add(sensors, BorderLayout.SOUTH);

        frame.setVisible(true);

        while (true) {
            try {
                // refactor to have an asynchronous runner
                Emotiv emotive = new Emotiv();
                for (Packet packet : emotive) {
                    database.receivePacket(packet);
                    quality.receivePacket(packet);
                    battery.receivePacket(packet);
                    gyro.receivePacket(packet);
                    sensors.receivePacket(packet);
                }
            } catch (IOException e) {
                log.log(Level.SEVERE, "", e);
                System.exit(0);
            }
        }
    }

    /**
     * @param window
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void enableOSXFullscreen(Window window) {
        Preconditions.checkNotNull(window);
        try {
            Class util = Class.forName("com.apple.eawt.FullScreenUtilities");
            Class params[] = new Class[]{Window.class, Boolean.TYPE};
            Method method = util.getMethod("setWindowCanFullScreen", params);
            method.invoke(util, window, true);
        } catch (ClassNotFoundException ignored) {
        } catch (Exception e) {
            log.log(Level.WARNING, "OS X Fullscreen FAIL", e);
        }
    }

}
