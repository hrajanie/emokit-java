// Copyright Samuel Halliday 2012
package fommil.logging;

import java.util.logging.*;

/**
 * Custom filter that matches categories defined in the
 * logging properties file against the classname of
 * {@link LogRecord}s.
 * <p/>
 * Note that this may appear to be the behaviour of the
 * default filter, but in fact the default filter is
 * matching against the {@link Logger}'s name and
 * convention is to use classnames as logger names. This
 * is not true in Actor systems such as Akka.
 * <p/>
 * Classnames need to be calculated for most log messages:
 * there is therefore a performance cost to use this filter.
 *
 * @author Sam Halliday
 * @see <a href="http://akka.io">Akka</a>
 */
public class ClassnameFilter implements Filter {

    @Override
    public boolean isLoggable(LogRecord record) {
        LogManager manager = LogManager.getLogManager();
        String source = record.getSourceClassName();
        if (source == null)
            return true;
        String prop = source + ".level";
        String level = manager.getProperty(prop);
        if (level == null)
            return true;
        // Level parse errors reported on startup
        Level allowed = Level.parse(level);
        if (allowed == Level.OFF)
            return false;
        return (allowed.intValue() <= record.getLevel().intValue());
    }
}
