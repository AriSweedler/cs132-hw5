import cs132.util.ProblemException;
import cs132.vapor.ast.*;
import cs132.vapor.parser.VaporParser;
import cs132.vapor.ast.VBuiltIn.Op;
import java.io.*;

public class VM2M extends VInstr.Visitor<RuntimeException> {
    
    public static void main (String [] args) {
        try {
            FileInputStream fs = new FileInputStream(
                  "/Users/ari/Desktop/cs132/testcases/hw5/BinaryTree.vaporm"
            );
            VaporProgram programAST = parseVapor(fs, System.out);

            compile_programStart(programAST);
            for (VFunction func: programAST.functions) {
                visit(func);
            }
            compile_programEnd();

        } catch (IOException e) {
            System.out.println("AST parse error:");
            System.out.println(e);
        }

        /* use the AST to translate the program */
        /* visit the AST */
    }

    private static void visit(VFunction func) {
        int itemsOnStack = (func.stack.local + func.stack.out);
        itemsOnStack += 2; /* returns addr & old value of $fp */
        int stackSize = itemsOnStack * 4;

        compile_functionStart(stackSize);
        for (VInstr instr: func.body) {
            //TODO visit each instr
            /* check ALL */
        }
        compile_functionEnd(stackSize);
    }

    private static void compile_functionStart(int stackSize) {
        String boilerplate = "  sw $fp -8($sp)\n" +
              "  move $fp $sp\n" +
              "  subu $sp $sp %d\n" +
              "  sw $ra -4($fp)";
        System.out.printf(boilerplate, stackSize);
    }

    private static void compile_functionEnd(int stackSize) {
        String boilerplate = "  lw $ra -4($fp)\n" +
              "  lw $fp -8($fp)\n" +
              "  addu $sp $sp %d\n" +
              "  jr $ra";
        System.out.printf(boilerplate, stackSize);
    }

    private static void compile_programStart(VaporProgram programAST)
    {
        /* print data segment */
        System.out.println(".data");
        System.out.println();
        for (VDataSegment v: programAST.dataSegments) {
            System.out.println(v); //TODO stuff
        }

        /* start the Main function */
        System.out.println(".text");
        System.out.println();
        String s = "\n" +
              "  jal Main\n" +
              "  li $v0 10\n" +
              "  syscall\n";
        System.out.println(s);
    }

    private static void compile_programEnd()
    {
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
              "_str0: .asciiz \"null pointer\\n\"\n" +
              "_str1: .asciiz \"array index out of bounds\\n\"";
        System.out.println(boilerplate);
    }

    public static VaporProgram parseVapor(InputStream in, PrintStream err)
          throws IOException
    {
        Op[] ops = {
              Op.Add, Op.Sub, Op.MulS, Op.Eq, Op.Lt, Op.LtS,
              Op.PrintIntS, Op.HeapAllocZ, Op.Error,
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
        }
        catch (ProblemException ex) {
            err.println(ex.getMessage());
            return null;
        }

        return program;
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


