package br.com.atous.demo.infrastructure.datastructure;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SegmentTreeAmplitudeTest {

    private SegmentTreeAmplitude segmentTreeAmplitude;

    @BeforeEach
    void setUp() {
        segmentTreeAmplitude = new SegmentTreeAmplitude();
    }

    @Test
    void testInitialize() {
        int size = 4;
        segmentTreeAmplitude.initialize(size);

        double expectedInitialAmplitude = 1.0 / Math.sqrt(size);
        double[] amplitudes = segmentTreeAmplitude.getAllAmplitudes();

        assertEquals(size, amplitudes.length);
        for (int i = 0; i < size; i++) {
            assertEquals(expectedInitialAmplitude, amplitudes[i], 1e-9);
        }
        // Verify total probability is approximately 1.0 after initialization
        assertEquals(1.0, segmentTreeAmplitude.getTotalProbability(), 1e-9);
    }

    @Test
    void testInitializeWithInvalidSize() {
        assertThrows(IllegalArgumentException.class, () -> segmentTreeAmplitude.initialize(0));
        assertThrows(IllegalArgumentException.class, () -> segmentTreeAmplitude.initialize(-1));
    }

    @Test
    void testApplyOracle() {
        int size = 4;
        int targetIndex = 1;
        segmentTreeAmplitude.initialize(size);

        double initialAmplitude = segmentTreeAmplitude.getAmplitude(targetIndex);
        segmentTreeAmplitude.applyOracle(targetIndex);

        double[] amplitudes = segmentTreeAmplitude.getAllAmplitudes();
        assertEquals(-initialAmplitude, amplitudes[targetIndex], 1e-9);
        // Other amplitudes should remain unchanged
        assertEquals(initialAmplitude, amplitudes[0], 1e-9);
        assertEquals(initialAmplitude, amplitudes[2], 1e-9);
        assertEquals(initialAmplitude, amplitudes[3], 1e-9);
        
        // Total probability should still be approximately 1.0
        assertEquals(1.0, segmentTreeAmplitude.getTotalProbability(), 1e-9);
    }

    @Test
    void testApplyOracleWithInvalidIndex() {
        int size = 4;
        segmentTreeAmplitude.initialize(size);
        assertThrows(IllegalArgumentException.class, () -> segmentTreeAmplitude.applyOracle(-1));
        assertThrows(IllegalArgumentException.class, () -> segmentTreeAmplitude.applyOracle(size));
    }

    @Test
    void testApplyDiffusion() {
        int size = 4;
        segmentTreeAmplitude.initialize(size);
        int targetIndex = 0;
        segmentTreeAmplitude.applyOracle(targetIndex); // Invert phase of target

        double initialAmplitude = 1.0 / Math.sqrt(size);
        double sumBeforeDiffusion = ( (size - 1) * initialAmplitude ) + (-initialAmplitude);
        double expectedMean = sumBeforeDiffusion / size;
        
        segmentTreeAmplitude.applyDiffusion();

        double[] amplitudes = segmentTreeAmplitude.getAllAmplitudes();
        
        // Verify diffusion formula: v_new = 2*mean - v_old
        // For target index:
        assertEquals(2 * expectedMean - (-initialAmplitude), amplitudes[targetIndex], 1e-9);
        // For non-target indices:
        assertEquals(2 * expectedMean - initialAmplitude, amplitudes[1], 1e-9);
        assertEquals(2 * expectedMean - initialAmplitude, amplitudes[2], 1e-9);
        assertEquals(2 * expectedMean - initialAmplitude, amplitudes[3], 1e-9);

        // Total probability should still be approximately 1.0
        assertEquals(1.0, segmentTreeAmplitude.getTotalProbability(), 1e-9);
    }

    @Test
    void testFindMaxAmplitudeIndex() {
        int size = 4;
        segmentTreeAmplitude.initialize(size);
        
        // Manually set amplitudes to have a clear max
        // This is done by applying oracle and diffusion to create a specific state
        // For N=4, target 1, after 1 iteration, amplitude at index 1 should be highest
        int targetIndex = 1;
        segmentTreeAmplitude.applyOracle(targetIndex);
        segmentTreeAmplitude.applyDiffusion();

        assertEquals(targetIndex, segmentTreeAmplitude.findMaxAmplitudeIndex());

        // Test with negative amplitudes (should still find max based on |amplitude|^2)
        // For N=4, target 0, after 1 iteration, amplitude at index 0 should be highest
        segmentTreeAmplitude.initialize(size); // Reset
        targetIndex = 0;
        segmentTreeAmplitude.applyOracle(targetIndex);
        segmentTreeAmplitude.applyDiffusion();
        assertEquals(targetIndex, segmentTreeAmplitude.findMaxAmplitudeIndex());
    }

    @Test
    void testFindMaxAmplitudeIndexNotInitialized() {
        assertThrows(IllegalStateException.class, () -> segmentTreeAmplitude.findMaxAmplitudeIndex());
    }

    @Test
    void testGroverLikeIterationSmallSize() {
        int size = 4;
        int targetIndex = 2;
        segmentTreeAmplitude.initialize(size);

        // Simulate one Grover iteration
        segmentTreeAmplitude.applyOracle(targetIndex);
        segmentTreeAmplitude.applyDiffusion();

        // After one iteration, the target amplitude should be amplified
        // and the others suppressed. The exact values are complex, but we expect
        // the target index to have the highest amplitude (or probability).
        assertEquals(targetIndex, segmentTreeAmplitude.findMaxAmplitudeIndex());
        
        // Simulate a second iteration (for N=4, 1 iteration is optimal, but testing behavior)
        segmentTreeAmplitude.applyDiffusion();
        // No assertion here, as for N=4, 2 iterations over-rotates and target might not be max
    }

    @Test
    void testRangeAffineUpdateAndQuerySum() {
        int size = 8;
        segmentTreeAmplitude.initialize(size); // All amplitudes 1/sqrt(8)

        // Apply f(x) = 2x + 1 to range [0, 3]
        // Original sum for [0,3] = 4 * (1/sqrt(8))
        // New sum should be sum(2*x_i + 1) = 2*sum(x_i) + sum(1) = 2*sum(x_i) + 4
        double initialAmplitude = 1.0 / Math.sqrt(size);
        double originalSumRange = 4 * initialAmplitude;
        
        // Access private method for testing
        try {
            java.lang.reflect.Method rangeAffineUpdateMethod = SegmentTreeAmplitude.class.getDeclaredMethod("rangeAffineUpdate", int.class, int.class, int.class, int.class, int.class, double.class, double.class);
            rangeAffineUpdateMethod.setAccessible(true);
            rangeAffineUpdateMethod.invoke(segmentTreeAmplitude, 1, 0, size - 1, 0, 3, 2.0, 1.0);

            java.lang.reflect.Method querySumMethod = SegmentTreeAmplitude.class.getDeclaredMethod("querySum", int.class, int.class, int.class, int.class, int.class);
            querySumMethod.setAccessible(true);
            double newSumRange = (double) querySumMethod.invoke(segmentTreeAmplitude, 1, 0, size - 1, 0, 3);

            assertEquals(2 * originalSumRange + 4, newSumRange, 1e-9);

            // Check a point within the updated range
            double expectedAmplitude0 = 2 * initialAmplitude + 1;
            assertEquals(expectedAmplitude0, segmentTreeAmplitude.getAmplitude(0), 1e-9);

            // Check a point outside the updated range
            assertEquals(initialAmplitude, segmentTreeAmplitude.getAmplitude(4), 1e-9);

            // Apply another update to a different range
            // Apply f(x) = -1x + 0.5 to range [4, 7]
            double originalSumRange2 = 4 * initialAmplitude;
            rangeAffineUpdateMethod.invoke(segmentTreeAmplitude, 1, 0, size - 1, 4, 7, -1.0, 0.5);
            double newSumRange2 = (double) querySumMethod.invoke(segmentTreeAmplitude, 1, 0, size - 1, 4, 7);
            assertEquals(-originalSumRange2 + 4 * 0.5, newSumRange2, 1e-9);

            // Check a point within the second updated range
            double expectedAmplitude4 = -initialAmplitude + 0.5;
            assertEquals(expectedAmplitude4, segmentTreeAmplitude.getAmplitude(4), 1e-9);

        } catch (Exception e) {
            fail("Reflection failed: " + e.getMessage());
        }
    }

    @Test
    void testLazyPropagationComposition() {
        int size = 4;
        segmentTreeAmplitude.initialize(size); // All amplitudes 1/sqrt(4) = 0.5

        // Apply f(x) = 2x + 1 to range [0, 3]
        // Then apply g(x) = 3x + 2 to range [0, 1]
        // For elements in [0,1], the transformation should be g(f(x)) = 3(2x+1) + 2 = 6x + 3 + 2 = 6x + 5

        double initialAmplitude = 1.0 / Math.sqrt(size); // 0.5

        try {
            java.lang.reflect.Method rangeAffineUpdateMethod = SegmentTreeAmplitude.class.getDeclaredMethod("rangeAffineUpdate", int.class, int.class, int.class, int.class, int.class, double.class, double.class);
            rangeAffineUpdateMethod.setAccessible(true);

            // First update: f(x) = 2x + 1 on [0, 3]
            rangeAffineUpdateMethod.invoke(segmentTreeAmplitude, 1, 0, size - 1, 0, 3, 2.0, 1.0);

            // Second update: g(x) = 3x + 2 on [0, 1]
            rangeAffineUpdateMethod.invoke(segmentTreeAmplitude, 1, 0, size - 1, 0, 1, 3.0, 2.0);

            // Expected amplitude for index 0: 6 * initialAmplitude + 5
            double expectedAmplitude0 = 6 * initialAmplitude + 5;
            assertEquals(expectedAmplitude0, segmentTreeAmplitude.getAmplitude(0), 1e-9);

            // Expected amplitude for index 2 (only f(x) applied): 2 * initialAmplitude + 1
            double expectedAmplitude2 = 2 * initialAmplitude + 1;
            assertEquals(expectedAmplitude2, segmentTreeAmplitude.getAmplitude(2), 1e-9);

        } catch (Exception e) {
            fail("Reflection failed: " + e.getMessage());
        }
    }
}