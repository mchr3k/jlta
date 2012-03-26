package org.jlta.agent;

public class Tracking
{

  public static void newThread()
  {
    System.out.println("New Thread");
    new Exception().printStackTrace();
  }

  public static void startThread()
  {
    System.out.println("Start Thread");
  }

  public static void endThread()
  {
    System.out.println("End Thread");
  }
}
