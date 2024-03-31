open module org.polyfrost.oneconfig.loader {
    requires java.desktop;
    requires static transitive cpw.mods.modlauncher;
    requires static org.jetbrains.annotations;
    requires static org.apache.logging.log4j;
    requires static org.apache.logging.log4j.core;
    requires static lombok;

    exports org.polyfrost.oneconfig.loader.stage0.j9;

    uses cpw.mods.modlauncher.api.ITransformationService;
    provides cpw.mods.modlauncher.api.ITransformationService
            with org.polyfrost.oneconfig.loader.stage0.j9.ModLauncherTransformationService;
}