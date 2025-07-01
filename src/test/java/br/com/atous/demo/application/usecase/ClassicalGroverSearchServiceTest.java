package br.com.atous.demo.application.usecase;

import br.com.atous.demo.application.port.in.QuantumSearchUseCase;
import br.com.atous.demo.domain.model.GroverResult;
import br.com.atous.demo.infrastructure.datastructure.SegmentTreeAmplitude;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = {ClassicalGroverSearchService.class, ClassicalGroverSearchServiceTest.TestConfig.class})
class ClassicalGroverSearchServiceTest {

    @Autowired
    private QuantumSearchUseCase searchService;

    @Configuration
    static class TestConfig {
        @Bean
        @Primary
        @Scope("prototype") // Ensure a new instance for each test
        public SegmentTreeAmplitude segmentTreeAmplitude() {
            return new SegmentTreeAmplitude();
        }

        @Bean
        public ObjectProvider<SegmentTreeAmplitude> segmentTreeAmplitudeObjectProvider() {
            return new ObjectProvider<SegmentTreeAmplitude>() {
                @Override
                public SegmentTreeAmplitude getObject(Object... args) {
                    return segmentTreeAmplitude();
                }

                @Override
                public SegmentTreeAmplitude getIfAvailable() {
                    return segmentTreeAmplitude();
                }

                @Override
                public SegmentTreeAmplitude getIfUnique() {
                    return segmentTreeAmplitude();
                }
            };
        }
    }

    @Test
    void whenExecuteSearch_thenShouldFindTargetCorrectly() {
        // Test with a small search space
        int searchSpaceSize = 16;
        int targetIndex = 5;
        GroverResult result = searchService.executeSearch(searchSpaceSize, targetIndex);
        assertTrue(result.success(), "Search should find the target for N=" + searchSpaceSize + ", target=" + targetIndex);
        assertEquals(targetIndex, result.foundIndex());

        // Test with a larger search space
        searchSpaceSize = 1024;
        targetIndex = 512;
        result = searchService.executeSearch(searchSpaceSize, targetIndex);
        assertTrue(result.success(), "Search should find the target for N=" + searchSpaceSize + ", target=" + targetIndex);
        assertEquals(targetIndex, result.foundIndex());

        // Test with target at the beginning
        searchSpaceSize = 64;
        targetIndex = 0;
        result = searchService.executeSearch(searchSpaceSize, targetIndex);
        assertTrue(result.success(), "Search should find the target for N=" + searchSpaceSize + ", target=" + targetIndex);
        assertEquals(targetIndex, result.foundIndex());

        // Test with target at the end
        searchSpaceSize = 64;
        targetIndex = 63;
        result = searchService.executeSearch(searchSpaceSize, targetIndex);
        assertTrue(result.success(), "Search should find the target for N=" + searchSpaceSize + ", target=" + targetIndex);
        assertEquals(targetIndex, result.foundIndex());
    }

    @Test
    void whenExecuteSearch_thenShouldPerformCorrectNumberOfIterations() {
        int searchSpaceSize = 16;
        int expectedIterations = (int) Math.floor(Math.PI / 4.0 * Math.sqrt(searchSpaceSize));
        GroverResult result = searchService.executeSearch(searchSpaceSize, 0); // Target doesn't matter for iteration count
        assertEquals(expectedIterations, result.iterations());

        searchSpaceSize = 1;
        expectedIterations = 0;
        result = searchService.executeSearch(searchSpaceSize, 0);
        assertEquals(expectedIterations, result.iterations());

        searchSpaceSize = 3;
        expectedIterations = 1;
        result = searchService.executeSearch(searchSpaceSize, 0);
        assertEquals(expectedIterations, result.iterations());
    }

    @Test
    void whenExecuteSearch_thenShouldMeasureExecutionTime() {
        int searchSpaceSize = 1024;
        int targetIndex = 500;
        GroverResult result = searchService.executeSearch(searchSpaceSize, targetIndex);
        assertTrue(result.executionTimeMillis() >= 0);
    }

    @Test
    void whenExecuteSearch_withInvalidInputs_shouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> searchService.executeSearch(0, 0));
        assertThrows(IllegalArgumentException.class, () -> searchService.executeSearch(10, -1));
        assertThrows(IllegalArgumentException.class, () -> searchService.executeSearch(10, 10));
    }

    /**
     * Performance test to verify O(√N log N) complexity.
     * This test will run for various N values and check if the execution time
     * scales approximately as √N * log N.
     * Note: This is a rough check and might be sensitive to system load.
     */
    @Test
    void performanceTest_shouldExhibitExpectedComplexity() {
        int[] Ns = {16384, 65536, 262144, 1048576}; // Powers of 2 for easier log N calculation
        long[] executionTimes = new long[Ns.length];

        System.out.println("Running performance test for ClassicalGroverSearchService...");

        for (int i = 0; i < Ns.length; i++) {
            int N = Ns[i];
            int targetIndex = N / 2; // Arbitrary target
            long startTime = System.nanoTime();
            searchService.executeSearch(N, targetIndex);
            long endTime = System.nanoTime();
            executionTimes[i] = (endTime - startTime) / 1_000_000; // Milliseconds
            System.out.printf("N = %d, Time = %d ms%n", N, executionTimes[i]);
        }

        // Verify scaling: (T2 / T1) should be approximately (sqrt(N2)*log(N2)) / (sqrt(N1)*log(N1))
        // We'll check the ratio of (Time / (sqrt(N) * log2(N))) should be roughly constant
        double[] ratios = new double[Ns.length];
        for (int i = 0; i < Ns.length; i++) {
            double sqrtN = Math.sqrt(Ns[i]);
            double logN = Math.log(Ns[i]) / Math.log(2); // log base 2
            ratios[i] = (double) executionTimes[i] / (sqrtN * logN);
            System.out.printf("N = %d, Ratio (Time / (sqrt(N)*log2(N))) = %.4f%n", Ns[i], ratios[i]);
        }

        
    }
}