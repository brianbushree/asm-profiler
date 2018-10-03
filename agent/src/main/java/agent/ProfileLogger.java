package agent;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProfileLogger {

	public static String APP_DIR;
  private static final String OUTFILE_FORMAT = "/out/thread_%d.txt";
	private static final String ENS = "UTF8";
	private static Map<Long, ProfileLogger> logMap = Collections.synchronizedMap(new HashMap<>());
	private static Map<Long, MethodCallNode> methodCallMap = new HashMap<>();

	private PrintStream out;
  private long threadId;
  private MethodCallNode lastNode;
  private List<MethodCallNode> initCache;

	/**
	 * Using a synchronized Map, fetches/creates
	 * new instances based on unique key of the
	 * thread id of current Thread.
	 *
	 * @return inst  appropriate instance
	 */
	public static ProfileLogger getInstance() {
		long tid = Thread.currentThread().getId();
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
	  this.initCache = new ArrayList<>();

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
	 * Logs the start of a new Thread in the parent
	 * Thread including the thread id instead of the
	 * duration.
	 * The format remains the same as a method start/end
	 * for parsing simplicity.
	 */
	public void logThreadStart(long tid) {
    MethodCallNode methodCall = new MethodCallNode(
        getDepth() + 1,  // simulating being inside Thread.start()
        "java/lang/Thread.start()V",
        MethodProtos.MethodCall.MethodCallType.THREAD_START
    );
    logMethodCallStart(methodCall);
	}

	/**
	 * Logs the start of a method call.
	 */
	public void logMethodStart(String methodSig) {
    MethodCallNode methodCall = new MethodCallNode(getDepth(), methodSig);
    logMethodCallStart(methodCall);
	}

	private void logMethodCallStart(MethodCallNode methodCall) {

	  AgentUtils.getMethodParamCount(methodCall.getSignature());

    if (methodCall.getDepth() == 0) {

      lastNode = methodCall;
      methodCallMap.put(
          threadId,
          methodCall
      );

    } else {

      StackTraceElement caller = getCaller(methodCall.getDepth());
      assert caller != null;
      methodCall.setCaller(
          MethodProtos.MethodCall.CallerInfo.newBuilder()
              .setFilename(caller.getFileName())
              .setLinenum(caller.getLineNumber())
              .build()
      );

      MethodCallNode parent = null;

      try {
        parent = getLastNodeParent(methodCall.getDepth());
      } catch (ArrayIndexOutOfBoundsException ex) {

        if (!initCache.isEmpty()) {
          ListIterator<MethodCallNode> iter = initCache.listIterator();
          MethodCallNode current = null;
          while(iter.hasNext()
              && methodCall.getDepth() - 1 > (current = iter.next()).getDepth()) {}

          if (current != null && methodCall.getDepth() - 1 == current.getDepth()) {
            methodCall.setParent(current);
            current.getCalls().add(methodCall);
            return;
          }
        }

        initCache.add(0, methodCall);
        return;
      }

      if (!initCache.isEmpty()) {
        MethodCallNode initParent = methodCall;
        for (MethodCallNode mc : initCache) {
          mc.setParent(initParent);
          initParent.getCalls().add(0 , mc);
          initParent = mc;
        }
        initCache = new ArrayList<>();
      }

      assert parent != null;
      methodCall.setParent(parent);
      parent.getCalls().add(methodCall);

    }
  }

	/**
	 * Logs the end of a method call and its duration.
	 */
	public void logMethodDuration(String methodSig, long duration) {
    int depth = getDepth();
    MethodCallNode last;

    try {
      last = getLastNode(depth);
    } catch (ArrayIndexOutOfBoundsException ex) {
      assert !initCache.isEmpty();
      initCache.get(0).setDuration(duration);
      return;
    }

    assert last != null;
    assert last.getSignature().equals(methodSig);

    last.setDuration(duration);

    if (depth == 0) {

      // TODO: build & write to file
      String s = last.toProto().toString();
      System.out.println(s);
      out.println(s);

    }
	}

	public void logVariableDeclaration(String name, String descriptor, String signature, int index, int line) {
	  // TODO *this*
  }

	public void logVariableUse(int var, int opcode, int line) {
    int depth = getDepth();
    MethodCallNode last;

    try {
      last = getLastNode(depth);
    } catch (ArrayIndexOutOfBoundsException ex) {
      assert !initCache.isEmpty();

      // TODO handle initCache

      return;
    }

    List<MethodProtos.MethodCall.Instruction> instr = last.getInstructions();
    instr.add(
        MethodProtos.MethodCall.Instruction.newBuilder()
//        .setType(
//            (isRead)
//            ? MethodProtos.MethodCall.InstructionType.READ
//            : MethodProtos.MethodCall.InstructionType.WRITE
//        )
        .build()
    );



  }

	/**
	 * Get the caller File/linenum of a stack trace
   *  based on method call's depth.
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

  private MethodCallNode getLastNodeParent(int depth) {
    return getLastNode(depth - 1);
  }

  private MethodCallNode getLastNode(int depth) {
    if (lastNode == null) {
      return null;
    } else {

      MethodCallNode parent = lastNode;
      int step = lastNode.getDepth();

      if (step < depth) {

        // traverse down
        while (step < depth) {
          List<MethodCallNode> callList = parent.getCalls();
          parent = callList.get(callList.size() - 1);
          step++;
        }


      } else if (step > depth) {

        // traverse up
        while (step > depth) {
          parent = parent.getParent();
          step--;
        }

      }

      lastNode = parent;
      return parent;
    }
  }
}