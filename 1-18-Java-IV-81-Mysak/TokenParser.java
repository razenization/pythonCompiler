package com.amaterasu.main;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.*;


public class TokenParser {
  private final HashMap<String, AST> astMap;
  private final AST coreAST;
  private final HashMap<String, String[]> templates;

  public TokenParser(ArrayList<Token> tokens) throws ParseException {
    TwoWayIterator<Token> tokenIterator = new TwoWayIterator<>(tokens);

    this.astMap = new HashMap<>();
    this.coreAST = new AST(tokenIterator.next());
    this.templates = new HashMap<>();

    populateTemplates("templates.json");
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
    int[] spacesAndTabs = {0, 0};
    String declarationSlug = "";
    Token token;
    ASTNode declaration = null;

    for (String part : templates.get("FUNC")) {
      token = tokenIterator.next();
      switch (part) {
        case "BLANK": {
          while (token.getType().matches("(TAB)|(SPACE)")) {
            if (token.getType().equals("TAB")) {
              spacesAndTabs[1]++;
            } else {
              spacesAndTabs[0]++;
            }
            token = tokenIterator.next();
          }
          if (spacesAndTabs[0] + spacesAndTabs[1] == 0) {
            throwAndPrint("Incorrect tab count", token);
          }
          break;
        }
        case "STAT": {
          tokenIterator.prev();
          ASTNode statement = parseStat(tokenIterator);
          declaration = new ASTNode(new Token(declarationSlug, "DECLARATION_NAME",
                  token.getRow(), token.getColumn()));
          declaration.appendChild(statement);
          statement.setParent(declaration);
          break;
        }
        case "IDENTIFIER": {
          declarationSlug = token.getValue();
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
    return declaration;
  }

  private ASTNode parseStat(TwoWayIterator<Token> tokenIterator) throws ParseException {
    Token token;
    ASTNode statement = null;
    tokenIterator.prev();
    for (String part : templates.get("STAT")) {
      token = tokenIterator.next();
      if (part.equals("EXP")) {
        tokenIterator.prev();
        ASTNode exp = parseSlug(ExpressionType.EXP, tokenIterator);
        statement = new ASTNode(new Token("return", "RETURN",
                token.getRow(), token.getColumn()));
        statement.appendChild(exp);
        exp.setParent(statement);
      } else {
        if (!token.getType().equals(part)) {
          throwAndPrint("Incorrect type", token);
        }
      }
    }
    return statement;
  }

  enum ExpressionType {
    EXP, TERM
  }


  private ASTNode parseSlug(ExpressionType type, TwoWayIterator<Token> tokenIterator) throws ParseException {
    ASTNode slug = type == ExpressionType.EXP ? parseSlug(ExpressionType.TERM, tokenIterator) : parseFactor(tokenIterator),
            operation = null;
    Token token = tokenIterator.next();
    tokenIterator.prev();
    String regToMatch = type == ExpressionType.EXP ? "(ADD)|(SUB)" : "(MUL)|(DIV)|(INT_DIV)";
    while (token.getType().matches(regToMatch)) {
      tokenIterator.next();
      operation = new ASTNode(token);
      ASTNode nextTerm = type == ExpressionType.EXP ? parseSlug(ExpressionType.TERM, tokenIterator) : parseFactor(tokenIterator);
      operation.appendChild(slug);
      operation.appendChild(nextTerm);
      assert slug != null;
      assert nextTerm != null;
      slug.setParent(operation);
      nextTerm.setParent(operation);
      token = tokenIterator.next();
      tokenIterator.prev();
    }

    return operation == null ? slug : operation;
  }

  private ASTNode parseFactor(TwoWayIterator<Token> tokenIterator) throws ParseException {
    Token token = tokenIterator.next();

    ASTNode operation;
    if (token.getType().equals("LBR")) {
      ASTNode exp = parseSlug(ExpressionType.EXP, tokenIterator);
      if (!tokenIterator.next().getType().equals("RBR")) {
        throwAndPrint(String.format("Expected closing ')'. Found: %s", tokenIterator.current().getValue()), tokenIterator.current());
      }
      return exp;
    } else {
      if (token.getType().matches("(ADD)|(SUB)|(NOT)")) {
        operation = new ASTNode(new Token(token.getValue(), "UNARY_" + token.getType(),
                token.getRow(), token.getColumn()));
        ASTNode nextTerm = parseFactor(tokenIterator);
        operation.appendChild(nextTerm);
        assert nextTerm != null;
        nextTerm.setParent(operation);
        return operation;
      } else {
        if (token.getType().matches("(INT)|(FLOAT)|(BIN)|(OCT)|(HEX)|(STRING)")) {
          return parseExpression(token);
        } else {
          throwAndPrint("Unexpected token", token);
        }
      }
    }
    return null;
  }

  private ASTNode parseExpression(Token token) throws ParseException {
    String value = token.getValue();
    String type = token.getType();

    switch (token.getType()) {
      case "INT": {
        break;
      }
      case "FLOAT": {
        StringBuilder casted = new StringBuilder();
        for (char ch : value.toCharArray()) {
          if (ch == '.') {
            break;
          }
          casted.append(ch);
        }
        value = casted.toString();
        type = "INT(FLOAT)";
        break;
      }
      case "HEX":
      case "OCT":
      case "BIN": {
        try {
          value = Long.decode(value).toString();
        } catch (Exception e) {
          value = Integer.valueOf(value.substring(2), 2).toString();
        }
        break;
      }
      case "STRING": {
        if (value.length() == 3) {
          value = (int) value.toCharArray()[1] + "";
          type = "INT(CHAR)";
        } else {
          throwAndPrint("Casting error", token);
        }
        break;
      }
      default:
        return null;
    }
    return new ASTNode(new Token(value, type, token.getRow(), token.getColumn()));
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
