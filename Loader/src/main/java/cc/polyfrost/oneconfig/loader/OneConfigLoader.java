package cc.polyfrost.oneconfig.loader;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraftforge.common.ForgeVersion;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

public class OneConfigLoader extends OneConfigLoaderBase implements ITweaker {
    private boolean update;
    private String channel;
    private static ITweaker loader = null;

    @Override
    protected LoaderInfo provideLoaderInfo() {
        boolean update = true;
        String channel = "release";
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(Files.newInputStream(new File(new File("./OneConfig"), "OneConfig.json").toPath()), StandardCharsets.UTF_8))) {
            JsonObject config = new JsonParser().parse(reader).getAsJsonObject();
            update = config.get("autoUpdate").getAsBoolean();
            channel = config.get("updateChannel").getAsInt() == 0 ? "release" : "snapshot";
        } catch (Exception ignored) {
        }
        this.update = update;
        this.channel = channel;

        String mcVersion = "1.8.9";
        try {
            mcVersion = ForgeVersion.mcVersion;
        } catch (Throwable e) {
            e.printStackTrace();
            System.out.println("Getting the Minecraft version failed, defaulting to 1.8.9. Please report this to https://inv.wtf/polyfrost");
        }
        return new LoaderInfo("Jar", mcVersion, "forge", "launchwrapper");
    }

    @Override
    protected boolean shouldUpdate() {
        return update;
    }

    @Override
    protected File provideFile(LoaderInfo loaderInfo) {
        File oneConfigDir = new File("./OneConfig");
        if (!oneConfigDir.exists() && !oneConfigDir.mkdirs())
            throw new IllegalStateException("Could not create OneConfig dir!");

        return new File(oneConfigDir, "OneConfig (" + loaderInfo.mcVersion + "-" + loaderInfo.modLoader + ").jar");
    }

    @Override
    protected JsonInfo provideJsonInfo(JsonObject object, LoaderInfo loaderInfo) {
        try {
            if (object.has(channel) && object.getAsJsonObject(channel).has("url")
                    && object.getAsJsonObject(channel).has("sha256")) {

                String checksum = object.getAsJsonObject(channel).get("sha256").getAsString();
                String downloadUrl = object.getAsJsonObject(channel).get("url").getAsString();
                return new JsonInfo(checksum, downloadUrl, true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new JsonInfo(null, null, false);
    }

    @Override
    protected void addToClasspath(File file) {
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

    @Override
    protected boolean isInitialized(File file) {
        try {
            URL url = file.toURI().toURL();
            return Arrays.asList(((URLClassLoader) Launch.classLoader.getClass().getClassLoader()).getURLs()).contains(url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    protected boolean getNextInstance() {
        try {
            loader = ((ITweaker) Launch.classLoader.findClass("cc.polyfrost.oneconfig.internal.plugin.asm.OneConfigTweaker").newInstance());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return loader != null;
    }

    @Override
    public void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile) {
        if (loader != null) loader.acceptOptions(args, gameDir, assetsDir, profile);
    }

    @Override
    public void injectIntoClassLoader(LaunchClassLoader classLoader) {
        if (loader != null) loader.injectIntoClassLoader(classLoader);
    }

    @Override
    public String getLaunchTarget() {
        return loader != null ? loader.getLaunchTarget() : null;
    }

    @Override
    public String[] getLaunchArguments() {
        return loader != null ? loader.getLaunchArguments() : new String[0];
    }
}
