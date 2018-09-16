package efimke.vmTranslator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class CodeWriter {

    private static final int offsetForTmpMS = 5;
    private FileWriter out;
    private String fileName;
    private static int eqNumber = 0;
    private static int ltNumber = 0;
    private static int gtNumber = 0;
    private static Map<String, Integer> funcNamesNum = new HashMap<>();

    public CodeWriter(String uri) throws IOException {
        Path path = Paths.get(uri);
        if(Files.isDirectory(path)) {
            fileName = path.getFileName().toString() + ".asm";
            out = new FileWriter(new File(path.toString(), this.fileName));
            this.writeInit();
        } else {
            fileName = path.getFileName().toString().replace(".vm", ".asm");
            out = new FileWriter(new File(path.toString().replace(".vm", ".asm")));
        }
    }

    public void writeArithmetic(String command) throws IOException {
        ArrayList<String> asmCode = new ArrayList<>(generatePopStack());
        switch (command) {
            case "add":
                asmCode.add("D=M");
                asmCode.addAll(generatePopStack());
                asmCode.add("M=D+M");
                break;
            case "sub":
                asmCode.add("D=M");
                asmCode.addAll(generatePopStack());
                asmCode.add("M=M-D");
                break;
            case "neg":
                asmCode.add("M=-M");
                break;
            case "eq":
                asmCode.add("D=M");
                asmCode.addAll(generatePopStack());
                asmCode.add("D=D-M");
                asmCode.add("@EQ_LABEL_POS_" + eqNumber);
                asmCode.add("D;JNE");
                asmCode.add("@1");
                asmCode.add("D=-A");
                asmCode.add("@EQ_LABEL_END_" + eqNumber);
                asmCode.add("0;JMP");
                asmCode.add("(EQ_LABEL_POS_" + eqNumber + ")");
                asmCode.add("@0");
                asmCode.add("D=A");
                asmCode.add("(EQ_LABEL_END_" + eqNumber + ")");
                asmCode.add("@SP");
                asmCode.add("A=M");
                asmCode.add("M=D");
                eqNumber++;
                break;
            case "gt":
                asmCode.add("D=M");
                asmCode.addAll(generatePopStack());
                asmCode.add("D=D-M");
                asmCode.add("@GT_LABEL_POS_" + gtNumber);
                asmCode.add("D;JGE");
                asmCode.add("@1");
                asmCode.add("D=-A");
                asmCode.add("@GT_LABEL_END_" + gtNumber);
                asmCode.add("0;JMP");
                asmCode.add("(GT_LABEL_POS_" + gtNumber + ")");
                asmCode.add("@0");
                asmCode.add("D=A");
                asmCode.add("(GT_LABEL_END_" + gtNumber + ")");
                asmCode.add("@SP");
                asmCode.add("A=M");
                asmCode.add("M=D");
                gtNumber++;
                break;
            case "lt":
                asmCode.add("D=M");
                asmCode.addAll(generatePopStack());
                asmCode.add("D=D-M");
                asmCode.add("@LT_LABEL_POS_" + ltNumber);
                asmCode.add("D;JLE");
                asmCode.add("@1");
                asmCode.add("D=-A");
                asmCode.add("@LT_LABEL_END_" + ltNumber);
                asmCode.add("0;JMP");
                asmCode.add("(LT_LABEL_POS_" + ltNumber + ")");
                asmCode.add("@0");
                asmCode.add("D=A");
                asmCode.add("(LT_LABEL_END_" + ltNumber + ")");
                asmCode.add("@SP");
                asmCode.add("A=M");
                asmCode.add("M=D");
                ltNumber++;
                break;
            case "and":
                asmCode.add("D=M");
                asmCode.addAll(generatePopStack());
                asmCode.add("M=D&M");
                break;
            case "or":
                asmCode.add("D=M");
                asmCode.addAll(generatePopStack());
                asmCode.add("M=D|M");
                break;
            case "not":
                asmCode.add("M=!M");
                break;
        }
        asmCode.addAll(generatePushStack());
        writeCode(asmCode);
    }

    public void writePushPop(CommandType type, String segment, Integer index) throws IOException {
        ArrayList<String> asmCode = new ArrayList<>();
        switch (segment) {
            case "local":
                asmCode.addAll(generateDynamicMemorySegments("LCL", type, index));
                break;
            case "argument":
                asmCode.addAll(generateDynamicMemorySegments("ARG", type, index));
                break;
            case "this":
                asmCode.addAll(generateDynamicMemorySegments("THIS", type, index));
                break;
            case "that":
                asmCode.addAll(generateDynamicMemorySegments("THAT", type, index));
                break;
            case "static":
                if (type.equals(CommandType.C_POP)) {
                    asmCode.addAll(generatePopStack());
                    asmCode.add("D=M");
                    asmCode.add("@" + fileName + "." + index);
                    asmCode.add("M=D");
                } else if(type.equals(CommandType.C_PUSH)) {
                    asmCode.add("@" + fileName + "." + index);
                    asmCode.add("D=M");
                    asmCode.add("@SP");
                    asmCode.add("A=M");
                    asmCode.add("M=D");
                    asmCode.addAll(generatePushStack());
                }
                break;
            case "temp":
                if (type.equals(CommandType.C_POP)) {
                    asmCode.addAll(generatePopStack());
                    asmCode.add("D=M");
                    asmCode.add("@" + (index + offsetForTmpMS));
                    asmCode.add("M=D");
                } else if(type.equals(CommandType.C_PUSH)) {
                    asmCode.add("@" + (index + offsetForTmpMS));
                    asmCode.add("D=M");
                    asmCode.add("@SP");
                    asmCode.add("A=M");
                    asmCode.add("M=D");
                    asmCode.addAll(generatePushStack());
                }
                break;
            case "constant":
                if (type.equals(CommandType.C_PUSH)) {
                    asmCode.add("@" + index);
                    asmCode.add("D=A");
                    asmCode.add("@SP");
                    asmCode.add("A=M");
                    asmCode.add("M=D");
                    asmCode.addAll(generatePushStack());
                }
                break;
            case "pointer":
                if (type.equals(CommandType.C_PUSH)) {
                    asmCode.add("@" + (index.equals(0) ? "THIS" : "THAT"));
                    asmCode.add("D=M");
                    asmCode.add("@SP");
                    asmCode.add("A=M");
                    asmCode.add("M=D");
                    asmCode.addAll(generatePushStack());
                } else if (type.equals(CommandType.C_POP)) {
                    asmCode.addAll(generatePopStack());
                    asmCode.add("D=M");
                    asmCode.add("@" + (index.equals(0) ? "THIS" : "THAT"));
                    asmCode.add("M=D");
                }
                break;
        }
        writeCode(asmCode);
    }

    private ArrayList<String> generateDynamicMemorySegments(String addr, CommandType type, Integer index) {
        ArrayList<String> code = new ArrayList<>();
        code.add("@" + index);
        code.add("D=A");
        code.add("@" + addr);
        code.add("A=M");
        code.add("A=D+A");
        if (type.equals(CommandType.C_PUSH)) {
            code.add("D=M");
            code.add("@SP");
            code.add("A=M");
            code.add("M=D");
            code.addAll(generatePushStack());
        } else if (type.equals(CommandType.C_POP)) {
            code.add("D=A");
            code.add("@R13");
            code.add("M=D");
            code.addAll(generatePopStack());
            code.add("D=M");
            code.add("@R13");
            code.add("A=M");
            code.add("M=D");
        }
        return code;
    }

    public void close() throws IOException {
        out.close();
    }

    private void writeCode(ArrayList<String> codeLines) throws IOException {
        for (String codeLine : codeLines) {
            out.write(codeLine);
            out.write('\n');
        }
    }

    private ArrayList<String> generatePopStack() {
        ArrayList<String> code = new ArrayList<>();
        code.add("@SP");
        code.add("M=M-1");
        code.add("A=M");
        return code;
    }

    private ArrayList<String> generatePushStack() {
        ArrayList<String> code = new ArrayList<>();
        code.add("@SP");
        code.add("M=M+1");
        return code;
    }

    public void writeLabel(CommandType commandType, String label) throws IOException {
        ArrayList<String> code = new ArrayList<>();
        code.add("(" + label + ")");
        this.writeCode(code);
    }

    public void writeGoto(CommandType commandType, String label) throws IOException {
        ArrayList<String> code = new ArrayList<>();
        code.add("@" + label);
        code.add("0;JMP");
        this.writeCode(code);
    }

    public void writeIf(CommandType commandType, String label) throws IOException {
        ArrayList<String> code = new ArrayList<>(this.generatePopStack());
        code.add("D=M");
        code.add("@" + label);
        code.add("D;JNE");
        this.writeCode(code);
    }

    public void writeFunction(CommandType commandType, String funcName, int numLocals) throws IOException {
        ArrayList<String> code = new ArrayList<>();
        code.add("(" + funcName + ")");
        while (numLocals > 0) {
            code.add("@0");
            code.add("D=A");
            code.add("@SP");
            code.add("A=M");
            code.add("M=D");
            code.addAll(this.generatePushStack());
            numLocals--;
        }
        this.writeCode(code);
    }

    public void writeReturn(CommandType commandType) throws IOException {
        ArrayList<String> code = new ArrayList<>();
        // frame R14, retAddr R15
        // frame = LCL
        code.add("@LCL");
        code.add("D=M");
        code.add("@R14");
        code.add("M=D");
        // retAddr = *(frame-5)
        code.add("@5");
        code.add("A=D-A");
        code.add("D=M");
        code.add("@R15");
        code.add("M=D");
        // *ARG = pop
        code.addAll(this.generatePopStack());
        code.add("D=M");
        code.add("@ARG");
        code.add("A=M");
        code.add("M=D");
        // SP=ARG+1
        code.add("@ARG");
        code.add("D=M");
        code.add("D=D+1");
        code.add("@SP");
        code.add("M=D");
        // THAT=*(frame-1)
        // mutate frame
        code.add("@R14");
        code.add("M=M-1");
        code.add("A=M");
        code.add("D=M");
        code.add("@THAT");
        code.add("M=D");
        // THIS=*(frame-2)
        // mutate frame
        code.add("@R14");
        code.add("M=M-1");
        code.add("A=M");
        code.add("D=M");
        code.add("@THIS");
        code.add("M=D");
        // ARG=*(frame-3)
        // mutate frame
        code.add("@R14");
        code.add("M=M-1");
        code.add("A=M");
        code.add("D=M");
        code.add("@ARG");
        code.add("M=D");
        // LCL=*(frame-4)
        // mutate frame
        code.add("@R14");
        code.add("M=M-1");
        code.add("A=M");
        code.add("D=M");
        code.add("@LCL");
        code.add("M=D");
        // goto retAddr
        code.add("@R15");
        code.add("A=M");
        code.add("0;JMP");
        this.writeCode(code);
    }

    public void writeCall(CommandType commandType, String funcName, int numArgs) throws IOException {
        ArrayList<String> code = new ArrayList<>();
        // push returnAddress
        code.add("@" + generateRetAddr(funcName));
        code.add("D=A");
        code.add("@SP");
        code.add("A=M");
        code.add("M=D");
        code.addAll(generatePushStack());
        // push LCL
        code.add("@LCL");
        code.add("D=M");
        code.add("@SP");
        code.add("A=M");
        code.add("M=D");
        code.addAll(generatePushStack());
        // push ARG
        code.add("@ARG");
        code.add("D=M");
        code.add("@SP");
        code.add("A=M");
        code.add("M=D");
        code.addAll(generatePushStack());
        // push THIS
        code.add("@THIS");
        code.add("D=M");
        code.add("@SP");
        code.add("A=M");
        code.add("M=D");
        code.addAll(generatePushStack());
        // push THAT
        code.add("@THAT");
        code.add("D=M");
        code.add("@SP");
        code.add("A=M");
        code.add("M=D");
        code.addAll(generatePushStack());
        // ARG = SP - nArgs - 5
        code.add("@"+ (numArgs + 5));
        code.add("D=A");
        code.add("@SP");
        code.add("D=M-D");
        code.add("@ARG");
        code.add("M=D");
        // LCL = SP
        code.add("@SP");
        code.add("D=M");
        code.add("@LCL");
        code.add("M=D");
        // goto funcName
        code.add("@" + funcName);
        code.add("0;JMP");
        code.add("(" + generateRetAddr(funcName) + ")");

        incrementFuncNameNum(funcName);
        writeCode(code);
    }

    private String generateRetAddr(String funcName) {
        int num = 0;
        if(funcNamesNum.containsKey(funcName)) {
            num = funcNamesNum.get(funcName);
        } else {
            funcNamesNum.put(funcName, num);
        }
        return funcName + num;
    }

    private void incrementFuncNameNum(String funcName) {
        if(funcNamesNum.containsKey(funcName)) {
            int num = funcNamesNum.get(funcName);
            funcNamesNum.put(funcName, ++num);
        }
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    private void writeInit() throws IOException {
        ArrayList<String> code = new ArrayList<>();
        code.add("@256");
        code.add("D=A");
        code.add("@SP");
        code.add("M=D");
        this.writeCode(code);
        this.writeCall(CommandType.C_CALL, "Sys.init", 0);
    }
}
