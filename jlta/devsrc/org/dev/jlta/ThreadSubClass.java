package org.dev.jlta;

import java.io.IOException;

public class ThreadSubClass extends Thread
{
  public ThreadSubClass()
  {
    super();
  }

  @Override
  public void run()
  {
    // try
    // {
    // Tracking.runEnter(this);
    try
    {
      System.out.println("Started...");
      safeSleep(1000);
      System.out.println("Throw...");
      if (System.currentTimeMillis() > 0)
        throw new IOException();
    }
    catch (IOException ex)
    {
      System.out.println("Catch...");
      return;
    }
    System.out.println("Bar");
    return;
    // }
    // finally
    // {
    // Tracking.runReturn(this);
    // }
  }

  private void safeSleep(int i)
  {
    try
    {
      Thread.sleep(1000);
    }
    catch (InterruptedException e)
    {
      e.printStackTrace();
    }
  }

  public static class ThreadSubSubClass extends ThreadSubClass
  {
    @Override
    public void run()
    {
      super.run();
    }
  }

  public static void main(String[] args) throws InterruptedException
  {
    for (int ii = 0; ii < 300; ii++)
    {
      Thread t = new ThreadSubSubClass();
      t.setName("Foo");
      t.start();
      Thread.sleep(5 * 1000);
    }
  }
}
