package com.amaterasu.main;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

public class ASMGenerator {
  private static final int MAX_VARS = 25;

  private int clause;
  private final AST ast;
  private final HashMap<String, AST> defAST;
  private final HashMap<String, String> masmSyntaxMap;
  private final String asmCode;

  public ASMGenerator(AST ast, HashMap<String, AST> defAST) throws ParseException {
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
    masmSyntaxMap.put("NOT", "\npop ebx\n" +
            "xor eax, eax\n" +
            "cmp eax, ebx\n" +
            "sete al\n" +
            "push eax");

    masmSyntaxMap.put("UNARY_NOT", "\npop ebx\n" +
            "xor eax, eax\n" +
            "cmp eax, ebx\n" +
            "sete al\n" +
            "push eax");

    masmSyntaxMap.put("ADD", "\npop ebx\n" +
            "pop eax\n" +
            "add ebx, eax\n" +
            "push ebx");

    masmSyntaxMap.put("UNARY_ADD", "\n\t\n");

    masmSyntaxMap.put("SUB", "\npop ebx\n" +
            "pop eax\n" +
            "sub eax, ebx\n" +
            "push eax");

    masmSyntaxMap.put("UNARY_SUB", "\npop ebx\n" +
            "neg ebx\n" +
            "push ebx");

    masmSyntaxMap.put("MUL", "\npop ebx\n" +
            "pop eax\n" +
            "imul ebx, eax\n" +
            "push ebx");

    masmSyntaxMap.put("DIV", "\npop ebx\n" +
            "pop eax\n" +
            "cdq\n" +
            "idiv ebx\n" +
            "push eax");

    masmSyntaxMap.put("NUM", "\npush %s\t");

    masmSyntaxMap.put("L_SHIFT",  "\n\npop ebx\n" +
            "pop eax\n" +
            "sal eax, ebx\n" +
            "push eax");

    masmSyntaxMap.put("R_SHIFT",  "\n\npop ebx\n" +
            "pop eax\n" +
            "sar eax, ebx\n" +
            "push eax");

    masmSyntaxMap.put("EQ",   "\n\npop ebx\n" +
            "pop eax\n" +
            "cmp eax, ebx\n" +
            "mov eax, 0\n" +
            "sete al\n" +
            "push eax");

    masmSyntaxMap.put("NE",   "\n\npop ebx\n" +
            "pop eax\n" +
            "cmp eax, ebx\n" +
            "mov eax, 0\n" +
            "setne al\n" +
            "push eax");

    masmSyntaxMap.put("GE",   "\n\npop ebx\n" +
            "pop eax\n" +
            "cmp eax, ebx\n" +
            "mov eax, 0\n" +
            "setge al\n" +
            "push eax");

    masmSyntaxMap.put("LE",   "\n\npop ebx\n" +
            "pop eax\n" +
            "cmp eax, ebx\n" +
            "mov eax, 0\n" +
            "setle al\n" +
            "push eax");

    masmSyntaxMap.put("GT",   "\n\npop ebx\n" +
            "pop eax\n" +
            "cmp eax, ebx\n" +
            "mov eax, 0\n" +
            "setg al\n" +
            "push eax");

    masmSyntaxMap.put("LT",   "\n\npop ebx\n" +
            "pop eax\n" +
            "cmp eax, ebx\n" +
            "mov eax, 0\n" +
            "setl al\n" +
            "push eax");

    masmSyntaxMap.put("AND",  "\n\npop eax\n" +
            "cmp eax, 0\n" +
            "jne _clause%1$d\n" +
            "jmp _end%1$d\n" +
            "_clause%1$d:\n" +
            "\n%2$s" +
            "\n\npop eax\n" +
            "cmp eax, 0\n" +
            "mov eax, 0\n" +
            "setne al\n" +
            "_end%1$d:\n" +
            "push eax");

    masmSyntaxMap.put("OR",  "\n\npop eax\n" +
            "cmp eax, 0\n" +
            "je _clause%1$d\n" +
            "mov eax, 1\n" +
            "jmp _end%1$d\n" +
            "_clause%1$d:\n" +
            "\n%2$s" +
            "\n\npop eax\n" +
            "cmp eax, 0\n" +
            "mov eax, 0\n" +
            "setne al\n" +
            "_end%1$d:\n" +
            "push eax");

    masmSyntaxMap.put("ID",   "\n\nmov ebx, [%d+ebp+4]\n" +
            "push ebx");
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

  private String[] createFunctions() throws ParseException {
    /* 0 - PROTO, 1 - functions */
    String[] functions = {"" ,""};

    for (String defName: defAST.keySet()) {
      /* make PROTO for {defName} */
      functions[0] += String.format("%s PROTO\n", defName);

      /* make {defName} prolog */
      StringBuilder funcTempl = new StringBuilder(String.format("%s PROC\n" +
              "mov ebp, esp\n" +
              "add esp, %d\n", defName, MAX_VARS * 4));
      boolean retFlag = false;
      HashMap<String, Integer> localVars = new HashMap<>();
      int vars = 0;

      for (ASTNode child: defAST.get(defName).getRoot().getChildren()) {
        switch (child.getCurrent().getType()){
          case "RETURN":{
            String retVar = genExpCode(localVars, child.getChild(0));
            funcTempl.append(retVar);
            retFlag = true;
            break;
          }
          case "ID":{
            if (child.getChildren().size() == 0){
              throw new ParseException("Variable referenced before assignment",
                      child.getCurrent().getRow(), child.getCurrent().getColumn());
            }
            String varExp = genExpCode(localVars, child.getChild(0).getChild(0));
            if (!localVars.containsKey(child.getCurrent().getValue())){
              localVars.put(child.getCurrent().getValue(), ++vars);
            }
            funcTempl.append(String.format("%s\n" +
                            "pop ebx\n" +
                            "mov [%d+ebp+4], ebx\n",
                    varExp, 4 * localVars.get(child.getCurrent().getValue())));
            break;
          }
        }
        if (vars >= MAX_VARS)
          throw new ParseException("Too many local variables",
                  child.getCurrent().getRow(), child.getCurrent().getColumn());
        if (retFlag)
          break;
      }

      /* make {defName} epilog */
      funcTempl.append(String.format("\npop ebx\n" +
              "sub esp, %d\n" +
              "ret\n" +
              "%s ENDP\n\n", MAX_VARS * 4, defName));
      functions[1] += funcTempl;
    }

    return functions;
  }

  private String genExpCode(HashMap<String, Integer> localVars, ASTNode current) throws ParseException {
    switch (current.getCurrent().getType()){
      case "UNAR_ADD":
      case "UNAR_SUB":
      case "NOT":{
        return genExpCode(localVars, current.getChild(0))+
                masmSyntaxMap.get(current.getCurrent().getType());
      }
      case "L_SHIFT":
      case "R_SHIFT":
      case "EQ":
      case "NE":
      case "GT":
      case "LT":
      case "GE":
      case "LE":
      case "SUB":
      case "DIV":
      case "MUL":
      case "ADD":{
        return genExpCode(localVars, current.getChild(0))+
                genExpCode(localVars, current.getChild(1))+
                masmSyntaxMap.get(current.getCurrent().getType());
      }
      case "OR":
      case "AND" :{
        return genExpCode(localVars, current.getChild(0))+
                String.format(masmSyntaxMap.get(current.getCurrent().getType()),
                        ++clause, genExpCode(localVars, current.getChild(1)));
      }
      case "INT(CHAR)":
      case "INT(BIN)":
      case "INT(HEX)":
      case "INT(OCT)":
      case "INT(FLOAT)":
      case "INT":{
        return String.format(masmSyntaxMap.get("NUM"),
                current.getCurrent().getValue());
      }
      case "ID": {
        if (!localVars.containsKey(current.getCurrent().getValue())){
          throw new ParseException("Unknown variable",
                  current.getCurrent().getRow(), current.getCurrent().getColumn());
        }

        return String.format(masmSyntaxMap.get(current.getCurrent().getType()),
                localVars.get(current.getCurrent().getValue())*4, current.getCurrent().getValue());
      }
      default:
        return "Unknown operation "+current.getCurrent().getType();
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
