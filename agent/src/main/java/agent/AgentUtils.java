package agent;

import org.objectweb.asm.ClassReader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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

}
