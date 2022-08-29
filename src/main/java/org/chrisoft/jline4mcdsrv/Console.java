package org.chrisoft.jline4mcdsrv;

import net.minecraft.server.dedicated.MinecraftDedicatedServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.rewrite.RewriteAppender;
import org.apache.logging.log4j.core.appender.rewrite.RewritePolicy;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.jetbrains.annotations.Nullable;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.chrisoft.jline4mcdsrv.JLineForMcDSrvMain.LOGGER;

public class Console
{
	public static MinecraftDedicatedServer server;

	public static void run()
	{
		MinecraftDedicatedServer srv = Objects.requireNonNull(server); // captureServer() happens-before

		LineReader lr = LineReaderBuilder.builder()
				.completer(new MinecraftCommandCompleter(srv.getCommandManager().getDispatcher(), srv.getCommandSource()))
				.highlighter(new MinecraftCommandHighlighter(srv.getCommandManager().getDispatcher(), srv.getCommandSource()))
				.variable(LineReader.SECONDARY_PROMPT_PATTERN, "/")
				.option(LineReader.Option.DISABLE_EVENT_EXPANSION, true)
				.build();

		JLineAppender jlineAppender = new JLineAppender(lr);
		jlineAppender.start();

		LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
		Logger rootLogger = ctx.getRootLogger();
		LoggerConfig conf = rootLogger.get();

		// compatibility hack for Not Enough Crashes
		RewritePolicy policy = getNECRewritePolicy(conf);
		if (policy != null) {
			jlineAppender.setRewritePolicy(policy);
			removeSysOutFromNECRewriteAppender(ctx, conf, policy);
		}

		// replace SysOut appender with Console appender
		conf.removeAppender("SysOut");
		conf.addAppender(jlineAppender, null, null);
		ctx.updateLoggers();

		while (!srv.isStopped() && srv.isRunning()) {
			try {
				// readLine can read multi-line inputs which we manually split up
				String[] lines = lr.readLine("/").split("\\n");

				for (String cmd : lines) {
					cmd = cmd.trim();

					if (cmd.isEmpty())
						continue;

					srv.enqueueCommand(cmd, srv.getCommandSource());

					if (cmd.equals("stop"))
						return;
				}
			} catch (EndOfFileException|UserInterruptException e) {
				srv.enqueueCommand("stop", srv.getCommandSource());
				return;
			}
		}
	}

	/** Read the RewritePolicy Not Enough Crashes uses to deobfuscate stack traces */
	@Nullable
	private static RewritePolicy getNECRewritePolicy(LoggerConfig conf) {
		for (Appender appender : conf.getAppenders().values()) {
			if (appender.getName().equals("NotEnoughCrashesDeobfuscatingAppender")) {
				try {
					// Could be replaced by a Mixin Accessor but this would add NEC as a compile dependency
					// This should be fine since speed isn't an issue and it hasn't failed yet
					Field field = appender.getClass().getDeclaredField("rewritePolicy");
					field.setAccessible(true);
					return (RewritePolicy) field.get(appender);
				} catch (Exception e) {
					LOGGER.error("Couldn't read Not Enough Crashes' rewritePolicy", e);
				}
			}
		}

		return null;
	}

	private static void removeSysOutFromNECRewriteAppender(LoggerContext ctx, LoggerConfig conf, RewritePolicy policy) {
		/*
		 * This method is pretty hacky: we remove, recreate and readd NEC's appender
		 * For (MIT-licensed) reference see:
		 * https://github.com/natanfudge/Not-Enough-Crashes/blob/147495dd4097017f4d243ead7f7e20d0ccfb7d40/notenoughcrashes/src/main/java/fudge/notenoughcrashes/DeobfuscatingRewritePolicy.java#L17-L41
		 */

		// get all AppenderRefs except SysOut
		List<AppenderRef> appenderRefs = new ArrayList<>(conf.getAppenderRefs());
		appenderRefs.removeIf((ref) -> ref.getRef().equals("SysOut"));

		// wrap them in a RewriteAppender
		RewriteAppender rewriteAppender = RewriteAppender.createAppender(
				"NotEnoughCrashesDeobfuscatingAppender",
				"true",
				appenderRefs.toArray(new AppenderRef[0]),
				ctx.getConfiguration(),
				policy,
				null
		);
		rewriteAppender.start();

		conf.removeAppender("NotEnoughCrashesDeobfuscatingAppender");
		conf.addAppender(rewriteAppender, null, null);
	}
}
