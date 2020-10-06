package com.amaterasu.main;

import java.util.ArrayList;

public class ASTNode {
  private final Token current;
  private ASTNode parent;
  private final ArrayList<ASTNode> children;

  public ASTNode(Token token) {
    this.current = token;
    this.parent = null;
    this.children = new ArrayList<>();
  }

  public ASTNode(Token token, ASTNode parent) {
    this.current = token;
    this.parent = parent;
    this.children = new ArrayList<>();
  }

  public ASTNode(Token token, ASTNode parent, ArrayList<ASTNode> children) {
    this.current = token;
    this.parent = parent;
    this.children = children;
  }

  public ASTNode getParent() {
    return parent;
  }

  public Token getCurrent() {
    return current;
  }

  public ArrayList<ASTNode> getChildren() {
    return children;
  }

  public ASTNode getChild(int id) {
    return children.get(id);
  }

  public void setParent(ASTNode parent) {
    this.parent = parent;
  }

  public void insertChild(int position, ASTNode child) {
    children.add(position, child);
  }

  public void appendChild(ASTNode child) {
    children.add(child);
  }
}
