package org.jlta.agent;


public class Tracking
{
  public static void newThread(Thread t)
  {
    System.out.println("New Thread: " + t.getName());
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
      System.out.println("Thread run... : " + t.getName());
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
      System.out.println("Thread return : " + t.getName());
    }
  }
}
