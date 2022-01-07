package org.chrisoft.jline4mcdsrv;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.appender.rewrite.RewritePolicy;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.jline.reader.LineReader;

public class JLineAppender extends AbstractAppender {

    protected LineReader lr;
    protected RewritePolicy policy;

    public JLineAppender(LineReader lr) {
        super("JLine", null, PatternLayout.newBuilder().withPattern(JLineForMcDSrvMain.config.logPattern).build(), false, Property.EMPTY_ARRAY);
        this.lr = lr;
    }

    public void setRewritePolicy(RewritePolicy policy) {
        this.policy = policy;
    }

    @Override
    public void append(LogEvent event) {
        if (policy != null)
            event = policy.rewrite(event);

        if (lr.isReading())
            lr.callWidget(lr.CLEAR);

        lr.getTerminal().writer().print(getLayout().toSerializable(event).toString());

        if (lr.isReading()) {
            lr.callWidget(lr.REDRAW_LINE);
            lr.callWidget(lr.REDISPLAY);
        }
        lr.getTerminal().writer().flush();
    }
}
