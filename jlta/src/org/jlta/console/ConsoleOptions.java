package org.jlta.console;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.OptionHandlerFilter;

/**
 * Command line options for console data receiver
 */
public class ConsoleOptions {
    @Option(name = "-host", required = true,
            usage = "Host name where agent is started")
    public String host;
    @Option(name = "-port", required = true,
            usage = "Port which agent listens to")
    public int port;
    @Option(name="-period", usage = "Period to query agent for data. Seconds. Default is 60")
    public int period = 60;
    @Option(name = "-retry", usage="Retry count in case of communication error with server. -1 - indefinite, 0 - no retry")
    public int retryCount;

    private CmdLineParser parser;


    public void initFromCommandLine(String[] args) throws CmdLineException {
        parser = new CmdLineParser(this);
        parser.setUsageWidth(80);
        parser.parseArgument(args);
    }

    public void handleIncorrectCmdLine(CmdLineException e) {
        System.err.println(e.getMessage());
        System.err.println("java "+Console.class.getName()+" [options...] arguments...");
        parser.printUsage(System.err);
        System.err.println();

        System.err.println(" Example: java "+Console.class.getName()+" "+parser.printExample(OptionHandlerFilter.ALL));
    }


}
