package org.jlta.common;

import java.io.Serializable;
import java.util.Arrays;

public class ThreadData implements Serializable, Comparable<ThreadData>
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

  @Override
  public String toString()
  {
    return name;
  }

  public static class StackTraceArrayWrap
  {
    public final StackTraceElement[] stack;

    public StackTraceArrayWrap(StackTraceElement[] stack)
    {
      this.stack = stack;
    }

    @Override
    public int hashCode()
    {
      return Arrays.hashCode(stack);
    }

    @Override
    public boolean equals(Object obj)
    {
      if (obj instanceof StackTraceArrayWrap)
      {
        StackTraceArrayWrap objStack = (StackTraceArrayWrap) obj;
        return Arrays.equals(stack, objStack.stack);
      }
      else
      {
        return false;
      }
    }

    @Override
    public String toString()
    {
      StringBuilder str = new StringBuilder();
      for (int ii = 0; ii < stack.length; ii++)
      {
        str.append(stack[ii].toString());
        if (ii < (stack.length - 1))
        {
          str.append("\n");
        }
      }
      return str.toString();
    }
  }

  @Override
  public int compareTo(ThreadData tdata)
  {
    return name.compareTo(tdata.name);
  }
}