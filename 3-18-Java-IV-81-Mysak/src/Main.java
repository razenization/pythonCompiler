package com.amaterasu.main;

import java.util.Scanner;

public class Main {
  public static void main(String[] args) {
    try {
      LexingChecker lexChecker = new LexingChecker("3-18-Java-IV-81-Mysak.py");

      System.out.println("\n====> LEXING CHECK RESULT <====\n");
      lexChecker.listTokens();

      TokenParser parser = new TokenParser(lexChecker.getTokens());
      parser.getCoreAST().printAST();
      System.out.println("\n====> END <====\n");

      ASMGenerator asmGen = new ASMGenerator(parser.getCoreAST(), parser.getAstMap());
      String generatedFileName = "3-18-Java-IV-81-Mysak.asm";
      boolean success = asmGen.createFile(generatedFileName);

      if (success) {
        System.out.println("Compilation succeeded, " + generatedFileName + " is located in " + System.getProperty("user.dir")
            + "\\" + generatedFileName);
      } else {
        System.err.println("Compilation failed");
      }
    } catch (ParseException e) {
      System.err.println(e.getMessage());
    } finally {
      Scanner scan = new Scanner(System.in);
      scan.nextLine();
      scan.close();
    }
  }
}
