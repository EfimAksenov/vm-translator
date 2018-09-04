package efimke.vmTranslator;

import java.io.*;

public class Parser {

    private BufferedReader in;
    private String currentCommand;
    private String arg1;
    private Integer arg2;
    private CommandType currentCommandType;

    Parser(String file) throws FileNotFoundException {
        this.in = new BufferedReader(new FileReader(new File(file)));
    }

    public boolean hasMoreCommands() throws IOException {
        return in.ready();
    }

    public void advance() throws IOException {
        if (hasMoreCommands()) {
            String command = in.readLine();
            if (command.startsWith("//") || command.isEmpty()) {
                advance();
                return;
            }
            System.out.println(command);
            String[] splittedLine = command.split(" ");
            currentCommand = splittedLine[0];
            if (splittedLine.length > 1) {
                arg1 = command.split(" ")[1];
            }
            if (splittedLine.length > 2) {
                arg2 = Integer.parseInt(command.split(" ")[2]);
            }
            currentCommandType = parseCommandType();
        }
    }

    public CommandType getCommandType() {
        return currentCommandType;
    }

    private CommandType parseCommandType() {
        switch (currentCommand) {
            case "add":
            case "sub":
            case "neg":
            case "eq":
            case "gt":
            case "lt":
            case "and":
            case "or":
            case "not":
                return CommandType.C_ARITHMETIC;
            case "pop":
                return CommandType.C_POP;
            case "push":
                return CommandType.C_PUSH;
            default:
                return null;
        }
    }

    public String getArg1() {
        switch (currentCommandType) {
            case C_ARITHMETIC:
                return currentCommand;
            case C_RETURN:
                return "";
            default:
                return arg1;
        }
    }

    public Integer getArg2() {
        switch (currentCommandType) {
            case C_PUSH:
            case C_POP:
            case C_FUNCTION:
            case C_CALL:
                return arg2;
            default:
                return -1;
        }
    }

    public void close() throws IOException{
        in.close();
    }

    public String getCurrentCommand () {
        return currentCommand;
    }
}
