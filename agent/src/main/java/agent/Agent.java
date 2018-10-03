package agent;

import org.objectweb.asm.*;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.objectweb.asm.Opcodes.ASM6;

public class Agent {

  public static final String ARGS_DELIM = "###";
  public static final String ARGS_PACKAGE_DELIM = ",";

	/**
	 * This is called before the standard main
	 * method and initializes a ClassFileTransformer
	 * which allows injection and recompilation of each  
	 * class file as it is requested by the JVM. 
	 *
	 * @param agentArgs  this should have the following format :
   *                   "$OUT_DIR###$PACKAGE,...$PACKAGE"
	 * @param inst       provides access/control of JVM
	 */
	public static void premain(String agentArgs, Instrumentation inst) {

		String temp[] = agentArgs.split(ARGS_DELIM);
		final String outDir = temp[0];
		final String args[];

		if (outDir == null || outDir.isEmpty()) {
      System.err.println("Error: Missing output directory...");
      return;
    }

    ProfileLogger.APP_DIR = outDir;

		try {
      args = temp[1].split(ARGS_PACKAGE_DELIM);
    } catch (IndexOutOfBoundsException e) {
      System.err.println("Error: Missing package arg(s)...");
      return;
    }


		inst.addTransformer(new ClassFileTransformer() {

			@Override
			public byte[] transform(
					ClassLoader classLoader,
					String className,
					Class<?> aClass,
					ProtectionDomain protectionDomain,
					byte[] bytes
			) throws IllegalClassFormatException {

			  byte[] modifiedClass = bytes;

        for (String pack : args) {
          if (!className.startsWith("java/")
              && !className.startsWith("agent/")
              && className.startsWith(pack + "/")) {

            // inject code to record method-calls & variable accessing
            ClassReader cr = new ClassReader(bytes);
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
            ClassAdapter ra = new ClassAdapter(cw, className);
            cr.accept(ra, ClassReader.EXPAND_FRAMES);
            modifiedClass = cw.toByteArray();

            // use asm.tree to get local variable metadata (name, index, type)
            //  note: (should be done AFTER we add locals...)
            ClassReader cr2 = new ClassReader(modifiedClass);
            ClassWriter cw2 = new ClassWriter(0);
            ClassNodeAdapter ra2 = new ClassNodeAdapter(cw2, className);
            cr2.accept(ra2, 0);
            modifiedClass = cw2.toByteArray();

            // write out to file for debugging
            //try {
            //  File f = new File("/Users/brianbush/Desktop/classes/" + className.replace('.', '-') + ".class");
            //  f.createNewFile();
            //  FileOutputStream out = new FileOutputStream(f);
            //  out.write(modifiedClass);
            //  out.close();
            //} catch (IOException ex) {
            //  ex.printStackTrace();
            //}

            return modifiedClass;

          }
        }

				return bytes;
			}
		});
	}

	public static class ClassAdapter extends ClassVisitor {

    private ClassVisitor cv;
    private String className;

    public ClassAdapter(ClassVisitor classVisitor, String className) {
      super(ASM6, classVisitor);
      this.cv = classVisitor;
      this.className = className;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
      MethodVisitor mv;
      mv = cv.visitMethod(access, name, descriptor, signature, exceptions);
      mv = new MethodAdapter(ASM6, className, access, name, descriptor, mv);
      return mv;
    }

  }

  static class MethodAdapter extends AdviceAdapter {

	  private static List<Integer> readInsnList = Arrays.asList(ILOAD, LLOAD, FLOAD, DLOAD, ALOAD, RET);
    private static List<Integer> writeInsnList = Arrays.asList(ISTORE, LSTORE, FSTORE, DSTORE, ASTORE);

	  private MethodVisitor mv;
    private String sig;

    private int loggerId;
    private int startTimeId;

    public MethodAdapter(
        int api,
        String owner,
        int access,
        String name,
        String desc,
        MethodVisitor mv) {
      super(ASM6, mv, access, name, desc);
      this.mv = mv;
      this.sig = owner + "." + getName() + methodDesc;
    }

    @Override
    public void visitLocalVariable(String name, String descriptor, String signature, Label start, Label end, int index) {

      super.visitLocalVariable(name, descriptor, signature, start, end, index);
      // TODO: record local variable name, type, and index

    }

    @Override
    public void visitVarInsn(int opcode, int var) {

      if (readInsnList.contains(opcode)) {

        // TODO: record read value
        super.visitVarInsn(opcode, var);

      } else if (writeInsnList.contains(opcode)) {

        // TODO use opcode to get type
        // AgentUtils.getLoadInst(opcode);

        // TODO: record before value
        super.visitVarInsn(opcode, var);
        // TODO: record after value

      } else {
        System.err.println("Error: Instruction opcode not recognized as read/write: " + opcode);
      }
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
      if (AgentUtils.isThreadStart(owner, name)) {
        mv.visitInsn(DUP);
        mv.visitVarInsn(ALOAD, loggerId);
        mv.visitInsn(SWAP);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Thread", "getId", "()J", false);
        mv.visitMethodInsn(INVOKEVIRTUAL, "agent/ProfileLogger", "logThreadStart", "(J)V", false);
      }

      super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
    }

    /* Note: for constructor calls that have a super class,
      this will happen after the call to super.init() */
    @Override
    protected void onMethodEnter() {
      loggerId = this.newLocal(Type.getObjectType("agent/ProfileLogger"));
      startTimeId = this.newLocal(Type.LONG_TYPE);

      // logger = getInstance()
      mv.visitMethodInsn(INVOKESTATIC, "agent/ProfileLogger", "getInstance", "()Lagent/ProfileLogger;", false);
      mv.visitVarInsn(ASTORE, loggerId);

      // TODO logParams on entry
      List<String> args = AgentUtils.getMethodParamCount(sig);
      for (String arg : args) {
        // mv.visitVarInsn(ALOAD, loggerId);
        //  ...
        // AgentUtils.getLoadInst(arg);
      }

      // logMethodStart( ... )
      mv.visitVarInsn(ALOAD, loggerId);
      mv.visitLdcInsn(sig);
      mv.visitMethodInsn(INVOKEVIRTUAL, "agent/ProfileLogger", "logMethodStart", "(Ljava/lang/String;)V", false);
      mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "nanoTime", "()J", false);
      mv.visitVarInsn(LSTORE, startTimeId);
    }

    @Override
    protected void onMethodExit(int opcode) {
      if (opcode != Opcodes.ATHROW) {
        mv.visitVarInsn(ALOAD, loggerId);
        mv.visitLdcInsn(sig);
        mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "nanoTime", "()J", false);
        mv.visitVarInsn(LLOAD, startTimeId);
        mv.visitInsn(LSUB);
        mv.visitMethodInsn(INVOKEVIRTUAL, "agent/ProfileLogger", "logMethodDuration", "(Ljava/lang/String;J)V", false);
      }
    }
  }

  public static class ClassNodeAdapter extends ClassNode implements Opcodes {
    private ClassVisitor cv;
    private String className;

    public ClassNodeAdapter(ClassVisitor classVisitor, String className) {
      super(ASM6);
      this.cv = classVisitor;
      this.className = className;
    }

    @Override
    public void visitEnd() {
      //System.out.println("Class : " + className);
      //for (MethodNode mn : methods) {
      //  System.out.println("\tmethod : " + mn.name + " " + mn.desc);
      //  for (LocalVariableNode local : mn.localVariables) {
      //    System.out.println("\t\t" + local.index + " : " + local.name + " : " + local.desc);
      //  }
      //}
      accept(cv);
    }
  }

}

