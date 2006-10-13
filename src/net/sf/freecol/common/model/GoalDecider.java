
package net.sf.freecol.common.model;


/**
 * Used by {@link Map#search(Unit, Tile, GoalDecider, CostDecider, int, Unit) search}
 * in order to determine a goal.
 * 
 * <br /><br />
 * 
 * The method {@link #check(Unit, PathNode)} will be called by
 * {@link Map#search(Unit, Tile, GoalDecider, CostDecider, int, Unit) search}
 * until:
 * 
 * <ol>
 *   <li>The method returns <code>true</code> and there is 
 *       {@link #hasSubGoals() no sub goals}.</li>
 *   <li>The maximum distance of the search has been reached.</li>
 * </ol>
 * 
 * The method {@link #getGoal()} will get called after this.
 */
public interface GoalDecider {

    public static final  String  COPYRIGHT = "Copyright (C) 2003-2006 The FreeCol Team";
    public static final  String  LICENSE   = "http://www.gnu.org/licenses/gpl.html";
    public static final  String  REVISION  = "$Revision$";


    /**
     * Gets the <code>PathNode</code> containing the goal.
     * @return The <code>PathNode</code> where the <code>Tile</code>
     *      returned by <code>pathNode.getTile()</code> is the goal.
     */
    public PathNode getGoal();
    
    /**
     * Determines wether this <code>GoalDecider</code> has any
     * sub goals.
     * 
     * @return <code>true</code> if there are any sub goals
     *      and <code>false</code> otherwise.
     */
    public boolean hasSubGoals();
    
    /**
     * Checks wether the given <code>PathNode</code> is a 
     * goal/sub-goal.
     * 
     * @param u The <code>Unit</code> which we are trying 
     *      to find a path for.
     * @param pathNode The <code>PathNode</code> where the 
     *      <code>Tile</code> returned by 
     *      <code>pathNode.getTile()</code> is the tile to be
     *      checked.
     * @return <code>true</code> if the <code>PathNode</code> was
     *      either a goal or a sub goal and <code>false</code> 
     *      otherwise. The goal should be returned by 
     *      {@link #getGoal()} right after a call to this method, 
     *      if this method returns <code>true</code> and
     *      {@link #hasSubGoals()} returns
     *      <code>false</code>.
     * 
     */
    public boolean check(Unit u, PathNode pathNode);
}
