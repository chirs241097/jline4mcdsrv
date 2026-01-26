package org.chrisoft.jline4mcdsrv.mixin;

import net.minecraft.server.dedicated.DedicatedServer;
import org.chrisoft.jline4mcdsrv.Console;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DedicatedServer.class)
public abstract class DServerInject
{
	@Inject(at = @At("HEAD"), method = "initServer()Z")
	private void captureServer(CallbackInfoReturnable<Boolean> info)
	{
		Console.server = (DedicatedServer) (Object) this;
	}
}
