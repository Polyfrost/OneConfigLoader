package cc.polyfrost.oneconfigwrapper;

import cc.polyfrost.oneconfigwrapper.ssl.SSLStore;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import org.apache.logging.log4j.LogManager;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import java.io.*;
import java.lang.reflect.Method;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.CodeSource;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Map;

@IFMLLoadingPlugin.MCVersion("1.8.9")
public class OneConfigWrapper implements IFMLLoadingPlugin {
    private final IFMLLoadingPlugin loader;

    public OneConfigWrapper() {
        super();

        try {
            SSLStore sslStore = new SSLStore();
            System.out.println("Attempting to load Polyfrost certificate.");
            sslStore = sslStore.load("/ssl/polyfrost.der");
            SSLContext context = sslStore.finish();
            SSLContext.setDefault(context);
            HttpsURLConnection.setDefaultSSLSocketFactory(context.getSocketFactory());
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Failed to add Polyfrost certificate to keystore.");
        }

        File oneConfigDir = new File(Launch.minecraftHome, "OneConfig");
        if (!oneConfigDir.exists() && !oneConfigDir.mkdir())
            throw new IllegalStateException("Could not create OneConfig dir!");

        File oneConfigLoaderFile = new File(oneConfigDir, "OneConfig-Loader (1.8.9).jar");

        if (!isInitialized(oneConfigLoaderFile)) {
            JsonElement json = getRequest("https://api.polyfrost.cc/oneconfig/1.8.9-forge");

            if (json != null && json.isJsonObject()) {
                JsonObject jsonObject = json.getAsJsonObject();

                if (jsonObject.has("loader") && jsonObject.getAsJsonObject("loader").has("url")
                        && jsonObject.getAsJsonObject("loader").has("sha256")) {

                    String checksum = jsonObject.getAsJsonObject("loader").get("sha256").getAsString();
                    String downloadUrl = jsonObject.getAsJsonObject("loader").get("url").getAsString();

                    if (!oneConfigLoaderFile.exists() || !checksum.equals(getChecksum(oneConfigLoaderFile))) {
                        System.out.println("Updating OneConfig Loader...");
                        File newLoaderFile = new File(oneConfigDir, "OneConfig-Loader-NEW (1.8.9).jar");

                        downloadFile(downloadUrl, newLoaderFile);

                        if (newLoaderFile.exists() && checksum.equals(getChecksum(newLoaderFile))) {
                            try {
                                Files.move(newLoaderFile.toPath(), oneConfigLoaderFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                                System.out.println("Updated OneConfig loader");
                            } catch (IOException ignored) {}
                        } else {
                            if (newLoaderFile.exists()) newLoaderFile.delete();
                            System.out.println("Failed to update OneConfig loader, trying to continue...");
                        }
                    }
                }
            }

            if (!oneConfigLoaderFile.exists()) throw new IllegalStateException("OneConfig jar doesn't exist");
            addToClasspath(oneConfigLoaderFile);
        }
        try {
            try {
                CodeSource codeSource = this.getClass().getProtectionDomain().getCodeSource();
                if (codeSource != null) {
                    URL location = codeSource.getLocation();
                    try {
                        File file = new File(location.toURI());
                        if (file.isFile()) {
                            Launch.blackboard.put("oneconfig.wrapper.modFile", file);
                        }
                    } catch (URISyntaxException ignored) {}
                } else {
                    LogManager.getLogger().warn("No CodeSource, if this is not a development environment we might run into problems!");
                    LogManager.getLogger().warn(this.getClass().getProtectionDomain());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            loader = ((IFMLLoadingPlugin) Launch.classLoader.findClass("cc.polyfrost.oneconfigloader.OneConfigLoader").newInstance());
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
        try {
            URLConnection con = new URL(url).openConnection();
            con.setRequestProperty("User-Agent", "OneConfig-Wrapper");
            InputStream in = con.getInputStream();
            Files.copy(in, location.toPath(), StandardCopyOption.REPLACE_EXISTING);
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static JsonElement getRequest(String site) {
        try {
            URL url = new URL(site);
            HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
            con.setRequestProperty("User-Agent", "OneConfig-Wrapper");
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
        return loader == null ? null : loader.getASMTransformerClass();
    }

    @Override
    public String getModContainerClass() {
        return loader == null ? null : loader.getModContainerClass();
    }

    @Override
    public String getSetupClass() {
        return loader == null ? null : loader.getSetupClass();
    }

    @Override
    public void injectData(Map<String, Object> data) {
        if (loader != null) loader.injectData(data);
    }

    @Override
    public String getAccessTransformerClass() {
        return loader == null ? null : loader.getAccessTransformerClass();
    }
}
