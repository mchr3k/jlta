package org.jlta.agent;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

public class ThreadClassWriter extends ClassVisitor
{
  private final String className;

  public ThreadClassWriter(ClassWriter writer, String className)
  {
    super(Opcodes.ASM4, writer);
    this.className = className;
  }

  @Override
  public MethodVisitor visitMethod(int access, String name, String desc,
                                   String signature, String[] exceptions)
  {
    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
    if ("java/lang/Thread".equals(className) &&
        "<init>".equals(name))
    {
      mv = new ThreadConstructorVisitor(mv, access, name, desc);
    }
    if ("run".equals(name) &&
        "()V".equals(desc))
    {
      mv = new ThreadRunVisitor(mv, access, name, desc);
    }
    return mv;
  }

  public class ThreadConstructorVisitor extends AdviceAdapter
  {
    public ThreadConstructorVisitor(MethodVisitor mv, int access, String name, String desc)
    {
      super(Opcodes.ASM4, mv, access, name, desc);
    }

    @Override
    protected void onMethodExit(int opcode)
    {
      this.loadThis();
      this.invokeStatic(Type.getType(Tracking.class),
                        Method.getMethod("void newThread (Thread)"));
      super.onMethodExit(opcode);
    }
  }

  public class ThreadRunVisitor extends GeneratorAdapter
  {
    private Label tryStart;
    private Label tryEnd;
    private Label finallyStart;

    public ThreadRunVisitor(MethodVisitor mv, int access, String name, String desc)
    {
      super(Opcodes.ASM4, mv, access, name, desc);
    }

    @Override
    public void visitCode()
    {
      super.visitCode();

      // Begin try/finally block
      tryStart = this.newLabel();
      tryEnd = this.newLabel();
      finallyStart = this.newLabel();
      mv.visitTryCatchBlock(tryStart, tryEnd, finallyStart, null);
      mv.visitLabel(tryStart);

      // Add tracking call to start of try block
      this.loadThis();
      this.invokeStatic(Type.getType(Tracking.class),
                        Method.getMethod("void runEnter (Thread)"));
    }

    @Override
    public void visitInsn(int opcode)
    {
      // Add a call to runreturn before every return instruction.
      // Don't add anything for ATHROW as this will be handled
      // by the finally block.
      switch (opcode)
      {
        case Opcodes.RETURN:
        case Opcodes.IRETURN:
        case Opcodes.FRETURN:
        case Opcodes.ARETURN:
        case Opcodes.LRETURN:
        case Opcodes.DRETURN:
          callRunReturn();
          break;
      }
      super.visitInsn(opcode);
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals)
    {
      // Jump to end of finally block
      Label finallyEnd = this.newLabel();
      this.goTo(finallyEnd);

      // Finally block is really a catch Throwable block
      mv.visitLabel(tryEnd);
      mv.visitLabel(finallyStart);

      // Store off the Throwable
      mv.visitVarInsn(Opcodes.ASTORE, 1);

      // Add call to Run Return method
      callRunReturn();

      // Rethrow stored Throwable
      mv.visitVarInsn(Opcodes.ALOAD, 1);
      mv.visitInsn(Opcodes.ATHROW);

      // Define end of finally block
      mv.visitLabel(finallyEnd);

      super.visitMaxs(maxStack, maxLocals);
    }

    private void callRunReturn()
    {
      this.loadThis();
      this.invokeStatic(Type.getType(Tracking.class),
                        Method.getMethod("void runReturn (Thread)"));
    }
  }
}
