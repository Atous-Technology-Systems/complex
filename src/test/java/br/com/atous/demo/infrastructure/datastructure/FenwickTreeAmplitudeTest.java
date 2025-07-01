package br.com.atous.demo.infrastructure.datastructure;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FenwickTreeAmplitudeTest {

    private FenwickTreeAmplitude fenwickTreeAmplitude;

    @BeforeEach
    void setUp() {
        fenwickTreeAmplitude = new FenwickTreeAmplitude();
    }

    @Test
    void testInitialize() {
        int size = 4;
        fenwickTreeAmplitude.initialize(size);

        double expectedInitialAmplitude = 1.0 / Math.sqrt(size);
        double[] amplitudes = fenwickTreeAmplitude.getAmplitudes();

        assertEquals(size, amplitudes.length);
        for (int i = 0; i < size; i++) {
            assertEquals(expectedInitialAmplitude, amplitudes[i], 1e-9);
        }
        // Verify total probability is approximately 1.0 after initialization
        assertEquals(1.0, fenwickTreeAmplitude.getTotalProbability(), 1e-9);
    }

    @Test
    void testInitializeWithInvalidSize() {
        assertThrows(IllegalArgumentException.class, () -> fenwickTreeAmplitude.initialize(0));
        assertThrows(IllegalArgumentException.class, () -> fenwickTreeAmplitude.initialize(-1));
    }

    @Test
    void testApplyOracle() {
        int size = 4;
        int targetIndex = 1;
        fenwickTreeAmplitude.initialize(size);

        double initialAmplitude = fenwickTreeAmplitude.getAmplitudes()[targetIndex];
        fenwickTreeAmplitude.applyOracle(targetIndex);

        double[] amplitudes = fenwickTreeAmplitude.getAmplitudes();
        assertEquals(-initialAmplitude, amplitudes[targetIndex], 1e-9);
        // Other amplitudes should remain unchanged
        assertEquals(initialAmplitude, amplitudes[0], 1e-9);
        assertEquals(initialAmplitude, amplitudes[2], 1e-9);
        assertEquals(initialAmplitude, amplitudes[3], 1e-9);
        
        // Total probability should still be approximately 1.0
        assertEquals(1.0, fenwickTreeAmplitude.getTotalProbability(), 1e-9);
    }

    @Test
    void testApplyOracleWithInvalidIndex() {
        int size = 4;
        fenwickTreeAmplitude.initialize(size);
        assertThrows(IllegalArgumentException.class, () -> fenwickTreeAmplitude.applyOracle(-1));
        assertThrows(IllegalArgumentException.class, () -> fenwickTreeAmplitude.applyOracle(size));
    }

    @Test
    void testApplyDiffusion() {
        int size = 4;
        fenwickTreeAmplitude.initialize(size);
        int targetIndex = 0;
        fenwickTreeAmplitude.applyOracle(targetIndex); // Invert phase of target

        double initialAmplitude = 1.0 / Math.sqrt(size);
        double expectedMean = ( (size - 1) * initialAmplitude - initialAmplitude ) / size; // Sum of amplitudes / size
        
        fenwickTreeAmplitude.applyDiffusion();

        double[] amplitudes = fenwickTreeAmplitude.getAmplitudes();
        
        // Verify diffusion formula: v_new = 2*mean - v_old
        // For target index:
        assertEquals(2 * expectedMean - (-initialAmplitude), amplitudes[targetIndex], 1e-9);
        // For non-target indices:
        assertEquals(2 * expectedMean - initialAmplitude, amplitudes[1], 1e-9);
        assertEquals(2 * expectedMean - initialAmplitude, amplitudes[2], 1e-9);
        assertEquals(2 * expectedMean - initialAmplitude, amplitudes[3], 1e-9);

        // Total probability should still be approximately 1.0
        assertEquals(1.0, fenwickTreeAmplitude.getTotalProbability(), 1e-9);
    }

    @Test
    void testFindMaxAmplitudeIndex() {
        int size = 4;
        fenwickTreeAmplitude.initialize(size);
        
        // Manually set amplitudes to have a clear max
        double[] testAmplitudes = {0.1, 0.8, 0.2, 0.3};
        // Directly set internal state for testing purposes (not ideal for production code)
        // This is done to bypass the Fenwick Tree update logic for this specific test
        // and focus on findMaxAmplitudeIndex.
        try {
            java.lang.reflect.Field field = FenwickTreeAmplitude.class.getDeclaredField("actualAmplitudes");
            field.setAccessible(true);
            field.set(fenwickTreeAmplitude, testAmplitudes);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Could not set actualAmplitudes for testing: " + e.getMessage());
        }

        assertEquals(1, fenwickTreeAmplitude.findMaxAmplitudeIndex());

        // Test with negative amplitudes (should still find max based on |amplitude|^2)
        double[] testAmplitudesNeg = {0.1, -0.9, 0.2, 0.3};
        try {
            java.lang.reflect.Field field = FenwickTreeAmplitude.class.getDeclaredField("actualAmplitudes");
            field.setAccessible(true);
            field.set(fenwickTreeAmplitude, testAmplitudesNeg);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Could not set actualAmplitudes for testing: " + e.getMessage());
        }
        assertEquals(1, fenwickTreeAmplitude.findMaxAmplitudeIndex());
    }

    @Test
    void testFindMaxAmplitudeIndexNotInitialized() {
        assertThrows(IllegalStateException.class, () -> fenwickTreeAmplitude.findMaxAmplitudeIndex());
    }

    @Test
    void testGroverLikeIterationSmallSize() {
        int size = 4;
        int targetIndex = 2;
        fenwickTreeAmplitude.initialize(size);

        // Simulate one Grover iteration
        fenwickTreeAmplitude.applyOracle(targetIndex);
        fenwickTreeAmplitude.applyDiffusion();

        // After one iteration, the target amplitude should be amplified
        // and the others suppressed. The exact values are complex, but we expect
        // the target index to have the highest amplitude (or probability).
        assertEquals(targetIndex, fenwickTreeAmplitude.findMaxAmplitudeIndex());
        
        // Simulate a second iteration (for N=4, 1 iteration is optimal, but testing behavior)
        fenwickTreeAmplitude.applyDiffusion();
        // No assertion here, as for N=4, 2 iterations over-rotates and target might not be max
    }
}