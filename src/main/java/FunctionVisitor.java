import cs132.util.SourcePos;
import cs132.vapor.ast.*;

public class FunctionVisitor extends VInstr.Visitor<RuntimeException> {
  private final VFunction myFunc;
  private int indentDepth;

  public FunctionVisitor(VFunction func) {
    myFunc = func;
    indentDepth = 0;
  }

  public void compileFunction() {
    int itemsOnStack = (myFunc.stack.local + myFunc.stack.out);
    /* we also place the "return addr" & "old value of $fp" onto the stack */
    itemsOnStack += 2;

    /* each item in this language is 4 bytes large */
    int stackSize = itemsOnStack * 4;

    compile(myFunc.ident + ":");
    indentDepth++;
    compile_functionStart(stackSize);
    for (VInstr instr : myFunc.body) {
      instr.accept(this);
      /* label printing */
      compile_labels(instr.sourcePos.line + 1);
    }
    compile_functionEnd(stackSize);
    System.out.println();
  }

  private void compile_labels(int targetLine) {
    for (VCodeLabel l : myFunc.labels) {
      if (targetLine == l.sourcePos.line) {
        compile(l.ident + ":");
        compile_labels(targetLine + 1);
      }
    }
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

  private void compileError(VBuiltIn a) {
    String err = a.args[0].toString();
    String preloadError;
    if (err.equals("\"null pointer\"")) {
      preloadError = "_nullptr";
    } else if (err.equals("\"array index out of bounds\"")) {
      preloadError = "_arriob";
    } else {
      preloadError = "TODO VBuiltIn.Op.Error string ... not seen yet: `" + err + "`";
    }

    compile("la $a0 " + preloadError);
    compile("j _error");
  }

  private void compileHeapAllocZ(VBuiltIn a) {
    if (a.args[0] instanceof VLitInt) {
      compile("li $a0 " + ((VLitInt) a.args[0]).value);
    } else {
      compile("ERROR compileHeapAllocZ not a Lit Int? what is is..." + a.args[0].getClass());
    }
    compile("jal _heapAlloc");
  }

  private void compile_EasyBuiltIn(VBuiltIn a, String opString) {
    compile(opString + a.dest + " " + a.args[0] + " " + a.args[1]);
  }

  private void compile_PrintIntS(VBuiltIn a) {
    if (a.args[0] instanceof VLitInt) {
      compile("li $a0 " + ((VLitInt) a.args[0]).value);
    } else if (a.args[0] instanceof VVarRef) {
      compile("move $a0 " + a.args[0]);
    } else {
      compile("ERROR compile_PrintIntS not a Lit Int  or VVarRef? what is is..." + a.args[0].getClass());
    }
    compile("jal _print");
  }

  private void compile(String s) {
    for (int i = 0; i < indentDepth * 2; i++) {
      System.out.print(' ');
    }
    System.out.println(s);
  }

  @Override
  public void visit(VAssign a) {
    /* An assignment instruction. This is only used for assignments of simple operands to registers */
    if (a.source instanceof VLitInt) {
      compile("li " + a.dest + " " + ((VLitInt) a.source).value);
    } else if (a.source instanceof VVarRef) {
      compile("move " + a.dest + " " + a.source);
    } else {
      compile("ERROR VAssign not a Lit Int or a VVarRef? what is is..." + a.source.getClass());
    }
  }

  @Override
  public void visit(VCall a) throws RuntimeException {
    /* call $t1 */ /* jalr $t1 */
    /* args are on out stack/registers - vaporM takes care of all this*/
    /* We just need to do the jalr */
    if (a.addr instanceof VAddr.Label) {
      compile("jalr " + ((VAddr.Label) a.addr).label);
    } else if (a.addr instanceof VAddr.Var) {
      compile("jalr " + a.addr);
    } else {
      compile("ERROR VCall addr not a Label or a Var. What is it? " + a.addr.getClass());
    }
  }

  @Override
  public void visit(VBuiltIn a) throws RuntimeException {
    String opString = null;
    if (a.op == VBuiltIn.Op.Add) compile_EasyBuiltIn(a, "addu ");
    else if (a.op == VBuiltIn.Op.Sub) compile_EasyBuiltIn(a, "subu ");
    else if (a.op == VBuiltIn.Op.MulS) compile_EasyBuiltIn(a, "mul ");
    else if (a.op == VBuiltIn.Op.Eq) compile_EasyBuiltIn(a, "eq ");
    else if (a.op == VBuiltIn.Op.Lt) compile_EasyBuiltIn(a, "lt ");
    else if (a.op == VBuiltIn.Op.LtS) compile_EasyBuiltIn(a, "slt ");
    else if (a.op == VBuiltIn.Op.PrintIntS) compile_PrintIntS(a);
    else if (a.op == VBuiltIn.Op.HeapAllocZ) compileHeapAllocZ(a);
    else if (a.op == VBuiltIn.Op.Error) compileError(a);
    else compile("ERROR VBuiltIn not a recognized operation. It is: " + a.op.toString());
  }

  @Override
  public void visit(VMemWrite vMemWrite) throws RuntimeException {
    compile("    TODO VMemWrite TODO");
  }

  @Override
  public void visit(VMemRead vMemRead) throws RuntimeException {
    compile("    TODO VMemRead TODO");
  }

  @Override
  public void visit(VBranch vBranch) throws RuntimeException {
    compile("    TODO VBranch TODO");
  }

  @Override
  public void visit(VGoto a) throws RuntimeException {
    /*  goto :if1_end  */
    /*  j if1_end */
    compile("j " + a.target.toString().substring(1));
  }

  @Override
  public void visit(VReturn vReturn) throws RuntimeException {
    compile("    TODO VReturn TODO");
  }
}
