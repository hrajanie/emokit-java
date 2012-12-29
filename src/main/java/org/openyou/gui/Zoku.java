// Copyright Samuel Halliday 2012
package org.openyou.gui;

import org.openyou.Emotiv;
import org.openyou.Packet;

import javax.swing.*;

/**
 * Zoku is a Swing GUI application for the acquisition of
 * EEG data from an Emotiv EPOC headset.
 *
 * @author Sam Halliday
 * @see <a href="http://en.wikipedia.org/wiki/Glossary_of_terms_in_The_Quantum_Thief#The_zoku">Zoku</a>
 */
public class Zoku {

    public static void main(String[] args) throws Exception {
        Emotiv emotive = new Emotiv();

        JFrame frame = new JFrame("Zoku");
        SensorQualityView quality = new SensorQualityView();
        frame.setContentPane(quality);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1200, 800);
        frame.setVisible(true);

        for (Packet packet : emotive) {
            quality.receivePacket(packet);
        }
    }

}
