package com.amaterasu.main;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

public class TokenParser {
  private final HashMap<String, AST> astMap;
  private final AST coreAST;

  public TokenParser(ArrayList<Token> tokens) throws ParseException {
    this.astMap = new HashMap<>();
    this.coreAST = new AST(new Token(null, "BEGIN", 0, 0));

    for (Iterator<Token> tokenIterator = tokens.iterator(); tokenIterator.hasNext();) {
      Token token = tokenIterator.next();
      String tokenValue = token.getValue();
      String tokenType = token.getType();
      if (tokenType.equals("DECLARATION")) {
        AST astBranch = new AST(parseDefinition(token, tokenIterator));
        astMap.put(astBranch.getRoot().getCurrent().getValue(), astBranch);
      } else if (tokenType.equals("IDENTIFIER")) {
        coreAST.getRoot().appendChild(parseIdentifier(token, tokenIterator));
      } else {
        System.out.println(tokenValue + " " + tokenType);
      }
    }
  }

  private void verifyLiteral(Token token, String expectedLiteral) throws ParseException {
    if (!token.getType().matches(expectedLiteral)) {
      throwAndPrint("Invalid type", token);
    }
  }

  private void iterateAndVerify(Iterator<Token> tokenIterator, String[] expectedTypes) throws ParseException {
    for (String expectedType : expectedTypes) {
      Token token = tokenIterator.next();
      verifyLiteral(token, expectedType);
    }
  }

  private ASTNode parseDefinition(Token prev, Iterator<Token> tokenIterator) throws ParseException {
    int[] spacesAndTabs = { 0, 0 };
    Token token = tokenIterator.next();
    String declarationName = token.getValue();
    String[] expectedIdentifiers = { "LBR", "RBR", "COLON", "NEW_LINE" };
    iterateAndVerify(tokenIterator, expectedIdentifiers);

    token = tokenIterator.next();

    while (token.getType().matches("(TAB)|(SPACE)")) {
      if (token.getType().equals("TAB")) {
        spacesAndTabs[1]++;
      } else {
        spacesAndTabs[0]++;
      }
      token = tokenIterator.next();
    }

    ASTNode statement = parseStatement(token, tokenIterator, spacesAndTabs);
    ASTNode declaration = new ASTNode(new Token(declarationName, "DECLARATION_NAME", prev.getRow(), prev.getColumn()));
    declaration.appendChild(statement);
    statement.setParent(declaration);

    return declaration;
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

  private ASTNode parseStatement(Token prev, Iterator<Token> tokenIterator, int[] spaceTabCount) throws ParseException {
    String[] numTypes = { "INT", "FLOAT", "HEX", "OCT", "BIN", "STRING" };
    if (spaceTabCount[0] + spaceTabCount[1] == 0) {
      throwAndPrint("Tab count is invalid", prev);
    }
    if (!prev.getType().equals("RETURN")) {
      throwAndPrint("Type is invalid", prev);
    }
    Token token = tokenIterator.next();
    if (!Arrays.asList(numTypes).contains(token.getType())) {
      throwAndPrint("Casting error", token);
    }
    ASTNode exp = parseExpression(token);
    ASTNode statement = new ASTNode(new Token("return", "RETURN", prev.getRow(), prev.getColumn()));
    statement.appendChild(exp);
    assert exp != null;
    exp.setParent(statement);

    token = tokenIterator.next();
    if (!token.getType().equals("NEW_LINE")) {
      throwAndPrint("Type is invalid", token);
    }

    return statement;
  }

  private ASTNode parseIdentifier(Token prev, Iterator<Token> tokenIterator) throws ParseException {
    Token token = tokenIterator.next();
    if (token.getType().equals("LBR")) {
      return parseDefinitionCall(prev, tokenIterator);
    }
    throwAndPrint("Unexpected token", token);
    return null;
  }

  private ASTNode parseDefinitionCall(Token prev, Iterator<Token> tokenIterator) throws ParseException {
    String[] expectedIdentifiers = { "RBR", "NEW_LINE" };
    iterateAndVerify(tokenIterator, expectedIdentifiers);

    ASTNode call = new ASTNode(new Token(prev.getValue(), "CALL", prev.getRow(), prev.getColumn()));
    if (!astMap.containsKey(prev.getValue())) {
      throwAndPrint("Unexpected token", prev);
    } else {
      call.appendChild(astMap.get(prev.getValue()).getRoot());
    }

    return call;
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
