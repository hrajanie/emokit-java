// Copyright Samuel Halliday 2012

package org.openyou.gui;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import lombok.extern.java.Log;
import org.openyou.Emotiv;
import org.openyou.Packet;

import javax.swing.*;
import java.awt.*;

/**
 * Component that can listen for Emotiv packets and show the current gyro position.
 *
 * @author Sam Halliday
 */
@Log
public class GyroView extends JPanel implements Emotiv.PacketListener {

    private final Config config = ConfigFactory.load().getConfig("org.openyou.gui.gyro");
    private final int xCorrection = config.getInt("correction.x");
    private final int yCorrection = config.getInt("correction.y");

    private volatile int gyroX = xCorrection, gyroY = yCorrection;

    @Override
    protected void paintComponent(Graphics g) {
        Dimension size = getSize();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, size.width, size.height);
        g.setColor(Color.BLACK);

        double x = (gyroX - xCorrection) / 127.0;
        double y = (gyroY - yCorrection) / 127.0;

        int xs = Math.max(1, (int)Math.round(Math.abs(x * 10)));
        ((Graphics2D)g).setStroke(new BasicStroke(xs));
        g.drawLine(
                size.width / 2,
                size.height / 2,
                (int)Math.round(size.width / 2 + x * size.width / 2),
                size.height / 2
        );

        int ys = Math.max(1, (int)Math.round(Math.abs(y * 10)));
        ((Graphics2D)g).setStroke(new BasicStroke(ys));
        g.drawLine(
                size.width / 2,
                size.height / 2,
                size.width / 2,
                (int)Math.round(size.height / 2 + y * size.height / 2)
        );

    }

    @Override
    public void receivePacket(Packet packet) {
        gyroX = packet.getGyroX();
        gyroY = packet.getGyroY();
        repaint();
    }
}
