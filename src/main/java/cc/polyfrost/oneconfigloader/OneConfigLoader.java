package cc.polyfrost.oneconfigloader;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;

import javax.imageio.ImageIO;
import javax.net.ssl.HttpsURLConnection;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;


public class OneConfigLoader implements IFMLLoadingPlugin {
    private long timeLast = System.currentTimeMillis();
    private final IFMLLoadingPlugin transformer;

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

        File oneConfigFile = new File(oneConfigDir, "OneConfig (1.8.9).jar");

        if (!isInitialized(oneConfigFile)) {
            JsonElement json = update ? getRequest("https://api.polyfrost.cc/oneconfig/1.8.9-forge") : null;

            if (json != null && json.isJsonObject()) {
                JsonObject jsonObject = json.getAsJsonObject();

                if (jsonObject.has(channel) && jsonObject.getAsJsonObject(channel).has("url")
                        && jsonObject.getAsJsonObject(channel).has("sha256")) {

                    String checksum = jsonObject.getAsJsonObject(channel).get("sha256").getAsString();
                    String downloadUrl = jsonObject.getAsJsonObject(channel).get("url").getAsString();

                    if (!oneConfigFile.exists() || !checksum.equals(getChecksum(oneConfigFile))) {
                        System.out.println("Updating OneConfig...");

                        File newOneConfigFile = new File(oneConfigDir, "OneConfig-NEW (1.8.9).jar");
                        downloadFile(downloadUrl, newOneConfigFile);
                        String newChecksum = getChecksum(newOneConfigFile);
                        if(!checksum.equals(newChecksum)) {
                            newOneConfigFile.delete();
                            throw new SecurityException("Checksum mismatch! Expected " + checksum + ", but got " + newChecksum + "!");
                        }
                        if (newOneConfigFile.exists()) {
                            try {
                                Files.move(newOneConfigFile.toPath(), oneConfigFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                                System.out.println("Updated OneConfig");
                            } catch (IOException ignored) {
                            }
                        } else {
                            if (newOneConfigFile.exists()) newOneConfigFile.delete();
                            System.out.println("Failed to update OneConfig, trying to continue...");
                        }
                    }
                }
            }

            if (!oneConfigFile.exists()) throw new IllegalStateException("OneConfig jar doesn't exist");
            addToClasspath(oneConfigFile);
        }
        try {
            transformer = ((IFMLLoadingPlugin) Launch.classLoader.findClass("cc.polyfrost.oneconfig.internal.plugin.LoadingPlugin").newInstance());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
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
        DownloadUI ui = new DownloadUI();
        try {
            URLConnection con = new URL(url).openConnection();
            con.setUseCaches(false);
            con.setRequestProperty("User-Agent", "OneConfig-Loader");
            InputStream in = con.getInputStream();
            int length = con.getContentLength();
            if(location.exists()) {
                System.out.println("Deleting old file...");
                location.delete();
            }
            location.createNewFile();
            System.out.println("Downloading new version of OneConfig... (" + length / 1024f + "KB)");
            for(byte[] buffer = new byte[1024]; in.read(buffer) != -1;) {
                Files.write(location.toPath(), buffer, StandardOpenOption.APPEND);
                float downloadPercent = (float) location.length() / (float) length;
                ui.update(downloadPercent);
                if(downloadPercent > 1f) {
                    downloadPercent = 1f;
                }
                if(System.currentTimeMillis() - timeLast > 1000) {
                    timeLast = System.currentTimeMillis();
                    System.out.println("Downloaded " + (location.length() / 1024f) + "KB out of " + (length / 1024f) + "KB (" + (downloadPercent * 100) + "%)");
                }
            }
            // remove empty bytes from the end of the file
            in.close();
            ui.dispose();
            System.out.println("Download finished successfully");
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
    public String[] getASMTransformerClass() {
        return transformer == null ? new String[]{} : transformer.getASMTransformerClass();
    }

    @Override
    public String getModContainerClass() {
        return transformer == null ? null : transformer.getModContainerClass();
    }

    @Override
    public String getSetupClass() {
        return transformer == null ? null : transformer.getSetupClass();
    }

    @Override
    public void injectData(Map<String, Object> data) {
        if (transformer != null) transformer.injectData(data);
    }

    @Override
    public String getAccessTransformerClass() {
        return transformer == null ? null : transformer.getAccessTransformerClass();
    }

    private static class DownloadUI extends JFrame {
        private BufferedImage base;
        private final Color GRAY_700 = new Color(34, 35, 38);       // Gray 700
        private final Color WHITE_60 = new Color(1f, 1f, 1f, 0.6f);    // White 60%
        private final Color PRIMARY_500 = new Color(26, 103, 255);    // Primary 500
        private Font inter;
        private float progress = 0f;
        public DownloadUI() {
            super("OneConfigLoader");
            Image icon;
            try {
                base = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/assets/frame.png")));
                icon = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/assets/icon.png")));
                inter = Font.createFont(Font.TRUETYPE_FONT, Objects.requireNonNull(getClass().getResourceAsStream("/assets/fonts/Regular.otf"))).deriveFont(3f);
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("Failed to display download UI, continuing anyway");
                return;
            }
            setIconImage(icon);
            setSize(300, 100);
            setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
            setUndecorated(true);
            setLocationRelativeTo(null);
            setBackground(new Color(0, 0, 0, 0));
            setVisible(true);
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
            g2d.fillRoundRect(12, 75, (int) (275 * progress), 12, 12, 12);
            g2d.setFont(inter);
            g2d.setColor(WHITE_60);
            g2d.drawString("Downloading... (" + (int) (progress * 100) + "%)", 115, 94);
            g2d.dispose();
        }

        public void update(float progress) {
            this.progress = progress;
            repaint();
        }
    }
}
