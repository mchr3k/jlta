package org.dev.jlta;

import java.io.IOException;
import java.net.Socket;

public class ExampleFoo
{

  /**
   * @param args
   */
  public static void main(String[] args)
  {
    try
    {
      try
      {
        new Socket("foo", 123);
      }
      catch (IOException ex)
      {
        ex.printStackTrace();
      }
    }
    finally
    {
      System.out.println("finally");
    }
  }

}
