package org.polyfrost.oneconfig.loader.ui;

import javax.swing.*;

import java.awt.*;
import java.text.DecimalFormat;

public class DownloadProgressPanel extends JPanel {

	private final Image logo;
	private float progress = 0f;
	private final DecimalFormat df = new DecimalFormat("#.##");

	public DownloadProgressPanel() {
		super();

		setBackground(new Color(0, 0, 0, 0));
		setPreferredSize(new Dimension(400, 150));

		// Preload the logo
		this.logo = Resources.getLogo();
	}

	@Override
	public void paint(Graphics g) {
		super.paint(g);

		Graphics2D g2d = (Graphics2D) g;
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.setColor(Palette.GRAY_900);
		g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
		g2d.drawImage(logo, 60, 32, 280, 33, null);
		g2d.setColor(Palette.GRAY_700);
		g2d.fillRoundRect(24, 150 - 16 - 8, 352, 8, 6, 6);
		g2d.setColor(Palette.PRIMARY_500);
		g2d.fillRoundRect(24, 150 - 16 - 8, (int) (352 * progress), 8, 6, 6);
		g2d.setColor(Color.WHITE);

		Font font = Resources.getFont();
		g2d.setFont(font.deriveFont(getAdjustedFontSize(13f, font, g2d)));

		g2d.drawString("Downloading OneConfig...", 24, 150 - 16 - 8 - 8);

		String percentage = df.format(progress * 100f) + "%";
		g2d.drawString(percentage, 400 - 24 - g2d.getFontMetrics().stringWidth(percentage), 150 - 16 - 8 - 8);

		g2d.dispose();
	}

	public void update(float progress) {
		this.progress = progress;
		repaint();
	}

	// Adjust the font size to the expected value that's the same everywhere by looking at the size that
	// the font is with 12pts size which normally is 16px
	private float getAdjustedFontSize(float fontSize, Font font, Graphics2D g2d) {
		return (16f / font.deriveFont(12f).getLineMetrics("", g2d.getFontRenderContext()).getHeight()) * fontSize;
	}

}
