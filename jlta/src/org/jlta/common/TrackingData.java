package org.jlta.common;

import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Wrapper object for all data capture on the server
 */
public class TrackingData implements Serializable {
    private static final long serialVersionUID = 1L;

    public final String jvmId;
    public final Map<Integer, ThreadData> threadsMap;
    public final List<String> constructorSites;

    public TrackingData() {
        jvmId = ManagementFactory.getRuntimeMXBean().getName();
        threadsMap =  new ConcurrentHashMap<Integer, ThreadData>();
        constructorSites = Collections.synchronizedList(new ArrayList<String>());
    }

    public TrackingData(String jvmId, Map<Integer, ThreadData> threadsMap, List<String> constructorSites) {
        this.jvmId = jvmId;
        this.threadsMap = threadsMap;
        this.constructorSites = constructorSites;
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
        List<String> constructorSites = new ArrayList<String>(this.constructorSites);
        return new TrackingData(jvmId,staticData,constructorSites);
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
