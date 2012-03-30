package org.jlta.common;

import java.io.Serializable;
import java.util.Arrays;

public class ThreadData implements Serializable
{
  private static final long serialVersionUID = 1L;

  public final StackTraceElement[] newThreadStack;
  public final String name;

  public long startTime;
  public ThreadData.ThreadState state;
  public long elapsed;

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