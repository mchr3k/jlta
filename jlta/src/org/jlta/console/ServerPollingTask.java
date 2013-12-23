package org.jlta.console;

import org.jlta.common.ServerCommunication;
import org.jlta.common.ServerDataProcessor;
import org.jlta.common.ThreadData;
import org.jlta.common.TrackingData;
import org.jlta.formatters.IDataFormatter;
import org.jlta.formatters.UIDataFormatter;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import static java.util.Map.Entry;

/**
 * Perform periodic server polling and log new information
 */
public class ServerPollingTask{

    private Set<Integer> allocated = new HashSet<Integer>();
    private Set<Integer> started = new HashSet<Integer>();
    private Set<Integer> finished = new HashSet<Integer>();

    private final String host;
    private final int port;
    private final int maxRetryCount;

    private int retryCount = 0;

    private boolean stopped = false;
    private String jvmId;

    private ServerCommunication server;
    private IDataFormatter formatter;

    public ServerPollingTask(String host, int port, int retryCount) {
        this.host = host;
        this.port = port;
        this.maxRetryCount = retryCount;

        server = new ServerCommunication();
        formatter = new UIDataFormatter();
    }

    public void poll() {
        if(!stopped) {
            try {
                if(server.getState() == ServerCommunication.State.DISCONNECTED)
                    server.connect(host, port);
                server.fetch();
                server.prune();

                processReceivedData(server.getData()) ;

            }catch(IOException e){
                ++retryCount;
                stopped = maxRetryCount >= 0 && retryCount > maxRetryCount;
                String retry = "indefinitely";
                if(maxRetryCount >= 0) {
                    retry = " " + (maxRetryCount-retryCount+1) + " more times";
                }
                logError(String.format("Server connection error. %s, will retry %s", e.getMessage(), retry));
            } catch (ClassNotFoundException e) {
                stopped = true;
                logError(String.format("Version error. %s" + e.getMessage()));
            }
        }
    }

    public boolean isStopped() {
        return stopped;
    }

    private void processReceivedData(TrackingData data) {
        if(jvmId == null){
            jvmId = data.jvmId;
        }
        else if(!jvmId.equals(data.jvmId)){
            onJvmChanged(jvmId, data.jvmId);
            jvmId = data.jvmId;
        }
        //remove finished threads data which were pruned from server
        for(Iterator<Integer> it = finished.iterator(); it.hasNext();){
            Integer key = it.next();
            if(!data.threadsMap.containsKey(key)) {
                it.remove();
            }
        }
        //Remove already logged threads
        for(Iterator<Entry<Integer,ThreadData>> it = data.threadsMap.entrySet().iterator(); it.hasNext();) {
            Entry<Integer,ThreadData> entry = it.next();
            Integer key = entry.getKey();
            ThreadData td = entry.getValue();

            if(td.state == ThreadData.ThreadState.ALLOCATED) { //Allocated threads
                if (allocated.contains(key))
                    it.remove();
                else
                    allocated.add(key);
            } else if(td.state == ThreadData.ThreadState.STARTED) { //Started threads
                if(allocated.contains(key))
                    allocated.remove(key);

                if(started.contains(key))
                    it.remove();
                else
                    started.add(key);
            } else if(td.state == ThreadData.ThreadState.FINISHED) { //Finished threads
                if(allocated.contains(key))
                    allocated.remove(key);
                if(started.contains(key))
                    started.remove(key);

                if(finished.contains(key))
                    it.remove();
            }
        }
        if(!data.threadsMap.isEmpty()) {
            ServerDataProcessor processor = new ServerDataProcessor(data);
            processor.processData(true,true,true,false,ServerDataProcessor.CONTEXT_ALL,false,0);
            log(formatter.format(processor));
        }
    }

    private void log(String msg) {
        System.out.println(msg);
    }

    private void logError(String msg) {
        System.err.println(msg);
    }

    private void onJvmChanged(String oldId, String newId) {
        allocated.clear();
        started.clear();
        finished.clear();

        logError(String.format("Remote JVM was restarted. Id changed from %s to %s", oldId, newId));
    }

}

