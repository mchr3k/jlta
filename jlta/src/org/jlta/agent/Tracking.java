package org.jlta.agent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jlta.common.ThreadData;
import org.jlta.common.ThreadData.ThreadState;


public class Tracking
{
  public static final Map<Integer, ThreadData> data = new ConcurrentHashMap<Integer, ThreadData>();

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
      threadData.runEnter(t);
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
