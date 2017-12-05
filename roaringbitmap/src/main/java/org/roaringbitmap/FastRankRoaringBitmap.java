package org.roaringbitmap;

import java.util.Arrays;

/**
 * This extends {@link RoaringBitmap} to provide better performance for .rank and .select
 * operations, at the cost of maintain a cache of cardinalities.
 * 
 * On {@link RoaringBitmap#select(int)} and {@link RoaringBitmap#rank(int)} operations,
 * {@link RoaringBitmap} needs to iterate along all underlying buckets to cumulate their
 * cardinalities. This may lead to sub-optimal performance for application doing a large amount of
 * .rank/.select over read-only {@link RoaringBitmap}, especially if the {@link RoaringBitmap} holds
 * a large number of underlying buckets.
 * 
 * This implementation will discard the cache of cardinality on any write operations, and it will
 * memoize the computed cardinalities on any .rank or .select operation
 * 
 * @author Benoit Lacelle
 *
 */
public class FastRankRoaringBitmap extends RoaringBitmap {
  // The cache of cardinalities: it maps the index of the underlying bucket to the cumulated
  // cardinalities (i.e. the sum of current bucket cardinalities plus all previous bucklet
  // cardinalities)
  private int[] highToCumulatedCardinality = null;

  private void resetCache() {
    // Reset the cache on any write operation
    // highToCumulatedCardinality = null;
  }

  @Override
  public void add(long rangeStart, long rangeEnd) {
    resetCache();
    super.add(rangeStart, rangeEnd);
  }

  @Override
  public void add(int x) {
    resetCache();
    super.add(x);
  }

  @Override
  public void add(int... dat) {
    resetCache();
    super.add(dat);
  }

  @Deprecated
  @Override
  public void add(int rangeStart, int rangeEnd) {
    resetCache();
    super.add(rangeStart, rangeEnd);
  }

  @Override
  public void clear() {
    resetCache();
    super.clear();
  }

  @Override
  public void flip(int x) {
    resetCache();
    super.flip(x);
  }

  @Deprecated
  @Override
  public void flip(int rangeStart, int rangeEnd) {
    resetCache();
    super.flip(rangeStart, rangeEnd);
  }

  @Override
  public void flip(long rangeStart, long rangeEnd) {
    resetCache();
    super.flip(rangeStart, rangeEnd);
  }

  @Override
  public void and(RoaringBitmap x2) {
    resetCache();
    super.and(x2);
  }

  @Override
  public void andNot(RoaringBitmap x2) {
    resetCache();
    super.andNot(x2);
  }

  @Deprecated
  @Override
  public void remove(int rangeStart, int rangeEnd) {
    resetCache();
    super.remove(rangeStart, rangeEnd);
  }

  @Override
  public void remove(int x) {
    resetCache();
    super.remove(x);
  }

  @Override
  public void remove(long rangeStart, long rangeEnd) {
    resetCache();
    super.remove(rangeStart, rangeEnd);
  }

  @Override
  public boolean checkedAdd(int x) {
    resetCache();
    return super.checkedAdd(x);
  }

  @Override
  public boolean checkedRemove(int x) {
    resetCache();
    return super.checkedRemove(x);
  }

  @Override
  public void or(RoaringBitmap x2) {
    resetCache();
    super.or(x2);
  }

  @Override
  public void xor(RoaringBitmap x2) {
    resetCache();
    super.xor(x2);
  }


  @Override
  public long rankLong(int x) {
    preComputeCardinalities();

    if (highToCumulatedCardinality.length == 0) {
      return 0L;
    }

    short xhigh = Util.highbits(x);

    int index = Util.hybridUnsignedBinarySearch(this.highLowContainer.keys, 0,
        this.highLowContainer.size(), xhigh);

    boolean hasBitmapOnIdex;
    if (index < 0) {
      hasBitmapOnIdex = false;
      index = -1 - index;
    } else {
      hasBitmapOnIdex = true;
    }

    long size = 0;
    if (index > 0) {
      size += highToCumulatedCardinality[index - 1];
    }

    long rank = size;
    if (hasBitmapOnIdex) {
      rank = size + this.highLowContainer.getContainerAtIndex(index).rank(Util.lowbits(x));
    }

    // TODO Should we keep the assertion?
    assert rank == super.rankLong(x);

    return rank;
  }

  /**
   * On any .rank or .select operation, we pre-compute all cumulated cardinalities. It will enable
   * using a binary-search to spot the relevant underlying bucket
   */
  private void preComputeCardinalities() {
    if (highToCumulatedCardinality == null) {
      highToCumulatedCardinality = new int[highLowContainer.size()];

      if (highToCumulatedCardinality.length == 0) {
        return;
      }
      highToCumulatedCardinality[0] = highLowContainer.getContainerAtIndex(0).getCardinality();

      for (int i = 1; i < highToCumulatedCardinality.length; i++) {
        highToCumulatedCardinality[i] = highToCumulatedCardinality[i - 1]
            + highLowContainer.getContainerAtIndex(i).getCardinality();
      }
    }
  }

  @Override
  public int select(int j) {
    preComputeCardinalities();

    if (highToCumulatedCardinality.length == 0) {
      // empty: .select is out-of-bounds

      throw new IllegalArgumentException(
          "select " + j + " when the cardinality is " + this.getCardinality());
    }

    int index = Arrays.binarySearch(highToCumulatedCardinality, j);

    int fixedIndex;

    long leftover = Util.toUnsignedLong(j);

    if (index == highToCumulatedCardinality.length - 1) {
      return this.last();
    } else if (index >= 0) {
      int keycontrib = this.highLowContainer.getKeyAtIndex(index + 1) << 16;

      // If first bucket has cardinality 1 and we select 1: we actual select the first item of
      // second bucket
      int output = keycontrib + this.highLowContainer.getContainerAtIndex(index + 1).first();

      // TODO Should we keep the assertion?
      assert output == super.select(j);

      return output;
    } else {
      fixedIndex = -1 - index;
      if (fixedIndex > 0) {
        leftover -= highToCumulatedCardinality[fixedIndex - 1];
      }
    }

    int keycontrib = this.highLowContainer.getKeyAtIndex(fixedIndex) << 16;
    int lowcontrib = Util.toIntUnsigned(
        this.highLowContainer.getContainerAtIndex(fixedIndex).select((int) leftover));
    int value = lowcontrib + keycontrib;

    // TODO Should we keep the assertion?
    assert value == super.select(j);

    return value;

    // throw new IllegalArgumentException("select " + j + " when the cardinality is " +
    // this.getCardinality());
  }
}
