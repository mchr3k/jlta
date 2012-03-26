package org.jlta.agent;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.util.jar.JarFile;

public class Agent
{
  /**
   * Entry point when loaded using -agent command line arg.
   *
   * @param agentArgs
   * @param inst
   */
  public static void premain(String agentArgs, Instrumentation inst)
  {
    initialize(agentArgs, inst);
  }

  /**
   * Entry point when loaded into running JVM.
   *
   * @param agentArgs
   * @param inst
   */
  public static void agentmain(String agentArgs, Instrumentation inst)
  {
    initialize(agentArgs, inst);
  }

  private static void initialize(String agentArgs, Instrumentation inst)
  {
    try
    {
      // Prepare boot classpath
      String agentPath = null;
      if ((Agent.class.getProtectionDomain() != null) &&
          (Agent.class.getProtectionDomain().getCodeSource() != null) &&
          (Agent.class.getProtectionDomain().getCodeSource().getLocation() != null))
      {
        agentPath = Agent.class.getProtectionDomain().getCodeSource().getLocation().getPath();
      }

      if ((agentPath == null) || !agentPath.endsWith(".jar"))
      {
        System.err.println("JLTA agent must be loaded from a .jar file. Detected path: " + agentPath);
        System.err.println("JLTA loading cancelled.");
        return;
      }

      inst.appendToBootstrapClassLoaderSearch(new JarFile(new File(agentPath)));

      // Class AgentInit in boot classloader
      Class<?> agentInit = Agent.class.getClassLoader().loadClass(
                           "org.jlta.agent.AgentInit");
      Method initMethod = agentInit.getMethod("initialize", String.class, Instrumentation.class);
      initMethod.invoke(null, agentArgs, inst);
    }
    catch (Exception ex)
    {
      throw new RuntimeException(ex);
    }
  }
}
