package org.polyfrost.oneconfig.loader.ui;

import javax.swing.*;

import java.awt.*;

public class LoaderFrame extends JFrame {

	private final ProgressPanel progressPanel = new ProgressPanel();

	public LoaderFrame() {
		super("OneConfig");

		// Styling
		setAlwaysOnTop(true);
		setResizable(false);
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		setUndecorated(true);
		setSize(400, 150);
		setIconImage(Resources.getIcon());
		setLocationRelativeTo(null);
		setBackground(new Color(0, 0, 0, 0));

		// Adding components
		add(progressPanel);

		// Display
		pack();
	}

	public void display() {
		setVisible(true);
	}

	public void destroy() {
		setVisible(false);
		dispose();
	}

	public void updateMessage(String message) {
		progressPanel.updateMessage(message);
		repaint();
	}

	public void updateProgress(float progress) {
		progressPanel.updateProgress(progress);
		repaint();
	}

}
