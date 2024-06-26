package org.chrisoft.jline4mcdsrv;

import net.minecraft.server.dedicated.MinecraftDedicatedServer;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.rewrite.RewriteAppender;
import org.apache.logging.log4j.core.appender.rewrite.RewritePolicy;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.filter.AbstractFilterable;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.utils.AttributedStyle;

import java.io.IOError;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toCollection;
import static org.chrisoft.jline4mcdsrv.JLineForMcDSrvMain.CONFIG;
import static org.chrisoft.jline4mcdsrv.JLineForMcDSrvMain.LOGGER;
import static org.jline.utils.AttributedStyle.*;

public class Console
{
	public static final Set<String> DEOBFUSCATING_APPENDERS = Stream.of("NotEnoughCrashesDeobfuscatingAppender", "StackDeobfAppender")
		.collect(collectingAndThen(toCollection(HashSet::new), Collections::unmodifiableSet));

	public static MinecraftDedicatedServer server;
	private static IOError lastError = null;

	public static void run() {
		MinecraftDedicatedServer srv = Objects.requireNonNull(server); // captureServer() happens-before

		do {
			LineReader lr = buildLineReader(srv); // note: can throw IOError as well (little we can do)

			updateLogConfig(lr);

			if (lastError != null) {
				LOGGER.error("restarted terminal due to IO error:", lastError);
				lastError = null;
			}

			try {
				processCommands(srv, lr);
			} catch (IOError e) {
				// try to recover by closing and restarting the terminal
				try {
					lr.getTerminal().close();
				} catch (IOException ignored) { }

				lastError = e;
			}
		} while (lastError != null);
	}

	private static void processCommands(MinecraftDedicatedServer srv, LineReader lr) {
		while (!srv.isStopped() && srv.isRunning()) {
			try {
				// readLine can read multi-line inputs which we manually split up
				String[] lines = lr.readLine(CONFIG.prompt).split("\\n");

				for (String cmd : lines) {
					cmd = cmd.trim();

					if (cmd.isEmpty())
						continue;

					srv.enqueueCommand(cmd, srv.getCommandSource());

					if (cmd.equals("stop"))
						return;
				}
			} catch (EndOfFileException | UserInterruptException e) {
				srv.enqueueCommand("stop", srv.getCommandSource());
			}
		}
	}

	private static LineReader buildLineReader(MinecraftDedicatedServer srv) throws IOError {
		return LineReaderBuilder.builder()
			.completer(new MinecraftCommandCompleter(srv.getCommandManager().getDispatcher(), srv.getCommandSource()))
			.highlighter(new MinecraftCommandHighlighter(srv.getCommandManager().getDispatcher(), srv.getCommandSource()))
			.variable(LineReader.SECONDARY_PROMPT_PATTERN, "/")
			.option(LineReader.Option.DISABLE_EVENT_EXPANSION, true)
			.build();
	}

	private static void updateLogConfig(LineReader lr) {
		LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
		LoggerConfig conf = ctx.getRootLogger().get();

		// copy SysOut Appender and AppenderRef level and filters
		Configuration configuration = ctx.getConfiguration();
		Appender sysOut = configuration.getAppender("SysOut");
		AppenderRef sysOutRef = configuration.getRootLogger().getAppenderRefs()
			.stream().filter(ref -> ref.getRef().equals("SysOut")).findAny().orElse(null);

		Filter filter = null;
		if (sysOut instanceof AbstractFilterable) {
			filter = ((AbstractFilterable) sysOut).getFilter();
		}

		Level refLevel = null;
		Filter refFilter = null;
		if (sysOutRef != null) {
			refLevel = sysOutRef.getLevel();
			refFilter = sysOutRef.getFilter();
		}

		// compatibility hack for Not Enough Crashes / StackDeobfuscator (note: stack trace deobfuscation was removed in NEC >= 4.3.0)
		RewritePolicy policy = getDeobfuscatingRewritePolicy(conf).orElse(null);
		if (policy != null) {
			removeSysOutFromObfuscatingAppenders(ctx, conf, policy);
		}

		JLineAppender jlineAppender = new JLineAppender(lr, filter, policy);
		jlineAppender.start();

		// replace SysOut appender with JLine appender
		conf.removeAppender("SysOut");
		conf.removeAppender("JLine");
		conf.addAppender(jlineAppender, refLevel, refFilter);
		ctx.updateLoggers();
	}

	/** Read the RewritePolicy Not Enough Crashes or StackDeobfuscator uses to deobfuscate stack traces */
	private static Optional<RewritePolicy> getDeobfuscatingRewritePolicy(LoggerConfig conf) {
		return conf.getAppenders().values().stream()
			.filter(appender -> DEOBFUSCATING_APPENDERS.contains(appender.getName()) || appender instanceof JLineAppender)
			.map(appender -> {
				if (appender instanceof JLineAppender) {
					return ((JLineAppender) appender).getRewritePolicy();
				}

				try {
					// Could be replaced by a Mixin Accessor but speed isn't an issue, and this hasn't failed yet
					Field field = appender.getClass().getDeclaredField("rewritePolicy");
					field.setAccessible(true);
					return (RewritePolicy) field.get(appender);
				} catch (Exception e) {
					LOGGER.error("Couldn't read deobfuscating RewritePolicy", e);
					return null;
				}
			})
			.filter(Objects::nonNull)
			.findAny();
	}

	private static void removeSysOutFromObfuscatingAppenders(LoggerContext ctx, LoggerConfig conf, RewritePolicy policy) {
		/*
		 * This method is pretty hacky: we remove, recreate and readd NEC's appender
		 * For (MIT-licensed) reference see:
		 * https://github.com/natanfudge/Not-Enough-Crashes/blob/147495dd4097017f4d243ead7f7e20d0ccfb7d40/notenoughcrashes/src/main/java/fudge/notenoughcrashes/DeobfuscatingRewritePolicy.java#L17-L41
		 */

		// get all AppenderRefs except SysOut
		List<AppenderRef> appenderRefs = new ArrayList<>(conf.getAppenderRefs());
		appenderRefs.removeIf(ref -> ref.getRef().equals("SysOut"));

		// wrap them in a RewriteAppender
		RewriteAppender rewriteAppender = RewriteAppender.createAppender(
				"WrappedDeobfuscatingAppender",
				"true",
				appenderRefs.toArray(new AppenderRef[0]),
				ctx.getConfiguration(),
				policy,
				null
		);
		rewriteAppender.start();

		for (String name : DEOBFUSCATING_APPENDERS) {
			conf.removeAppender(name);
		}

		conf.removeAppender("WrappedDeobfuscatingAppender");
		conf.addAppender(rewriteAppender, null, null);
	}

	public static AttributedStyle applyMinecraftStyle(char c, AttributedStyle style, AttributedStyle defaultStyle) {
		// reset
		if (('0' <= c && c <= '9') || ('a' <= c && c <= 'f') || c == 'r')
			style = defaultStyle;

		switch (c) {
			case 'l': { style = style.bold(); break; }
			case 'o': { style = style.italic(); break; }
			case 'n': { style = style.underline(); break; }
			case 'k': { style = CONFIG.concealObfuscatedText ? style.conceal() : style.blink(); break; }
			case 'm': { style = style.crossedOut(); break; }
			case '0': { style = style.foreground(BLACK); if (CONFIG.styleContrastBackground) style = style.background(BRIGHT | WHITE); /* workaround for invisible text */ break; }
			case '1': { style = style.foreground(BLUE); if (CONFIG.styleContrastBackground) style = style.background(BRIGHT | WHITE); break; }
			case '2': { style = style.foreground(GREEN); break; }
			case '3': { style = style.foreground(CYAN); break; }
			case '4': { style = style.foreground(RED); break; }
			case '5': { style = style.foreground(MAGENTA); break; }
			case '6': { style = style.foreground(YELLOW); break; }
			case '7': { style = style.foreground(WHITE); /* gray */ break; }
			case '8': { style = style.foreground(BRIGHT | BLACK); /* dark grey */ break; }
			case '9': { style = style.foreground(BRIGHT | BLUE); break; }
			case 'a': { style = style.foreground(BRIGHT | GREEN); break; }
			case 'b': { style = style.foreground(BRIGHT | CYAN); break; }
			case 'c': { style = style.foreground(BRIGHT | RED); break; }
			case 'd': { style = style.foreground(BRIGHT | MAGENTA); break; }
			case 'e': { style = style.foreground(BRIGHT | YELLOW); break; }
			case 'f': { style = style.foreground(BRIGHT | WHITE); if (CONFIG.styleContrastBackground) style = style.background(BLACK);} // white break; }
		}

		return style;
	}

	// by vlad2305m, https://github.com/chirs241097/jline4mcdsrv/issues/18#issue-1533489282
	public static String applyMinecraftStyle(String s) {
		s = s
			.replace("§r", "\033[0m")     // reset
			.replace("§l", "\033[1m")     // bold
			.replace("§o", "\033[3m")     // italic
			.replace("§n", "\033[4m")     // underline
			.replace("§k", CONFIG.concealObfuscatedText ? "\033[8m" : "\033[5m")     // obfuscated (conceal or blink)
			.replace("§m", "\033[9m")     // strikethrough
			.replace("§0", "\033[0;30m")  // black
			.replace("§1", "\033[0;34m")  // blue
			.replace("§2", "\033[0;32m")  // green
			.replace("§3", "\033[0;36m")  // cyan
			.replace("§4", "\033[0;31m")  // red
			.replace("§5", "\033[0;35m")  // purple
			.replace("§6", "\033[0;33m")  // gold
			.replace("§7", "\033[0;37m")  // gray
			.replace("§8", "\033[0;90m")  // D grey
			.replace("§9", "\033[0;94m")  // B blue
			.replace("§a", "\033[0;92m")  // B green
			.replace("§b", "\033[0;96m")  // B cyan
			.replace("§c", "\033[0;91m")  // B red
			.replace("§d", "\033[0;95m")  // B purple
			.replace("§e", "\033[0;93m")  // B yellow
			.replace("§f", "\033[0;97m"); // white

		if (CONFIG.styleContrastBackground) s = s
				.replace("\033[0;30m", "\033[0;30m\033[0;107m") // black on white (cmd)
				.replace("\033[0;34m", "\033[0;34m\033[0;107m") // blue on white (powershell)
				.replace("\033[0;97m", "\033[0;97m\033[0;40m"); // white on black (light theme)

		return s;
	}
}
