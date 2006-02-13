
package net.sf.freecol.common.model;


/**
 * Interface for retriving information about
 * a the creation/deletion of {@link FreeColGameObject}s.
 */
public interface FreeColGameObjectListener {

    public static final  String  COPYRIGHT = "Copyright (C) 2003-2006 The FreeCol Team";
    public static final  String  LICENSE   = "http://www.gnu.org/licenses/gpl.html";
    public static final  String  REVISION  = "$Revision$";


    public void setFreeColGameObject(String id, FreeColGameObject freeColGameObject);


    public void removeFreeColGameObject(String id);

}
