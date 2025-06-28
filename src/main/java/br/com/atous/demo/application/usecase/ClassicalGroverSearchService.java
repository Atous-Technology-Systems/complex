package br.com.atous.demo.application.usecase;

import br.com.atous.demo.application.port.in.QuantumSearchUseCase;
import br.com.atous.demo.domain.model.GroverResult;
import br.com.atous.demo.domain.port.out.AmplitudeDataStructure;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
public class ClassicalGroverSearchService implements QuantumSearchUseCase {

    private final ObjectProvider<AmplitudeDataStructure> amplitudeProvider;

    // Usamos ObjectProvider para obter uma nova instância de FenwickTreeAmplitude (escopo prototype) a cada chamada.
    public ClassicalGroverSearchService(ObjectProvider<AmplitudeDataStructure> amplitudeProvider) {
        this.amplitudeProvider = amplitudeProvider;
    }

    @Override
    public GroverResult executeSearch(int searchSpaceSize, int targetIndex) {
        long startTime = System.nanoTime();

        AmplitudeDataStructure amplitudes = amplitudeProvider.getObject();
        amplitudes.initialize(searchSpaceSize);

        int iterations = (int) Math.floor(Math.PI / 4.0 * Math.sqrt(searchSpaceSize));

        for (int i = 0; i < iterations; i++) {
            amplitudes.applyOracle(targetIndex);
            amplitudes.applyDiffusion();
        }

        int foundIndex = amplitudes.findMaxAmplitudeIndex();
        long endTime = System.nanoTime();
        long durationMillis = (endTime - startTime) / 1_000_000;

        return new GroverResult(
            foundIndex,
            targetIndex,
            foundIndex == targetIndex,
            durationMillis,
            searchSpaceSize,
            iterations
        );
    }
} 