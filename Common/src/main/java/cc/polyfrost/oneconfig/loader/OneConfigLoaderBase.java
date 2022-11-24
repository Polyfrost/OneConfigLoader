package cc.polyfrost.oneconfig.loader;

import cc.polyfrost.oneconfig.loader.wrapper.OneConfigWrapperBase;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

public abstract class OneConfigLoaderBase extends OneConfigWrapperBase {

    private long timeLast = System.currentTimeMillis();
    private float downloadPercent = 0f;
    private static final Logger logger = LogManager.getLogger("OneConfigLoader");

    @Override
    protected void downloadFile(String url, File location) {
        if (showDownloadUI()) {
            Frame ui = new Frame();
            try {
                URLConnection con = new URL(url).openConnection();
                con.setRequestProperty("User-Agent", "OneConfig-Loader");
                int length = con.getContentLength();
                if (location.exists()) location.delete();
                location.createNewFile();
                logger.info("Downloading new version of OneConfig... (" + length / 1024f + "KB)");
                Thread downloader = new Thread(() -> {
                    try (InputStream in = con.getInputStream()) {
                        Files.copy(in, location.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
                downloader.start();
                while (downloadPercent < 1f) {
                    downloadPercent = (float) location.length() / (float) length;
                    ui.update(downloadPercent);
                    if (System.currentTimeMillis() - timeLast > 1000) {
                        timeLast = System.currentTimeMillis();
                        logger.info("Downloaded " + (location.length() / 1024f) + "KB out of " + (length / 1024f) + "KB (" + (downloadPercent * 100) + "%)");
                    }
                }
                ui.dispose();
                logger.info("Download finished successfully");
            } catch (IOException e) {
                ui.dispose();
                e.printStackTrace();
            }
        } else {
            super.downloadFile(url, location);
        }
    }

    protected boolean showDownloadUI() {
        return true;
    }

    private static class DownloadUI extends JPanel {
        private BufferedImage base;
        private float progress = 0f;

        public DownloadUI() {
            super();
            try {
                base = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/assets/oneconfig-loader/frame.png")));
            } catch (Exception ignored) {
            }
            setBackground(new Color(0, 0, 0, 0));
            setPreferredSize(new Dimension(300, 100));
        }

        @Override
        public void paint(Graphics g) {
            super.paint(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.drawImage(base, null, 0, 0);
            g2d.setColor(GRAY_700);
            g2d.fillRoundRect(12, 74, 275, 12, 12, 12);
            g2d.setColor(PRIMARY_500);
            g2d.fillRoundRect(12, 74, (int) (275 * progress), 12, 12, 12);
            g2d.dispose();
        }

        public void update(float progress) {
            this.progress = progress;
            repaint();
        }
    }

    private static class Frame extends JFrame {
        private final DownloadUI downloadUI = new DownloadUI();

        public Frame() {
            super("OneConfig");
            setAlwaysOnTop(true);
            setResizable(false);
            Image icon = null;
            try {
                icon = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/assets/oneconfig-loader/icon.png")));
            } catch (Exception ignored) {
            }
            setAlwaysOnTop(true);
            setResizable(false);
            setIconImage(icon);
            setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
            setUndecorated(true);
            setLocationRelativeTo(null);
            setBackground(new Color(0, 0, 0, 0));
            add(downloadUI);
            pack();
            setVisible(true);
        }

        public void update(float progress) {
            downloadUI.update(progress);
            repaint();
        }
    }
}
