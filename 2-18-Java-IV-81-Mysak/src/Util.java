package com.amaterasu.main;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class Util {
  public static String readFile(String filePath) throws IOException {
    if (!new File(filePath).exists()) {
      System.out.println(filePath);
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
}
