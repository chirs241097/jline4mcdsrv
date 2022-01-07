package org.chrisoft.jline4mcdsrv.mixin;

import org.chrisoft.jline4mcdsrv.Console;
import org.chrisoft.jline4mcdsrv.JLineForMcDSrvMain;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(targets = {"net.minecraft.server.dedicated.MinecraftDedicatedServer$1"})
public abstract class DServerConsoleThreadInject
{
    /**
     * @author Fourmisain
     * @reason Replaces the vanilla console handler logic
     */
    @Overwrite
    public void run()
    {
        JLineForMcDSrvMain.LOGGER.info("Starting JLine4MCDSrv.");
        Console.run();
    }
}
