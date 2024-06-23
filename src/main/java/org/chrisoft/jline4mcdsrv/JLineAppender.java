package org.chrisoft.jline4mcdsrv;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.appender.rewrite.RewritePolicy;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.jline.reader.LineReader;

import static org.chrisoft.jline4mcdsrv.Console.applyMinecraftStyle;

public class JLineAppender extends AbstractAppender {

	protected final LineReader lr;
	protected RewritePolicy rewritePolicy;

	@SuppressWarnings("deprecation") // allows running on 1.16 and 1.17+
	public JLineAppender(LineReader lr) {
		super("JLine", null, PatternLayout.newBuilder().withPattern(JLineForMcDSrvMain.CONFIG.logPattern).build(), false);
		this.lr = lr;
	}

	public RewritePolicy getRewritePolicy() {
		return this.rewritePolicy;
	}

	public void setRewritePolicy(RewritePolicy policy) {
		this.rewritePolicy = policy;
	}

	@Override
	public void append(LogEvent event) {
		if (rewritePolicy != null)
			event = rewritePolicy.rewrite(event);

		if (lr.isReading())
			lr.callWidget(lr.CLEAR);

		String s = getLayout().toSerializable(event).toString();
		if (JLineForMcDSrvMain.CONFIG.applyMinecraftStyle)
			s = applyMinecraftStyle(s);
		lr.getTerminal().writer().print(s);

		if (lr.isReading()) {
			lr.callWidget(lr.REDRAW_LINE);
			lr.callWidget(lr.REDISPLAY);
		}
		lr.getTerminal().writer().flush();
	}
}
