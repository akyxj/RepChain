package rep.log;

/**
 * Created by User on 2017/7/23.
 */
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;
/**
 * log过滤类实现
 * @author shidianyue
 * @version	1.0
 * @since	1.0
 * */
public class LogFilterExp extends Filter<ILoggingEvent> {

    @Override
    public FilterReply decide(ILoggingEvent event) {
        if (event.getMessage().contains("Opt Time")) {
            return FilterReply.ACCEPT;
        } else {
            return FilterReply.DENY;
        }
    }
}
