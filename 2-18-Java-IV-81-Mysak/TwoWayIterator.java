package com.amaterasu.main;

import java.util.ArrayList;
import java.util.Iterator;

public class TwoWayIterator<E> implements Iterator<E> {
  private final ArrayList<E> iterable;
  private int currentIndex = -1;

  public TwoWayIterator(ArrayList<E> list) {
    this.iterable = list;
  }

  @Override
  public E next() {
    currentIndex++;
    return current();
  }

  public void prev() {
    currentIndex--;
    current();
  }

  @Override
  public boolean hasNext() {
    return currentIndex < iterable.size() - 1;
  }

  @Override
  public void remove() {
    iterable.remove(currentIndex);
  }

  public E current() {
    return iterable.get(currentIndex);
  }

  public E get(int i) {
    return iterable.get(i);
  }
}