package org.jlta.agent;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jlta.agent.Tracking.ThreadData.ThreadState;

public class Tracking
{
  private static final Map<Integer, ThreadData> data = new ConcurrentHashMap<Integer, ThreadData>();

  public static class ThreadData
  {
    private long elapsed;

    public final StackTraceElement[] newThreadStack;
    public long startTime;
    public ThreadState state;
    private final String name;

    public enum ThreadState
    {
      ALLOCATED,
      STARTED,
      FINISHED;
    }

    public ThreadData(Thread t)
    {
      name = t.getName();
      StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
      if ((stackTrace != null) && (stackTrace.length > 4))
      {
        newThreadStack = Arrays.copyOfRange(stackTrace, 3, stackTrace.length);
      }
      else
      {
        newThreadStack = new StackTraceElement[0];
      }
      state = ThreadState.ALLOCATED;
    }

    public void runEnter()
    {
      startTime = System.currentTimeMillis();
      state = ThreadState.STARTED;
    }

    public void runReturn()
    {
      elapsed = System.currentTimeMillis() - startTime;
      state = ThreadState.FINISHED;
    }
  }

  private static AtomicBoolean shutdownHookRequired = new AtomicBoolean(true);

  public static void newThread(Thread t)
  {
    if (shutdownHookRequired.getAndSet(false))
    {
      Runtime.getRuntime().addShutdownHook(new Thread()
      {
        @Override
        public void run()
        {
          System.out.println();
          System.out.println(" >> Collected Thread Data:");
          System.out.println();
          for (ThreadData tdata : data.values())
          {
            System.out.println(tdata.name + " : " + tdata.state);
            if (tdata.state == ThreadState.FINISHED)
            {
              System.out.println(" >> Runtime: " + tdata.elapsed + " ms");
            }
            for (StackTraceElement stackline : tdata.newThreadStack)
            {
              System.out.println(" > " + stackline.toString());
            }
            System.out.println();
          }
        }
      });
    }

    data.put(t.hashCode(), new ThreadData(t));
  }

  private static final ThreadLocal<Boolean> sEnterTracked = new ThreadLocal<Boolean>()
  {
    @Override
    protected Boolean initialValue()
    {
      return Boolean.FALSE;
    }
  };

  public static void runEnter(Thread t)
  {
    if (!sEnterTracked.get())
    {
      sEnterTracked.set(Boolean.TRUE);
      ThreadData threadData = data.get(t.hashCode());
      threadData.runEnter();
    }
  }

  private static final ThreadLocal<Boolean> sReturnTracked = new ThreadLocal<Boolean>()
  {
    @Override
    protected Boolean initialValue()
    {
      return Boolean.FALSE;
    }
  };

  public static void runReturn(Thread t)
  {
    if (!sReturnTracked.get())
    {
      sReturnTracked.set(Boolean.TRUE);
      ThreadData threadData = data.get(t.hashCode());
      threadData.runReturn();
    }
  }

}
