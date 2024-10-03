package org.polyfrost.oneconfig.loader.ui;

import javax.imageio.ImageIO;

import java.awt.*;
import java.io.IOException;
import java.util.Objects;

class Resources {

	static Image getIcon() {
		try {
			return ImageIO.read(Objects.requireNonNull(Resources.class.getResourceAsStream("/assets/oneconfig-loader/oneconfig-icon.png")));
		} catch (Exception ignored) {
			return null;
		}
	}

	static Font getFont() {
		try {
			return Font.createFont(Font.TRUETYPE_FONT, Objects.requireNonNull(Resources.class.getResourceAsStream("/assets/oneconfig-loader/Inter.ttf")));
		} catch (FontFormatException | IOException e) {
			e.printStackTrace();
			return new Font("Arial", Font.PLAIN, 13);
		}
	}

	static Image getLogo() {
		try {
			return ImageIO.read(Objects.requireNonNull(Resources.class.getResourceAsStream("/assets/oneconfig-loader/oneconfig.png")));
		} catch (Exception ignored) {
			return null;
		}
	}

}
