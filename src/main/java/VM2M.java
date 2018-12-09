import cs132.util.ProblemException;
import cs132.vapor.ast.*;
import cs132.vapor.parser.VaporParser;
import cs132.vapor.ast.VBuiltIn.Op;

import java.io.*;

public class VM2M {

  public static void main(String[] args) throws Exception {
    FileInputStream fs = new FileInputStream(
          "/Users/ari/Desktop/cs132/testcases/hw5/BinaryTree.vaporm"
    );
    VaporProgram programAST = parseVapor(fs, System.out);
    if (programAST == null) {throw new RuntimeException();}

    compile_programStart(programAST);
    for (VFunction func : programAST.functions) {
      FunctionVisitor v = new FunctionVisitor(func);
      v.compileFunction();
    }
    compile_programEnd();
  }

  private static void compile_programStart(VaporProgram programAST) {
    /* print data segment */
    System.out.println(".data");
    System.out.println();
    for (VDataSegment d : programAST.dataSegments) {
      System.out.println(d.ident + ":");
      for (VOperand.Static v : d.values) {
        System.out.println("  " + v.toString().substring(1));
      }
      System.out.println();
    }

    /* start the Main function */
    System.out.println(".text");
    String s = "  jal Main\n" +
          "  li $v0 10\n" +
          "  syscall\n";
    System.out.println(s);
  }

  private static void compile_programEnd() {
    String boilerplate = "_print:\n" +
          "  li $v0 1   # syscall: print integer\n" +
          "  syscall\n" +
          "  la $a0 _newline\n" +
          "  li $v0 4   # syscall: print string\n" +
          "  syscall\n" +
          "  jr $ra\n" +
          "\n" +
          "_error:\n" +
          "  li $v0 4   # syscall: print string\n" +
          "  syscall\n" +
          "  li $v0 10  # syscall: exit\n" +
          "  syscall\n" +
          "\n" +
          "_heapAlloc:\n" +
          "  li $v0 9   # syscall: sbrk\n" +
          "  syscall\n" +
          "  jr $ra\n" +
          "\n" +
          ".data\n" +
          ".align 0\n" +
          "_newline: .asciiz \"\\n\"\n" +
          "_nullptr: .asciiz \"null pointer\\n\"\n" +
          "_arriob: .asciiz \"array index out of bounds\\n\"";
    System.out.println(boilerplate);
  }

  public static VaporProgram parseVapor(InputStream in, PrintStream err)
        throws IOException {
    VBuiltIn.Op[] ops = {
          VBuiltIn.Op.Add, VBuiltIn.Op.Sub, VBuiltIn.Op.MulS, VBuiltIn.Op.Eq, VBuiltIn.Op.Lt, VBuiltIn.Op.LtS,
          VBuiltIn.Op.PrintIntS, VBuiltIn.Op.HeapAllocZ, VBuiltIn.Op.Error,
    };
    boolean allowLocals = false;
    String[] registers = {
          "v0", "v1",
          "a0", "a1", "a2", "a3",
          "t0", "t1", "t2", "t3", "t4", "t5", "t6", "t7",
          "s0", "s1", "s2", "s3", "s4", "s5", "s6", "s7",
          "t8",
    };
    boolean allowStack = true;

    VaporProgram program;
    try {
      program = VaporParser.run(new InputStreamReader(in), 1, 1,
            java.util.Arrays.asList(ops),
            allowLocals, registers, allowStack);
    } catch (ProblemException ex) {
      err.println(ex.getMessage());
      return null;
    }

    return program;
  }
}


