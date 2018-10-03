package agent;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AgentUtils {

  protected static boolean isThreadStart(String className, String methodName) {
    return methodName.equals("start")
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

  protected static int getLoadInst(int opcode) {

    // TODO only handle opcode-writes?
    return 0;
  }

  protected static int getLoadInst(String type) {
    // TODO fill this out
    switch (type) {
      case "C":
        return Opcodes.CALOAD; // ?
      case "Z":
        return 0;
      case "S":
        return 0;
      case "I":
        return 0;
      case "J":
        return 0;
      case "F":
        return 0;
      case "D":
        return 0;
      default:
        return Opcodes.ALOAD;
      // TODO what about arrays?
    }
  }

  // https://stackoverflow.com/questions/11087404/how-to-compute-the-number-of-arguments-from-a-method-descriptor
  private static Pattern allParamsPattern = Pattern.compile("(\\(.*?\\))");
  private static Pattern paramsPattern = Pattern.compile("(\\[?)(C|Z|S|I|J|F|D|(:?L[^;]+;))");

  protected static List<String> getMethodParamCount(String methodRefType) {
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
