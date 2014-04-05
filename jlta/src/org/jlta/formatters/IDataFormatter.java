package org.jlta.formatters;

import org.jlta.common.ServerDataProcessor;

/**
 * Interface for logic which will format processed server data into a message
 */
public interface IDataFormatter {
    public String format(ServerDataProcessor processor);
}
