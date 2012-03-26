package org.jlta.agent;

import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

import org.apache.commons.io.IOUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

public class ThreadClassTransformer implements ClassFileTransformer
{

  @Override
  public byte[] transform(ClassLoader loader,
                          String className,
                          Class<?> classBeingRedefined,
                          ProtectionDomain protectionDomain,
                          byte[] classfileBuffer) throws IllegalClassFormatException
  {
    ClassReader cr = new ClassReader(classfileBuffer);
    String comparisonClassName = className;
    String superName = cr.getSuperName();

    boolean isThreadClass = false;
    while (true)
    {
      if ("java/lang/Thread".equals(comparisonClassName) ||
          "java/lang/Thread".equals(superName))
      {
        System.out.println("Seen JLT or child: " + className);
        isThreadClass = true;
        break;
      }
      else if ("java/lang/Object".equals(comparisonClassName) ||
               "java/lang/Object".equals(superName))
      {
        break;
      }
      else if (loader != null)
      {
        try
        {
          InputStream in = loader.getResourceAsStream(superName + ".class");
          if (in != null)
          {
            byte[] superclassBytes = IOUtils.toByteArray(in);
            ClassReader superClassreader = new ClassReader(superclassBytes);
            comparisonClassName = superClassreader.getClassName();
            superName = superClassreader.getSuperName();
          }
        }
        catch (IOException e)
        {
          System.err.println("Class: " + superName);
          System.err.println("Error: " + e);
          break;
        }
      }
      else
      {
        // Can't check superclasses in boot classloader
        break;
      }
    }

    if (isThreadClass)
    {
      System.out.println("Transforming: " + className);
      ClassWriter cw = new ClassWriter(0);
      ClassVisitor cv = new ThreadClassWriter(cw);
      cr.accept(cv, 0);
      classfileBuffer = cw.toByteArray();
    }

    return classfileBuffer;
  }

}
