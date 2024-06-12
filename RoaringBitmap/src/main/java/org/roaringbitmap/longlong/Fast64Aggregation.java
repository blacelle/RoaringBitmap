/*
 * (c) the authors Licensed under the Apache License, Version 2.0.
 */

package org.roaringbitmap.longlong;

import java.util.*;


/**
 * Fast algorithms to aggregate many 64b bitmaps.
 *
 * @author Benoit Lacelle
 */
public final class Fast64Aggregation {

  /**
   * Compute overall AND between bitmaps two-by-two.
   *
   * This function runs in linear time with respect to the number of bitmaps.
   *
   * @param bitmaps input bitmaps
   * @return aggregated bitmap
   */
  public static Roaring64NavigableMap naive_and(Iterator<? extends Roaring64NavigableMap> bitmaps) {
    if (!bitmaps.hasNext()) {
      return new Roaring64NavigableMap();
    }
    Roaring64NavigableMap answer = bitmaps.next().clone();
    while (bitmaps.hasNext() && !answer.isEmpty()) {
      answer.and(bitmaps.next());
    }
    return answer;
  }

  /**
   * Compute overall AND between bitmaps two-by-two.
   *
   * This function runs in linear time with respect to the number of bitmaps.
   *
   * @param bitmaps input bitmaps
   * @return aggregated bitmap
   */
  public static Roaring64NavigableMap naive_and(Roaring64NavigableMap... bitmaps) {
    if (bitmaps.length == 0) {
      return new Roaring64NavigableMap();
    }
    Roaring64NavigableMap answer = bitmaps[0].clone();
    for (int k = 1; k < bitmaps.length && !answer.isEmpty(); ++k) {
      answer.and(bitmaps[k]);
    }
    return answer;
  }

  /**
   * Compute overall OR between bitmaps two-by-two.
   *
   * This function runs in linear time with respect to the number of bitmaps.
   *
   * @param bitmaps input bitmaps
   * @return aggregated bitmap
   */
  public static Roaring64NavigableMap naive_or(Iterator<? extends Roaring64NavigableMap> bitmaps) {
    Roaring64NavigableMap answer = new Roaring64NavigableMap();
    while (bitmaps.hasNext()) {
      answer.naivelazyor(bitmaps.next());
    }
    answer.repairAfterLazy();
    return answer;
  }

  /**
   * Compute overall OR between bitmaps two-by-two.
   *
   * This function runs in linear time with respect to the number of bitmaps.
   *
   * @param bitmaps input bitmaps
   * @return aggregated bitmap
   */
  public static Roaring64NavigableMap naive_or(Roaring64NavigableMap... bitmaps) {
    Roaring64NavigableMap answer = new Roaring64NavigableMap();
    for (int k = 0; k < bitmaps.length; ++k) {
      answer.naivelazyor(bitmaps[k]);
    }
    answer.repairAfterLazy();
    return answer;
  }


  /**
   * Compute overall XOR between bitmaps two-by-two.
   *
   * This function runs in linear time with respect to the number of bitmaps.
   *
   * @param bitmaps input bitmaps
   * @return aggregated bitmap
   */
  public static Roaring64NavigableMap naive_xor(Iterator<? extends Roaring64NavigableMap> bitmaps) {
    Roaring64NavigableMap answer = new Roaring64NavigableMap();
    while (bitmaps.hasNext()) {
      answer.xor(bitmaps.next());
    }
    return answer;
  }


  /**
   * Compute overall XOR between bitmaps two-by-two.
   *
   * This function runs in linear time with respect to the number of bitmaps.
   *
   * @param bitmaps input bitmaps
   * @return aggregated bitmap
   */
  public static Roaring64NavigableMap naive_xor(Roaring64NavigableMap... bitmaps) {
    Roaring64NavigableMap answer = new Roaring64NavigableMap();
    for (int k = 0; k < bitmaps.length; ++k) {
      answer.xor(bitmaps[k]);
    }
    return answer;
  }

  /**
   * Compute overall OR between bitmaps.
   *
   *
   * @param bitmaps input bitmaps
   * @return aggregated bitmap
   */
  public static Roaring64NavigableMap or(Iterator<? extends Roaring64NavigableMap> bitmaps) {
    return naive_or(bitmaps);
  }

  /**
   * Compute overall OR between bitmaps.
   *
   *
   * @param bitmaps input bitmaps
   * @return aggregated bitmap
   */
  public static Roaring64NavigableMap or(Roaring64NavigableMap... bitmaps) {
    return naive_or(bitmaps);
  }

  /**
   * Compute overall XOR between bitmaps.
   *
   *
   * @param bitmaps input bitmaps
   * @return aggregated bitmap
   */
  public static Roaring64NavigableMap xor(Iterator<? extends Roaring64NavigableMap> bitmaps) {
    return naive_xor(bitmaps);
  }

  /**
   * Compute overall XOR between bitmaps.
   *
   *
   * @param bitmaps input bitmaps
   * @return aggregated bitmap
   */
  public static Roaring64NavigableMap xor(Roaring64NavigableMap... bitmaps) {
    return naive_xor(bitmaps);
  }

  /**
   * Private constructor to prevent instantiation of utility class
   */
  private Fast64Aggregation() {

  }

}
