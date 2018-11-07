package agent;

import org.objectweb.asm.*;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
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

            // use asm.tree to get local variable metadata (name, index, type)
            ClassReader preClassReader = new ClassReader(modifiedClass);
            ClassWriter preClassWriter = new ClassWriter(0);
            PreClassNodeAdapter preClassNodeAdapter =
                new PreClassNodeAdapter(preClassWriter, className);
            preClassReader.accept(preClassNodeAdapter, 0);
            modifiedClass = preClassWriter.toByteArray();

            // inject code to record method-calls & variable accessing
            ClassReader classReader = new ClassReader(modifiedClass);
            ClassWriter classWriter =
                new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
            ClassAdapter classAdapter = new ClassAdapter(classWriter, className);
            classReader.accept(classAdapter, ClassReader.EXPAND_FRAMES);
            modifiedClass = classWriter.toByteArray();

            // write out to file for debugging
            //try {
            //  File f = new File("/Users/brianbush/Desktop/classes/" + className.replace('.', '-') + ".class");
            //  if (!f.getParentFile().exists())
            //    f.getParentFile().mkdirs();
            //  if (!f.exists())
            //    f.createNewFile();
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

	  private final MethodVisitor mv;
    private final String owner;
    private final String name;
    private final String desc;
    private final String sig;

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
      this.owner = owner;
      this.name = name;
      this.desc = desc;
      this.sig = owner + "." + getName() + methodDesc;

      this.loggerId = -1;
      this.startTimeId = -1;
    }

    @Override
    public void visitLineNumber(int line, Label label) {
      if (loggerId != -1) {
        mv.visitVarInsn(ALOAD, loggerId);
        mv.visitLdcInsn(line);
        mv.visitMethodInsn(INVOKEVIRTUAL, "agent/ProfileLogger", "logLineNumber", "(I)V", false);
      }
      super.visitLineNumber(line, label);
    }

    @Override
    public void visitVarInsn(int opcode, int var) {

      if (AgentUtils.isRead(opcode)) {

        super.visitVarInsn(opcode, var);

        if (loggerId != -1) {
          mv.visitVarInsn(ALOAD, loggerId);
          mv.visitLdcInsn(var);
          super.visitVarInsn(opcode, var);
          mv.visitMethodInsn(
              INVOKESTATIC, "java/lang/String", "valueOf",
              "(" + AgentUtils.loadToStringValueOf(opcode) + ")" + "Ljava/lang/String;",
              false
          );
          mv.visitMethodInsn(INVOKEVIRTUAL, "agent/ProfileLogger", "logLocalRead", "(ILjava/lang/String;)V", false);
        }

      } else if (AgentUtils.isWrite(opcode)) {

        super.visitVarInsn(opcode, var);

        if (loggerId != -1) {
          mv.visitVarInsn(ALOAD, loggerId);
          mv.visitLdcInsn(var);
          super.visitVarInsn(AgentUtils.storeToLoad(opcode), var);
          mv.visitMethodInsn(
              INVOKESTATIC, "java/lang/String", "valueOf",
              "(" + AgentUtils.loadToStringValueOf(AgentUtils.storeToLoad(opcode)) + ")" + "Ljava/lang/String;",
              false
          );
          mv.visitMethodInsn(INVOKEVIRTUAL, "agent/ProfileLogger", "logLocalWrite", "(ILjava/lang/String;)V", false);
        }

      } else {
        System.err.println("Error: Instruction opcode not recognized as read/write: " + opcode);
      }
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
      if (loggerId != -1 && AgentUtils.isThreadStart(owner, name, descriptor)) {
        mv.visitInsn(DUP);
        mv.visitVarInsn(ALOAD, loggerId);
        mv.visitInsn(SWAP);
        mv.visitMethodInsn(INVOKEVIRTUAL, owner, "getId", "()J", false);
        mv.visitLdcInsn(owner + "." + name + descriptor);
        mv.visitMethodInsn(INVOKEVIRTUAL, "agent/ProfileLogger", "logThreadStart", "(JLjava/lang/String;)V", false);
      }

      super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
    }

    /* Note: for constructor calls that have a super class,
      this will happen after the call to super.init() */
    @Override
    protected void onMethodEnter() {

      if (AgentUtils.isThreadGetId(owner, name, methodDesc)) {
        /* Note:
            Since, getInstance() will call MyThread.getId(),
            without this we will infinite loop...
            With this however, if a method is called inside getId(),
             we'll have a missing parent...
         */
        return;
      }

      loggerId = this.newLocal(Type.getObjectType("agent/ProfileLogger"));
      startTimeId = this.newLocal(Type.LONG_TYPE);

      // logger = getInstance()
      mv.visitMethodInsn(INVOKESTATIC, "agent/ProfileLogger", "getInstance", "()Lagent/ProfileLogger;", false);
      mv.visitVarInsn(ASTORE, loggerId);

      // logMethodStart( ... )
      mv.visitVarInsn(ALOAD, loggerId);
      mv.visitLdcInsn(sig);

      // new String[]
      List<LocalVariable> args = ProfileLogger.getMethodParameters(sig);
      mv.visitLdcInsn(args.size());
      this.newArray(Type.getType(String.class));

      int index = 0;
      for (LocalVariable arg : args) {
        mv.visitInsn(Opcodes.DUP);
        mv.visitLdcInsn(index);
        int loadOpcode = AgentUtils.typeToLoad(arg.desc);
        mv.visitVarInsn(loadOpcode, arg.index);
        mv.visitMethodInsn(
            INVOKESTATIC, "java/lang/String", "valueOf",
            "(" + AgentUtils.loadToStringValueOf(loadOpcode) + ")" + "Ljava/lang/String;",
            false
        );
        mv.visitInsn(Opcodes.AASTORE);

        index++;
      }

      mv.visitMethodInsn(INVOKEVIRTUAL, "agent/ProfileLogger", "logMethodStart", "(Ljava/lang/String;[Ljava/lang/String;)V", false);
      mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "nanoTime", "()J", false);
      mv.visitVarInsn(LSTORE, startTimeId);
    }

    @Override
    protected void onMethodExit(int opcode) {
      if (loggerId != -1 && opcode != Opcodes.ATHROW) {
        // get return value
        if (opcode == Opcodes.RETURN) {
          mv.visitLdcInsn("");
        } else {
          mv.visitInsn(Opcodes.DUP);
          mv.visitMethodInsn(
              INVOKESTATIC, "java/lang/String", "valueOf",
              "(" + AgentUtils.loadToStringValueOf(AgentUtils.returnToLoad(opcode)) + ")" + "Ljava/lang/String;",
              false
          );
        }

        // load logger
        mv.visitVarInsn(ALOAD, loggerId);
        mv.visitInsn(Opcodes.SWAP);

        // get duration
        mv.visitLdcInsn(sig);
        mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "nanoTime", "()J", false);
        mv.visitVarInsn(LLOAD, startTimeId);
        mv.visitInsn(LSUB);

        // make call to logger.logMethodDuration(...)
        mv.visitMethodInsn(INVOKEVIRTUAL, "agent/ProfileLogger", "logMethodDuration", "(Ljava/lang/String;Ljava/lang/String;J)V", false);
      }
    }
  }

  public static class PreClassNodeAdapter extends ClassNode implements Opcodes {
    private ClassVisitor cv;
    private String className;

    public PreClassNodeAdapter(ClassVisitor classVisitor, String className) {
      super(ASM6);
      this.cv = classVisitor;
      this.className = className;
    }

    @Override
    public void visitEnd() {
      for (MethodNode mn : methods) {
        ProfileLogger.registerLocals(className, mn.name, mn.desc, mn.localVariables);
      }
      accept(cv);
    }
  }
}

