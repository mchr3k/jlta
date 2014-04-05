package org.jlta.common;

import java.util.Arrays;

public class StackTraceArrayWrap
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
