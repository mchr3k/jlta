package org.dev.jlta;


public class ThreadSubClass extends Thread
{
  public ThreadSubClass()
  {
    super();
  }

  @Override
  public void run()
  {
    try
    {
      System.out.println("Running...");
    }
    finally
    {
      System.out.println("Ending...");
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
      new ThreadSubSubClass().start();
      Thread.sleep(5 * 1000);
    }
  }
}
