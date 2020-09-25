package com.amaterasu.main;

import org.json.*;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;

public class LexingChecker {
  private String text;

  private Map<String, String> keywords;
  private Map<String, String> symbols;
  private Map<String, String> whitespaces;

  private final ArrayList<Token> tokens = new ArrayList<>();

  public LexingChecker(String filePath) {
    keywords = new HashMap<>();
    symbols = new HashMap<>();
    whitespaces = new HashMap<>();
    populateData("python.json");

    try {
      text = readFile(filePath);
    } catch (IOException e) {
      System.out.println(e.getMessage());
    }

    generateTokens();
  }

  private String readFile(String filePath) throws IOException {
    if (!new File(filePath).exists()) {
      throw new IOException("File does not exist");
    }
    StringBuilder returnText = new StringBuilder();
    try (FileReader reader = new FileReader(filePath)) {
      int charCode;
      while ((charCode = reader.read()) != -1) {
        returnText.append((char) charCode);
      }
    } catch (IOException e) {
      System.out.println(e.getMessage());
    }
    return returnText.toString();
  }

  private void populateData(String pathToTokens) {
    try {
      String fileContents = readFile(pathToTokens);
      JSONObject pythonTokensJSON = new JSONObject(fileContents);
      keywords = new ObjectMapper().readValue(pythonTokensJSON.get("keywords").toString(), HashMap.class);
      symbols = new ObjectMapper().readValue(pythonTokensJSON.get("symbols").toString(), HashMap.class);
      whitespaces = new ObjectMapper().readValue(pythonTokensJSON.get("whitespaces").toString(), HashMap.class);
    } catch (IOException e ) {
      System.out.println(e.getMessage());
    }
  }

  private void generateTokens() {
    if (text.startsWith("null")) {
      text = text.substring(4);
    }
    String[] parseLines = text.split("\n");

    for (int i = 0; i < parseLines.length; i++) {
      if (parseLine(parseLines[i], i)) {
        tokens.add(new Token("\n", whitespaces.get("\n"), i, parseLines.length));
      }
    }
  }

  private boolean parseLine(String line, int row) {
    String cleanLine = line.split("#")[0];
    if (cleanLine.matches("^\\s*$")) {
      return false;
    }

    String[] splitCodeLine = cleanLine.split("");
    boolean tabSpace = true;

    for (int i = 0; i < splitCodeLine.length; i++) {
      if (i + 1 != splitCodeLine.length && symbols.containsKey(splitCodeLine[i] + splitCodeLine[i + 1])) {
        tokens.add(new Token(splitCodeLine[i] + splitCodeLine[i + 1], symbols.get(splitCodeLine[i] + splitCodeLine[i + 1]), row, i));
        i++;
      } else {
        if (symbols.containsKey(splitCodeLine[i])) {
          if (i + 1 != splitCodeLine.length && (splitCodeLine[i].matches("[\"']"))) {
            StringBuilder num = new StringBuilder(splitCodeLine[i]);
            short j = 1;
            while (i + j < splitCodeLine.length) {
              num.append(splitCodeLine[i + j]);
              j++;
              if (splitCodeLine[i + j - 1].equals(splitCodeLine[i])) {
                break;
              }
            }

            tokens.add(new Token(num.toString(), "STRING", row, i));
            i += num.length() - 1;
          } else {
            tokens.add(new Token(splitCodeLine[i], symbols.get(splitCodeLine[i]), row, i));
          }
        } else {
          if (whitespaces.containsKey(splitCodeLine[i])) {
            if (tabSpace) {
              tokens.add(new Token(splitCodeLine[i], whitespaces.get(splitCodeLine[i]), row, i));
            }
          } else {
            if (i + 1 != splitCodeLine.length && splitCodeLine[i].equals("0") && splitCodeLine[i + 1].matches("[xob]")) {
              StringBuilder nonDecimalNum = new StringBuilder("0" + splitCodeLine[i + 1]);
              short j = 2;
              while ((i + j < splitCodeLine.length) && isValidNum(splitCodeLine[i + j], splitCodeLine[i + 1]) && j < 8) {
                nonDecimalNum.append(splitCodeLine[i + j]);
                j++;
              }

              String numType = "HEX";
              switch (splitCodeLine[i + 1]) {
                case "o":
                  numType = "OCT";
                  break;
                case "b":
                  numType = "BIN";
                  break;
                default:
                  break;
              }
              tokens.add(new Token(nonDecimalNum.toString(), numType, row, i));

              i += nonDecimalNum.length() - 1;
            } else {
              if (splitCodeLine[i].matches("\\d")) {
                StringBuilder stringifiedNumber = new StringBuilder();
                boolean isFloat = false;
                short j = 0;
                while (i + j < splitCodeLine.length && splitCodeLine[i + j].matches("[\\d.]")) {
                  if (splitCodeLine[i + j].equals(".")) {
                    isFloat = true;
                  }
                  stringifiedNumber.append(splitCodeLine[i + j]);
                  j++;
                }

                if (isFloat) {
                  tokens.add(new Token(stringifiedNumber.toString(), "FLOAT", row, i));
                } else {
                  tokens.add(new Token(stringifiedNumber.toString(), "INT", row, i));
                }
                i += stringifiedNumber.length() - 1;
              } else {
                if (splitCodeLine[i].matches("[a-zA-Z]")) {
                  StringBuilder stringifiedNumber = new StringBuilder();
                  short j = 0;
                  while (i + j < splitCodeLine.length && splitCodeLine[i + j].matches("\\w")) {
                    stringifiedNumber.append(splitCodeLine[i + j]);
                    j++;
                  }

                  tokens.add(new Token(stringifiedNumber.toString(), keywords.getOrDefault(stringifiedNumber.toString(), "IDENTIFIER"), row, i));
                  tabSpace = false;
                  i += stringifiedNumber.length() - 1;
                } else {
                  tokens.add(new Token(splitCodeLine[i], "UNDEF", row, i));
                  try {
                    throw new ParseException("Undefined symbol", row, i);
                  } catch (ParseException e) {
                    System.err.println(e.getMessage());
                    System.err.println((int) splitCodeLine[i].toCharArray()[0]);
                  }
                }
              }
            }
          }
        }
      }
    }
    return true;
  }

  private boolean isValidNum(String s, String system) {
    switch (system) {
      case "x":
        return s.matches("[\\da-fA-F]");
      case "o":
        return s.matches("[0-7]");
      case "b":
        return s.equals("1") || s.equals("0");
      default:
        return false;
    }
  }
  
  public ArrayList<Token> getTokens() {
    return tokens;
  }

  public void listTokens() {
    for (Token token : tokens) {
      String tokenValue = token.getValue();
      switch (tokenValue) {
        case "\n":
          tokenValue = "\\n";
          break;
        case "\t":
          tokenValue = "\\t";
          break;
        case " ":
          tokenValue = "\\s";
          break;
        default:
          break;
      }
      System.out.printf("%s => %s%n", tokenValue, token.getType());
    }
  }
}
