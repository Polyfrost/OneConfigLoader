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
import java.math.BigDecimal;
import java.math.RoundingMode;
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
    }

    private static class DownloadUI extends JPanel {
        private BufferedImage logo;
        private float progress = 0f;

        public DownloadUI() {
            super();
            try {
                logo = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/assets/oneconfig-loader/oneconfig.png")));
            } catch (Exception ignored) {
            }
            setBackground(new Color(0, 0, 0, 0));
            setPreferredSize(new Dimension(400, 150));
        }

        @Override
        public void paint(Graphics g) {
            super.paint(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setColor(GRAY_900);
            g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
            g2d.drawImage(logo, 60, 32, 280, 33, null);
            g2d.setColor(GRAY_700);
            g2d.fillRoundRect(24, 150 - 16 - 8, 352, 8, 6, 6);
            g2d.setColor(PRIMARY_500);
            g2d.fillRoundRect(24, 150 - 16 - 8, (int) (352 * progress), 8, 6, 6);
            g2d.setColor(Color.WHITE);
            try {
                g2d.setFont(Font.createFont(Font.TRUETYPE_FONT, Objects.requireNonNull(getClass().getResourceAsStream("/assets/oneconfig-loader/Regular.otf"))).deriveFont(13f));
            } catch (FontFormatException | IOException e) {
                e.printStackTrace();
                g2d.setFont(new Font("Arial", Font.PLAIN, 13));
            }
            g2d.drawString("Downloading OneConfig...", 24, 150 - 16 - 8 - 8);
            String percent = new BigDecimal(progress * 100f).setScale(2, RoundingMode.HALF_UP).doubleValue() + "%";
            g2d.drawString(percent, 400 - 24 - g2d.getFontMetrics().stringWidth(percent), 150 - 16 - 8 - 8);
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
                icon = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/assets/oneconfig-loader/oneconfig-icon.png")));
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
