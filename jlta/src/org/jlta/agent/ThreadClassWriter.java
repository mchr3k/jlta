package org.jlta.agent;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.commons.Method;

public class ThreadClassWriter extends ClassVisitor
{
  public ThreadClassWriter(ClassWriter writer)
  {
    super(Opcodes.ASM4, writer);
  }

  @Override
  public MethodVisitor visitMethod(int access, String name, String desc,
                                   String signature, String[] exceptions)
  {
    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
    if ("<init>".equals(name))
    {
      mv = new ThreadMethodVisitor(mv, access, name, desc);
    }
    return mv;
  }

  public class ThreadMethodVisitor extends AdviceAdapter
  {
    public ThreadMethodVisitor(MethodVisitor mv, int access, String name, String desc)
    {
      super(Opcodes.ASM4, mv, access, name, desc);
    }

    @Override
    protected void onMethodEnter()
    {
      this.invokeStatic(Type.getType(Tracking.class),
                        Method.getMethod("void newThread ()"));
      super.onMethodEnter();
    }
  }
}
