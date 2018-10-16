package agent;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.objectweb.asm.Opcodes.*;

public class AgentUtils {

  private static List<Integer> readInsnList = Arrays.asList(ILOAD, LLOAD, FLOAD, DLOAD, ALOAD, RET);
  private static List<Integer> writeInsnList = Arrays.asList(ISTORE, LSTORE, FSTORE, DSTORE, ASTORE);

  protected static boolean isRead(int opcode) {
    return readInsnList.contains(opcode);
  }

  protected static boolean isWrite(int opcode) {
    return writeInsnList.contains(opcode);
  }

  protected static boolean isThreadStart(String className, String methodName, String methodDesc) {
    return methodName.equals("start")
        && methodDesc.equals("()V")
        && (className.equals("java/lang/Thread")
        || listAllSuperClasses(className).contains("java/lang/Thread"));
  }

  protected static boolean isThreadGetId(String className, String methodName, String methodDesc) {
    return methodName.equals("getId")
        && methodDesc.equals("()J")
        && (className.equals("java/lang/Thread")
        || listAllSuperClasses(className).contains("java/lang/Thread"));
  }

  protected static List<String> listAllSuperClasses(String className) {
    return listAllSuperClasses(className, new ArrayList<>());
  }

  protected static List<String> listAllSuperClasses(String className, List<String> list) {
    ClassReader reader = null;
    try {
      reader = new ClassReader(className);
    } catch (IOException e) {}

    if (reader != null && reader.getSuperName() != null) {
      listAllSuperClasses(reader.getSuperName(), list);
      list.add(reader.getSuperName());
    }
    return list;
  }

  protected static int storeToLoad(int opcode) {
    switch (opcode) {
      case Opcodes.ASTORE:
        return Opcodes.ALOAD;
      case Opcodes.DSTORE:
        return Opcodes.DLOAD;
      case Opcodes.FSTORE:
        return Opcodes.FLOAD;
      case Opcodes.ISTORE:
        return Opcodes.ILOAD;
      case Opcodes.LSTORE:
        return Opcodes.LLOAD;
      default:
        // error
        System.out.println("ERROR");
        return 0;
    }
  }

  protected static String loadToStringValueOf(int opcode) {
    switch (opcode) {
      case Opcodes.ILOAD:
        return "I";
      case Opcodes.DLOAD:
        return "D";
      case Opcodes.FLOAD:
        return "F";
      case Opcodes.LLOAD:
        return "J";
      default:
        return "Ljava/lang/Object;";
    }
  }

  protected static int typeToLoad(String type) {
    // https://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.3.2-200
    switch (type) {
      case "B":                               // byte
        return Opcodes.ILOAD;
      case "C":                               // char
        return Opcodes.ILOAD;
      case "D":                               // double
        return Opcodes.DLOAD;
      case "F":                               // float
        return Opcodes.FLOAD;
      case "I":                               // int
        return Opcodes.ILOAD;
      case "J":                               // long
        return Opcodes.LLOAD;
      case "S":                               // short
        return Opcodes.ILOAD;
      case "Z":                               // boolean
        return Opcodes.ILOAD;
      default:
        if (type.startsWith("[")) {           // array
          return Opcodes.ALOAD;
        } else if (type.startsWith("L")) {    // type ref
          return Opcodes.ALOAD;
        }
        return 0; // error
    }
  }

  // https://stackoverflow.com/questions/11087404/how-to-compute-the-number-of-arguments-from-a-method-descriptor
  private static Pattern allParamsPattern = Pattern.compile("(\\(.*?\\))");
  private static Pattern paramsPattern = Pattern.compile("(\\[?)(C|Z|S|I|J|F|D|(:?L[^;]+;))");

  protected static List<String> getMethodParamTypes(String methodRefType) {
    Matcher m = allParamsPattern.matcher(methodRefType);
    if (!m.find()) {
      throw new IllegalArgumentException("Method signature does not contain parameters");
    }
    String paramsDescriptor = m.group(1);
    Matcher mParam = paramsPattern.matcher(paramsDescriptor);

    List<String> paramTypes = new ArrayList<>();
    while (mParam.find()) {
      paramTypes.add(mParam.group(2));
    }
    return paramTypes;
  }

}
