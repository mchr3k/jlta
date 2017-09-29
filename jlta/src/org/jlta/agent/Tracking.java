package org.jlta.agent;

import org.jlta.common.ThreadData;
import org.jlta.common.ThreadData.ThreadState;
import org.jlta.common.TrackingData;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.lang.reflect.*;
import java.util.concurrent.Executors;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;



public class Tracking
{
  public static final TrackingData data = new TrackingData();

  private static AtomicBoolean shutdownHookRequired = new AtomicBoolean(true);

  public static void newClass(Object t) {
    String logicalId = "nothing-";
    try {
    //if (t instanceof Executors.DefaultThreadFactory) {
      //Executors.DefaultThreadFactory x = (Executors.DefaultThreadFactory)t;
      //Field field = Executors.DefaultThreadFactory.class.getDeclaredField("poolNumber");
      Field field = t.getClass().getDeclaredField("namePrefix");
      field.setAccessible(true);
      String value = (String)field.get(t);
      logicalId = value;
    //}
    } catch (Exception e) {
    }
    StringWriter sw = new StringWriter();
    new Throwable("").printStackTrace(new PrintWriter(sw));
    String stackTrace = sw.toString();
    data.constructorSites.add(
      t.getClass().getCanonicalName() + "\t" + logicalId  + "\t" + stackTrace);
  }

  public static void newThread(Thread t)
  {
    if (shutdownHookRequired.getAndSet(false))
    {
      Runtime.getRuntime().addShutdownHook(new Thread("JLTA - JVM Shutdown Hook")
      {
        @Override
        public void run()
        {
          System.out.println();
          System.out.println(" >> Collected Thread Data:");
          System.out.println();
          for (ThreadData tdata : data.threadsMap.values())
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
    ThreadData td = new ThreadData(t);
    data.threadsMap.put(td.id, td);
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
      ThreadData threadData = data.threadsMap.get(t.hashCode());
      if (threadData != null)
      {
        threadData.runEnter();
      }
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
      ThreadData threadData = data.threadsMap.get(t.hashCode());
      if (threadData != null)
      {
        threadData.runReturn();
      }
    }
  }

  public static void setName(Thread t)
  {
    ThreadData threadData = data.threadsMap.get(t.hashCode());
    threadData.name = t.getName();
  }

}
