
package net.sf.freecol.common.model;


/**
* The <code>ModelController</code> is used by the model to perform
* tasks that are not allowed to perform within the model (like generating
* random numbers or creating new {@link FreeColGameObject FreeColGameObjects}).
*
* <br><br>
*
* Any {@link FreeColGameObject} may get access to the <code>ModelController</code>
* by using {@link Game#getModelController getGame().getModelController()}.
*/
public interface ModelController {

    /**
    * Creates a new unit.
    *
    * @param taskID The <code>taskID</code> should be a unique identifier.
    *               One method to make a unique <code>taskID</code>:
    *               <br><br>
    *               getID() + "methodName:taskDescription"
    *               <br>br>
    *               As long as the "taskDescription" is unique
    *               within the method ("methodName"), you get a unique
    *               identifier.
    * @param location The <code>Location</code> where the <code>Unit</code>
    *               will be created.
    * @param owner  The <code>Player</code> owning the <code>Unit</code>.
    * @param type   The type of unit (Unit.FREE_COLONIST...).
    */
    public Unit createUnit(String taskID, Location location, Player owner, int type);

    /**
    * Puts the specified <code>Unit</code in America.
    * @param unit The <code>Unit</code>.
    * @return The <code>Location</code> where the <code>Unit</code> appears.
    */
    public Location setToVacantEntryLocation(Unit unit);

    /**
    * Tells the <code>ModelController</code> that an internal
    * change (that is; not caused by the control) has occured in the model.
    */
    //public void update(Tile tile);
}
