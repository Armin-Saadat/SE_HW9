package parser;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import log.Log;
import codegenerator.CodeGenerator;
import errorhandler.ErrorHandler;
import scanner.LexicalAnalyzer;
import scanner.token.Token;


public class Parser {
  private List<Rule> rules;
  private Stack<Integer> parsStack;
  private ParseTable parseTable;
  private LexicalAnalyzer lexicalAnalyzer;
  private CodeGenerator cg;

  public Parser() {
    initParseStack();
    initParseTable();
    initRules();
    cg = new CodeGenerator();
  }

  private void initParseStack() {
    parsStack = new Stack<Integer>();
    parsStack.push(0);
  }

  private void initParseTable() {
    try {
      parseTable = new ParseTable(Files.readAllLines(Paths.get("src/main/resources/parseTable")).get(0));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void initRules() {
    rules = new ArrayList<Rule>();
    try {
      for (String stringRule : Files.readAllLines(Paths.get("src/main/resources/Rules"))) {
        rules.add(new Rule(stringRule));
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void startParse(java.util.Scanner sc) {
    lexicalAnalyzer = new LexicalAnalyzer(sc);
    Token lookAhead = lexicalAnalyzer.getNextToken();
    boolean finish = false;
    Action currentAction;
    while (!finish) {
      try {
        Log.print(lookAhead.toString() + "\t" + parsStack.peek());
        currentAction = parseTable.getActionTable(parsStack.peek(), lookAhead);
        Log.print(currentAction.toString());

        switch (currentAction.action) {
          case shift:
            parsStack.push(currentAction.number);
            lookAhead = lexicalAnalyzer.getNextToken();

            break;
          case reduce:
            Rule rule = rules.get(currentAction.number);
            for (int i = 0; i < rule.RHS.size(); i++) {
              parsStack.pop();
            }

            Log.print(parsStack.peek() + "\t" + rule.LHS);
            parsStack.push(parseTable.getGotoTable(parsStack.peek(), rule.LHS));
            Log.print(parsStack.peek() + "");
            try {
              cg.semanticFunction(rule.semanticAction, lookAhead);
            } catch (Exception e) {
              Log.print("Code Generator Error");
            }
            break;
          case accept:
            finish = true;
            break;
        }
        Log.print("");

      } catch (Exception e) {
        e.printStackTrace();
      }


    }
    if (!ErrorHandler.hasError) {
      cg.printMemory();
    }


  }


}
