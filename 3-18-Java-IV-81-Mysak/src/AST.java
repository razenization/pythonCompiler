package com.amaterasu.main;

public class AST {
  private final ASTNode root;

  public AST(ASTNode root) {
    this.root = root;
  }

  public AST(Token token) {
    this.root = new ASTNode(token, null);
  }

  public void printAST() {
    System.out.println("\n====> SYNTAX TREE <====\n");
    printChildren(root, 0);
  }

  private void printChildren(ASTNode child, int depth) {
    StringBuilder prev = new StringBuilder();
    if (depth > 1) {
      for (int i = 0; i < depth - 1; i++) {
        prev.append("\t");
      }
      prev.append("=> ");
    } else {
      if (depth > 0) {
        prev = new StringBuilder("=> ");
      }
    }
    System.out
            .printf("%s%1s (%-1s)%n", prev.toString(), child.getCurrent().getValue(), child.getCurrent().getType());
    for (ASTNode node : child.getChildren()) {
      printChildren(node, depth + 1);
    }
  }

  public ASTNode getRoot() {
    return root;
  }
}
