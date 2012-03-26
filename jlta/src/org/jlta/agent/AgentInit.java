package org.jlta.agent;

import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;

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

    // Add transformer
    inst.addTransformer(new ThreadClassTransformer(), true);

    // Retransform all loaded classes
    for (Class<?> loadedClass : inst.getAllLoadedClasses())
    {
      try
      {
        if (!loadedClass.getName().startsWith("org.jlta.agent") &&
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

