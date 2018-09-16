package efimke.vmTranslator;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.Queue;
import java.util.stream.Stream;

public class Parser {

    private Queue<Path> pathsToVmFiles = new LinkedList<>();
    private BufferedReader in;
    private String currentFileName;
    private String currentCommand;
    private String arg1;
    private Integer arg2;
    private CommandType currentCommandType;

    Parser(String uri) throws IOException {
        Path path = Paths.get(uri);
        if (Files.isDirectory(path)) {
//            for (File file : path.toFile().listFiles()) {
//                if (file.getName().endsWith(".vm")) {
//                    this.in.offer(new BufferedReader(new FileReader(file)));
//                }
//            }
            Stream<Path> paths = Files.walk(path);
            paths.forEach(filePath -> {
                if (filePath.getFileName().toString().endsWith(".vm")) {
                    this.pathsToVmFiles.offer(filePath);
                        System.out.println(filePath.toString());
                }
            });
            this.in = new BufferedReader(new FileReader(new File(pathsToVmFiles.poll().toString())));
        } else {
            this.pathsToVmFiles.offer(path);
        }
    }

    public boolean hasMoreCommands() throws IOException {
        boolean result = false;
        if (!in.ready()) {
            in.close();
            while (pathsToVmFiles.peek() != null) {
               Path path = pathsToVmFiles.poll();
               this.currentFileName = path.getFileName().toString().replace(".vm", "");
               this.in = new BufferedReader(new FileReader(new File(path.toString())));
               return hasMoreCommands();
            }
        } else {
            result = true;
        }
        return result;
    }

    public void advance() throws IOException {
        if (hasMoreCommands()) {
            String command = in.readLine();
            if (command.startsWith("//") || command.isEmpty()) {
                advance();
                return;
            }
            String[] splittedLine = command.split("//")[0].trim().split(" ");
            currentCommand = splittedLine[0];
            if (splittedLine.length > 1) {
                arg1 = splittedLine[1];
            }
            if (splittedLine.length > 2) {
                arg2 = Integer.parseInt(splittedLine[2]);
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
            case "label":
                return CommandType.C_LABEL;
            case "goto":
                return CommandType.C_GOTO;
            case "if-goto":
                return CommandType.C_IF;
            case "function":
                return CommandType.C_FUNCTION;
            case "return":
                return CommandType.C_RETURN;
            case "call":
                return CommandType.C_CALL;
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

    public void close() throws IOException {
        in.close();
    }

    public String getCurrentFileName() {
        return currentFileName;
    }

    public String getCurrentCommand() {
        return currentCommand;
    }
}
