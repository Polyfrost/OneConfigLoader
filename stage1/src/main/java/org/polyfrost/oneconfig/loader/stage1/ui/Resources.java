package org.polyfrost.oneconfig.loader.stage1.ui;

import lombok.SneakyThrows;

import org.polyfrost.oneconfig.loader.base.Capabilities;

import javax.imageio.ImageIO;

import java.awt.*;
import java.io.IOException;
import java.util.Objects;

public class Resources {

	private static Class<?> assetsClass;

	private Resources() {}

	@SneakyThrows
	public static void loadAssetsClass(Capabilities.RuntimeAccess runtimeAccess) {
		assetsClass = runtimeAccess.getClassLoader().loadClass("org.polyfrost.oneconfig.loader.assets.AssetsImpl");
	}

	static Image getIcon() {
		try {
			return ImageIO.read(Objects.requireNonNull(assetsClass.getResourceAsStream("/assets/oneconfig-loader/oneconfig-icon.png")));
		} catch (Exception ignored) {
			return null;
		}
	}

	static Font getFont() {
		try {
			return Font.createFont(Font.TRUETYPE_FONT, Objects.requireNonNull(assetsClass.getResourceAsStream("/assets/oneconfig-loader/Poppins-Regular.ttf")));
		} catch (FontFormatException | IOException e) {
			e.printStackTrace();
		} catch (Exception ignored) {

		}
		return new Font("Arial", Font.PLAIN, 13);
	}

	static Image getLogo() {
		try {
			return ImageIO.read(Objects.requireNonNull(assetsClass.getResourceAsStream("/assets/oneconfig-loader/oneconfig.png")));
		} catch (Exception ignored) {
			return null;
		}
	}

}
