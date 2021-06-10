package org.chrisoft.jline4mcdsrv;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.Toml4jConfigSerializer;
import net.fabricmc.api.ModInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class JLineForMcDSrvMain implements ModInitializer
{
	public static JLineForMcDSrvConfig config;
	public static final Logger LOGGER = LogManager.getLogger("jline4mcdsrv");

	@Override
	public void onInitialize()
	{
		AutoConfig.register(JLineForMcDSrvConfig.class, Toml4jConfigSerializer::new);
		config = AutoConfig.getConfigHolder(JLineForMcDSrvConfig.class).getConfig();
	}
}
