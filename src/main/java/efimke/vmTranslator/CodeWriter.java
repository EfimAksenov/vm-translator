package efimke.vmTranslator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class CodeWriter {

    private static final int offsetForTmpMS = 5;
    private FileWriter out;
    private String fileName;
    private static int eqNumber = 0;
    private static int ltNumber = 0;
    private static int gtNumber = 0;

    public CodeWriter(String file) throws IOException {
        out = new FileWriter(new File(file.replace(".vm", ".asm")));
        String[] tmp = file.split("/");
        fileName = tmp[tmp.length - 1].split("[.]")[0];
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
}
