# OneConfig Early Loading Stages

This repository contains the source code for early loading of [OneConfig].

This below documentation is not intended for end users or consuming developers of OneConfig, check out the
[Polyfrost Documentation] instead.

## Overview

### Design principles:
- No hardcoding
  - If a class entrypoint is needed, it should be specified in either the Jar's Manifest or a resource file (ex. [`loader.json`]).
    -  This allows for easier versioning and compatibility, notably future API changes.
- Minimal platform-specific code
  - Platform-relative code should be kept to a minimum, and should be isolated to the `stage0` bundle; everything else should be platform-agnostic.
- No extra-downloading
  - If a file is needed, it should belong in a local shared cache on the computer.

## Stage 0: Wrapper

The **Wrapper** is the first entrypoint for **OneConfig** and is called depending on the platform as:

- an [`ITweaker`](./stage0/src/launchwrapper/java/org/polyfrost/oneconfig/loader/stage0/LaunchWrapperTweaker.java) for [LaunchWrapper]
- an [`ITransformationService`](./stage0/src/modlauncher/java/org/polyfrost/oneconfig/loader/stage0/ModLauncherLegacyTransformationService.java) for [ModLauncher]
  - and [another one](./stage0/src/modlauncher/java9/org/polyfrost/oneconfig/loader/stage0/j9/ModLauncherTransformationService.java) for [ModLauncher] 9+
- a [`LanguageAdapter`](./stage0/src/fabriclike/java/org/polyfrost/oneconfig/loader/stage0/FabricLikeLanguageAdapter.java) for [Fabric Loader] (and subsequently, [Quilt Loader])

This direct first entrypoint encapsulates all the required metadata and methods and abstracts them into a platform-agnostic
[`Capabilities`](./common/src/main/java/org/polyfrost/oneconfig/loader/ILoader.java#L33) interface, and delegates further loading to the 
[`org.polyfrost.oneconfig.loader.stage0.Stage0Loader`](./stage0/src/main/java/org/polyfrost/oneconfig/loader/stage0/Stage0Loader.java) class.


> [!IMPORTANT]  
> This causes the **Wrapper** to also be a "standalone mod" for downloading OneConfig, however you should **not** use those artifacts, and should be using [OneConfig Bootstrap] for that purpose.

The **Wrapper** checks the loaded mod loader version and attempts to load the **Loader** corresponding to that
version, by first downloading it, trying cache, and then delegating loading to `org.polyfrost.oneconfig.loader.stage1.Stage1Loader`.

## Stage 1: Loader

The **Loader** is where the actual loading of OneConfig happens. It is a platform-agnostic jar that tries to download
the **OneConfig** jar from the API, its dependencies to a cache, and hands off loading to it, also delegating 
**capabilities** obtained earlier in the chain, such as setting-up Transformers or mixing-in ClassLoading. 

**Note**: For backwards-compatibility reasons, the **Loader** also contains the two legacy classes:
- `cc.polyfrost.oneconfigloader.OneConfigLoader` 
- `cc.polyfrost.oneconfig.loader.OneConfigLoader`

Those classes are located in the [`src/legacy/java`] source-set and are loaded by older versions of the stage 0 **Wrapper**.

> [!CAUTION]  
> These classes should **never** be used as they are prone to removal at any time.

## API

The **Polyfrost API** regarding artifacts and metadata is currently a work-in-progress, and will be documented in the future.

If you wish to see the older version, please check out the [main branch readme](https://github.com/Polyfrost/OneConfigLoader?tab=readme-ov-file#api).

## License

This project and its files (code, assets, etc.), except where stated otherwise, is licensed under the GNU Lesser General Public License v3.0, see the [LICENSE](./LICENSE) file for more information.

[Polyfrost Documentation]: https://docs.polyfrost.org/
[OneConfig]: https://github.com/Polyfrost/OneConfig

[LaunchWrapper]: https://github.com/Mojang/legacy-launcher
[ModLauncher]: https://github.com/McModLauncher/modlauncher
[Fabric Loader]: https://github.com/FabricMC/fabric-loader
[Quilt Loader]: https://github.com/QuiltMC/quilt-loader

[OneConfig Bootstrap]: https://github.com/Polyfrost/OneConfig-Bootstrap
