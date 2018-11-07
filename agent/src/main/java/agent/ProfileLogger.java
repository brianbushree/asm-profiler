package agent;

import org.objectweb.asm.tree.LocalVariableNode;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class ProfileLogger {

  public static String APP_DIR;
  private static final String OUTFILE_FORMAT = "/thread_%d.txt";
  private static final String ENS = "UTF8";

  private static Map<Long, ProfileLogger> logMap = Collections.synchronizedMap(new HashMap<>());
  private static Map<Long, MethodCallNode> methodCallMap = Collections.synchronizedMap(new HashMap<>());
  private static final Map<String, List<LocalVariable>> methodArgMap = Collections.synchronizedMap(new HashMap<>());
  private static final Map<String, List<LocalVariable>> allLocalsMap = Collections.synchronizedMap(new HashMap<>());

  /**
   * Register method local variable metadata.
   * @param className   class name
   * @param methodName  method name
   * @param methodDesc  method descriptor
   * @param locals      list of local variable metadata
   */
  protected static void registerLocals(
      String className,
      String methodName,
      String methodDesc,
      List<LocalVariableNode> locals
  ) {
    String fullSig = className + "." + methodName + methodDesc;

    // put method args
    methodArgMap.put(fullSig, getMethodParameters(methodDesc, locals));

    // put all locals
    List<LocalVariable> localList = new ArrayList<>();
    for (LocalVariableNode local : locals) {
      localList.add(new LocalVariable(local));
    }
    allLocalsMap.put(fullSig, localList);
  }


  /**
   * Determine if this is a static method based on local
   *  variable metadata.
   * @param locals  list of local variable metadata
   * @return  true if static method, false otherwise
   */
  private static boolean isStatic(List<LocalVariableNode> locals) {
    boolean isStatic = true;
    for (LocalVariableNode local : locals) {
      if (local.index == 0 && local.name.equals("this")) {
        isStatic = false;
        break;
      }
    }
    return isStatic;
  }

  /**
   * Get metadata for a method's parameters.
   * @param methodSig  full method signature
   * @return  list of local variable metadata
   */
  protected static List<LocalVariable> getMethodParameters(String methodSig) {
    return methodArgMap.get(methodSig);
  }

  /**
   * Produce a list of only a method's parameters from
   *  all of its local variables based on its method descriptor.
   * @param methodDesc  method descriptor
   * @param locals      list of method's local variables
   * @return sorted list of the method's parameters
   */
  private static List<LocalVariable> getMethodParameters(String methodDesc, List<LocalVariableNode> locals) {
    List<LocalVariable> methodArgs = new ArrayList<>();
    int params = AgentUtils.getMethodParamTypes(methodDesc).size();

    boolean isStatic = isStatic(locals);
    for (LocalVariableNode local : locals) {
      if (local.index < params + ((!isStatic) ? 1 : 0)) {
        if (isStatic || local.index > 0) {
          methodArgs.add(new LocalVariable(local));
        }
      }
    }

    Collections.sort(methodArgs, new Comparator<LocalVariable>() {
      @Override
      public int compare(LocalVariable o1, LocalVariable o2) {
        return o1.index - o2.index;
      }
    });

    return methodArgs;
  }

  private Stack<Map<Integer, LocalVariable>> localsTableStack;
	private PrintStream out;
  private long threadId;
  private MethodCallNode lastNode;
  private MethodCallNode initCache;
  private int lastLine;

	/**
	 * Using a synchronized Map, fetches/creates
	 * new instances based on unique key of the
	 * thread id of current Thread.
	 *
	 * @return inst  appropriate instance
	 */
	public static ProfileLogger getInstance() {
    return getInstance(Thread.currentThread().getId());
	}

  public static ProfileLogger getInstance(long tid) {
    ProfileLogger inst = null;

    synchronized (logMap) {
      if (!logMap.containsKey(tid)) {
        inst = new ProfileLogger(tid);
        logMap.put(tid, inst);
      } else {
        inst = logMap.get(tid);
      }
    }

    return inst;
  }


  /**
	 * Creates an instance with a corresponding output
	 * file. This is a private constructor, only called
	 * by ProfileLogger.getInstance().
	 *
	 * @param tid  thread-id to include in file name
	 */
	private ProfileLogger(long tid) {

	  this.threadId = tid;
	  this.initCache = null;
	  this.lastLine = -1;
	  this.localsTableStack = new Stack<>();

		String fileName = APP_DIR + String.format(OUTFILE_FORMAT, tid);

		File file;

		try {

			file = new File(fileName);
			file.getParentFile().mkdirs();
			this.out = new PrintStream(file, ENS);

		} catch (SecurityException e) {
			System.err.println("SecurityException!");
			System.exit(-1);
		} catch (FileNotFoundException e) {
			System.err.println(fileName + " not found.");
			System.exit(-1);
		} catch (UnsupportedEncodingException e) {
			System.err.println(ENS + " not found.");
			System.exit(-1);
		}

	}

  /**
   * Logs a line number instruction.
   * @param line  line number
   */
	public void logLineNumber(int line) {
	  lastLine = line;
  }

	/**
	 * Logs the start of a new Thread in the parent
	 *  Thread including the thread id instead of the
	 *  duration.
   *
   * @param tid  thread id
	 */
	public void logThreadStart(long tid, String methodSig) {
    MethodCallNode methodCall = new MethodCallNode(
        getDepth() + 1,  // simulating being inside Thread.start()
        methodSig,
        MethodProtos.MethodCall.MethodCallType.THREAD_START
    );
    methodCall.setNewThreadId(tid);
    logMethodCallStart(methodCall);
	}

	/**
	 * Logs the start of a method call.
   *
   * @param methodSig  full method-signature
   * @param params     list representing input values
	 */
	public void logMethodStart(String methodSig, String... params) {
    MethodCallNode methodCall = new MethodCallNode(getDepth(), methodSig, params);
    logMethodCallStart(methodCall);

    Map<Integer, LocalVariable> localMap = new HashMap<>();
    for (LocalVariable local : allLocalsMap.get(methodSig)) {
      localMap.put(local.index, local);
    }
    localsTableStack.push(localMap);
	}

  /**
   * Log a method call into the data-structure.
   * @param methodCall  method call node to log
   */
	private void logMethodCallStart(MethodCallNode methodCall) {

    if (methodCall.getDepth() == 0) {

      lastNode = methodCall;
      methodCallMap.put(
          threadId,
          methodCall
      );

    } else {

      StackTraceElement caller = getCaller(methodCall.getDepth());
      methodCall.setCaller(
          MethodProtos.MethodCall.CallerInfo.newBuilder()
              .setFilename(caller.getFileName())
              .setLinenum(caller.getLineNumber())
              .build()
      );

      MethodCallNode parent = getLastNode(methodCall.getDepth() - 1);

      if (parent == null) {

        // stack in cache
        if (initCache != null) {
          methodCallSetParent(methodCall, initCache);
        }
        initCache = methodCall;

      } else {

        // found in tree (not cache)
        if (parent == lastNode) {
          if (initCache != null) {
            methodCallSetParent(methodCall, initCache);
            initCache = null;
          }
        }

        methodCallSetParent(parent, methodCall);

      }

    }
  }

  /**
   * Set a method call's parent/child relationship.
   * @param parent  parent node
   * @param child   child node
   */
  private void methodCallSetParent(MethodCallNode parent, MethodCallNode child) {
    child.setParent(parent);
    parent.getCalls().add(child);
    parent.getInstructions().add(
        MethodProtos.MethodCall.Instruction.newBuilder()
            .setType(MethodProtos.MethodCall.InstructionType.METHOD_CALL)
            .setLinenum(child.getCaller().getLinenum())
            .setCallSignature(child.getSignature())
            .build()
    );
  }


	/**
	 * Logs the end of a method call and its duration.
   * @param methodSig  full method signature
   * @param duration   method duration
	 */
	public void logMethodDuration(String returnValue, String methodSig, long duration) {

    localsTableStack.pop();

    int depth = getDepth();
    MethodCallNode last;

    last = getLastNode(depth);

    assert last != null;
    assert last.getSignature().equals(methodSig);
    last.setDuration(duration);
    last.setReturnValue(returnValue);

    if (depth == 0) {
      try {
        last.toProto().writeTo(out);
      } catch (IOException e) {
        System.out.println("ERROR : " + e);
      }
    }
	}

  /**
   * Log a local variable read instruction.
   * @param index  local variable index
   * @param value  value of read
   */
  public void logLocalRead(int index, String value) {
	  LocalVariable local = localsTableStack.peek().get(index);
	  if (local != null) {

      MethodProtos.MethodCall.Instruction insn =
          MethodProtos.MethodCall.Instruction.newBuilder()
              .setType(MethodProtos.MethodCall.InstructionType.READ)
              .setVariable(
                  MethodProtos.MethodCall.Instruction.Variable.newBuilder()
                      .setName(local.name)
                      .setIndex(local.index)
                      .setType(local.desc)
                      .build()
              )
              .setValue(value)
              .setLinenum(lastLine)
              .build();
      logInstruction(insn);

    }
  }

  /**
   * Log a local variable write instruction.
   * @param index  local variable index
   * @param value  value of write
   */
  public void logLocalWrite(int index, String value) {
    LocalVariable local = localsTableStack.peek().get(index);
    if (local != null) {

      MethodProtos.MethodCall.Instruction insn =
          MethodProtos.MethodCall.Instruction.newBuilder()
              .setType(MethodProtos.MethodCall.InstructionType.WRITE)
              .setVariable(
                  MethodProtos.MethodCall.Instruction.Variable.newBuilder()
                      .setName(local.name)
                      .setIndex(index)
                      .setType(local.desc)
                      .build()
              )
              .setValue(value)
              .setLinenum(lastLine)
              .build();
      logInstruction(insn);

    }
  }

  /**
   * Log a instruction into the data structure.
   * @param insn  instruction to log
   */
	private void logInstruction(MethodProtos.MethodCall.Instruction insn) {
    int depth = getDepth();
    MethodCallNode last = getLastNode(depth);

    if (last != null) {
      List<MethodProtos.MethodCall.Instruction> instr = last.getInstructions();
      instr.add(insn);
    } else {
      System.out.println("ERROR, cant find node.");
    }
  }

	/**
	 * Get the caller File/linenum of a stack trace
   *  based on method call's depth.
   * @param depth  depth of call to return
   * @return  stack element at that depth
	 */
	private static StackTraceElement getCaller(int depth) {
    StackTraceElement[] trace = Thread.currentThread().getStackTrace();
		return trace[trace.length - depth];
	}

	/**
	 * Get the actual depth of the method call based on
   *  the stack-trace.
	 *
	 * @return depth  depth of method (main.depth == 0, main->test.depth == 1, ...)
	 */
  private static int getDepth() {
    StackTraceElement[] trace = Thread.currentThread().getStackTrace();
    int first = -1;
    for (int i = 1; i < trace.length && first == -1; i++) {
      if (!trace[i].getClassName().equals("agent.ProfileLogger")) {
        first = i;
      }
    }
    return (trace.length - 1) - first;
  }

  /**
   * Traverse from lastNode OR look inside init-cache for the last call
   *  where node.depth == depth.
   * @param depth  depth to search for
   * @return node or null if can't be found
   */
  private MethodCallNode getLastNode(int depth) {
    if (lastNode == null) {
      return null;
    }

    MethodCallNode current = lastNode;
    int step = lastNode.getDepth();

    // find in tree
    try {
      if (step < depth) {

        // traverse down
        while (step < depth) {
          current = current.getLastCall();
          step++;
        }


      } else if (step > depth) {

        // traverse up
        while (step > depth) {
          current = current.getParent();
          step--;
        }

      }
    } catch (NullPointerException e) {}

    if (current != null) {

      // found in tree
      lastNode = current;
      return current;

    } else if (initCache != null) {

      // look in cache
      current = initCache;
      while (current != null && depth > current.getDepth()) {
        current = current.getLastCall();
      }

      if (current != null && depth == current.getDepth()) {
        return current;
      } else {
        return null;
      }

    } else {
      return null;
    }
  }
}