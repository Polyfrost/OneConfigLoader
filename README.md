# OneConfig Early Loading Stages

This repository contains the source code for early loading of [OneConfig].

This below documentation is not intended for end users or consuming developers of OneConfig, check out the
[Polyfrost Documentation] instead.

## Stage 0: Wrapper

The **Wrapper** is the first entrypoint for OneConfig and is called depending on the platform as:
- an `ITweaker` class by [LaunchWrapper] 
- an `ITransformationService` by [ModLauncher]
- a `PreLaunchEntrypoint` by [Fabric Loader]

This first entrypoint delegates further loading to the platform-agnostic 
`cc.polyfrost.oneconfig.loader.stage0.Stage0Loader` class.

This causes the **Wrapper** to also be a "standalone mod" for downloading OneConfig, however you should prefer using 
[OneConfig Bootstrap] for that purpose.

The **Wrapper** checks the loaded mod loader version and attempts to load the **Loader** corresponding to that 
version, by first downloading it, and then delegating loading to `cc.polyfrost.oneconfig.loader.stage1.Stage1Loader`.

## Stage 1: Loader

The Loader is also a tweaker class, although this one is loaded by the wrapper, and should not be loaded by the FML
directly. In theory a version specific loader may exist and be loaded by the wrapper, but currently both 1.8.9 and
1.12.2 use the same loader. The loader then downloads the version specific OneConfig jar and
loads `cc.polyfrost.oneconfig.internal.plugin.asm.OneConfigTweaker` and delegates to that tweaker.

**Note**: For retro-compatibility reasons, the **Loader** also contains the legacy 
`cc.polyfrost.oneconfigloader.OneConfigLoader` and `cc.polyfrost.oneconfig.loader.OneConfigLoader` classes, which
are loaded by older versions of the stage 0 **Wrapper**, but should not be used otherwise.

## API

The API hosts download URLs and hashes of downloaded files at:
 - https://api.polyfrost.cc/oneconfig/1.8.9-forge
 - https://api.polyfrost.cc/oneconfig/1.12.2-forge

[Polyfrost Documentation]: https://docs.polyfrost.cc/
[OneConfig]: https://github.com/Polyfrost/OneConfig

[LaunchWrapper]: https://github.com/Mojang/legacy-launcher
[ModLauncher]: https://github.com/McModLauncher/modlauncher
[Fabric Loader]: https://github.com/FabricMC/fabric-loader
[OneConfig Bootstrap]: https://github.com/Polyfrost/OneConfig-Bootstrap