package agent;

import java.util.ArrayList;
import java.util.List;

class MethodCallNode {

  private int depth;
  private String signature;
  private MethodProtos.MethodCall.MethodCallType type;
  private MethodCallNode parent;
  private MethodProtos.MethodCall.CallerInfo caller;
  private List<MethodCallNode> calls;
  private List<MethodProtos.MethodCall.Instruction> instructions;
  private long duration;
  private long newThreadId;

  public MethodCallNode(int depth, String signature) {
    this(depth, signature, MethodProtos.MethodCall.MethodCallType.NORMAL);
  }

  public MethodCallNode(int depth, String signature, MethodProtos.MethodCall.MethodCallType type) {
    this.depth = depth;
    this.signature = signature;
    this.type = type;
    calls = new ArrayList<>();
    instructions = new ArrayList<>();
  }

  public int getDepth() {
    return depth;
  }

  public String getSignature() {
    return signature;
  }

  public MethodProtos.MethodCall.MethodCallType getType() {
    return type;
  }

  public MethodCallNode getParent() {
    return parent;
  }

  public void setParent(MethodCallNode parent) {
    this.parent = parent;
  }

  public MethodProtos.MethodCall.CallerInfo getCaller() {
    return caller;
  }

  public void setCaller(MethodProtos.MethodCall.CallerInfo caller) {
    this.caller = caller;
  }

  public List<MethodCallNode> getCalls() {
    return calls;
  }

  public void setCalls(List<MethodCallNode> calls) {
    this.calls = calls;
  }

  public List<MethodProtos.MethodCall.Instruction> getInstructions() {
    return instructions;
  }

  public void setInstructions(List<MethodProtos.MethodCall.Instruction> instructions) {
    this.instructions = instructions;
  }

  public long getDuration() {
    return duration;
  }

  public void setDuration(long duration) {
    this.duration = duration;
  }

  public long getNewThreadId() {
    return newThreadId;
  }

  public void setNewThreadId(long newThreadId) {
    this.newThreadId = newThreadId;
  }

  public MethodProtos.MethodCall toProto() {
    MethodProtos.MethodCall.Builder builder =
        MethodProtos.MethodCall.newBuilder()
        .setSignature(signature)
        .setType(type)
        .addAllCalls(callsToProto())
        .addAllInstructions(instructions)
        .setDuration(duration)
        .setDepth(depth);

    if (caller != null) {
      builder.setCaller(caller);
    }

    if (type == MethodProtos.MethodCall.MethodCallType.THREAD_START) {
      builder.setNewThreadId(newThreadId);
    }

    return builder.build();
  }

  public List<MethodProtos.MethodCall> callsToProto() {
    List<MethodProtos.MethodCall> list = new ArrayList<>();
    for (MethodCallNode mc : calls) {
      list.add(mc.toProto());
    }
    return list;
  }

}
