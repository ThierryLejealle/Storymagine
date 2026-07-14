package storymagine.chat.coeur.domaine.session;

/**
 * A named, player-triggered snapshot of a ChatSession (turns + summary + currentAct), distinct
 * from the automatic save that overwrites the same slot after every exchange. id is the storage
 * adapter's own timestamp-derived identifier (also its creation order — newest first when listed),
 * opaque to the core beyond that. See ChatStoragePort.createSavePoint/listSavePoints/loadSavePoint.
 */
public record SavePoint(String id) {
}
