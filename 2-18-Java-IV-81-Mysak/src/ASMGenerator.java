package com.amaterasu.main;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

public class ASMGenerator {
  private final AST ast;
  private final HashMap<String, AST> defAST;
  private final HashMap<String, String> masmSyntaxMap;
  private final String asmCode;

  public ASMGenerator(AST ast, HashMap<String, AST> defAST) {
    this.ast = ast;
    this.defAST = defAST;
    this.masmSyntaxMap = new HashMap<>();
    populateASMTemplates();

    String[] functions = createFunctions();
    String masmTemplate = ".386\n" +
            ".model flat,stdcall\n" +
            "option casemap:none\n\n" +
            "include     D:\\masm32\\include\\windows.inc\n" +
            "include     D:\\masm32\\include\\kernel32.inc\n" +
            "include     D:\\masm32\\include\\masm32.inc\n" +
            "includelib  D:\\masm32\\lib\\kernel32.lib\n" +
            "includelib  D:\\masm32\\lib\\masm32.lib\n\n" +
            "_NumbToStr   PROTO :DWORD,:DWORD\n" +
            "_main        PROTO\n\n" +
            "%s\n" +
            ".data\n" +
            "buff        db 11 dup(?)\n\n" +
            ".code\n" +
            "_start:\n" +
            "\tinvoke  _main\n" +
            "\tinvoke  _NumbToStr, ebx, ADDR buff\n" +
            "\tinvoke  StdOut,eax\n" +
            "\tinvoke  ExitProcess,0\n\n" +
            "_main PROC\n\n" +
            "%s" +
            "\n\tret\n\n" +
            "_main ENDP\n\n" +
            "%s" +
            "\n_NumbToStr PROC uses ebx x:DWORD,buffer:DWORD\n\n" +
            "\tmov     ecx,buffer\n" +
            "\tmov     eax,x\n" +
            "\tmov     ebx,10\n" +
            "\tadd     ecx,ebx\n" +
            "@@:\n" +
            "\txor     edx,edx\n" +
            "\tdiv     ebx\n" +
            "\tadd     edx,48\n" +
            "\tmov     BYTE PTR [ecx],dl\n" +
            "\tdec     ecx\n" +
            "\ttest    eax,eax\n" +
            "\tjnz     @b\n" +
            "\tinc     ecx\n" +
            "\tmov     eax,ecx\n" +
            "\tret\n\n" +
            "_NumbToStr ENDP\n\n" +
            "END _start\n";
    this.asmCode = String.format(masmTemplate, functions[0], mainCode(), functions[1]);
  }

  private void populateASMTemplates() {
    masmSyntaxMap.put("NOT", "\npop ebx\t; not\n" +
            "xor eax, eax\n" +
            "cmp eax, ebx\n" +
            "sete al\n" +
            "push eax");

    masmSyntaxMap.put("UNARY_NOT", "\npop ebx\t; not\n" +
            "xor eax, eax\n" +
            "cmp eax, ebx\n" +
            "sete al\n" +
            "push eax");

    masmSyntaxMap.put("ADD", "\npop ebx\t; add\n" +
            "pop eax\n" +
            "add ebx, eax\n" +
            "push ebx");

    masmSyntaxMap.put("UNARY_ADD", "\n\t\t; unar add\n");

    masmSyntaxMap.put("SUB", "\npop ebx\t; sub\n" +
            "pop eax\n" +
            "sub eax, ebx\n" +
            "push eax");

    masmSyntaxMap.put("UNARY_SUB", "\npop ebx\t; unar sub\n" +
            "neg ebx\n" +
            "push ebx");

    masmSyntaxMap.put("MUL", "\npop ebx\t; mul\n" +
            "pop eax\n" +
            "imul ebx, eax\n" +
            "push ebx");

    masmSyntaxMap.put("DIV", "\npop ebx\t; div\n" +
            "pop eax\n" +
            "cdq\n" +
            "idiv ebx\n" +
            "push eax");

    masmSyntaxMap.put("NUM", "\npush %s\t");
  }

  public boolean createFile(String fileName) {
    try (FileWriter writer = new FileWriter(fileName, false)) {
      writer.write(asmCode);
      writer.flush();
      return true;
    } catch (IOException ex) {
      System.out.println(ex.getMessage());
      return false;
    }
  }

  private String[] createFunctions() {
    String[] functions = {"", ""};

    for (String defName : defAST.keySet()) {
      functions[0] += String.format("%s\tPROTO\n", defName);
      String funcTempl = String.format("%s PROC\n\n", defName);
      for (ASTNode child : defAST.get(defName).getRoot().getChildren()) {
        // do smth what is function body
        if (child.getCurrent().getType().equals("RETURN")) {
          String retVar = genExpCode(child.getChild(0));
          funcTempl += String.format("%s\n\npop ebx\nret\n", retVar);
          break;
        }
      }
      funcTempl += String.format("\n%s ENDP\n", defName);
      functions[1] += funcTempl;
    }

    return functions;
  }

  private String genExpCode(ASTNode current) {
    switch (current.getCurrent().getType()) {
      case "UNARY_ADD":
      case "UNARY_SUB":
      case "UNARY_NOT":
      case "NOT": {
        return genExpCode(current.getChild(0)) +
                masmSyntaxMap.get(current.getCurrent().getType());
      }
      case "ADD":
      case "SUB":
      case "MUL":
      case "DIV": {
        return genExpCode(current.getChild(0)) +
                genExpCode(current.getChild(1)) +
                masmSyntaxMap.get(current.getCurrent().getType());
      }
      case "INT(CHAR)":
      case "INT(BIN)":
      case "INT(HEX)":
      case "INT(OCT)":
      case "INT(FLOAT)":
      case "INT": {
        return String.format(masmSyntaxMap.get("NUM"),
                current.getCurrent().getValue());
      }
      default:
        return "Unknown operation " + current.getCurrent().getType();
    }
  }

  private String mainCode() {
    StringBuilder code = new StringBuilder();

    for (ASTNode node : ast.getRoot().getChildren()) {
      if ("CALL".equals(node.getCurrent().getType())) {
        code.append(String.format("\tcall %s\n", node.getCurrent().getValue()));
      }
    }

    return code.toString();
  }
}
