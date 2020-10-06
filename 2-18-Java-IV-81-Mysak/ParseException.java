package com.amaterasu.main;

public class ParseException extends Exception {
  private static final long serialVersionUID = 227786097791240244L;

  public ParseException(String message, int row, int column) {
    super(String.format("%s\n\tat row=%d column=%d", message, (row + 1), (column + 1)));
  }
}
