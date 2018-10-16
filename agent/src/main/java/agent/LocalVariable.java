package agent;

import org.objectweb.asm.tree.LocalVariableNode;

public class LocalVariable {
  int index;
  String name;
  String desc;

  LocalVariable(LocalVariable local) {
    this(local.index, local.name, local.desc);
  }

  LocalVariable(LocalVariableNode local) {
    this(local.index, local.name, local.desc);
  }

  LocalVariable(int index, String name, String desc) {
    this.index = index;
    this.name = name;
    this.desc = desc;
  }

  @Override
  public String toString() {
    return "index: " + index + "  name: " + name + " desc: " + desc;
  }
}
