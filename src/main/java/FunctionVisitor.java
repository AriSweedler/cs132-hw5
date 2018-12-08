import cs132.vapor.ast.*;

public class FunctionVisitor extends VInstr.Visitor<RuntimeException> {
  private final VFunction myFunc;

  public FunctionVisitor(VFunction func) {
    myFunc = func;
  }

  public void visit() {
    int itemsOnStack = (myFunc.stack.local + myFunc.stack.out);
    itemsOnStack += 2; /* returns addr & old value of $fp */
    int stackSize = itemsOnStack * 4;

    System.out.println(myFunc.ident + ":");
    compile_functionStart(stackSize);
    for (VInstr instr: myFunc.body) {
      instr.accept(this);
      System.out.println("XX function body XX");
    }
    compile_functionEnd(stackSize);
    System.out.println();
  }

  private static void compile_functionStart(int stackSize) {
    String boilerplate = "  sw $fp -8($sp)\n" +
          "  move $fp $sp\n" +
          "  subu $sp $sp %d\n" +
          "  sw $ra -4($fp)\n";
    System.out.printf(boilerplate, stackSize);
  }

  private static void compile_functionEnd(int stackSize) {
    String boilerplate = "  lw $ra -4($fp)\n" +
          "  lw $fp -8($fp)\n" +
          "  addu $sp $sp %d\n" +
          "  jr $ra\n";
    System.out.printf(boilerplate, stackSize);
  }

  @Override
  public void visit(VAssign a) {

  }

  @Override
  public void visit(VCall vCall) throws RuntimeException {

  }

  @Override
  public void visit(VBuiltIn vBuiltIn) throws RuntimeException {

  }

  @Override
  public void visit(VMemWrite vMemWrite) throws RuntimeException {

  }

  @Override
  public void visit(VMemRead vMemRead) throws RuntimeException {

  }

  @Override
  public void visit(VBranch vBranch) throws RuntimeException {

  }

  @Override
  public void visit(VGoto vGoto) throws RuntimeException {

  }

  @Override
  public void visit(VReturn vReturn) throws RuntimeException {

  }
}
