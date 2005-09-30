
package net.sf.freecol.common.model;

/**
 * Interface for retriving information about
 * a the creation/deletion of {@link FreeColGameObject}s.
 */
public interface FreeColGameObjectListener {
    public void setFreeColGameObject(String id, FreeColGameObject freeColGameObject);
    public void removeFreeColGameObject(String id);
}
