package org.jlta.dev;


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

  public static void main(String[] args)
  {
    new ThreadSubSubClass().start();
  }
}
