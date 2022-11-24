package cc.polyfrost.oneconfig.loader;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import net.fabricmc.loader.impl.ModContainerImpl;
import net.fabricmc.loader.impl.discovery.ClasspathModCandidateFinder;
import net.fabricmc.loader.impl.discovery.ModCandidate;
import net.fabricmc.loader.impl.launch.FabricLauncherBase;
import net.fabricmc.loader.impl.metadata.LoaderModMetadata;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

public class OneConfigLoader extends OneConfigLoaderBase implements PreLaunchEntrypoint {
    private boolean update;
    private String channel;
    private PreLaunchEntrypoint loader = null;
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

        String mcVersion = FabricLoader.getInstance().getModContainer("minecraft").map(it -> it.getMetadata().getVersion().getFriendlyString()).orElseThrow(() -> new RuntimeException("Could not find Minecraft version"));
        return new LoaderInfo("Jar", mcVersion, "fabric", "prelaunch");
    }

    @Override
    protected boolean showDownloadUI() {
        return false; //todo
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
    protected void addToClasspath(File file) { //todo
        try {
            FabricLauncherBase.getLauncher().addToClassPath(file.toPath());
            ModCandidate container;
            try {
                Method method = ModCandidate.class.getDeclaredMethod("createPlain", List.class, LoaderModMetadata.class, boolean.class, Collection.class);
                method.setAccessible(true);
            } catch (Exception e) {
                Method method = ModCandidate.class.getDeclaredMethod("createPlain", Path.class, LoaderModMetadata.class, boolean.class, Collection.class);
                method.setAccessible(true);
            }
            ModContainerImpl modContainerImpl = new ModContainerImpl();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected boolean isInitialized(File file, String clazz) {
        return FabricLauncherBase.getLauncher().isClassLoaded(clazz);
    }

    @Override
    protected boolean getNextInstance() {
        try {
            loader = (PreLaunchEntrypoint) FabricLauncherBase.getClass("cc.polyfrost.oneconfig.internal.plugin.OneConfigPreLaunch").newInstance();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    @Override
    public void onPreLaunch() {
        if (loader != null) {
            loader.onPreLaunch();
        }
    }
}
