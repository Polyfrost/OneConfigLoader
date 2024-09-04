package org.polyfrost.oneconfig.loader.stage0;

import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.swing.*;

import cpw.mods.modlauncher.serviceapi.ILaunchPluginService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

/**
 * @author xtrm
 * @since 1.1.0
 */
public class ModLauncherLaunchPluginService implements ILaunchPluginService {
    private static final Logger LOGGER = LogManager.getLogger(ModLauncherLaunchPluginService.class);
    private static final EnumSet<Phase> ENUM_SET_NONE = EnumSet.noneOf(Phase.class);

    @Override
    public String name() {
        return "oneconfig-loader";
    }

    @Override
    public EnumSet<Phase> handlesClass(Type classType, boolean isEmpty) {
//        LOGGER.info("handlesClass({}, {})", classType, isEmpty);
        return ENUM_SET_NONE;
    }

    @Override
    public void offerResource(Path resource, String name) {
        LOGGER.info("offerResource({}, {})", resource, name);
        ILaunchPluginService.super.offerResource(resource, name);
    }

    @Override
    public void addResources(List<Map.Entry<String, Path>> resources) {
        LOGGER.info("addResources({})", resources);
        JOptionPane.showMessageDialog(null, "addResources({})", "oneconfig-loader", JOptionPane.INFORMATION_MESSAGE);
        ILaunchPluginService.super.addResources(resources);
    }

    @Override
    public void initializeLaunch(ITransformerLoader transformerLoader, Path[] specialPaths) {
        LOGGER.info("initializeLaunch({}, {})", transformerLoader, specialPaths);
        ILaunchPluginService.super.initializeLaunch(transformerLoader, specialPaths);
    }

    @Override
    public <T> T getExtension() {
        LOGGER.info("getExtension()");
        return ILaunchPluginService.super.getExtension();
    }

    @Override
    public void customAuditConsumer(String className, Consumer<String[]> auditDataAcceptor) {
        LOGGER.info("customAuditConsumer({}, {})", className, auditDataAcceptor);
        ILaunchPluginService.super.customAuditConsumer(className, auditDataAcceptor);
    }

    @Override
    public boolean processClass(Phase phase, ClassNode classNode, Type classType, String reason) {
        LOGGER.info("processClass({}, {}, {}, {})", phase, classNode, classType, reason);
        return false;
    }
}
