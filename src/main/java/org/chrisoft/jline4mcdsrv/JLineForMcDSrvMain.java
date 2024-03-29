package org.chrisoft.jline4mcdsrv;

import net.fabricmc.api.ModInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class JLineForMcDSrvMain implements ModInitializer
{
	public static final String MOD_ID = "jline4mcdsrv";
	public static final JLineForMcDSrvConfig CONFIG;
	public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

	static {
		CONFIG = new JLineForMcDSrvConfig();

		try {
			CONFIG.read();
		} catch (Exception e) {
			LOGGER.error("couldn't fully read config file! {}", e.getMessage());
		}

		try {
			CONFIG.write();
		} catch (Exception e) {
			LOGGER.error("couldn't write config file! {}", e.getMessage());
		}
	}

	@Override
	public void onInitialize() {

	}
}
