package br.com.atous.demo.infrastructure.datastructure;

import br.com.atous.demo.domain.port.out.AmplitudeDataStructure;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import java.util.Arrays;

@Component
@Scope("prototype")
public class FenwickTreeAmplitude implements AmplitudeDataStructure {

    private double[] amplitudes;
    private double[] fenwickTree;
    private int size;

    @Override
    public void initialize(int size) {
        this.size = size;
        this.amplitudes = new double[size];
        this.fenwickTree = new double[size + 1]; // 1-based indexing

        double initialAmplitude = 1.0 / Math.sqrt(size);
        Arrays.fill(amplitudes, initialAmplitude);

        // Constrói a árvore de Fenwick
        for (int i = 0; i < size; i++) {
            updateTree(i, initialAmplitude);
        }
    }

    @Override
    public void applyOracle(int targetIndex) {
        double currentAmplitude = amplitudes[targetIndex];
        double newAmplitude = -currentAmplitude;
        
        // A atualização é a diferença entre o novo e o antigo valor.
        update(targetIndex, newAmplitude - currentAmplitude);
    }

    @Override
    public void applyDiffusion() {
        double sum = querySum(size - 1);
        double mean = sum / size;

        for (int i = 0; i < size; i++) {
            double currentAmplitude = amplitudes[i];
            double newAmplitude = 2 * mean - currentAmplitude;
            update(i, newAmplitude - currentAmplitude);
        }
    }

    @Override
    public int findMaxAmplitudeIndex() {
        int maxIndex = -1;
        double maxProb = -1.0;
        for (int i = 0; i < size; i++) {
            double probability = amplitudes[i] * amplitudes[i];
            if (probability > maxProb) {
                maxProb = probability;
                maxIndex = i;
            }
        }
        return maxIndex;
    }

    // --- Métodos auxiliares da Árvore de Fenwick ---

    private void update(int index, double delta) {
        amplitudes[index] += delta;
        updateTree(index, delta);
    }
    
    private void updateTree(int index, double delta) {
        index++; // Converte para 1-based
        while (index <= size) {
            fenwickTree[index] += delta;
            index += index & -index; // Vai para o próximo nó ancestral
        }
    }

    private double querySum(int index) {
        index++; // Converte para 1-based
        double sum = 0.0;
        while (index > 0) {
            sum += fenwickTree[index];
            index -= index & -index; // Vai para o próximo nó na árvore
        }
        return sum;
    }
} 