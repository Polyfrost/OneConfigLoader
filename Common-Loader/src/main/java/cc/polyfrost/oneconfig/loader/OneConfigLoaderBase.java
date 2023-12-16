package cc.polyfrost.oneconfig.loader;

import cc.polyfrost.oneconfig.loader.stage0.OneConfigWrapperBase;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import javax.net.ssl.HttpsURLConnection;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
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

    //TODO when we have the opportunity to, make this and OneConfigWrapperBase's constructor not duplicated
    // the only reason its not is because static methods can't be overridden
    public OneConfigLoaderBase() {
        try {
            final LoaderInfo loaderInfo = provideLoaderInfo();
            // TODO: make this not spam the log everytime the wrapper is called from a mod
            System.out.println("OneConfig has detected the version " + loaderInfo.mcVersion + ". If this is false, report this at https://inv.wtf/polyfrost");

            File oneconfigFile = provideFile(loaderInfo);

            if (!isInitialized(oneconfigFile) && shouldUpdate()) {
                JsonElement json = getLoaderRequest("https://api.polyfrost.cc/oneconfig/" + loaderInfo.mcVersion + "-" + loaderInfo.modLoader);
                if (json != null && json.isJsonObject()) {
                    JsonObject jsonObject = json.getAsJsonObject();
                    JsonInfo jsonInfo = provideJsonInfo(jsonObject, loaderInfo);
                    if (jsonInfo.success) {
                        if (!oneconfigFile.exists() || !jsonInfo.checksum.equals(getChecksum(oneconfigFile))) {
                            System.out.println("Updating OneConfig " + loaderInfo.stageLoading + "...");
                            File newLoaderFile = new File(oneconfigFile.getParentFile(), oneconfigFile.getName().substring(0, oneconfigFile.getName().lastIndexOf(".")) + "-NEW.jar");

                            downloadFile(jsonInfo.downloadUrl, newLoaderFile);

                            if (newLoaderFile.exists() && jsonInfo.checksum.equals(getChecksum(newLoaderFile))) {
                                try {
                                    Files.move(newLoaderFile.toPath(), oneconfigFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                                    System.out.println("Updated OneConfig " + loaderInfo.stageLoading + "!");
                                } catch (IOException ignored) {
                                }
                            } else {
                                if (newLoaderFile.exists()) newLoaderFile.delete();
                                System.out.println("Failed to update OneConfig " + loaderInfo.stageLoading + ", trying to continue...");
                            }
                        }
                    }
                }

                if (!oneconfigFile.exists()) showErrorScreen();
                addToClasspath(oneconfigFile);
            }
            if (!getNextInstance()) {
                showErrorScreen();
            }
        } catch (Exception e) {
            e.printStackTrace();
            showErrorScreen();
        }
    }

    //todo this sucks
    protected static JsonElement getLoaderRequest(String site) {
        try {
            URL url = new URL(site);
            HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
            con.setRequestProperty("User-Agent", "OneConfigLoader");
            con.setRequestMethod("GET");
            con.setConnectTimeout(15000);
            con.setReadTimeout(15000);
            int status = con.getResponseCode();
            if (status != 200) {
                System.out.println("API request failed, status code " + status);
                return null;
            }
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuilder content = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            in.close();
            JsonParser parser = new JsonParser();
            return parser.parse(content.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

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
            con.setRequestProperty("User-Agent", "OneConfigLoader");
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
                Font font = Font.createFont(Font.TRUETYPE_FONT, Objects.requireNonNull(getClass().getResourceAsStream("/assets/oneconfig-loader/Regular.ttf")));
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
