package cc.polyfrost.oneconfig.loader;

import cc.polyfrost.oneconfig.loader.stage0.OneConfigWrapperBase;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import javax.net.ssl.HttpsURLConnection;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.DecimalFormat;
import java.util.Objects;

/**
 * This abstract base class allows us to implement the OneConfig Loader.
 * <p>
 * OneConfigLoaderBase SHOULD NOT be modified, added on to, or included in
 * third party mods. If you have a special use case that requires the loader
 * to be modified rather than downloaded by the wrapper please contact us at
 * https://inv.wtf/polyfrost so we can work to accommodate your use case.
 */
public abstract class OneConfigLoaderBase extends OneConfigWrapperBase {

    private long timeLast = System.currentTimeMillis();
    private float downloadPercent = 0f;
    private static final Logger logger = LogManager.getLogger("OneConfigLoader");

    private Throwable error = null;

    @Override
    protected void downloadFile(String url, File location) {
        Frame ui = null;
        try {
            ui = new Frame();
        } catch (Exception e) {
            logger.error("Continuing without GUI", e);
        }
        try {
            HttpsURLConnection con = (HttpsURLConnection) new URL(url).openConnection();
            con.setRequestProperty("User-Agent", "OneConfig-Loader");
            con.setRequestMethod("GET");
            con.setConnectTimeout(15000);
            con.setReadTimeout(15000);
            con.setDoOutput(true);
            int length = con.getContentLength();
            if (location.exists() && !location.delete()) {
                throw new RuntimeException("Could not delete old version of OneConfig! (" + location.getAbsolutePath() + ")");
            }
            if (!location.createNewFile()) {
                throw new RuntimeException("Could not create file! (" + location.getAbsolutePath() + ")");
            }
            logger.info("Downloading new version of OneConfig... (" + length / 1024f + "KB)");
            Thread downloader = new Thread(() -> {
                try (InputStream in = con.getInputStream()) {
                    Files.copy(in, location.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (Exception e) {
                    error = e;
                }
            });
            downloader.start();
            while (downloadPercent < 1f) {
                if (error != null) {
                    throw new RuntimeException(error);
                }
                downloadPercent = (float) location.length() / (float) length;
                if (ui != null) {
                    ui.update(downloadPercent);
                }
                if (System.currentTimeMillis() - timeLast > 1000) {
                    timeLast = System.currentTimeMillis();
                    logger.info("Downloaded " + (location.length() / 1024f) + "KB out of " + (length / 1024f) + "KB (" + (downloadPercent * 100) + "%)");
                }
            }
            logger.info("Download finished successfully");
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (ui != null) {
                ui.dispose();
            }
        }
    }

    private static class DownloadUI extends JPanel {
        private BufferedImage logo;
        private float progress = 0f;
        private final DecimalFormat df = new DecimalFormat("#.##");

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
                Font font = Font.createFont(Font.TRUETYPE_FONT, Objects.requireNonNull(getClass().getResourceAsStream("/assets/oneconfig-loader/Regular.otf")));
                g2d.setFont(font.deriveFont(getAdjustedFontSize(13f, font, g2d)));
            } catch (FontFormatException | IOException e) {
                e.printStackTrace();
                Font font = new Font("Arial", Font.PLAIN, 13);
                g2d.setFont(font.deriveFont(getAdjustedFontSize(13f, font, g2d)));
            }
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
            setSize(400, 150);
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
