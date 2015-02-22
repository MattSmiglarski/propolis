package propolis.shared;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.pattern.color.ANSIConstants;
import ch.qos.logback.core.pattern.color.ForegroundCompositeConverterBase;

public class ColourThreadConverter extends ForegroundCompositeConverterBase<ILoggingEvent> {

    @Override
    protected String getForegroundColorCode(ILoggingEvent event) {

        String threadName = event.getThreadName();

        if (threadName.startsWith("client")) {
            return ANSIConstants.MAGENTA_FG;
        } else if (threadName.startsWith("server")) {
            return ANSIConstants.CYAN_FG;
        } else {
            return ANSIConstants.DEFAULT_FG;
        }
    }
}
