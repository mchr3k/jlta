package org.jlta.formatters;

import org.jlta.common.ServerDataProcessor;
import org.jlta.common.StackTraceArrayWrap;
import org.jlta.common.ThreadData;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * UI data formatter
 */
public class UIDataFormatter implements IDataFormatter {
    public String format(ServerDataProcessor processor){
        StringBuilder str = new StringBuilder();

        str.append(new Date().toString()).append(" >> Thread Data for JVM ").append(processor.getJvmId()).append(":\n");
        str.append("\n");
        str.append("Total threads: ").append(processor.getAllThreadCount()).append("\n");
        str.append("Displayed threads: ").append(processor.getFilteredThreadCount()).append("\n");
        str.append("\n");
        Map<StackTraceArrayWrap, List<ThreadData>> sortedGroupedData = processor.getSortedGroupedData();
        if (sortedGroupedData.size() > 0)
        {
            for (Map.Entry<StackTraceArrayWrap, List<ThreadData>> entry : sortedGroupedData.entrySet())
            {
                StackTraceElement[] stack = entry.getKey().stack;
                List<ThreadData> threads = entry.getValue();
                Collections.sort(threads);
                str.append(threads.size()).append(" threads:\n");
                for (ThreadData tdata : threads)
                {
                    str.append(" > ").append(tdata.name).append(" : ").append(tdata.state);
                    if(tdata.state == ThreadData.ThreadState.STARTED || tdata.state == ThreadData.ThreadState.FINISHED) {
                        str.append(" (Started: ").append(new Date(tdata.startTime)).append(")");
                    }
                    if (tdata.state == ThreadData.ThreadState.FINISHED)
                    {
                        str.append(" (Runtime: ").append(tdata.elapsed).append(" ms)");
                    }
                    if ((tdata.context != null) && (tdata.context.length() > 0))
                    {
                        str.append(" (").append(tdata.context).append(")");
                    }
                    str.append("\n");
                }
                str.append("Stack:\n");
                for (StackTraceElement stackline : stack)
                {
                    str.append(" > ").append(stackline.toString()).append("\n");
                }
                str.append("\n");
            }
        }
        return str.toString();
    }
}
