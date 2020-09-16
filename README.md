![Project icon](https://avatars0.githubusercontent.com/u/2760645?s=144&v=4)

# Chlorine (for Forge)
![GitHub license](https://img.shields.io/github/license/HalogenMods/Chlorine.svg)
![GitHub issues](https://img.shields.io/github/issues/HalogenMods/Chlorine.svg)
![GitHub tag](https://img.shields.io/github/tag/HalogenMods/Chlorine.svg)
[![CurseForge downloads](http://cf.way2muchnoise.eu/full_408362_downloads.svg)](https://www.curseforge.com/minecraft/mc-mods/chlorine)

Chlorine is a free and open-source optimization mod for the Minecraft client that improves frame rates, reduces
micro-stutter, and fixes graphical issues in Minecraft. 

Chlorine is a fork of JellySquid's excellent fabric mod, [sodium-fabric](https://github.com/jellysquid3/sodium-fabric), ported to minecraftforge
under the terms of the LGPLv3, with additional modifications to make it fit better within the forge ecosystem. It may inherit
bugs and issues from its upstream. However, please do not file bugs with them. If the issue is on my side, I and the team will
fix it as soon as we can, and if it can be reproduced on fabric, we'll file an upstream issue.

:warning: Chlorine has had a lot of time to shape up lately, but the mod is still alpha software. You may run into small
graphical issues or crashes while using it.

### Downloads

You can find downloads for Chlorine on the [official CurseForge page](https://www.curseforge.com/minecraft/mc-mods/chlorine).

### Building from source

If you're hacking on the code or would like to compile a custom build of Chlorine from the latest sources, you'll want
to start here.

#### Prerequisites

You will need to install JDK 8 (or newer, see below) in order to build Chlorine. You can either install this through
a package manager such as [Chocolatey](https://chocolatey.org/) on Windows or [SDKMAN!](https://sdkman.io/) on other
platforms. If you'd prefer to not use a package manager, you can always grab the installers or packages directly from
[AdoptOpenJDK](https://adoptopenjdk.net/).

On Windows, the Oracle JDK/JRE builds should be avoided where possible due to their poor quality. Always prefer using
the open-source builds from AdoptOpenJDK when possible.

#### Compiling

Navigate to the directory you've cloned this repository and launch a build with Gradle using `gradlew build` (Windows)
or `./gradlew build` (macOS/Linux). If you are not using the Gradle wrapper, simply replace `gradlew` with `gradle`
or the path to it.

The initial setup may take a few minutes. After Gradle has finished building everything, you can find the resulting
artifacts in `build/libs`.

### Tuning for optimal performance

_This section is entirely optional and is only aimed at users who are interested in squeezing out every drop from their
game. Chlorine will work without issue in the default configuration of almost all launchers._

Generally speaking, newer versions of Java will provide better performance not only when playing Minecraft, but when
using Chlorine as well. The default configuration your game launcher provides will usually be some old version of Java 8
that has been selected to maximize hardware compatibility instead of performance.

For most users, these compatibility issues are not relevant, and it should be relatively easy to upgrade the game's Java
runtime and apply the required patches. For more information on upgrading and tuning the Java runtime, see the
guide [here](https://gist.github.com/jellysquid3/8a7b21e57f47f5711eb5697e282e502e).

### License

Chlorine is licensed under GNU LGPLv3, a free and open-source license. For more information, please see the
[license file](/LICENSE.txt).
