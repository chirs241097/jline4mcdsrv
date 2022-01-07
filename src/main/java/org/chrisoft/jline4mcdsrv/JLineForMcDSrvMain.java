package org.chrisoft.jline4mcdsrv;

import net.fabricmc.api.ModInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class JLineForMcDSrvMain implements ModInitializer
{
	public static String MOD_ID = "jline4mcdsrv";
	public static JLineForMcDSrvConfig config;
	public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

	static {
		config = new JLineForMcDSrvConfig();

		try {
			config.read();
		} catch (Exception e) {
			LOGGER.error("couldn't read config file! {}", e.getMessage());
		}

		try {
			config.write();
		} catch (Exception e) {
			LOGGER.error("couldn't write config file! {}", e.getMessage());
		}
	}

	@Override
	public void onInitialize() {

	}
}
