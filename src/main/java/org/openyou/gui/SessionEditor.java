package org.openyou.gui;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;
import org.openyou.jpa.EmotivJpaController;
import org.openyou.jpa.EmotivSession;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * @author Sam Halliday
 */
@Log
public class SessionEditor extends JPanel {

    @Getter
    @Setter
    private EmotivJpaController controller;

    private final JTextField title = new JTextField();
    private final JTextArea notes = new JTextArea();

    public SessionEditor() {
        super(new BorderLayout());

        JPanel titleRow = new JPanel(new BorderLayout());
        JLabel titleLabel = new JLabel("Title:");
        titleRow.add(titleLabel, BorderLayout.WEST);
        titleRow.add(title, BorderLayout.CENTER);
        add(titleRow, BorderLayout.NORTH);

        JPanel notesRow = new JPanel(new BorderLayout());
        JLabel notesLabel = new JLabel("Notes:");
        notes.setLineWrap(true);
        notesRow.add(notesLabel, BorderLayout.WEST);
        notesRow.add(notes, BorderLayout.CENTER);
        add(notesRow, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new BorderLayout());
        JButton start = new JButton("Record");
        JButton stop = new JButton("Stop");
        buttons.add(start, BorderLayout.WEST);
        buttons.add(stop, BorderLayout.EAST);
        add(buttons, BorderLayout.SOUTH);

        log.info(notes.getBorder().getClass().toString());

        start.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                start();
            }
        });
        stop.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                stop();
            }
        });
        title.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                title.setBorder(BorderFactory.createLineBorder(Color.RED));
            }
        });
        notes.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                notes.setBorder(BorderFactory.createLineBorder(Color.RED));
            }
        });
        title.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                titleChanged();
            }
        });
        notes.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                notesChanged();
            }
        });

    }

    private void start() {
        if (controller.isRecording())
            return;
        EmotivSession session = new EmotivSession();
        session.setName(title.getText());
        session.setNotes(notes.getText());
        controller.setSession(session);
        controller.setRecording(true);
    }

    private void stop() {
        controller.setRecording(false);
        title.setText("");
    }

    private void titleChanged() {
        EmotivSession session = controller.getSession();
        if (session != null) {
            session.setName(title.getText());
            controller.updateSession(session);
        }
        title.setBorder(null);
    }

    private void notesChanged() {
        EmotivSession session = controller.getSession();
        if (session != null) {
            session.setNotes(notes.getText());
            controller.updateSession(session);
        }
        notes.setBorder(null);
    }

}
