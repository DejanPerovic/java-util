package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;
import java.util.*;

/**
 * Micro-benchmark to compare OR chain vs Set.contains() performance 
 * for checking 10 specific array classes.
 */
public class OrVsSetPerformanceTest {
    
    // The 10 classes from the code
    private static final Set<Class<?>> WRAPPER_ARRAY_CLASSES;
    static {
        Set<Class<?>> classes = new HashSet<>();
        classes.add(String[].class);
        classes.add(Integer[].class);
        classes.add(Long[].class);
        classes.add(Double[].class);
        classes.add(Date[].class);
        classes.add(Boolean[].class);
        classes.add(Float[].class);
        classes.add(Short[].class);
        classes.add(Byte[].class);
        classes.add(Character[].class);
        WRAPPER_ARRAY_CLASSES = Collections.unmodifiableSet(classes);
    }
    
    // Test data - mix of classes that are and aren't in the set
    private static final Class<?>[] TEST_CLASSES = {
        String[].class,      // in set - first
        Integer[].class,     // in set - second  
        Double[].class,      // in set - middle
        Character[].class,   // in set - last
        Object[].class,      // not in set
        int[].class,         // not in set
        String.class,        // not in set
        List.class,          // not in set
        Map.class           // not in set
    };
    
    @Test
    void compareOrVsSetPerformance() {
        // Warmup
        for (int i = 0; i < 100_000; i++) {
            for (Class<?> clazz : TEST_CLASSES) {
                orChainMethod(clazz);
                setContainsMethod(clazz);
            }
        }
        
        // Test OR chain
        long startOr = System.nanoTime();
        for (int i = 0; i < 1_000_000; i++) {
            for (Class<?> clazz : TEST_CLASSES) {
                orChainMethod(clazz);
            }
        }
        long endOr = System.nanoTime();
        long orTime = endOr - startOr;
        
        // Test Set contains
        long startSet = System.nanoTime();
        for (int i = 0; i < 1_000_000; i++) {
            for (Class<?> clazz : TEST_CLASSES) {
                setContainsMethod(clazz);
            }
        }
        long endSet = System.nanoTime();
        long setTime = endSet - startSet;
        
        System.out.println("=== OR Chain vs Set.contains() Performance Comparison ===");
        System.out.printf("OR chain time:    %,d ns (%.2f ms)%n", orTime, orTime / 1_000_000.0);
        System.out.printf("Set contains time: %,d ns (%.2f ms)%n", setTime, setTime / 1_000_000.0);
        System.out.printf("OR chain per operation: %.2f ns%n", orTime / 9_000_000.0);
        System.out.printf("Set contains per operation: %.2f ns%n", setTime / 9_000_000.0);
        
        if (orTime < setTime) {
            double speedup = (double) setTime / orTime;
            System.out.printf("OR chain is %.2fx faster%n", speedup);
        } else {
            double speedup = (double) orTime / setTime;
            System.out.printf("Set contains is %.2fx faster%n", speedup);
        }
        
        // Verify both methods produce same results
        System.out.println("\n=== Correctness Verification ===");
        for (Class<?> clazz : TEST_CLASSES) {
            boolean orResult = orChainMethod(clazz);
            boolean setResult = setContainsMethod(clazz);
            System.out.printf("%-20s: OR=%5s, Set=%5s %s%n", 
                clazz.getSimpleName(), orResult, setResult, 
                orResult == setResult ? "✓" : "✗");
        }
    }
    
    private static boolean orChainMethod(Class<?> clazz) {
        return clazz == String[].class || clazz == Integer[].class || clazz == Long[].class || 
               clazz == Double[].class || clazz == Date[].class || clazz == Boolean[].class || 
               clazz == Float[].class || clazz == Short[].class || clazz == Byte[].class || 
               clazz == Character[].class;
    }
    
    private static boolean setContainsMethod(Class<?> clazz) {
        return WRAPPER_ARRAY_CLASSES.contains(clazz);
    }
    
    @Test
    void analyzeDistribution() {
        // Test with different hit patterns to see if position matters
        System.out.println("=== Position Impact Analysis ===");
        
        Class<?>[] firstHit = {String[].class};        // First in OR chain
        Class<?>[] middleHit = {Date[].class};         // Middle in OR chain
        Class<?>[] lastHit = {Character[].class};      // Last in OR chain
        Class<?>[] noHit = {Object[].class};           // Not in set
        
        testPattern("First position hit", firstHit);
        testPattern("Middle position hit", middleHit);
        testPattern("Last position hit", lastHit);
        testPattern("No hit", noHit);
    }
    
    private void testPattern(String name, Class<?>[] classes) {
        // Warmup
        for (int i = 0; i < 50_000; i++) {
            for (Class<?> clazz : classes) {
                orChainMethod(clazz);
                setContainsMethod(clazz);
            }
        }
        
        // Test OR chain
        long startOr = System.nanoTime();
        for (int i = 0; i < 1_000_000; i++) {
            for (Class<?> clazz : classes) {
                orChainMethod(clazz);
            }
        }
        long endOr = System.nanoTime();
        
        // Test Set contains
        long startSet = System.nanoTime();
        for (int i = 0; i < 1_000_000; i++) {
            for (Class<?> clazz : classes) {
                setContainsMethod(clazz);
            }
        }
        long endSet = System.nanoTime();
        
        long orTime = endOr - startOr;
        long setTime = endSet - startSet;
        
        System.out.printf("%-20s: OR=%6.2f ns, Set=%6.2f ns, OR is %.2fx %s%n", 
            name, 
            orTime / (double)(classes.length * 1_000_000),
            setTime / (double)(classes.length * 1_000_000),
            orTime < setTime ? (double) setTime / orTime : (double) orTime / setTime,
            orTime < setTime ? "faster" : "slower"
        );
    }
}