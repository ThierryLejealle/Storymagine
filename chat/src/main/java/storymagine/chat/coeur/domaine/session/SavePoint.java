package storymagine.chat.coeur.domaine.session;

/**
 * A named, player-triggered snapshot of a whole run (every turn and compaction so far, plus the
 * presence settings), distinct from the live run that grows after every turn. id is the storage
 * adapter's own timestamp-derived identifier (also its creation order — newest first when listed),
 * opaque to the core beyond that. lastTurnNumber is the number of the last turn the snapshot holds
 * (0 if none), shown next to it in the list. See ChatStoragePort.createSavePoint/listSavePoints/
 * loadSavePoint.
 */
public record SavePoint(String id, int lastTurnNumber) {
}
