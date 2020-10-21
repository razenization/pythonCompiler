package com.amaterasu.main;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.*;


public class TokenParser {
  private final HashMap<String, AST> astMap;
  private final AST coreAST;
  private final HashMap<String, String[]> templates;
  private final HashMap<Integer, String> priorities;

  public TokenParser(ArrayList<Token> tokens) throws ParseException {
    TwoWayIterator<Token> tokenIterator = new TwoWayIterator<>(tokens);

    this.astMap = new HashMap<>();
    this.coreAST = new AST(tokenIterator.next());
    this.templates = new HashMap<>();
    this.priorities = new HashMap<>();

    populateTemplates("3-18-Java-IV-81-Mysak/util/templates.json");
    populatePriorities("");
    run(tokenIterator);
  }

  private void populateTemplates(String pathToTemplates) {
    try {
      String fileContents = Util.readFile(pathToTemplates);
      JSONObject templatesJSON = new JSONObject(fileContents);
      Iterator<String> keysIterator = templatesJSON.keys();
      while (keysIterator.hasNext()) {
        String key = keysIterator.next();
        JSONArray templateArray = templatesJSON.getJSONArray(key);
        ArrayList<String> templateList = new ArrayList<>();
        for (int i = 0; i < templateArray.length(); i++) {
          templateList.add(templateArray.getString(i));
        }
        String[] templateStringArray = templateList.toArray(new String[0]);
        templates.put(key, templateStringArray);
      }
    } catch (IOException e) {
      System.out.println(e.getMessage());
    }
  }

  private void populatePriorities(String pathToPriorities) {
    priorities.put(4, "POW");
    priorities.put(5, "(ADD)|(SUB)|(BIT_NOT)");
    priorities.put(6, "(MUL)|(DIV)|(INT_DIV)|(PERCENT)");
    priorities.put(7, "(ADD)|(SUB)");
    priorities.put(8, "(L_SHIFT)|(R_SHIFT)");
    priorities.put(9, "BIT_AND");
    priorities.put(10, "BIT_XOR");
    priorities.put(11, "BIT_OR");
    priorities.put(12, "(IN)|(IS)|(LT)|(GT)|(LE)|(GE)|(NE)|(EQ)");
    priorities.put(13, "NOT");
    priorities.put(14, "AND");
    priorities.put(15, "OR");
  }

  private void run(TwoWayIterator<Token> tokenIterator) throws ParseException {
    while (tokenIterator.next().getType().equals("DECLARATION")) {
      tokenIterator.prev();
      AST tmp = new AST(parseDefinition(tokenIterator));
      astMap.put(tmp.getRoot().getCurrent().getValue(), tmp);
    }
    tokenIterator.prev();
    while (tokenIterator.hasNext()) {
      if (tokenIterator.next().getType().equals("END")) {
        break;
      }
      tokenIterator.prev();
      ASTNode call = parseDefinitionCall(tokenIterator);
      if (!astMap.containsKey(call.getCurrent().getValue()))
        throwAndPrint("Unexpected call", call.getCurrent());
      call.appendChild(astMap.get(call.getCurrent().getValue()).getRoot());
      coreAST.getRoot().appendChild(call);
    }
  }

  private ASTNode parseDefinition(TwoWayIterator<Token> tokenIterator) throws ParseException {
    int spaceCount = -1;
    Token declarationName = null;
    Token token = null;
    ArrayList<ASTNode> statements;

    for (String part : templates.get("FUNC")) {
      token = tokenIterator.next();
      switch (part) {
        case "BLANK": {
          if (!token.getType().matches("(TAB)|(SPACE)")) {
            System.out.println(token.getType());
            System.out.println("96");
            throwAndPrint("Incorrect type", token);
          }
          break;
        }
        case "IDENTIFIER": {
          declarationName = token;
          break;
        }
        default: {
          if (!token.getType().equals(part)) {
            if (part.equals("RBR")) {
              throwAndPrint(String.format("Expected closing ')'. Found: '%s'", token.getValue()), token);
            }
            throwAndPrint("Incorrect type", token);
          }
        }
      }
    }

    tokenIterator.prev();
    statements = new ArrayList<>();

    assert token != null;
    assert declarationName != null;

    while (token.getType().equals("TAB") || token.getType().equals("SPACE")) {
      int tempSpaces = 0;
      tokenIterator.next();

      while (token.getType().matches("(TAB)|(SPACE)")) {
        if (token.getType().equals("TAB"))
          tempSpaces += 8;
        else
          tempSpaces++;
        token = tokenIterator.next();
      }

      if (spaceCount == -1)
        spaceCount = tempSpaces;
      if (tempSpaces != spaceCount) {
        throwAndPrint("Incorrect tab count", token);
      }
      tokenIterator.prev();

      statements.add(parseStat(tokenIterator));

      token = tokenIterator.next();
      tokenIterator.prev();
    }
    ASTNode def = new ASTNode(new Token(declarationName.getValue(), "DEF_IDENTIFIER",
            declarationName.getRow(), declarationName.getColumn()));

    def.appendChildren(statements);
    for (ASTNode child : statements) {
      child.setParent(def);
    }

    return def;
  }

  private ASTNode parseStat(TwoWayIterator<Token> tokenIterator) throws ParseException {
    Token token;

    token = tokenIterator.next();
    tokenIterator.prev();
    if (token.getType().equals("RETURN")) {
      tokenIterator.next();
      ASTNode returnNode = new ASTNode(new Token("return", "RETURN",
              token.getRow(), token.getColumn())),
              retExp = parseSlug(tokenIterator);

      returnNode.appendChild(retExp);
      retExp.setParent(returnNode);

      token = tokenIterator.next();
      if (!token.getType().equals("NEW_LINE")) {
        System.out.println(token.getType());
        System.out.println("176");
        throwAndPrint("Incorrect type", token);
      }

      return returnNode;
    } else {
      ASTNode exp = parseSlug(tokenIterator);

      token = tokenIterator.next();
      if (!token.getType().equals("NEW_LINE")) {
        System.out.println(token.getType());
        System.out.println("187");
        throwAndPrint("Incorrect type", token);
      }

      return exp;
    }
  }

  private ASTNode parseSlug(TwoWayIterator<Token> tokenIterator) throws ParseException {
    Token token = tokenIterator.next(),
            token2 = tokenIterator.next();

    tokenIterator.prev();
    tokenIterator.prev();
    if (token.getType().equals("IDENTIFIER") && token2.getType().equals("ASSIGNMENT")) {
      tokenIterator.next();
      tokenIterator.next();
      ASTNode id = new ASTNode(new Token(token.getValue(), "ID", token.getRow(), token.getColumn()));

      ASTNode assign = new ASTNode(token2),
              exp = parseSlug(tokenIterator);

      assign.appendChild(exp);
      exp.setParent(assign);

      id.appendChild(assign);
      assign.setParent(id);

      return id;
    } else {
      return parsePriority(15, tokenIterator);
    }
  }

  private ASTNode parsePriority(int prior, TwoWayIterator<Token> tokenIterator) throws ParseException {
    ASTNode operationSign = null, left;
    if (prior <= 6)
      left = parseFactor(tokenIterator);
    else
      left = parsePriority(prior - 1, tokenIterator);

    ArrayList<ASTNode> nodeQueue = new ArrayList<>(),
            operationQueue = new ArrayList<>();
    nodeQueue.add(left);

    Token token = tokenIterator.next();
    tokenIterator.prev();

    if (token.getType().matches(priorities.get(prior))) {
      while (token.getType().matches(priorities.get(prior))) {
        tokenIterator.next();

        operationQueue.add(new ASTNode(token));

        ASTNode node;
        if (prior <= 6)
          node = parseFactor(tokenIterator);
        else
          node = parsePriority(prior - 1, tokenIterator);
        nodeQueue.add(node);

        token = tokenIterator.next();
        tokenIterator.prev();
      }
      Collections.reverse(operationQueue);
      Collections.reverse(nodeQueue);

      TwoWayIterator<ASTNode> operationIterator = new TwoWayIterator<>(operationQueue);
      TwoWayIterator<ASTNode> nodeIterator = new TwoWayIterator<>(nodeQueue);
      operationSign = operationIterator.next();
      operationSign.appendChild(nodeIterator.next());

      while (operationIterator.hasNext()) {
        ASTNode deepest = operationSign.getDeepestLeft(),
                tmpOper = operationIterator.next(),
                tmpNode = nodeIterator.next();

        tmpOper.appendChild(tmpNode);
        tmpNode.setParent(tmpOper);

        deepest.setFirstChild(tmpOper);
        tmpOper.setParent(deepest);
      }
      operationSign.getDeepestLeft().setFirstChild(nodeIterator.next());
    }
    return operationSign == null ? left : operationSign;
  }

  private ASTNode parseFactor(TwoWayIterator<Token> tokenIterator) throws ParseException {
    Token token = tokenIterator.next();

    ASTNode operation;
    if (token.getType().equals("LBR")){
      ASTNode exp = parseSlug(tokenIterator);
      if (!tokenIterator.next().getType().equals("RBR")){
        throwAndPrint("Expected closing ')'.", tokenIterator.next());
      }
      return exp;
    }
    else {
      if (token.getType().matches("(ADD)|(SUB)|(NOT)")){
        operation = new ASTNode(new Token(token.getValue(), "UNAR_"+token.getType(),
                token.getRow(), token.getColumn()));
        ASTNode nextFactor = parseFactor(tokenIterator);
        operation.appendChild(nextFactor);
        assert nextFactor != null;
        nextFactor.setParent(operation);
        return operation;
      }
      else {
        if (token.getType().equals("IDENTIFIER")){
          return new ASTNode(new Token(token.getValue(), "ID", token.getRow(), token.getColumn()));
        }
        else {
          if (token.getType().matches("(INT)|(FLOAT)|(BIN)|(OCT)|(HEX)|(STRING)")) {
            return parseExpression(token);
          } else {
            throwAndPrint("Unexpected token", token);
          }
        }
      }
    }
    return null;
  }

  private ASTNode parseExpression(Token token) throws ParseException {
    String value = token.getValue();

    switch (token.getType()) {
      case "INT": {
        return new ASTNode(token);
      }
      case "FLOAT": {
        StringBuilder casted = new StringBuilder();
        for (char ch : value.toCharArray()) {
          if (ch == '.') {
            break;
          }
          casted.append(ch);
        }
        return new ASTNode(new Token(casted.toString(),
                "INT(FLOAT)", token.getRow(), token.getColumn()));
      }
      case "HEX": {
        return new ASTNode(new Token(Long.decode(value).toString(),
                "INT(HEX)", token.getRow(), token.getColumn()));
      }
      case "OCT": {
        return new ASTNode(new Token(Integer.parseInt(value.substring(2), 8) + "",
                "INT(OCT)", token.getRow(), token.getColumn()));
      }
      case "BIN": {
        return new ASTNode(new Token(Integer.parseInt(value.substring(2), 2) + "",
                "INT(BIN)", token.getRow(), token.getColumn()));
      }
      case "STRING": {
        if (value.length() == 3) {
          return new ASTNode(new Token((int) value.toCharArray()[1] + "",
                  "INT(CHAR)", token.getRow(), token.getColumn()));
        } else {
          throwAndPrint("Casting error", token);
        }
        break;
      }
    }
    return null;
  }

  private ASTNode parseDefinitionCall(TwoWayIterator<Token> tokenIterator) throws ParseException {
    Token token;
    ASTNode defCall = null;
    for (String lexeme : templates.get("CALL")) {
      token = tokenIterator.next();
      if (lexeme.equals("IDENTIFIER")) {
        defCall = new ASTNode(new Token(token.getValue(), "CALL",
                token.getRow(), token.getColumn()));
      } else {
        if (!token.getType().equals(lexeme)) {
          throwAndPrint("Unexpected token", token);
        }
      }
    }

    return defCall;
  }

  private void throwAndPrint(String message, Token token) throws ParseException {
    throw new ParseException(message, token.getRow(), token.getColumn());
  }


  public HashMap<String, AST> getAstMap() {
    return astMap;
  }

  public AST getCoreAST() {
    return coreAST;
  }
}
