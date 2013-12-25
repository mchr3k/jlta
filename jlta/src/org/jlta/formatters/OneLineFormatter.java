package org.jlta.formatters;

import org.jlta.common.ServerDataProcessor;
import org.jlta.common.StackTraceArrayWrap;
import org.jlta.common.ThreadData;
import org.jlta.common.TrackingData;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.jlta.common.ThreadData.ThreadState;

/**
 * Formats data for one thread as a single line which later can be processed by grep and awk easily
 */
public class OneLineFormatter implements IDataFormatter {
    public static final String SEP = "|";

    @Override
    public String format(ServerDataProcessor processor) {

        StringBuilder str = new StringBuilder();

        Map<StackTraceArrayWrap, List<ThreadData>> sortedGroupedData = processor.getSortedGroupedData();
        String jvmId = processor.getJvmId();
        for (Map.Entry<StackTraceArrayWrap, List<ThreadData>> entry : sortedGroupedData.entrySet()) {

            StackTraceArrayWrap stack = entry.getKey();
            List<ThreadData> threads = entry.getValue();
            str.append(escape(jvmId)).append(SEP).append(threads.size()).append(SEP);
            for(ThreadData td: threads){
                str.append(td.id).append(SEP)
                        .append(escape(td.name)).append(SEP)
                        .append(td.state).append(SEP)
                        .append(td.startTime).append(SEP)
                        .append(td.elapsed).append(SEP);
            }
            for(StackTraceElement frame: stack.stack){
                str.append(frame.toString()).append("|");
            }
            str.append("\n");
        }
        return str.toString();
    }

    public TrackingData load(String data) {
        return load(new StringReader(data));
    }

    /**
     * jvmId|threadNum|tid1|tName1|status1|start1|elapsed1|tid2|...|frame1|frame2|...|
     */
    public TrackingData load(Reader reader) {
        BufferedReader br = new BufferedReader(reader);
        TrackingData data = new TrackingData();
        int lineCounter = 1;

        String line = null;

        do {
            try {
                line = br.readLine();
            } catch (IOException ignored) {
                line = null;
            }
            if(line == null || line.isEmpty())
                continue;
            String[] fields = line.split("\\"+SEP);
            if(fields.length < 2 ){
                formatError(lineCounter,"");
                continue;
            }
            int idx = 0;
            String jvmId = fields[idx++];
            if(!jvmId.equals(data.jvmId)) {
                //new monitoring started
                data = new TrackingData(jvmId, new HashMap<Integer, ThreadData>());
            }
            int threadNum = 0;
            try {
                threadNum = Integer.parseInt(fields[idx++]);
            } catch (NumberFormatException e) {
                formatError(lineCounter, e.getMessage());
                continue;
            }
            //ensure all threads info and parse stack trace first
            int threadsInfoEnd = idx + threadNum * 5;
            if(fields.length <  threadsInfoEnd) {
                formatError(lineCounter,"");
            }
            StackTraceElement[] stack = parseStackTrace(fields, threadsInfoEnd);
            List<ThreadData> threads = parseThreads(fields, idx, threadsInfoEnd, stack);

            for(ThreadData th: threads) {
                ThreadData previous = data.threadsMap.get(th.id);
                //Update thread data in the map only if its state was capture later
                if(previous ==null || previous.state.compareTo(th.state) < 0) {
                    data.threadsMap.put(th.id,th);
                }
            }

            ++lineCounter;
        }while (line != null);
        return data;
    }

    private List<ThreadData> parseThreads(String[] fields, int begin, int end, StackTraceElement[] stack) {
        List<ThreadData> threads = new ArrayList<ThreadData>();
        for(int idx=begin; idx < end; ++idx) {
            int id = 0;
            try {
                id = Integer.parseInt(fields[idx++]);
            } catch (NumberFormatException ignored) {
            }
            String name = fields[idx++];
            ThreadState state = null;
            try {
                state = ThreadState.valueOf(fields[idx++]);
            } catch (IllegalArgumentException ignored) {
            }
            long start = 0l;
            try {
                start = Long.parseLong(fields[idx++]);
            } catch (NumberFormatException ignored) {
            }
            long elapsed = 0l;
            try {
                elapsed = Long.parseLong(fields[idx]);
            } catch (NumberFormatException ignored) {
            }
            //fail-safe state
            if(state == null){
                if(elapsed>0) state = ThreadState.FINISHED;
                else if(start > 0) state = ThreadState.STARTED;
                else state = ThreadState.ALLOCATED;
            }
            threads.add(new ThreadData(id,name,stack,"",start,elapsed,state));
        }
        return threads;
    }

    private static final Pattern STACK_FRAME = Pattern.compile("(.*?)\\((.*?)\\)");
    private static final String NATIVE_METHOD = "Native Method";
    private static final String UNKNOWN_SOURCE = "Unknown Source";

    private StackTraceElement[] parseStackTrace(String[] fields, int start) {
        List<StackTraceElement> stack = new ArrayList<StackTraceElement>();

        for(int idx = start; idx < fields.length; ++idx){
            //skip empty line
            if(fields[idx].trim().isEmpty())
                continue;
            StackTraceElement frame;
            Matcher matcher = STACK_FRAME.matcher(fields[idx]);
            if(matcher.matches()){
                String clz = matcher.group(1);
                String method = "";
                String source = matcher.group(2);
                int line = -1;

                int pos = clz.lastIndexOf('.');
                if(pos > 0) {
                    method = clz.substring(pos+1);
                    clz = clz.substring(0,pos);
                }

                if (NATIVE_METHOD.equals(source)) {
                    line = -2;
                } else if(UNKNOWN_SOURCE.equals(source)) {
                    source = null;
                } else {
                    pos = source.lastIndexOf(':');
                    if(pos >= 0) {
                        try {
                            line = Integer.parseInt(source.substring(pos+1));
                            source = source.substring(0,pos);
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }
                frame = new StackTraceElement(clz, method, source, line);

            } else {
                frame = new StackTraceElement("Invalid","frame",null,-1);
            }
            stack.add(frame);
        }

        return stack.toArray(new StackTraceElement[stack.size()]);
    }

    private void formatError(int lineCounter, String message) {
        System.err.println(String.format("Invalid log format on line %d. %s", lineCounter, message));
    }

    private String escape(String name) {
        if(name == null)
            return "";
        return name.replaceAll("\\|","_");
    }


    public static void main(String[] args) throws IOException {
        OneLineFormatter line = new OneLineFormatter();
        Reader reader = null;
        try {
            reader = new FileReader("D:\\log");
            TrackingData data = line.load(reader);
            ServerDataProcessor processor = new ServerDataProcessor(data);
            processor.processData(true, true, true, false, ServerDataProcessor.CONTEXT_ALL, false, 0);
            UIDataFormatter formatter = new UIDataFormatter();
            System.out.println(formatter.format(processor));
        } finally {
            if (reader != null) {
                reader.close();
            }
        }

    }
}
