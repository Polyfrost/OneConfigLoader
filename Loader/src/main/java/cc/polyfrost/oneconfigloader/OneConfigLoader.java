package cc.polyfrost.oneconfigloader;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraftforge.common.ForgeVersion;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import javax.net.ssl.HttpsURLConnection;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.Method;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class OneConfigLoader implements ITweaker {
    public static final Color GRAY_900 = new Color(13, 14, 15, 255);
    public static final Color GRAY_700 = new Color(34, 35, 38);
    public static final Color PRIMARY_500 = new Color(26, 103, 255);
    public static final Color PRIMARY_500_80 = new Color(26, 103, 204);
    public static final Color WHITE_80 = new Color(255, 255, 255, 204);
    public static final Color TRANSPARENT = new Color(0, 0, 0, 0);

    private long timeLast = System.currentTimeMillis();
    private float downloadPercent = 0f;
    private ITweaker transformer = null;
    private static final Logger logger = LogManager.getLogger("OneConfigLoader");

    public OneConfigLoader() {
        File oneConfigDir = new File(Launch.minecraftHome, "OneConfig");

        boolean update = true;
        String channel = "release";
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(Files.newInputStream(new File(oneConfigDir, "OneConfig.json").toPath()), StandardCharsets.UTF_8))) {
            JsonObject config = new JsonParser().parse(reader).getAsJsonObject();
            update = config.get("autoUpdate").getAsBoolean();
            channel = config.get("updateChannel").getAsInt() == 0 ? "release" : "snapshot";
        } catch (Exception ignored) {
        }

        if (!oneConfigDir.exists() && !oneConfigDir.mkdir())
            throw new IllegalStateException("Could not create OneConfig dir!");

        Object mcVersion = "1.8.9";
        try {
            mcVersion = ForgeVersion.class.getDeclaredField("mcVersion").get(null);
            System.out.println("OneConfig has detected the version " + mcVersion + ". If this is false, report this at https://inv.wtf/polyfrost");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Getting the Minecraft version failed, defaulting to 1.8.9. Please report this to https://inv.wtf/polyfrost");
        }

        File oneConfigFile = new File(oneConfigDir, "OneConfig (" + mcVersion + "-forge" + ").jar");

        if (!isInitialized(oneConfigFile)) {
            JsonElement json = update ? getRequest("https://api.polyfrost.cc/oneconfig/" + mcVersion + "-forge") : null;

            if (json != null && json.isJsonObject()) {
                JsonObject jsonObject = json.getAsJsonObject();

                if (jsonObject.has(channel) && jsonObject.getAsJsonObject(channel).has("url")
                        && jsonObject.getAsJsonObject(channel).has("sha256")) {

                    String checksum = jsonObject.getAsJsonObject(channel).get("sha256").getAsString();
                    String downloadUrl = jsonObject.getAsJsonObject(channel).get("url").getAsString();

                    if (!oneConfigFile.exists() || !checksum.equals(getChecksum(oneConfigFile))) {
                        File newOneConfigFile = new File(oneConfigDir, "OneConfig-NEW (" + mcVersion + "-forge" + ").jar");
                        downloadFile(downloadUrl, newOneConfigFile);
                        if (newOneConfigFile.exists() && checksum.equals(getChecksum(newOneConfigFile))) {
                            try {
                                Files.move(newOneConfigFile.toPath(), oneConfigFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                                logger.info("Updated OneConfig");
                            } catch (IOException ignored) {
                            }
                        } else {
                            if (newOneConfigFile.exists()) newOneConfigFile.delete();
                            logger.error("Failed to update OneConfig, trying to continue...");
                        }
                    }
                }
            }

            if (!oneConfigFile.exists()) showErrorScreen();
            addToClasspath(oneConfigFile);
        }
        try {
            transformer = ((ITweaker) Launch.classLoader.findClass("cc.polyfrost.oneconfig.internal.plugin.asm.OneConfigTweaker").newInstance());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
            showErrorScreen();
        }
    }

    private boolean isInitialized(File file) {
        try {
            URL url = file.toURI().toURL();

            return Arrays.asList(((URLClassLoader) Launch.classLoader.getClass().getClassLoader()).getURLs()).contains(url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void addToClasspath(File file) {
        try {
            URL url = file.toURI().toURL();
            Launch.classLoader.addURL(url);
            ClassLoader classLoader = Launch.classLoader.getClass().getClassLoader();
            Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
            method.setAccessible(true);
            method.invoke(classLoader, url);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void downloadFile(String url, File location) {
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

    private static JsonElement getRequest(String site) {
        try {
            URL url = new URL(site);
            HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
            con.setRequestProperty("User-Agent", "OneConfig-Loader");
            con.setRequestMethod("GET");
            int status = con.getResponseCode();
            if (status != 200) {
                logger.error("API request failed, status code " + status);
                return null;
            }
            try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
                String inputLine;
                StringBuilder content = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine);
                }
                JsonParser parser = new JsonParser();
                return parser.parse(content.toString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String getChecksum(File file) {
        try (FileInputStream in = new FileInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[1024];
            int count;
            while ((count = in.read(buffer)) != -1) {
                digest.update(buffer, 0, count);
            }
            byte[] digested = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : digested) {
                sb.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
            }
            return sb.toString();
        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }


    @Override
    public void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile) {
        if (transformer != null) transformer.acceptOptions(args, gameDir, assetsDir, profile);
    }

    @Override
    public void injectIntoClassLoader(LaunchClassLoader classLoader) {
        if (transformer != null) transformer.injectIntoClassLoader(classLoader);
    }

    @Override
    public String getLaunchTarget() {
        return transformer != null ? transformer.getLaunchTarget() : null;
    }

    @Override
    public String[] getLaunchArguments() {
        return transformer != null ? transformer.getLaunchArguments() : new String[0];
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

    private void showErrorScreen() {
        try {
            Icon icon = new ImageIcon(Objects.requireNonNull(getClass().getResource("/assets/oneconfig-loader/icon.png")));
            UIManager.put("OptionPane.background", GRAY_900);
            UIManager.put("Panel.background", GRAY_900);
            UIManager.put("OptionPane.messageForeground", WHITE_80);
            UIManager.put("Button.background", PRIMARY_500);
            UIManager.put("Button.select", PRIMARY_500_80);
            UIManager.put("Button.foreground", WHITE_80);
            UIManager.put("Button.focus", TRANSPARENT);
            int response = JOptionPane.showOptionDialog(
                    null,
                    "OneConfig has failed to download!\n" +
                            "Please join our discord server at https://polyfrost.cc/discord\n" +
                            "for support, or try again later.",
                    "OneConfig has failed to download!", JOptionPane.OK_CANCEL_OPTION, JOptionPane.ERROR_MESSAGE, icon,
                    new Object[]{"Join Discord", "Close"}, "Join Discord"
            );
            if (response == 0) {
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(new URI("https://polyfrost.cc/discord"));
                }
            }
        } catch (Exception ignored) {
        } finally {
            try {
                Method exit = Class.forName("java.lang.Shutdown").getDeclaredMethod("exit", int.class);
                exit.setAccessible(true);
                exit.invoke(null, 1);
            } catch (Exception e) {
                System.exit(1);
            }
        }
    }
}
