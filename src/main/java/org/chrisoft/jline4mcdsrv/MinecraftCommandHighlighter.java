package org.chrisoft.jline4mcdsrv;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.context.ParsedCommandNode;
import net.minecraft.server.command.ServerCommandSource;
import org.chrisoft.jline4mcdsrv.JLineForMcDSrvConfig.StyleColor;
import org.jline.reader.Highlighter;
import org.jline.reader.LineReader;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

import java.util.regex.Pattern;

public class MinecraftCommandHighlighter implements Highlighter
{
	private final CommandDispatcher<ServerCommandSource> cmdDispatcher;
	private final ServerCommandSource cmdSrc;

	public MinecraftCommandHighlighter(CommandDispatcher<ServerCommandSource> cmdDispatcher, ServerCommandSource cmdSrc)
	{
		this.cmdDispatcher = cmdDispatcher;
		this.cmdSrc = cmdSrc;
	}

	@Override
	public AttributedString highlight(LineReader reader, String buffer)
	{
		StyleColor[] colors = JLineForMcDSrvMain.CONFIG.highlightColors;
		String[] lines = buffer.split("\\n", -1);
		AttributedStringBuilder sb = new AttributedStringBuilder();

		for (int i = 0; i < lines.length; i++) {
			String line = lines[i];
			ParseResults<ServerCommandSource> parsed = cmdDispatcher.parse(line, cmdSrc);

			int pos = 0;
			int component = -1;
			for (ParsedCommandNode<ServerCommandSource> pcn : parsed.getContext().getNodes()) {
				if (++component >= colors.length)
					component = 0;

				int start = pcn.getRange().getStart();
				int end = Math.min(pcn.getRange().getEnd(), line.length());

				if (start >= line.length())
					break;

				sb.append(line.substring(pos, start), AttributedStyle.DEFAULT);
				sb.append(line.substring(start, end), AttributedStyle.DEFAULT.foreground(colors[component].ordinal()));
				pos = end;
			}
			if (pos < line.length())
				sb.append(line.substring(pos), AttributedStyle.DEFAULT);

			if (i != lines.length - 1)
				sb.append("\n", AttributedStyle.DEFAULT);
		}

		return sb.toAttributedString();
	}

	@Override
	public void setErrorPattern(Pattern errorPattern)
	{
	}

	@Override
	public void setErrorIndex(int errorIndex)
	{
	}
}
