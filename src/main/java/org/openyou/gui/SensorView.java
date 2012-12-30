// Copyright Samuel Halliday 2012

package org.openyou.gui;

import com.google.common.collect.MinMaxPriorityQueue;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import lombok.extern.java.Log;
import org.openyou.Emotiv.PacketListener;
import org.openyou.Packet;

import javax.annotation.concurrent.GuardedBy;
import javax.swing.*;
import java.awt.*;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Gives visual feedback on the readings from the Emotiv sensors.
 * <p/>
 * This may take liberties in the conversion of the data in order
 * to produce visually appealing results.
 *
 * @author Sam Halliday
 */
@Log
public class SensorView extends JPanel implements PacketListener {

    private final Config config = ConfigFactory.load().getConfig("org.openyou.gui.sensors");

    // TODO: offset per channel
    private final int offset = 8464;

    public SensorView() {
        setPreferredSize(new Dimension(-1, 250));
    }

    // a little bit of a break from the MVC model, we cache the last N entries here
    // which requires a bit of safety because the receivePacket is in a different
    // thread to the Swing thread.
    @GuardedBy("lock")
    private final MinMaxPriorityQueue<Packet> queue =
            MinMaxPriorityQueue
                    .orderedBy(Ordering.natural().reverse())
                    .maximumSize(config.getInt("cache"))
                    .create();
    private final Lock lock = new ReentrantLock();

    @Override
    protected void paintComponent(Graphics g) {
        Set<Packet> packets = Sets.newTreeSet();
        lock.lock();
        try {
            packets.addAll(queue);
        } finally {
            lock.unlock();
        }
        Dimension size = getSize();

        g.setColor(Color.WHITE);
        g.fillRect(0, 0, size.width, size.height);

        g.setColor(Color.BLACK);
        Graphics2D g2 = (Graphics2D) g;
        int i = 0;
        for (Packet packet : packets) {
            double x = i * size.getWidth() / packets.size();
            for (Map.Entry<Packet.Sensor, Integer> entry : packet.getSensors().entrySet()) {
                double y = 20 * (entry.getValue() - offset) / 2048.0;
                Shape shape = new Rectangle.Double(x, y + 15 * entry.getKey().ordinal(), 2, 2);
                g2.fill(shape);
            }
            i++;
        }
    }

    @Override
    public void receivePacket(Packet packet) {
        lock.lock();
        try {
            queue.add(packet);
        } finally {
            lock.unlock();
        }
        repaint();
    }
}
