package br.com.atous.demo.infrastructure.datastructure;

import br.com.atous.demo.domain.port.out.AmplitudeDataStructure;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * IMPLEMENTAÇÃO CORRIGIDA - ALGORITMO DE GROVER CLÁSSICO
 * 
 * Esta implementação foi refatorada para utilizar uma Fenwick Tree (Binary Indexed Tree)
 * para otimizar as operações de soma e atualização de amplitudes.
 * 
 * CORREÇÕES APLICADAS:
 * 1. ✅ Fenwick Tree real implementada para `update` e `query_sum`.
 * 2. ✅ Operação de Difusão Correta: v_new = 2*mean - v_old.
 * 3. ✅ Validações de Entrada: Parâmetros validados corretamente.
 * 
 * COMPLEXIDADE ALGORÍTMICA (com Fenwick Tree):
 * - initialize(): O(N log N)
 * - applyOracle(): O(log N)
 * - applyDiffusion(): O(N log N) - Iteração sobre N elementos, cada um com update O(log N)
 * - findMaxAmplitudeIndex(): O(N) - Iteração sobre actualAmplitudes
 * - Total para Grover: O(N log N + √N * (log N + N log N)) = O(N√N log N)
 * 
 * NOTA: A otimização da difusão para O(log N) (conforme sugerido no pseudocódigo do PDF)
 * exigiria uma Fenwick Tree mais avançada ou uma abordagem diferente para transformações
 * afins globais, o que não é trivial e será considerado em etapas futuras, se necessário.
 */
@Component
@Scope("prototype")
public class FenwickTreeAmplitude implements AmplitudeDataStructure {

    private double[] bit; // Fenwick Tree array (1-indexed)
    private double[] actualAmplitudes; // Stores actual amplitude values (0-indexed)
    private int size;

    @Override
    public void initialize(int size) {
        validateSize(size);
        this.size = size;
        this.bit = new double[size + 1]; // Fenwick Tree is 1-indexed
        this.actualAmplitudes = new double[size];
        
        // Inicialização com superposição uniforme: |ψ⟩ = (1/√N) Σ|i⟩
        double initialAmplitude = 1.0 / Math.sqrt(size);
        for (int i = 0; i < size; i++) {
            actualAmplitudes[i] = initialAmplitude;
            updateFenwickTree(i, initialAmplitude); // Initialize Fenwick Tree
        }
    }

    @Override
    public void applyOracle(int targetIndex) {
        validateTargetIndex(targetIndex);
        
        // Oracle: inverte a fase do elemento alvo
        // |target⟩ → -|target⟩
        double oldAmplitude = actualAmplitudes[targetIndex];
        double newAmplitude = -oldAmplitude;
        double delta = newAmplitude - oldAmplitude;

        actualAmplitudes[targetIndex] = newAmplitude;
        updateFenwickTree(targetIndex, delta);
    }

    @Override
    public void applyDiffusion() {
        // CORREÇÃO CRÍTICA: Operação de difusão matematicamente correta
        // 
        // Diffusion Operator: 2|s⟩⟨s| - I
        // onde |s⟩ = (1/√N) Σ|i⟩ é o estado de superposição uniforme
        //
        // Efeito: v_new = 2*mean - v_old
        
        // 1. Calcula a média das amplitudes usando a Fenwick Tree (O(log N))
        double totalSum = queryFenwickTree(size - 1);
        double mean = totalSum / size;
        
        // 2. Aplica a transformação de difusão: v_new = 2*mean - v_old
        // Esta etapa é O(N log N) pois itera sobre todos os N elementos
        // e cada atualização na Fenwick Tree é O(log N).
        for (int i = 0; i < size; i++) {
            double oldAmplitude = actualAmplitudes[i];
            double newAmplitude = 2.0 * mean - oldAmplitude;
            double delta = newAmplitude - oldAmplitude;

            actualAmplitudes[i] = newAmplitude;
            updateFenwickTree(i, delta);
        }
    }

    @Override
    public int findMaxAmplitudeIndex() {
        if (actualAmplitudes == null) {
            throw new IllegalStateException("Amplitudes not initialized");
        }
        
        int maxIndex = 0;
        double maxProbability = actualAmplitudes[0] * actualAmplitudes[0];
        
        // Encontra o índice com maior probabilidade |amplitude|²
        // Esta operação é O(N)
        for (int i = 1; i < size; i++) {
            double probability = actualAmplitudes[i] * actualAmplitudes[i];
            if (probability > maxProbability) {
                maxProbability = probability;
                maxIndex = i;
            }
        }
        
        return maxIndex;
    }
    
    // --- Fenwick Tree (BIT) Helper Methods ---
    
    // Adds 'delta' to the element at 'idx' (0-indexed)
    private void updateFenwickTree(int idx, double delta) {
        idx++; // Convert to 1-indexed
        while (idx <= size) {
            bit[idx] += delta;
            idx += idx & (-idx);
        }
    }

    // Returns the sum of elements from 0 to 'idx' (0-indexed)
    private double queryFenwickTree(int idx) {
        idx++; // Convert to 1-indexed
        double sum = 0;
        while (idx > 0) {
            sum += bit[idx];
            idx -= idx & (-idx);
        }
        return sum;
    }
    
    // --- Validation Methods ---
    
    private void validateSize(int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("Size must be positive, got: " + size);
        }
        if (size > 1_000_000) {
            throw new IllegalArgumentException("Size too large for practical use: " + size);
        }
    }
    
    private void validateTargetIndex(int targetIndex) {
        if (targetIndex < 0 || targetIndex >= size) {
            throw new IllegalArgumentException(
                String.format("Target index %d is out of bounds [0, %d)", targetIndex, size)
            );
        }
    }
    
    // --- Auxiliary Methods (for debugging/testing) ---
    
    public double[] getAmplitudes() {
        return actualAmplitudes.clone();
    }
    
    public double getTotalProbability() {
        double total = 0.0;
        for (double amplitude : actualAmplitudes) {
            total += amplitude * amplitude;
        }
        return total;
    }
} 