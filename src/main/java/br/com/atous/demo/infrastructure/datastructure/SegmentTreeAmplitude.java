package br.com.atous.demo.infrastructure.datastructure;

import br.com.atous.demo.domain.port.out.AmplitudeDataStructure;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Primary;

/**
 * Segment Tree implementation for amplitude management in Grover's algorithm.
 * Supports range affine updates (f(x) = ax + b) and range sum queries in O(log N).
 * This is crucial for achieving the O(√N log N) complexity for the classical Grover search.
 */
@Component
@Scope("prototype")
@Primary
public class SegmentTreeAmplitude implements AmplitudeDataStructure {

    private static final double INITIAL_AMPLITUDE_VALUE = 1.0; // Placeholder, will be adjusted by initialize

    private Node[] tree;
    private double[] initialAmplitudes; // To store the initial state for building the tree
    private int size;

    private static class Node {
        double sum;
        double lazyMul; // Multiplier for lazy propagation
        double lazyAdd; // Additive for lazy propagation

        Node() {
            this.sum = 0.0;
            this.lazyMul = 1.0; // Default: no multiplication
            this.lazyAdd = 0.0; // Default: no addition
        }
    }

    @Override
    public void initialize(int size) {
        validateSize(size);
        this.size = size;
        this.tree = new Node[4 * size]; // Max size for segment tree
        this.initialAmplitudes = new double[size];

        double initialValue = INITIAL_AMPLITUDE_VALUE / Math.sqrt(size);
        for (int i = 0; i < size; i++) {
            initialAmplitudes[i] = initialValue;
        }
        build(1, 0, size - 1);
    }

    // Builds the segment tree
    private void build(int nodeIdx, int start, int end) {
        tree[nodeIdx] = new Node();
        if (start == end) {
            tree[nodeIdx].sum = initialAmplitudes[start];
        } else {
            int mid = (start + end) / 2;
            build(2 * nodeIdx, start, mid);
            build(2 * nodeIdx + 1, mid + 1, end);
            tree[nodeIdx].sum = tree[2 * nodeIdx].sum + tree[2 * nodeIdx + 1].sum;
        }
    }

    // Pushes lazy tags down to children
    private void push(int nodeIdx, int start, int end) {
        if (tree[nodeIdx].lazyMul != 1.0 || tree[nodeIdx].lazyAdd != 0.0) {
            if (start != end) { // Not a leaf node
                int mid = (start + end) / 2;
                // Apply current node's lazy tags to left child
                apply(2 * nodeIdx, start, mid, tree[nodeIdx].lazyMul, tree[nodeIdx].lazyAdd);
                // Apply current node's lazy tags to right child
                apply(2 * nodeIdx + 1, mid + 1, end, tree[nodeIdx].lazyMul, tree[nodeIdx].lazyAdd);
            }
            // Reset lazy tags at current node
            tree[nodeIdx].lazyMul = 1.0;
            tree[nodeIdx].lazyAdd = 0.0;
        }
    }

    // Applies affine transformation (mul * x + add) to a node's sum
    // Applies affine transformation (mul * x + add) to a node's sum
    private void apply(int nodeIdx, int start, int end, double mul, double add) {
        // Update sum
        tree[nodeIdx].sum = mul * tree[nodeIdx].sum + add * (end - start + 1);

        // Compose and update lazy tags for the current node
        tree[nodeIdx].lazyMul *= mul;
        tree[nodeIdx].lazyAdd = mul * tree[nodeIdx].lazyAdd + add;
    }

    @Override
    public void applyOracle(int targetIndex) {
        validateTargetIndex(targetIndex);
        // Oracle inverts the phase of the target element.
        // This is equivalent to applying f(x) = -x to the target element.
        // We can achieve this with a range affine update on a single element.
        rangeAffineUpdate(1, 0, size - 1, targetIndex, targetIndex, -1.0, 0.0);
    }

    @Override
    public void applyDiffusion() {
        // 1. Calculate the mean of amplitudes using the Segment Tree (O(log N))
        double totalSum = querySum(1, 0, size - 1, 0, size - 1);
        double mean = totalSum / size;

        // 2. Apply the diffusion transformation: new_amplitude = 2 * mean - old_amplitude
        // This is an affine transformation: f(x) = -1 * x + 2 * mean
        // Apply this transformation to the entire range [0, size-1] in O(log N)
        rangeAffineUpdate(1, 0, size - 1, 0, size - 1, -1.0, 2.0 * mean);
    }

    // Performs a range affine update (mul * x + add) on the segment tree
    private void rangeAffineUpdate(int nodeIdx, int start, int end, int queryStart, int queryEnd, double mul, double add) {
        push(nodeIdx, start, end); // Push down lazy tags before processing

        // No overlap
        if (start > end || start > queryEnd || end < queryStart) {
            return;
        }

        // Full overlap
        if (queryStart <= start && end <= queryEnd) {
            apply(nodeIdx, start, end, mul, add);
            return;
        }

        // Partial overlap, recurse
        int mid = (start + end) / 2;
        rangeAffineUpdate(2 * nodeIdx, start, mid, queryStart, queryEnd, mul, add);
        rangeAffineUpdate(2 * nodeIdx + 1, mid + 1, end, queryStart, queryEnd, mul, add);

        // Update current node's sum based on children
        tree[nodeIdx].sum = tree[2 * nodeIdx].sum + tree[2 * nodeIdx + 1].sum;
    }

    // Queries the sum of amplitudes in a given range
    double querySum(int nodeIdx, int start, int end, int queryStart, int queryEnd) {
        push(nodeIdx, start, end); // Push down lazy tags before querying

        // No overlap
        if (start > end || start > queryEnd || end < queryStart) {
            return 0.0;
        }

        // Full overlap
        if (queryStart <= start && end <= queryEnd) {
            return tree[nodeIdx].sum;
        }

        // Partial overlap, recurse
        int mid = (start + end) / 2;
        double p1 = querySum(2 * nodeIdx, start, mid, queryStart, queryEnd);
        double p2 = querySum(2 * nodeIdx + 1, mid + 1, end, queryStart, queryEnd);
        return p1 + p2;
    }

    @Override
    public int findMaxAmplitudeIndex() {
        // For finding the max amplitude index, we need to query individual amplitudes.
        // A segment tree can do point queries (querySum for a range of 1 element) in O(log N).
        // However, finding the maximum over N elements would still be O(N log N) if done by N point queries.
        // For now, we'll use a direct O(N) scan after ensuring all lazy updates are pushed down.
        // In a real scenario, if this is a bottleneck, a different approach for max query might be needed
        // (e.g., storing max in nodes and updating it, but that complicates affine transformations).

        // Ensure all lazy updates are propagated to leaf nodes before reading actual values
        // This is a temporary measure for correctness. A more efficient way to get actual values
        // at leaves might be needed if this becomes a performance bottleneck.
        double[] actualAmplitudes = new double[size];
        for (int i = 0; i < size; i++) {
            actualAmplitudes[i] = querySum(1, 0, size - 1, i, i);
        }

        if (actualAmplitudes.length == 0) {
            throw new IllegalStateException("Amplitudes not initialized or empty.");
        }

        int maxIndex = 0;
        double maxProbability = actualAmplitudes[0] * actualAmplitudes[0];

        for (int i = 1; i < size; i++) {
            double probability = actualAmplitudes[i] * actualAmplitudes[i];
            if (probability > maxProbability) {
                maxProbability = probability;
                maxIndex = i;
            }
        }
        return maxIndex;
    }

    // --- Validation Methods ---

    private void validateSize(int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("Size must be positive, got: " + size);
        }
        // Consider adding a max size limit if memory becomes an issue for very large N
    }

    private void validateTargetIndex(int targetIndex) {
        if (targetIndex < 0 || targetIndex >= size) {
            throw new IllegalArgumentException(
                String.format("Target index %d is out of bounds [0, %d)", targetIndex, size)
            );
        }
    }

    // --- Auxiliary Methods (for debugging/testing) ---
    // These methods are for testing and debugging purposes to inspect the internal state.
    // They might not be efficient for production use.
    public double getAmplitude(int index) {
        return querySum(1, 0, size - 1, index, index);
    }

    public double[] getAllAmplitudes() {
        double[] amplitudes = new double[size];
        for (int i = 0; i < size; i++) {
            amplitudes[i] = querySum(1, 0, size - 1, i, i);
        }
        return amplitudes;
    }

    public double getTotalProbability() {
        double total = 0.0;
        double[] amplitudes = getAllAmplitudes();
        for (double amplitude : amplitudes) {
            total += amplitude * amplitude;
        }
        return total;
    }
}
