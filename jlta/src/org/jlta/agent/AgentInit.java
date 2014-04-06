package org.jlta.agent;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;

import org.jlta.agent.server.Server;

public class AgentInit
{
  /**
   * Common init function.
   *
   * @param agentArgs
   * @param inst
   */
  public static void initialize(String agentArgs, Instrumentation inst)
  {
    System.out.println("## Loaded JLTA Agent.");

    // Parse server port
    int port = 0;
    if (agentArgs != null)
    {
      try
      {
        port = Integer.parseInt(agentArgs);
      }
      catch (NumberFormatException ex)
      {
        System.err.println("## (JLTA) ## NumberFormatException: " + agentArgs);
      }
    }

    // Start server
    try
    {
      new Server(port);
    }
    catch (IOException ex)
    {
      System.err.println("## (JLTA) ## IOException: " + ex.toString());
    }

    // Add transformer
    inst.addTransformer(new ThreadClassTransformer(false), true);

    // Retransform all loaded classes
    for (Class<?> loadedClass : inst.getAllLoadedClasses())
    {
      try
      {
        if (!loadedClass.getName().startsWith("org.jlta") &&
            inst.isModifiableClass(loadedClass))
        {
          inst.retransformClasses(loadedClass);
        }
      }
      catch (UnmodifiableClassException ex)
      {
        // Ignore
      }
    }
  }
}

