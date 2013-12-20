package org.jlta.common;

import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Map.Entry;

/**
 * //TODO
 *
 * @author: akabelytskyi
 * @since: 12.1
 */
public class TrackingData {
    public final String jvmId;
    public final Map<Integer, ThreadData> threadsMap;

    public TrackingData() {
        jvmId = ManagementFactory.getRuntimeMXBean().getName();
        threadsMap =  new ConcurrentHashMap<Integer, ThreadData>();
    }

    public TrackingData(String jvmId, Map<Integer, ThreadData> threadsMap) {
        this.jvmId = jvmId;
        this.threadsMap = threadsMap;
    }

    /**
     * Clear all threads' data
     */
    public void clear() {
        threadsMap.clear();
    }

    /**
     * Prepare static copy for transmission over network
     */
    public TrackingData staticCopy() {
        Map<Integer,ThreadData> staticData = new HashMap<Integer, ThreadData>(threadsMap);
        return new TrackingData(jvmId,staticData);
    }

    /**
     * Remove 'FINISHED' threads records. Useful for long running applications to avoid memory leaks
     */
    public void prune() {
        Set<Entry<Integer, ThreadData>> entries = threadsMap.entrySet();
        for(Iterator<Entry<Integer, ThreadData>> it=entries.iterator(); it.hasNext();) {
            Entry<Integer, ThreadData> entry = it.next();
            if(entry.getValue().state == ThreadData.ThreadState.FINISHED){
                it.remove();
            }
        }
    }
}
