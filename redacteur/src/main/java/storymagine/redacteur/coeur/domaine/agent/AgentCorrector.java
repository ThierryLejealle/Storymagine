package storymagine.redacteur.coeur.domaine.agent;

import storymagine.redacteur.coeur.domaine.agent.commun.RetryStrategy;

/** Marker interface for Correctors — exposes how their self-repeat loop should decide to continue. */
public interface AgentCorrector extends Agent {
    RetryStrategy retryStrategy();
}
