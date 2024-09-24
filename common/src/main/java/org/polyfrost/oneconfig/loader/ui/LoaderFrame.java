package org.polyfrost.oneconfig.loader.ui;

import javax.swing.*;

import java.awt.*;

public class LoaderFrame extends JFrame {

	private final DownloadProgressPanel downloadUI = new DownloadProgressPanel();

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
		add(downloadUI);

		// Display
		pack();
		setVisible(true);
	}

	public void update(float progress) {
		downloadUI.update(progress);
		repaint();
	}

}
