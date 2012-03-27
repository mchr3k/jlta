package org.jlta.dev;

public class TryFinally
{

  /**
   * @param args
   */
  public static void main(String[] args)
  {
    new TryFinally().method();
  }

  private void method()
  {
    try
    {
      output(this);
    }
    finally
    {
      output(this);
    }
  }

  private void output(TryFinally tryFinally)
  {
    // TODO Auto-generated method stub

  }

}
