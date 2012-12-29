// Copyright Samuel Halliday 2008
package fommil.logging;

import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;

/**
 * A {@link Formatter} that may be customised in a {@code logging.properties}
 * file. The syntax of the property
 * {@code org.cakesolutions.logging.CustomFormatter.format}
 * specifies the output. A newline will be appended to the string and the
 * following special characters will be expanded (case sensitive):-
 * <ul>
 * <li>{@code %m} - message</li>
 * <li>{@code %L} - log level</li>
 * <li>{@code %n} - name of the logger</li>
 * <li>{@code %t} - a timestamp (in ISO-8601 "yyyy-MM-dd HH:mm:ss Z" format)</li>
 * <li>{@code %M} - source method name (if available, otherwise "?")</li>
 * <li>{@code %c} - source class name (if available, otherwise "?")</li>
 * <li>{@code %C} - source simple class name (if available, otherwise "?")</li>
 * <li>{@code %T} - thread ID</li>
 * <li>{@code %e} - exception message</li>
 * <li>{@code %E} - exception class</li>
 * <li>{@code %S} - pruned stack trace (best at the end)</li>
 * </ul>
 * The default format is {@value #DEFAULT_FORMAT}. Curly brace characters are not
 * allowed.
 * <p>
 * Stack trace elements beginning with entries from
 * {@code fommil.logging.CustomFormatter.stackExclude}
 * will not be printed.
 *
 * @author Samuel Halliday
 */
public class CustomFormatter extends Formatter {

    private static final String DEFAULT_FORMAT = "%L: %m [%c.%M %t]";

    private static final String[] DEFAULT_EXCLUDE = new String[0];

    private final MessageFormat messageFormat;

    private final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");

    private final String[] excluded;

    /** */
    public CustomFormatter() {
        // load the format from logging.properties
        String propName = getClass().getName() + ".format";
        String format = LogManager.getLogManager().getProperty(propName);
        messageFormat = readFormat(format);
        String excludePropName = getClass().getName() + ".stackExclude";
        String excludeProperty = LogManager.getLogManager().getProperty(excludePropName);
        excluded = readExclude(excludeProperty);
    }

    @Override
    public String format(LogRecord record) {
        String[] arguments = new String[11];
        // %L
        arguments[0] = record.getLevel().toString();
        // %m
        // ignoring localisation
        arguments[1] = record.getMessage();
        // sometimes the message is empty, but there is a throwable
        if (arguments[1] == null || arguments[1].length() == 0) {
            Throwable thrown = record.getThrown();
            if (thrown != null) {
                arguments[1] = thrown.getMessage();
            }
        }
        // %M
        if (record.getSourceMethodName() != null) {
            arguments[2] = record.getSourceMethodName();
        } else {
            arguments[2] = "?";
        }
        // %t
        Date date = new Date(record.getMillis());
        synchronized (dateFormat) {
            arguments[3] = dateFormat.format(date);
        }
        // %c
        if (record.getSourceClassName() != null) {
            arguments[4] = record.getSourceClassName();
        } else {
            arguments[4] = "?";
        }
        // %T
        arguments[5] = Integer.valueOf(record.getThreadID()).toString();
        // %n
        arguments[6] = record.getLoggerName();
        // %C
        int start = arguments[4].lastIndexOf(".") + 1;
        if (start > 0 && start < arguments[4].length()) {
            arguments[7] = arguments[4].substring(start);
        } else {
            arguments[7] = arguments[4];
        }

        if (record.getThrown() != null) {
            // %e
            arguments[8] = record.getThrown().getMessage();
            // %E
            arguments[9] = record.getThrown().getClass().getName();
            // %S
            arguments[10] = filteredStackTrace(record.getThrown());
        } else {
            arguments[8] = "";
            arguments[9] = "";
            arguments[10] = "";
        }

        synchronized (messageFormat) {
            return messageFormat.format(arguments);
        }
    }

    private MessageFormat readFormat(String format) {
        if (format == null || format.trim().length() == 0) {
            format = DEFAULT_FORMAT;
        }
        if (format.contains("{") || format.contains("}")) {
            throw new IllegalArgumentException("curly braces not allowed");
        }
        format = format.replace("%L", "{0}").replace("%m", "{1}").replace("%M",
                "{2}").replace("%t", "{3}").replace("%c", "{4}").replace("%T", "{5}").
                replace("%n", "{6}").replace("%C", "{7}").
                replace("%e", "{8}").replace("%E", "{9}").replace("%S", "{10}") + "\n";

        return new MessageFormat(format);
    }

    private String[] readExclude(String property) {
        if (property == null || property.trim().length() == 0) {
            return DEFAULT_EXCLUDE;
        }
        String[] pieces = property.split(" ");
        for (int i = 0 ; i < pieces.length ; i++) {
            pieces[i] = pieces[i].trim();
        }
        return pieces;
    }

    private String filteredStackTrace(Throwable thrown) {
        StackTraceElement[] trace = thrown.getStackTrace();
        if (trace.length == 0)
            return "";
        StringBuilder builder = new StringBuilder();
        elements: for (StackTraceElement traceElement : trace) {
            String element = traceElement.toString();
            for (String exclude : excluded)
                if (element.startsWith(exclude))
                    continue elements;
            builder.append("\n\tat ");
            builder.append(element);
        }
        if (thrown.getCause() != null) {
            builder.append("\n");
            builder.append("CAUSE: ");
            builder.append(thrown.getCause().getClass().getName());
            builder.append(": ");
            builder.append(thrown.getCause().getMessage());
        }
        return builder.toString();
    }
}
