package org.jlta.common;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Performs postprocessing of server data
 */
public class ServerDataProcessor {
    private static final Pattern unnamedThread = Pattern.compile("Thread-[\\d]+");
    private static final Pattern unnamedTimer = Pattern.compile("Timer-[\\d]+");

    private final Map<Integer, ThreadData> data;
    private final TrackingData trackingData;

    private int allThreadCount;
    private int filteredThreadCount;
    private List<String> contextsList;
    private Map<StackTraceArrayWrap, List<ThreadData>> sortedGroupedData;

    public ServerDataProcessor(TrackingData trackingData) {
        this.trackingData = trackingData;
        this.data = trackingData.threadsMap;
    }

    public void processData(boolean allocated,
                             boolean started,
                             boolean finished,
                             boolean ignorenamed,
                             final String context,
                             boolean stacklimit,
                             int stacklimitval)
    {
        Set<String> contextsSet = new HashSet<String>();
        final Map<StackTraceArrayWrap, List<ThreadData>> groupedData = new HashMap<StackTraceArrayWrap, List<ThreadData>>();
        allThreadCount = 0;
        filteredThreadCount = 0;
        for (ThreadData tdata : data.values())
        {
            allThreadCount++;
            if ((tdata.context != null) &&
                    (tdata.context.trim().length() > 0))
            {
                contextsSet.add(tdata.context.trim());
            }

            if ((tdata.state == ThreadData.ThreadState.ALLOCATED) && !allocated)
                continue;
            if ((tdata.state == ThreadData.ThreadState.STARTED) && !started)
                continue;
            if ((tdata.state == ThreadData.ThreadState.FINISHED) && !finished)
                continue;
            if (ignorenamed &&
                    (tdata.name != null) &&
                    !unnamedThread.matcher(tdata.name).matches() &&
                    !unnamedTimer.matcher(tdata.name).matches())
            {
                continue;
            }
            if (!"All".equals(context) &&
                    !context.equals(tdata.context))
            {
                continue;
            }

            StackTraceElement[] tstack = tdata.newThreadStack;
            if (stacklimit &&
                    (tstack.length > stacklimitval))
            {
                tstack = Arrays.copyOfRange(tstack, 0, stacklimitval);
            }

            StackTraceArrayWrap stackWrap = new StackTraceArrayWrap(tstack);
            List<ThreadData> threadData = groupedData.get(stackWrap);
            if (threadData == null) {
                threadData = new ArrayList<ThreadData>();
                groupedData.put(stackWrap, threadData);
            }
            threadData.add(tdata);
            filteredThreadCount++;
        }

        sortedGroupedData = new TreeMap<StackTraceArrayWrap, List<ThreadData>>(new Comparator<StackTraceArrayWrap>()
        {
            @Override
            public int compare(StackTraceArrayWrap o1, StackTraceArrayWrap o2)
            {
                int o1Count = groupedData.get(o1).size();
                int o2Count = groupedData.get(o2).size();
                if (o1Count < o2Count){
                    return 1;
                } else if (o1Count > o2Count){
                    return -1;
                } else {
                    if (groupedData.get(o1).hashCode() < groupedData.get(o2).hashCode()) {
                        return 1;
                    } else {
                        return -1;
                    }
                }
            }
        });
        sortedGroupedData.putAll(groupedData);
        contextsList = new ArrayList<String>(contextsSet);
        Collections.sort(contextsList);
        contextsList.add(0, "All");
    }

    public int getAllThreadCount() {
        return allThreadCount;
    }

    public int getFilteredThreadCount() {
        return filteredThreadCount;
    }

    public List<String> getContextsList() {
        return contextsList;
    }

    public Map<StackTraceArrayWrap, List<ThreadData>> getSortedGroupedData() {
        return sortedGroupedData;
    }

    public String getJvmId() {
        return trackingData.jvmId;
    }
}
