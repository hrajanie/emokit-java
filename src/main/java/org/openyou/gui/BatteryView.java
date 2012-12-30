// Copyright Samuel Halliday 2012

package org.openyou.gui;

import org.openyou.Emotiv;
import org.openyou.Packet;

import javax.swing.*;

/**
 * Component that can listen for Emotiv packets and show the battery level.
 *
 * @author Sam Halliday
 */
public class BatteryView extends JProgressBar implements Emotiv.PacketListener {

    @Override
    public void receivePacket(Packet packet) {
        int battery = packet.getBatteryLevel();
        setValue(battery);
        repaint();
    }
}
