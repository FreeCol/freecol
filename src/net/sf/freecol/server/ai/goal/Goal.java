/**
 *  Copyright (C) 2002-2015   The FreeCol Team
 *
 *  This file is part of FreeCol.
 *
 *  FreeCol is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  FreeCol is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FreeCol.  If not, see <http://www.gnu.org/licenses/>.
 */

/**********************************************
 * Please see "Howto" at the end of this file! *
 **********************************************/

package net.sf.freecol.server.ai.goal;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.logging.Logger;

import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.UnitType;
import static net.sf.freecol.common.util.StringUtils.*;

import net.sf.freecol.server.ai.AIObject;
import net.sf.freecol.server.ai.AIPlayer;
import net.sf.freecol.server.ai.AIUnit;


/**
 * A Goal is used to encapsulate a specific part of the
 * decision-making process of an AI.
 * </p><p>
 * Using a top-down approach, every {@link AIPlayer} has a set of Goals which,
 * in turn, may have further subgoals. In combination, this tree of goals
 * and subgoals reflects the current strategy of the AIPlayer.
 * </p><p>
 * Units (each one wrapped in an {@link AIUnit} object) will be moved between
 * existing Goal objects.
 * FIXME: Turn {@link AIUnit} into a simple wrapper for individual units.
 * </p><p>
 * Specific AI goals are created by extending this class; some of
 * these could also be used to assist the human player (i.e. GoTo,
 * Scouting, Trade, Piracy).
 */
public abstract class Goal extends AIObject implements GoalConstants {

    private static final Logger logger = Logger.getLogger(Goal.class.getName());

    private float relativeWeight;
    protected boolean needsPlanning;
    protected boolean isFinished;
    protected final List<AIUnit> availableUnitsList;

    protected final AIPlayer player;
    private final Goal parentGoal;

    /**
     * Standard constructor
     *
     * @param p The {@link AIPlayer} this goal belongs to
     * @param g The parent goal; may be null if we're a direct goal of the AIPlayer
     * @param w The relativeWeight of this goal
     */
    public Goal(AIPlayer p, Goal g, float w) {
        super(p.getAIMain());
        player = p;
        parentGoal = g;
        relativeWeight = w;
        getGame().getTurn().getNumber();
        needsPlanning = true; //a newly created Goal always needs planning
        isFinished = false; //only plan() should set this to true!
        availableUnitsList = new ArrayList<>();
    }

    /**
     * Alternate constructor - directly add a unit to this Goal.
     * The calling object ensures that this unit is not currently part of another Goal.
     *
     * @param p The {@link AIPlayer} this goal belongs to
     * @param g The parent goal; may be null if we're a direct goal of the AIPlayer
     * @param w The relativeWeight of this goal
     * @param u An initial {@link AIUnit} given to this goal
     */
    public Goal (AIPlayer p, Goal g, float w, AIUnit u) {
        this(p,g,w);
        addUnit(u);
    }

    /**
     * Determines whether this goal is finished.
     * If it is, this means it can be cancelled by its parent.
     *
     * @return true, if the goal is finished, false otherwise
     */
    public boolean isFinished() {
        return isFinished;
    }

    /**
     * Cancels a goal and all of its subgoals.
     * If a goal is cancelled, it will recursively cancelGoal() its subgoals first,
     * and return all units to the object calling this.
     * After this method has been called, it should be safe for the parent
     * to remove this goal from its list of subgoals.
     * </p><p>
     * NOTE: Preferably, only the direct parent should call this.
     *
     * @return A list of all {@link AIUnit} being freed up by this action
     */
    public List<AIUnit> cancelGoal() {
        logger.finest("Entering method cancelGoal() for "+getDebugDescription());
        List<AIUnit> cancelledUnitsList = new ArrayList<>();

        //get units from subgoals
        Iterator<Goal> git = getSubGoalIterator();
        while (git!=null && git.hasNext()) {
            Goal g = git.next();
            List<AIUnit> ulist = g.cancelGoal();
            cancelledUnitsList.addAll(ulist);
        }

        //get own units
        Iterator<AIUnit> uit = getOwnedAIUnitsIterator();
        while (uit.hasNext()) {
            AIUnit u = uit.next();
            cancelledUnitsList.add(u);
        }
        logger.info("Got "+cancelledUnitsList.size()+" units from cancelled subgoals");
        return cancelledUnitsList;
    }

    /**
     * Recursively calls {@link #doPlanning} in subgoals that {@link #needsPlanning()},
     * then calls its own planning method.
     */
    public void doPlanning() {
        logger.finest("Entering method doPlanning() for "+getDebugDescription());
        boolean subgoalsPlanned = false;

        normalizeSubGoalWeights();

        Iterator<Goal> git = getSubGoalIterator();
        while (git!=null && git.hasNext()) {
            Goal g = git.next();
            if (g.needsPlanning()) {
                g.doPlanning();
                subgoalsPlanned = true;
            }
        }

        //after all subgoals have been planned, let's plan ourselves
        if (needsPlanning || subgoalsPlanned) {
            plan();
            needsPlanning = false;
        }
    }

    /**
     * Determines whether this or a subgoal {@link #needsPlanning}.
     *
     * @return true if this Goal or at least one subgoal needs planning, false otherwise
     */
    public boolean needsPlanning() {
        logger.finest("Entering method needsPlanning() for "+getDebugDescription());
        if (needsPlanning) {
            return true;
        } else {
            Iterator<Goal> git = getSubGoalIterator();
            while (git!=null && git.hasNext()) {
                Goal g = git.next();
                if (g.needsPlanning()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Sets the {@link #needsPlanning} status of this Goal and all its subgoals.
     * Should be called by the {@link AIPlayer} once for each of its subgoals
     * at the start of a turn. The Goal will handle all other instances of this
     * flag needing to be reset internally.
     *
     * @param p Boolean determining whether to set needsPlanning =true or =false
     */
    public void setNeedsPlanningRecursive(boolean p) {
        logger.finest("Entering method setNeedsPlanningRecursive() for "+getDebugDescription());
        needsPlanning = p;

        Iterator<Goal> git = getSubGoalIterator();
        while (git!=null && git.hasNext()) {
            Goal g = git.next();
            g.setNeedsPlanningRecursive(p);
        }
    }

    /**
     * Returns the relativeWeight this goal has been weighted with by its parent.
     * </p><p>
     * NOTE: In many cases, you will want to use {@link #getAbsoluteWeight()} instead.
     *
     * @return The relative weight {@link #relativeWeight} of this goal
     */
    public float getWeight() {
        return relativeWeight;
    }

    /**
     * Gets the weight of the parent goal, or 1 if there is no parent goal.
     *
     * @return The absolute weight [0;1] of the parent goal, or 1 if a parent goal does not exist
     */
    public float getParentWeight() {
        if (parentGoal == null) {
            //we must be a direct goal of our AIPlayer
            return 1.0f;
        } else {
            return parentGoal.getAbsoluteWeight();
        }
    }

    /**
     * Returns the absolute weight of this goal.
     * </p><p>
     * The absolute weight is the weight of this goal in relation to the {@link AIPlayer}.
     * This is used when making requests, to allow the AIPlayer to find the
     * "most important" request.
     *
     * @return The absolute weight [0;1] of this goal
     */
    public float getAbsoluteWeight() {
        return getParentWeight() * relativeWeight;
    }

    /**
     * Sets a relative weight for this goal.
     * Each Goal is weighted by its parent goal.
     * The parent should assure that the sum of weights given to its subgoals is =1
     *
     * @param w A relative weight, should be in range [0;1]
     */
    public void setWeight(float w) {
        relativeWeight = w;
    }

    /**
     * Calling this ensures that the relative weights given to
     * subgoals add up to 1.
     * </p><p>
     * NOTE: This allows for a small margin of error (+/- 0.05),
     * to avoid recalculating too often.
     */
    public void normalizeSubGoalWeights() {
        float sumWeights = 0f;

        Iterator<Goal> git = getSubGoalIterator();
        while (git!=null && git.hasNext()) {
            Goal g = git.next();
            sumWeights += g.getWeight();
        }

        //allow for a small rounding or other error margin before normalizing
        if (sumWeights>0f && (sumWeights<0.95f || sumWeights>1.05f)) {
            git = getSubGoalIterator();
            while (git!=null && git.hasNext()) {
                Goal g = git.next();
                g.setWeight(g.getWeight()/sumWeights);
            }
        }
    }

//     /**
//      * Wrapper method for a unit request sent to the {@link AIPlayer}.
//      * </p><p>
//      * Each Goal can request necessary units from the AIPlayer.
//      * Here, such a request is wrapped in a private method for convenience.
//      * Each request contains a weight, which is {@link #getAbsoluteWeight()}
//      * of this goal, and the number of turns since a unit request has last been granted.
//      * The latter should be taken into account as a "bonus weight" by the AIPlayer.
//      * </p><p>
//      * FIXME:Should that be role, instead or alternatively?
//      * </p><p>
//      * FIXME: {@link AIPlayer#addUnitWish(Goal,UnitType,float,int)}; should add
//      * requests to a set-like structure, so that there's only one active request
//      * per Goal at any time. Since fulfilling a request using {@link #addUnit(AIUnit)}
//      * means that {@link #plan()} will be called again during the turn,
//      * the Goal will be able to request again.
//      *
//      * @param ut The {@link UnitType} we'd like to request
//      */
//     protected void requestUnit(UnitType ut) {
//         int turnsWithoutUnit = getGame().getTurn().getNumber() - turnLastUnitAdded;
//
//         //FIXME: Uncomment after AIPlayer.addUnitWish() has been written.
//         //player.addUnitWish(this, ut, getAbsoluteWeight(), turnsWithoutUnit);
//     }

    /**
     * Wrapper method for a worker request sent to the {@link AIPlayer}.
     * </p><p>
     * Each Goal can request necessary units from the AIPlayer.
     * Here, such a request is wrapped in a private method for convenience.
     * Each request contains a weight, which is {@link #getAbsoluteWeight()}
     * of this goal, and the number of turns since a unit request has last been granted.
     * The latter should be taken into account as a "bonus weight" by the AIPlayer.
     * </p><p>
     * FIXME: AIPlayer#addUnitWish(Goal,GoodsType,int,float,int); should add
     * requests to a set-like structure, so that there's only one active request
     * per Goal at any time. Since fulfilling a request using {@link #addUnit(AIUnit)}
     * means that {@link #plan()} will be called again during the turn,
     * the Goal will be able to request again.
     *
     * @param gt The {@link GoodsType} we're requesting a worker for.
     * @param minProduction The minimum a unit needs to produce to be considered.
     */
    protected void requestWorker(GoodsType gt, int minProduction) {

        //FIXME: Uncomment after AIPlayer.addWorkerWish() has been written.
        //int turnsWithoutUnit = getGame().getTurn().getNumber() - turnLastUnitAdded;
        //player.addWorkerWish(this, gt, minProduction, getAbsoluteWeight(), turnsWithoutUnit);
    }

    /**
     * Adds a unit to this goal.
     * This may be from {@link AIPlayer} fulfilling a unit request,
     * by the parent goal, or by a subgoal that no longer needs the unit.
     * </p><p>
     * Possible FIXME: If the unit we're requesting is a high-interest one,
     * such as a Galleon, AIUnit#setLoaningGoal() may be used to
     * signal that this unit may _only_ be moved to subgoals, or back to
     * {@link AIPlayer}, but not further up the hierarchy or to any other requesting Goal.
     *
     * @param u The {@link AIUnit} being added to this goal
     */
    public final void addUnit(AIUnit u) {
        logger.finest("Entering method addUnit() for "+getDebugDescription()+" with unit: "+u.getId());
        getGame().getTurn().getNumber();
        availableUnitsList.add(u);
        u.setGoal(this);
        needsPlanning = true; //adding a unit to the Goal means it might need planning
        isFinished = false; //in case the goal was finished but not yet cancelled
    }

    /**
     * Adds a unit to the parent goal.
     * If this goal doesn't have a parent goal,
     * the unit will be added to {@link AIPlayer} instead.
     *
     * @param u The {@link AIUnit} to be added to the parent
     */
    protected void addUnitToParent(AIUnit u) {
        logger.finest("Entering method addUnitToParent() for "+getDebugDescription()+" with unit: "+u.getId());
        if (parentGoal != null) {
            parentGoal.addUnit(u);
        } else {
            //Setting goal=null will make the unit appear in the unit iterator next turn.
            //FIXME: What about this turn?
            u.setGoal(null);
        }
    }

    /**
     * Used by a parent goal to check whether this goal, including subgoals,
     * can yield a specific unit.
     * </p><p>
     * This recursively checks its subgoals, if there's no match among the own units.
     * </p><p>
     * Possible FIXME: Check whether AIUnit#isOnLoan() - in which case, we mustn't
     * yield a unit unless it's the {@link AIPlayer} that requests.
     *
     * @param ut The {@link UnitType} wanted by the parent
     * @param o The {@link AIObject} (should be AIPlayer or another Goal) calling this
     * @return true if this goal or one of its subgoals can yield the specified {@link UnitType}, false otherwise
     */
    public boolean canYieldUnit(UnitType ut, AIObject o) {
        Iterator<AIUnit> uit = getOwnedAIUnitsIterator();
        while (uit.hasNext()) {
            AIUnit u = uit.next();
            //first found unit is enough
            if (u.getUnit().getType().equals(ut)) {
                return true;
            }
        }
        //None found among our own units, check subgoals
        Iterator<Goal> git = getSubGoalIterator();
        while (git!=null && git.hasNext()) {
            Goal g = git.next();
            if (g.canYieldUnit(ut, o)) {
                return true;
            }
        }
        //None found among subgoals
        return false;
    }

    /**
     * Returns the absolute weight of the unit which would be yielded by {@link #yieldUnit(UnitType,AIObject)}.
     * This is the same as {@link #getAbsoluteWeight()} of the yielding goal.
     *
     * @param ut The {@link UnitType} wanted by the parent
     * @param o The {@link AIObject} (should be AIPlayer or another Goal) calling this
     * @return The absolute weight ([0;1]) of the goal currently owning
     * the unit that would be yielded.
     * Note that the returned value might be 99f if there is no unit to yield.
     * The calling function should use {@link #canYieldUnit(UnitType,AIObject)} first,
     * or is responsible to sanitize this result itself before trying
     * to {@link #yieldUnit(UnitType,AIObject)} based on it.
     */
    public float getYieldedUnitWeight(UnitType ut, AIObject o) {
        //weights should normally be in range [0;1]
        //if there is a matching unit, this will be overwritten
        float unitWeight = 99f;

        Iterator<AIUnit> uit = getOwnedAIUnitsIterator();
        while (uit.hasNext()) {
            AIUnit u = uit.next();
            //all units in one goal have the same weight, so no need to compare
            if (u.getUnit().getType().equals(ut)) {
                unitWeight = getAbsoluteWeight();
            }
        }
        //check subgoals
        Iterator<Goal> git = getSubGoalIterator();
        while (git!=null && git.hasNext()) {
            Goal g = git.next();
            float newWeight = g.getYieldedUnitWeight(ut, o);
            if (newWeight < unitWeight) {
                unitWeight = newWeight;
            }
        }
        return unitWeight;
    }

    /**
     * Removes a unit from the goal, potentially from a subgoal,
     * and yields it to the caller.
     * </p><p>
     * Returned unit should be the one with minimum absolute weight,
     * see {@link #getYieldedUnitWeight(UnitType,AIObject)}.
     *
     * @param ut The {@link UnitType} wanted by the parent
     * @param o The {@link AIObject} (should be AIPlayer or another Goal) calling this
     * @return The {@link AIUnit} with minimal absolute weight.
     * Note that this may be null if no matching unit is found!
     */
    public AIUnit yieldUnit(UnitType ut, AIObject o) {
        float unitWeight = 99f;
        AIUnit yieldedUnit = null;
        boolean isOwnUnit = false;


        Iterator<AIUnit> uit = getOwnedAIUnitsIterator();
        while (uit.hasNext()) {
            AIUnit u = uit.next();
            //all units in one goal have the same weight, so no need to compare
            if (u.getUnit().getType().equals(ut)) {
                unitWeight = getAbsoluteWeight();
                yieldedUnit = u;
                isOwnUnit = true;
            }
        }
        //check subgoals
        Iterator<Goal> git = getSubGoalIterator();
        while (git!=null && git.hasNext()) {
            Goal g = git.next();
            float newWeight = g.getYieldedUnitWeight(ut, o);
            if (newWeight < unitWeight) {
                unitWeight = newWeight;
                yieldedUnit = g.yieldUnit(ut, o);
                isOwnUnit = false;
            }
        }
        if (isOwnUnit) {
            removeUnit(yieldedUnit);
            needsPlanning = true;
        }
        return yieldedUnit;
    }

    /**
     * Checks all owned AIUnits for validity, and removes invalid ones.
     * An AIUnit is supposed to be invalid if it no longer contains a valid Unit.
     * This may be the case if the Unit has been removed from the game between turns.
     * </p><p>
     * NOTE: The assumption here is that AIUnit#isValid() will return true
     * as long as the {@link net.sf.freecol.common.model.Unit} wrapped in it exists.
     */
    protected void validateOwnedUnits() {
        Iterator<AIUnit> uit = getOwnedAIUnitsIterator();
        while (uit.hasNext()) {
            AIUnit u = uit.next();
            if (!(u.getGoal()==this)) {
                logger.warning("Goal "+ getGoalDescription() + " owns unit with another goal: "
                               + u.getGoal().getGoalDescription());
                removeUnit(u);
            }
            //FIXME: Uncomment after AIUnit.isValid() has been written.
            //if (!u.isValid()) {
            //    removeUnit(u);
            //}
        }
    }

    /**
     * Returns a string describing just this goal.  An implementing
     * class may override this method to add specialized information.
     * Used by {@link #getDebugDescription()}.
     *
     * @return a string describing this goal
     */
    public String getGoalDescription() {
        String goalName = lastPart(getClass().getName(), ".");
        return goalName.substring(0, goalName.length() - "goal".length());
    }

    /**
     * Build and return a string describing this goal including its parent goal.
     * Used by "Display AI-missions" in debug mode.
     *
     * @return a string describing this goal
     */
    public String getDebugDescription() {
        String descr = "";

        //if goal has parent goal, add that as well
        //no recursive call, to avoid lengthy descriptions
        if (parentGoal!=null) {
            descr = parentGoal.getGoalDescription() + ">>";
        }
        return descr + getGoalDescription();
    }

    /**
     * Returns the tag name of the root element representing this object.
     *
     * @return "aiGoal"
     */
    public static String getXMLElementTagName() {
        return "aiGoal";
    }

/* INTERFACE ******************************************************************/


    /**
     * Since internal implementation details may vary,
     * each Goal will define an iterator over all of its units.
     *
     * @return An Iterator over all {@link AIUnit} currently managed by this goal.
     */
    protected abstract Iterator<AIUnit> getOwnedAIUnitsIterator();

    /**
     * Since internal implementation details may vary,
     * each Goal will define an iterator over all of its subgoals.
     *
     * @return An Iterator over all currently existing subgoals.
     */
    protected abstract Iterator<Goal> getSubGoalIterator();

    /**
     * Ensures that a unit moved to another Goal is properly removed from this.
     * If a unit is removed from this goal via {@link #yieldUnit(UnitType,AIObject)},
     * or if the Unit contained in the {@link AIUnit} no longer is valid,
     * this method is called to clean up any remaining references to the unit.
     * </p><p>
     * Any implementation of this will need to iterate over all AIUnit object
     * references used by this goal, and remove those that equal the given parameter.
     *
     * @param u The AIUnit supposed to be removed from this goal.
     */
    protected abstract void removeUnit(AIUnit u);

    /**
     * This is the method that actually does the planning for this goal.
     *
     * It should contain:
     * <ul>
     * <li>calling {@link #validateOwnedUnits()} to remove AIUnits no longer
     * containing a valid unit</li>
     * <li>putting units on the {@link #availableUnitsList} to work
     *   <ul><li>eventually by adding it to one of the subgoals, or</li>
     *   <li>by adding it back to the {@link AIPlayer}, or</li>
     *   <li>last but not least, by spending their movement points for some internal mission</li></ul></li>
     * <li>requesting new units (via a method like {@link #requestWorker(GoodsType,int)})</li>
     * <li>managing direct subgoals, including:
     *   <ul><li>creating new ones, if necessary</li>
     *   <li>cancelling those with isFinished()==true</li>
     *   <li>setting new weights using {@link #setWeight(float)}</li></ul></li>
     * <li>setting our own isFinished status</li>
     * </ul>
     */
    protected abstract void plan();

    /**
     * Writes this object to an XML stream.
     *
     * @param xw The <code>FreeColXMLWriter</code> to write to.
     * @throws XMLStreamException if there are any problems writing to the
     *             stream.
     */
    //protected abstract void toXML(FreeColXMLWriter xw) throws XMLStreamException;

    /**
     * Reads information for this object from an XML stream.
     *
     * @param xr The input stream with the XML.
     * @throws XMLStreamException if there are any problems reading from the
     *             stream.
     */
    //protected abstract void readFromXML(FreeColXMLReader xr) throws XMLStreamException;



/* How this is supposed to work:
 *
 * Assuming the AIPlayer has a set of goals...
 * At the start of a turn, it will call
 *
 *    setNeedsPlanningRecursive(true);
 *
 * for all its goals. This will set all goals in a state of needing a check.
 * After that, it will iterate through its goals in a WHILE-loop:
 *
 *    boolean furtherPlanning = true;
 *    while (furtherPlanning) {
 *        FOR ALL GOALS g DO {
 *            g.doPlanning();
 *        }
 *
 *        SATISFY_UNIT_REQUESTS();
 *        HANDLE_REMAINING_REQUESTS();
 *
 *        furtherPlanning = false;
 *        FOR ALL GOALS g DO {
 *            furtherPlanning = (furtherPlanning || g.needsPlanning());
 *        }
 *    }
 *
 * The first FOR-loop will recursively reach all existing goals and plan()
 * each of them once, bottom-up.
 *
 * After that, unit requests from the goals will have piled up.
 * SATISFY_UNIT_REQUESTS() will try to satisfy as many as possible,
 * in order of importance, by using the units that were created between turns
 * or that have been given back by other goals.
 *
 * HANDLE_REMAINING_REQUESTS() will try to do something about remaining requests,
 * for example by buying/building new units, setting up new goals etc.
 *
 * This, as well as goals called later during this process moving a unit to a former goal,
 * may have set one or more of the goals to needsPlanning=true. In the second FOR-loop,
 * we check whether this is the case, to eventually repeat the process.
 *
 * Eventually, the WHILE-loop will exit. Any units in the goals will have
 * been used for this turn. There may still be some excess units that haven't been
 * requested by any Goal. The AIPlayer may now choose to create a new Goal for these,
 * add them to an existing Goal (which will make use of them next turn),
 * or just keep them unused to have something to deal out next turn.
 */
}
