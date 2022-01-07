package org.chrisoft.jline4mcdsrv;

import com.moandjiezana.toml.Toml;
import com.moandjiezana.toml.TomlWriter;
import net.fabricmc.loader.api.FabricLoader;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static java.nio.file.StandardOpenOption.*;
import static org.chrisoft.jline4mcdsrv.JLineForMcDSrvMain.MOD_ID;

public class JLineForMcDSrvConfig
{
	public transient static Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve(MOD_ID + ".toml");

	// Represent AttributedStyle.BLACK = 0, AttributedStyle.RED = 1, ... AttributedStyle.WHITE = 7
	private enum StyleColor {
		BLACK, RED, GREEN, YELLOW, BLUE, MAGENTA, CYAN, WHITE
	}

	public String logPattern = "%style{[%d{HH:mm:ss}]}{blue} "
		+ "%highlight{[%t/%level]}{FATAL=red, ERROR=red, WARN=yellow, INFO=green, DEBUG=green, TRACE=blue} "
		+ "%style{(%logger{1})}{cyan} "
		+ "%highlight{%msg%n}{FATAL=red, ERROR=red, WARN=normal, INFO=normal, DEBUG=normal, TRACE=normal}";

	private StyleColor[] highlightColors = {StyleColor.CYAN, StyleColor.YELLOW, StyleColor.GREEN, StyleColor.MAGENTA, StyleColor.WHITE};

	public transient int[] highlightColorIndices;

	public JLineForMcDSrvConfig() {
		validatePostLoad();
	}

	public void read() throws Exception {
		if (!Files.exists(JLineForMcDSrvConfig.CONFIG_PATH))
			return;

		try (BufferedReader reader = Files.newBufferedReader(CONFIG_PATH)) {
			Toml toml = new Toml().read(reader);

			logPattern = toml.getString("logPattern", logPattern);

			if (toml.contains("highlightColors")) {
				// convert list of strings to array of StyleColors
				highlightColors = toml.getList("highlightColors").stream()
					.map(obj -> StyleColor.valueOf((String) obj)).toArray(StyleColor[]::new);
			}

			validatePostLoad();
		}
	}

	public void write() throws Exception {
		try (BufferedWriter writer = Files.newBufferedWriter(CONFIG_PATH, WRITE, TRUNCATE_EXISTING, CREATE);) {
			new TomlWriter().write(this, writer);
		}
	}

	public void validatePostLoad() throws IllegalArgumentException {
		// transform the color names into their AttributedStyle index
		highlightColorIndices = new int[highlightColors.length];
		for (int i = 0; i < highlightColors.length; i++) {
			highlightColorIndices[i] = highlightColors[i].ordinal();
		}
	}
}
