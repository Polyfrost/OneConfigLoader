# OneConfig Early Loading Stages

This repository contains the source code for early loading of [OneConfig].

This below documentation is not intended for end users or consuming developers of OneConfig.
Check out the [Polyfrost Documentation] instead.

## Stage 0: Wrapper

The **Wrapper** is the first entrypoint for OneConfig and is called as a tweaker class by [LaunchWrapper] on 
`cc.polyfrost.oneconfig.loader.wrapper.OneConfigWrapper`.

This causes it to also be a standalone mod for downloading OneConfig, however you should prefer using 
[OneConfig Bootstrap] for that purpose.

The **Wrapper** checks the loaded MinecraftForge version and attempts to load the **Loader** corresponding to that 
version, by first downloading it, and then delegating loading to `cc.polyfrost.oneconfigloader.OneConfigLoader`.

## Stage 1: Loader

The Loader is also a tweaker class, although this one is loaded by the wrapper, and should not be loaded by the FML
directly. In theory a version specific loader may exist and be loaded by the wrapper, but currently both 1.8.9 and
1.12.2 use the same loader. The loader then downloads the version specific OneConfig jar and
loads `cc.polyfrost.oneconfig.internal.plugin.asm.OneConfigTweaker` and delegates to that tweaker.

## API

The API hosts download URLs and hashes of downloaded files at:
 - https://api.polyfrost.cc/oneconfig/1.8.9-forge
 - https://api.polyfrost.cc/oneconfig/1.12.2-forge

[Polyfrost Documentation]: https://docs.polyfrost.cc/
[OneConfig]: https://github.com/Polyfrost/OneConfig

[LaunchWrapper]: https://github.com/Mojang/legacy-launcher
[OneConfig Bootstrap]: https://github.com/Polyfrost/OneConfig-Bootstrap