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

package net.sf.freecol.server.ai.goal;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Tile;
import static net.sf.freecol.common.util.CollectionUtils.*;
import net.sf.freecol.server.ai.AIPlayer;
import net.sf.freecol.server.ai.AIUnit;


/**
 * This {@link Goal} deals with all missionaries of one {@link AIPlayer}.
 * </p><p>
 * For each missionary unit that is being added, this goal will try to find
 * an {@link IndianSettlement} needing a visit.
 * Distance and reachability from the current position of the unit are
 * taken into account, with the implicit assumption that the current
 * location of the unit is sensible in that a nearby settlement is
 * even worth visiting.
 * Since missionary units are either created in a player-owned colony,
 * or brought there from Europe, this assumption will most often be valid.
 * </p><p>
 * If a settlement has been found, a {@link CreateMissionAtSettlementGoal}
 * will be created, and the unit be moved there.
 */
public class ManageMissionariesGoal extends Goal {

    private static final Logger logger = Logger.getLogger(ManageMissionariesGoal.class.getName());

    //Since all our subgoals are the same, we're keeping them on a simple list
    private final List<Goal> subGoalList;

    public ManageMissionariesGoal(AIPlayer p, Goal g, float w) {
        super(p,g,w);
        subGoalList = new ArrayList<>();
    }

    @Override
    protected Iterator<AIUnit> getOwnedAIUnitsIterator() {
        //we're managing units by directly putting them to individual subgoals,
        //so all our own units at any moment are the unused ones
        return availableUnitsList.iterator();
    }

    @Override
    protected Iterator<Goal> getSubGoalIterator() {
        //all our subgoals are on the subGoalList
        return subGoalList.iterator();
    }

    @Override
    protected void removeUnit(AIUnit u) {
        Iterator<AIUnit> uit = availableUnitsList.iterator();
        while (uit.hasNext()) {
            AIUnit unit = uit.next();
            if (unit.equals(u)) {
                uit.remove();
            }
        }
    }

    /**
     * Plans this goal.
     * NOTE: This goal currently does not send unit requests, but only deals
     * with the units it gets passively.
     */
    @Override
    protected void plan() {
        isFinished = false;

        //cancel already finished subgoals first
        //most of the time, we won't get any units back from this
        Iterator<Goal> git = subGoalList.iterator();
        while (git.hasNext()) {
            Goal g = git.next();
            if (g.isFinished()) {
                List<AIUnit> units = g.cancelGoal();
                availableUnitsList.addAll(units);
                git.remove();
            }
        }

        //check whether our unit references are still valid,
        //so that we can use them in the following step
        validateOwnedUnits();

        //Run through available units. If it's a missionary, create a subgoal
        //for it. If not, return unit to AIPlayer.
        Iterator<AIUnit> uit = availableUnitsList.iterator();
        while (uit.hasNext()) {
            AIUnit u = uit.next();
            uit.remove();

            if ("model.role.missionary".equals(u.getUnit().getRole().getId())) {
                IndianSettlement i = findSettlement(u.getUnit().getTile());
                if (i != null) {
                    PathNode pathNode = u.getUnit().findPath(i.getTile());
                    if (pathNode != null) {
                        logger.info("Creating subgoal CreateMissionAtSettlementGoal.");
                        CreateMissionAtSettlementGoal g = new CreateMissionAtSettlementGoal(player,this,1,u,i);
                        subGoalList.add(g);
                    }
                }
            } else {
                //Setting goal=null will make the unit appear in the unit iterator next turn.
                //FIXME: What about this turn?
                u.setGoal(null);
            }
        }

        if (availableUnitsList.isEmpty() && subGoalList.isEmpty()) {
            //we don't have any units to deal with, and no active subgoals
            //signal that we may safely be cancelled now
            isFinished = true;
        } else {
            //set subgoal weights in case their number has changed
            float newWeight = 1f/subGoalList.size();
            git = subGoalList.iterator();
            while (git.hasNext()) {
                Goal g = git.next();
                g.setWeight(newWeight);
            }
        }
    }

    @Override
    public String getGoalDescription() {
        String descr = super.getGoalDescription();
        return descr + ":" + availableUnitsList.size();
    }

/* INTERNAL *******************************************************************/


    private IndianSettlement findSettlement(Tile tile) {
        return (tile == null)
            //FIXME: We're in europe - let's deal with it.
            ? null
            //Possible FIXME: Slightly randomize findings?
            //Otherwise, missionaries starting from the same position will find
            //the same settlement.
            : find(map(tile.getSurroundingTiles(1, MAX_SEARCH_RADIUS),
                    t -> t.getIndianSettlement()),
                is -> is != null && !is.hasMissionary(player.getPlayer()),
                null);
    }


    @Override
    public void toXML(FreeColXMLWriter xw) throws XMLStreamException {
        //FIXME
    }

    @Override
    public void readFromXML(FreeColXMLReader xr) throws XMLStreamException {
        //FIXME
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getXMLTagName() { return getXMLElementTagName(); }
}
