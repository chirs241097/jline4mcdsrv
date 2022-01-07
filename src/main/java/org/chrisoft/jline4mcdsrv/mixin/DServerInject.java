package org.chrisoft.jline4mcdsrv.mixin;

import net.minecraft.server.dedicated.MinecraftDedicatedServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.chrisoft.jline4mcdsrv.Console;

@Mixin(MinecraftDedicatedServer.class)
public abstract class DServerInject
{
    @Inject(at = @At("TAIL"), method = "setupServer()Z")
    private void setupServer(CallbackInfoReturnable<Boolean> info)
    {
        Console.setup((MinecraftDedicatedServer) (Object) this);
    }
}
