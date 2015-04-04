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

import java.util.Iterator;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit.MoveType;
import net.sf.freecol.server.ai.AIPlayer;
import net.sf.freecol.server.ai.AIUnit;


/**
 * A goal to make a move to a tile adjacent to a target tile.
 */
public class GotoAdjacentGoal extends Goal {

    private static final Logger logger = Logger.getLogger(GotoAdjacentGoal.class.getName());

    //the destination tile
    private final Tile target;

    public GotoAdjacentGoal(AIPlayer p, Goal g, float w, AIUnit u, Tile t) {
        super(p,g,w,u);
        target = t;
    }

    @Override
    protected Iterator<AIUnit> getOwnedAIUnitsIterator() {
        //we're keeping units on the availableUnitsList,
        //so all our own units at any moment are these
        return availableUnitsList.iterator();
    }

    @Override
    protected Iterator<Goal> getSubGoalIterator() {
        return null;
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
    
    @Override
    protected void plan() {
        isFinished = false;
        
        //Run through available units. For each unit, find a path to the
        //target and move towards it. Return to parent if adjacent to target,
        //or no path can be found.
        Iterator<AIUnit> uit = availableUnitsList.iterator();
        nextUnit: while (uit.hasNext()) {
            AIUnit u = uit.next();

            PathNode pathNode = u.getUnit().findPath(target);
            if (pathNode==null) {
                uit.remove();
                addUnitToParent(u);            
            } else {
                while (pathNode.next != null 
                        && pathNode.getTurns() == 0
                        && pathNode.getTile() != target
                        && (u.getUnit().getMoveType(pathNode.getDirection()) == MoveType.MOVE
                          ||u.getUnit().getMoveType(pathNode.getDirection()) == MoveType.EXPLORE_LOST_CITY_RUMOUR)) {
                        
                            if(u.getUnit().getMoveType(pathNode.getDirection()) == MoveType.EXPLORE_LOST_CITY_RUMOUR) {
                                logger.warning("Accidental rumour exploration!");
                            }
                        
                            u.move(pathNode.getDirection());
                            if (u.getUnit() == null
                                || u.getUnit().isDisposed()) {
                                continue nextUnit;
                            }
                            pathNode = pathNode.next;
                }
                if (u.getUnit().getTile().isAdjacent(target)) {
                    //If unit is adjacent after moving, return to parent
                    uit.remove();
                    addUnitToParent(u);
                }
            }
        }
        
        if (availableUnitsList.isEmpty()) {
            //we don't have any units left to deal with,
            //signal that we may safely be cancelled now
            isFinished = true;
        }
    }

    @Override
    public String getGoalDescription() {
        String descr = super.getGoalDescription();
        if (target!=null) {
            descr += ":"+target.getX()+","+target.getY();
        } else {
            descr += ":null";
        }
        return descr;
    }
    

    @Override
    public void toXML(FreeColXMLWriter xw) throws XMLStreamException {
    }
    
    @Override
    public void readFromXML(FreeColXMLReader xr) throws XMLStreamException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getXMLTagName() { return getXMLElementTagName(); }
}
