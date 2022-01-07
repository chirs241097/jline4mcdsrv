# JLine for Minecraft Dedicated Server
![Icon](jline4mcdsrvicon.png)

A server side fabric mod to enable command history, auto completion and syntax
highlighting on the server console. Should have zero impact on the gameplay.

Note: Since Minecraft 1.17 the `-Dlog4j.skipJansi=false` command line argument is needed for `%style`/`%highlight` log output (syntax highlighting is unaffected).

This mod is configurable with `jline4mcdsrv.toml` in the `config` folder:
* `logPattern` is the pattern used for Log4J2 (documentation [here](https://logging.apache.org/log4j/2.x/manual/layouts.html#Patterns))
* `highlightColors` is a list of colors used to highlight parameters in order
   (see image above for an example)

This is my first mod for Minecraft. Actually this is also the first time for me
to do anything serious using Java. So please be gentle if you want to roast my
code.

## Downloads
The mod can be downloaded for Minecraft 1.16 onwards on [CurseForge](https://www.curseforge.com/minecraft/mc-mods/jline-for-minecraft-dedicated-server/files)
