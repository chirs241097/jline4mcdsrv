package org.chrisoft.jline4mcdsrv;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import net.minecraft.server.command.ServerCommandSource;
import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class MinecraftCommandCompleter implements Completer
{
	private final CommandDispatcher<ServerCommandSource> cmdDispatcher;
	private final ServerCommandSource cmdSrc;

	public MinecraftCommandCompleter(CommandDispatcher<ServerCommandSource> cmdDispatcher, ServerCommandSource cmdSrc)
	{
		this.cmdDispatcher = cmdDispatcher;
		this.cmdSrc = cmdSrc;
	}

	@Override
	public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates)
	{
		//trim previous and next lines
		String input = line.line();
		int left = input.lastIndexOf('\n', line.cursor() - 1); left = left == -1 ? 0 : left + 1;
		int right = input.indexOf('\n', line.cursor()); right = right == -1 ? input.length() : right;
		String[] inputBuf = {input.substring(0, left), input.substring(left, right), input.substring(right)};
		//get the suggestions
		ParseResults<ServerCommandSource> parseRes = cmdDispatcher.parse(inputBuf[1], cmdSrc);
		CompletableFuture<Suggestions> cs = cmdDispatcher.getCompletionSuggestions(parseRes, line.cursor()-inputBuf[0].length());
		Suggestions sl = cs.join();
		for (Suggestion s : sl.getList()) {
			String applied = s.apply(inputBuf[1]);
			ParsedLine apl = reader.getParser().parse(inputBuf[0] + applied + inputBuf[2], line.cursor());
			String candidateStr = apl.word();
			candidates.add(new Candidate(candidateStr));
		}
	}
}
