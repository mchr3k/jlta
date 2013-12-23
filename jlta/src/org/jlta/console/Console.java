package org.jlta.console;

import org.kohsuke.args4j.CmdLineException;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Console version of threading data consumer. Periodically queries remote agent and displays only new data.
 * Remote agent's stale data (FINISHED threads which are already logged) is pruned
 */
public class Console {
    public static void main(String[] args) {
        ConsoleOptions options = new ConsoleOptions();
        try {
            options.initFromCommandLine(args);
            startDataProcessing(options);
        } catch (CmdLineException e) {
            options.handleIncorrectCmdLine(e);
        }
    }

    private static void startDataProcessing(final ConsoleOptions options) {
        final Timer t = new Timer("Server Polling");
        final ServerPollingTask pollingTask = new ServerPollingTask(options.host, options.port, options.retryCount);
        t.schedule(new TimerTask() {
            @Override
            public void run() {
                if(pollingTask.isStopped()) {
                    t.cancel();
                } else {
                    pollingTask.poll();
                }
            }
        }, 0, options.period*1000);
    }

}
