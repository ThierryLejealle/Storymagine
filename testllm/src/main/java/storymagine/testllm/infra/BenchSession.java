package storymagine.testllm.infra;

import storymagine.testllm.coeur.service.TestLlmService;

/**
 * Paire (service metier + logger) retournee par l'assembleur pour un tir GPU.
 * Permet a la CLI d'acceder aux GpuState captures apres le bench.
 */
public record BenchSession(TestLlmService service, ConsoleRunLogger logger) {}
