package org.roaringbitmap.longlong;

import org.roaringbitmap.BitmapDataProvider;
import org.roaringbitmap.ParallelAggregation;
import org.roaringbitmap.RoaringBitmap;

import java.util.*;
import java.util.stream.Collectors;

/**
 *
 * These utility methods provide parallel implementations of
 * logical aggregation operators. AND is not implemented
 * because it is unlikely to be profitable.
 *
 * There is a temporary memory overhead in using these methods,
 * since a materialisation of the rotated containers grouped by key
 * is created in each case.
 *
 * Each method executes on the default fork join pool by default.
 * If this is undesirable (it usually is) wrap the call inside
 * a submission of a runnable to your own thread pool.
 *
 * <pre>
 * {@code
 *
 *       //...
 *
 *       ExecutorService executor = ...
 *       RoaringBitmap[] bitmaps = ...
 *       // executes on executors threads
 *       RoaringBitmap result = executor.submit(() -> ParallelAggregation.or(bitmaps)).get();
 * }
 * </pre>
 */
public class Parallel64Aggregation {

    public static Roaring64NavigableMap or(Roaring64NavigableMap... longBitmaps) {
        if (longBitmaps.length == 0) {
            return new Roaring64NavigableMap();
        }

        Map<Integer, List<BitmapDataProvider>> groupKeys = Arrays.stream(longBitmaps).flatMap(b -> b.getHighToBitmap().entrySet().stream())
                .map(e -> new AbstractMap.SimpleEntry<Integer, BitmapDataProvider>(e.getKey(), e.getValue()))
                .collect(Collectors.groupingBy(e -> e.getKey(), Collectors.collectingAndThen(Collectors.toList(), l -> l.stream().map(e -> e.getValue()).collect(Collectors.toList())) ));

        BitmapDataProvider firstBitmap = groupKeys.values().stream().flatMap(b -> b.stream()).findFirst().get();
        if (!(firstBitmap instanceof RoaringBitmap)) {
            // Fallback to non-concurrent as only RoaringBitmap are managed
            // Also, Roaring64NavigableMap expects all buckets to be of the same type
            return Fast64Aggregation.or(longBitmaps);
        }

        Map<Integer, RoaringBitmap> mergedHighToBitmaps = groupKeys
                .<Integer, List<BitmapDataProvider>>entrySet()
                .parallelStream().map(e -> {
                    RoaringBitmap[] bitmaps = e.getValue().stream().toArray(RoaringBitmap[]::new);
                    return new AbstractMap.SimpleEntry<>(e.getKey(), ParallelAggregation.or(bitmaps));
                }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        Roaring64NavigableMap merged = new Roaring64NavigableMap();

        mergedHighToBitmaps.forEach((high, bitmap) -> merged.add(high, bitmap));

        return merged;
    }
}
