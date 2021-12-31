package codegenerator;

import log.Log;
import errorhandler.ErrorHandler;
import scanner.token.Token;
import semantic.symbol.Symbol;
import semantic.symbol.SymbolTable;
import semantic.symbol.SymbolType;

import java.util.Stack;

import static semantic.symbol.SymbolType.Bool;

/**
 * Created by Alireza on 6/27/2015.
 */
public class CodeGenerator {
    private Memory memory = new Memory();
    private Stack<Address> ss = new Stack<Address>();
    private Stack<String> symbolStack = new Stack<>();
    private Stack<String> callStack = new Stack<>();
    private SymbolTable symbolTable;

    public CodeGenerator() {
        symbolTable = new SymbolTable(memory);
        //TODO
    }

    public void printMemory() {
        memory.pintCodeBlock();
    }

    public void semanticFunction(int func, Token next) {
        Log.print("codegenerator : " + func);

        if (func == 1) {
            checkID();
        } else if (func == 2) {
            pid(next);
        } else if (func == 3) {
            fpid();
        } else if (func == 4) {
            kpid(next);
        } else if (func == 5) {
            intpid(next);
        } else if (func == 6) {
            startCall();
        } else if (func == 7) {
            call();
        } else if (func == 8) {
            arg();
        } else if (func == 9) {
            assign();
        } else if (func == 10) {
            add();
        } else if (func == 11) {
            sub();
        } else if (func == 12) {
            mult();
        } else if (func == 13) {
            label();
        } else if (func == 14) {
            save();
        } else if (func == 15) {
            whileCommand();
        } else if (func == 16) {
            saveJPF();
        } else if (func == 17) {
            jpHere();
        } else if (func == 18) {
            print();
        } else if (func == 19) {
            equal();
        } else if (func == 20) {
            lessThan();
        } else if (func == 21) {
            and();
        } else if (func == 22) {
            not();
        } else if (func == 23) {
            defClass();
        } else if (func == 24) {
            defMethod();
        } else if (func == 25) {
            popClass();
        } else if (func == 26) {
            extend();
        } else if (func == 27) {
            defField();
        } else if (func == 28) {
            defVar();
        } else if (func == 29) {
            methodReturn();
        } else if (func == 30) {
            defParam();
        } else if (func == 31) {
            lastTypeBool();
        } else if (func == 32) {
            lastTypeInt();
        } else if (func == 33) {
            defMain();
        }
    }

    private void defMain() {
        memory.add3AddressCode(ss.pop().getNum(), Operation.JP, new Address(memory.getCurrentCodeBlockAddress(), VarType.Address), null, null);
        String methodName = "main";
        String className = symbolStack.pop();

        symbolTable.addMethod(className, methodName, memory.getCurrentCodeBlockAddress());

        symbolStack.push(className);
        symbolStack.push(methodName);
    }

    public void checkID() {
        symbolStack.pop();
        //TODO : check ss.peek().varType == VarType.Non
    }

    public void pid(Token next) {
        if (symbolStack.size() > 1) {
            String methodName = symbolStack.pop();
            String className = symbolStack.pop();
            try {

                Symbol s = symbolTable.get(className, methodName, next.value);
                VarType t = VarType.Int;
                switch (s.type) {
                    case Bool:
                        t = VarType.Bool;
                        break;
                    case Int:
                        t = VarType.Int;
                        break;
                }
                ss.push(new Address(s.address, t));


            } catch (Exception e) {
                ss.push(new Address(0, VarType.Non));
            }
            symbolStack.push(className);
            symbolStack.push(methodName);
        } else {
            ss.push(new Address(0, VarType.Non));
        }
        symbolStack.push(next.value);
    }

    public void fpid() {
        ss.pop();
        ss.pop();

        Symbol s = symbolTable.get(symbolStack.pop(), symbolStack.pop());
        VarType t = VarType.Int;
        switch (s.type) {
            case Bool:
                t = VarType.Bool;
                break;
            case Int:
                t = VarType.Int;
                break;
        }
        ss.push(new Address(s.address, t));

    }

    public void kpid(Token next) {
        ss.push(symbolTable.get(next.value));
    }

    public void intpid(Token next) {
        ss.push(new Address(Integer.parseInt(next.value), VarType.Int, TypeAddress.Imidiate));
    }

    public void startCall() {
        //TODO: method ok
        ss.pop();
        ss.pop();
        String methodName = symbolStack.pop();
        String className = symbolStack.pop();
        symbolTable.startCall(className, methodName);
        callStack.push(className);
        callStack.push(methodName);
    }

    public void call() {
        //TODO: method ok
        String methodName = callStack.pop();
        String className = callStack.pop();
        try {
            symbolTable.getNextParam(className, methodName);
            ErrorHandler.printError("The few argument pass for method");
            call(methodName, className);
        } catch (IndexOutOfBoundsException e) {
            call(methodName, className);
        }
    }

    private void call(String methodName, String className) {
        VarType t = VarType.Int;
        switch (symbolTable.getMethodReturnType(className, methodName)) {
            case Int:
                t = VarType.Int;
                break;
            case Bool:
                t = VarType.Bool;
                break;
        }
        Address temp = new Address(memory.getTemp(), t);
        ss.push(temp);
        memory.add3AddressCode(Operation.ASSIGN, new Address(temp.getNum(), VarType.Address, TypeAddress.Imidiate), new Address(symbolTable.getMethodReturnAddress(className, methodName), VarType.Address), null);
        memory.add3AddressCode(Operation.ASSIGN, new Address(memory.getCurrentCodeBlockAddress() + 2, VarType.Address, TypeAddress.Imidiate), new Address(symbolTable.getMethodCallerAddress(className, methodName), VarType.Address), null);
        memory.add3AddressCode(Operation.JP, new Address(symbolTable.getMethodAddress(className, methodName), VarType.Address), null, null);
    }

    public void arg() {
        //TODO: method ok

        String methodName = callStack.pop();
        try {
            Symbol s = symbolTable.getNextParam(callStack.peek(), methodName);
            VarType t = VarType.Int;
            switch (s.type) {
                case Bool:
                    t = VarType.Bool;
                    break;
                case Int:
                    t = VarType.Int;
                    break;
            }
            Address param = ss.pop();
            if (param.getVarType() != t) {
                ErrorHandler.printError("The argument type isn't match");
            }
            memory.add3AddressCode(Operation.ASSIGN, param, new Address(s.address, t), null);
        } catch (IndexOutOfBoundsException e) {
            ErrorHandler.printError("Too many arguments pass for method");
        }
        callStack.push(methodName);

    }

    public void assign() {
        Address s1 = ss.pop();
        Address s2 = ss.pop();
        if (s1.getVarType() != s2.getVarType()) {
            ErrorHandler.printError("The type of operands in assign is different ");
        }
        memory.add3AddressCode(Operation.ASSIGN, s1, s2, null);
    }

    public void add() {
        mathOperation(Operation.ADD);
    }

    public void sub() {
        mathOperation(Operation.SUB);
    }

    public void mult() {
        mathOperation(Operation.MULT);
    }

    private void mathOperation(Operation operation) {
        Address temp = new Address(memory.getTemp(), VarType.Int);
        Address s2 = ss.pop();
        Address s1 = ss.pop();
        if (s1.getVarType() != VarType.Int || s2.getVarType() != VarType.Int) {
            ErrorHandler.printError(String.format("In %s two operands must be integer", operation.name()));
        }
        memory.add3AddressCode(operation, s1, s2, temp);
        ss.push(temp);
    }

    public void label() {
        ss.push(new Address(memory.getCurrentCodeBlockAddress(), VarType.Address));
    }

    public void save() {
        ss.push(new Address(memory.saveMemory(), VarType.Address));
    }

    public void whileCommand() {
        memory.add3AddressCode(ss.pop().getNum(), Operation.JPF, ss.pop(), new Address(memory.getCurrentCodeBlockAddress() + 1, VarType.Address), null);
        memory.add3AddressCode(Operation.JP, ss.pop(), null, null);
    }

    public void saveJPF() {
        Address save = new Address(memory.saveMemory(), VarType.Address);
        memory.add3AddressCode(ss.pop().getNum(), Operation.JPF, ss.pop(), new Address(memory.getCurrentCodeBlockAddress(), VarType.Address), null);
        ss.push(save);
    }

    public void jpHere() {
        memory.add3AddressCode(ss.pop().getNum(), Operation.JP, new Address(memory.getCurrentCodeBlockAddress(), VarType.Address), null, null);
    }

    public void print() {
        memory.add3AddressCode(Operation.PRINT, ss.pop(), null, null);
    }

    public void equal() {
        Address temp = new Address(memory.getTemp(), VarType.Bool);
        Address s2 = ss.pop();
        Address s1 = ss.pop();
        if (s1.getVarType() != s2.getVarType()) {
            ErrorHandler.printError("The type of operands in equal operator is different");
        }
        memory.add3AddressCode(Operation.EQ, s1, s2, temp);
        ss.push(temp);
    }

    public void lessThan() {
        Address temp = new Address(memory.getTemp(), VarType.Bool);
        Address s2 = ss.pop();
        Address s1 = ss.pop();
        if (s1.getVarType() != VarType.Int || s2.getVarType() != VarType.Int) {
            ErrorHandler.printError("The type of operands in less than operator is different");
        }
        memory.add3AddressCode(Operation.LT, s1, s2, temp);
        ss.push(temp);
    }

    public void and() {
        Address temp = new Address(memory.getTemp(), VarType.Bool);
        Address s2 = ss.pop();
        Address s1 = ss.pop();
        if (s1.getVarType() != VarType.Bool || s2.getVarType() != VarType.Bool) {
            ErrorHandler.printError("In and operator the operands must be boolean");
        }
        memory.add3AddressCode(Operation.AND, s1, s2, temp);
        ss.push(temp);

    }

    public void not() {
        Address temp = new Address(memory.getTemp(), VarType.Bool);
        Address s2 = ss.pop();
        Address s1 = ss.pop();
        if (s1.getVarType() != VarType.Bool) {
            ErrorHandler.printError("In not operator the operand must be boolean");
        }
        memory.add3AddressCode(Operation.NOT, s1, s2, temp);
        ss.push(temp);

    }

    public void defClass() {
        ss.pop();
        symbolTable.addClass(symbolStack.peek());
    }

    public void defMethod() {
        ss.pop();
        String methodName = symbolStack.pop();
        String className = symbolStack.pop();

        symbolTable.addMethod(className, methodName, memory.getCurrentCodeBlockAddress());

        symbolStack.push(className);
        symbolStack.push(methodName);

    }

    public void popClass() {
        symbolStack.pop();
    }

    public void extend() {
        ss.pop();
        symbolTable.setSuperClass(symbolStack.pop(), symbolStack.peek());
    }

    public void defField() {
        ss.pop();
        symbolTable.addField(symbolStack.pop(), symbolStack.peek());
    }

    public void defVar() {
        ss.pop();

        String var = symbolStack.pop();
        String methodName = symbolStack.pop();
        String className = symbolStack.pop();

        symbolTable.addMethodLocalVariable(className, methodName, var);

        symbolStack.push(className);
        symbolStack.push(methodName);
    }

    public void methodReturn() {
        //TODO : call ok

        String methodName = symbolStack.pop();
        Address s = ss.pop();
        SymbolType t = symbolTable.getMethodReturnType(symbolStack.peek(), methodName);
        VarType temp = VarType.Int;
        if (t == Bool) {
            temp = VarType.Bool;
        }
        if (s.getVarType() != temp) {
            ErrorHandler.printError("The type of method and return address was not match");
        }
        memory.add3AddressCode(Operation.ASSIGN, s, new Address(symbolTable.getMethodReturnAddress(symbolStack.peek(), methodName), VarType.Address, TypeAddress.Indirect), null);
        memory.add3AddressCode(Operation.JP, new Address(symbolTable.getMethodCallerAddress(symbolStack.peek(), methodName), VarType.Address), null, null);
    }

    public void defParam() {
        //TODO : call Ok
        ss.pop();
        String param = symbolStack.pop();
        String methodName = symbolStack.pop();
        String className = symbolStack.pop();

        symbolTable.addMethodParameter(className, methodName, param);

        symbolStack.push(className);
        symbolStack.push(methodName);
    }

    public void lastTypeBool() {
        symbolTable.setLastType(Bool);
    }

    public void lastTypeInt() {
        symbolTable.setLastType(SymbolType.Int);
    }

    public void main() {

    }

}
