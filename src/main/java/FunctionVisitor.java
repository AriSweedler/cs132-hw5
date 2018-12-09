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
      System.exit(1);
    }
    compile("jal _heapAlloc");
    compile("move " + a.dest + " $v0");
  }

  private void compile_EasyBuiltIn(VBuiltIn a, String opString) {
    compile(opString + a.dest + " " + a.args[0] + " " + a.args[1]);
  }

  private void compile_PrintIntS(VBuiltIn a) {
    if (a.args[0] instanceof VLitInt) {
      compile("li $a0 " + ((VLitInt) a.args[0]).value);
    } else if (a.args[0] instanceof VVarRef.Register) {
      compile("move $a0 " + a.args[0]);
    } else {
      compile("ERROR compile_PrintIntS not a Lit Int  or VVarRef.Register? what is is..." + a.args[0].getClass());
      System.exit(1);
    }
    compile("jal _print");
  }

  private void compile(String s) {
    for (int i = 0; i < indentDepth * 2; i++) {
      System.out.print(' ');
    }
    System.out.println(s);
  }

  private String get_memGlobal(VMemRef.Global g) {
    int byteOffset = g.byteOffset;
    String ans = null;
    if (g.base instanceof VAddr.Label) {
      ans = String.format("%d(%s)", byteOffset, ((VAddr.Label<VDataSegment>) g.base).label);
    } else if (g.base instanceof VAddr.Var) {
      ans = String.format("%d(%s)", byteOffset, ((VAddr.Var<VDataSegment>) g.base).var);
    }
    return ans;
  }

  private String get_memStack(VMemRef.Stack s) {
    int aboveSP, aboveFP;
    String ans = null;
    switch (s.region) {
      case In:
        aboveFP = (s.index)*4;
        ans = String.format("%d($fp)", aboveFP);
        break;
      case Local:
        aboveSP = (myFunc.stack.out + s.index)*4;
        ans = String.format("%d($sp)", aboveSP);
        break;
      case Out:
        aboveSP = (s.index)*4;
        ans = String.format("%d($sp)", aboveSP);
        break;
      default:
        compile("ERROR memStack region. It is: " + s.region);
        System.exit(1);
        break;
    }
    return ans;
  }

  @Override
  public void visit(VAssign a) {
    /* An assignment instruction. This is only used for assignments of simple operands to registers */
    if (a.source instanceof VLitInt) {
      compile("li " + a.dest + " " + ((VLitInt) a.source).value);
    } else if (a.source instanceof VVarRef.Register) {
      compile("move " + a.dest + " " + a.source);
    } else {
      compile("ERROR VAssign not a Lit Int or a VVarRef.Register? what is is..." + a.source.getClass());
      System.exit(1);
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
      System.exit(1);
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
    else {
      compile("ERROR VBuiltIn not a recognized operation. It is: " + a.op.toString());
      System.exit(1);
    }
  }

  @Override
  public void visit(VMemWrite a) throws RuntimeException {
    String src = null;
    if (a.source instanceof VVarRef.Register) {
      /* [$t0+4] = $t1 * --> * sw $t1 4($t0) */
      src = ((VVarRef.Register)(a.source)).toString();
    } else if (a.source instanceof VLabelRef) {
      /* [$t0] = :vmt_BT *  --> * la $t9 vmt_BT; sw $t9 0($t0) */
      src = "$t9";
      compile("la " + src + " " + ((VLabelRef)(a.source)).ident);
    } else if (a.source instanceof VLitInt) {
      compile("li $t9 " + ((VLitInt) a.source).value);
      src = "$t9";
    } else {
      compile("ERROR VMemWrite source. It is: " + a.source.toString());
      System.exit(1);
    }

    String dest = null;
    /*  a.dest is a VMemRef */
    if (a.dest instanceof VMemRef.Global) {
      VMemRef.Global g = ((VMemRef.Global) a.dest);
      dest = get_memGlobal(g);
    } else if (a.dest instanceof VMemRef.Stack) {
      /* local[0] = $s0 * --> * sw $s0 0($sp) */
      VMemRef.Stack s = ((VMemRef.Stack)a.dest);
      dest = get_memStack(s);
    }

    compile("sw " + src + " " + dest);
  }

  @Override
  public void visit(VMemRead a) throws RuntimeException {
    /* $t1 = [$t0] * --> * lw $t1 0($t0) */
    String dest = null;
    if (a.dest instanceof VVarRef.Register) {
      dest = ((VVarRef.Register) a.dest).toString();
    } else {
      compile("ERROR VMemRead dest not a register. It is: " + a.source.toString());
      System.exit(1);
    }

    String src = null;
    /* a.src is a VMemRef */
    if (a.source instanceof VMemRef.Global) {
      VMemRef.Global g = ((VMemRef.Global) a.source);
      src = get_memGlobal(g);
    } else if (a.source instanceof VMemRef.Stack) {
      VMemRef.Stack s = ((VMemRef.Stack)a.source);
      src = get_memStack(s);
    }

    compile("lw " + dest + " " + src);
  }

  @Override
  public void visit(VBranch a) throws RuntimeException {
    /*
     if0 $t1 goto :if1_else
     beqz $t1 if1_else

     if $s2 goto :null21
     bnez $s2 null21
    */

    String op = a.positive ? "bnez" : "beqz";

    String val = null;
    /* the value to determine the jump */
    if (a.value instanceof VLitInt) {
      val = ((VLitInt)a.value).toString();
    } else if (a.value instanceof VVarRef.Register) {
      val = ((VVarRef.Register)(a.value)).toString();
    } else {
      compile("ERROR VBranch not a LitInt or a VVarRef? what is is..." + a.value.getClass());
      System.exit(1);
    }

    compile(op + " " + val + " " + a.target.toString().substring(1));
  }

  @Override
  public void visit(VGoto a) throws RuntimeException {
    /*  goto :if1_end  */
    /*  j if1_end */
    compile("j " + a.target.toString().substring(1));
  }

  @Override
  public void visit(VReturn a) throws RuntimeException {
    /* endFunction boilerplate manages the stack frame portion of the 'ret' call */
    /* all we need to do is jump our instruction pointer to the return addr */
    compile("jr $ra");
  }
}
