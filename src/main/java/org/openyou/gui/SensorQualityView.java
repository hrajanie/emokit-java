package org.openyou.gui;

import com.google.common.collect.Maps;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import lombok.Cleanup;
import lombok.Getter;
import lombok.Setter;
import org.openyou.Emotiv;
import org.openyou.Packet;
import org.openyou.Packet.Sensor;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * Gives visual feedback on the quality of the Emotiv sensor contacts.
 *
 * @author Sam Halliday
 */
public class SensorQualityView extends JPanel implements Emotiv.PacketListener {

    private final Config config = ConfigFactory.load().getConfig("org.openyou.gui.quality");
    private final Config positions = config.getConfig("positions");
    private final BufferedImage image;
    private final Map<Sensor, Point> sensors = Maps.newHashMap();
    private final Font font = new Font("Verdana", Font.BOLD, 14);

    private volatile Map<Sensor, Integer> quality = Maps.newEnumMap(Sensor.class);

    @Getter
    @Setter
    private volatile boolean showLabels = true;

    public SensorQualityView() {
        super(new BorderLayout());

        try {
            @Cleanup InputStream stream = getClass().getResourceAsStream(config.getString("image"));
            image = ImageIO.read(stream);

            for (Sensor sensor : Sensor.values()) {
                if (sensor == Sensor.QUALITY)
                    continue;
                Config position = positions.getConfig(sensor.name());
                sensors.put(sensor, new Point(
                        Integer.parseInt(position.getString("x")),
                        Integer.parseInt(position.getString("y"))
                ));
            }
        } catch (IOException e) {
            throw new ConfigException.Missing(config.getString("image"));
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Dimension size = getSize();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, size.width, size.height);

        float scale = Math.min(1f * size.width / image.getWidth(), 1f * size.height / image.getHeight());
        int width = Math.round(scale * image.getWidth());
        int height = Math.round(scale * image.getHeight());
        int xoff = (size.width - width) / 2;
        int yoff = (size.height - height) / 2;

        g.drawImage(image, xoff, yoff, width, height, null);

        int diam = Math.round(scale * 50);

        for (Map.Entry<Sensor, Point> entry: sensors.entrySet()) {
            Sensor sensor = entry.getKey();
            Integer level = quality.get(sensor);
            if (level == null) continue;

            Color color = levelToColor(level);

            Point point = entry.getValue();
            Point centre = new Point(
                    xoff + Math.round(point.x * scale) - diam / 2,
                    yoff + Math.round(point.y * scale) - diam / 2
            );
            g.setColor(color);
            g.fillOval(centre.x, centre.y, diam, diam);

            if (showLabels) {
                g.setColor(Color.BLACK);
                g.setFont(font);
                FontMetrics fm = g.getFontMetrics();
                Rectangle2D bounds = fm.getStringBounds(sensor.name(), g);

                g.drawString(
                        sensor.name(),
                        (int)(centre.x + diam / 2 - Math.round(bounds.getWidth() / 2)),
                        (int)(centre.y + diam / 2 + Math.round(bounds.getHeight() / 2) - fm.getDescent())
                );
            }
        }
    }

    private Color levelToColor(Integer level) {
        if (level >= 432) return Color.GREEN;
        if (level >= 216) return Color.ORANGE;
        return Color.RED;
    }

    @Override
    public void receivePacket(Packet packet) {
        quality = packet.getQuality();
    }
}
