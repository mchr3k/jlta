package org.jlta.agent;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

public class ThreadClassTransformer implements ClassFileTransformer
{
  private final boolean writeClasses;

  /**
   * @param writeClasses
   */
  public ThreadClassTransformer(boolean writeClasses)
  {
    this.writeClasses = writeClasses;
  }

  @Override
  public byte[] transform(ClassLoader loader,
                          String className,
                          Class<?> classBeingRedefined,
                          ProtectionDomain protectionDomain,
                          byte[] classfileBuffer) throws IllegalClassFormatException
  {
    try
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

      if (isThreadClass &&
          !className.startsWith("org/jlta"))
      {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES |
                                         ClassWriter.COMPUTE_MAXS)
        {
          @Override
          protected String getCommonSuperClass(String type1, String type2)
          {
            Class<?> c, d;
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            if (classLoader == null)
            {
              classLoader = getClass().getClassLoader();
            }
            try {
                c = Class.forName(type1.replace('/', '.'), false, classLoader);
                d = Class.forName(type2.replace('/', '.'), false, classLoader);
            } catch (Exception e) {
                throw new RuntimeException(e.toString());
            }
            if (c.isAssignableFrom(d)) {
                return type1;
            }
            if (d.isAssignableFrom(c)) {
                return type2;
            }
            if (c.isInterface() || d.isInterface()) {
                return "java/lang/Object";
            } else {
                do {
                    c = c.getSuperclass();
                } while (!c.isAssignableFrom(d));
                return c.getName().replace('.', '/');
            }
          }
        };
        ClassVisitor cv = new ThreadClassWriter(cw, className);
        cr.accept(cv, ClassReader.EXPAND_FRAMES);
        classfileBuffer = cw.toByteArray();

        if (writeClasses)
        {
          try
          {
            File file = new File(className.replace('/', '.') + ".class");
            FileUtils.writeByteArrayToFile(file, classfileBuffer);
          }
          catch (IOException e)
          {
            throw new RuntimeException(e);
          }
        }
      }

    }
    catch (Throwable ex)
    {
      ex.printStackTrace();
    }

    return classfileBuffer;
  }

}
