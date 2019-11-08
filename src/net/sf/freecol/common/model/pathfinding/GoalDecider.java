/**
 *  Copyright (C) 2002-2019   The FreeCol Team
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


package net.sf.freecol.common.model.pathfinding;

import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Unit;


/**
 * Used by {@link net.sf.freecol.common.model.Map#search} in order to
 * determine a goal.
 * 
 * The method {@link #check(Unit, PathNode)} will be called by {@link
 * net.sf.freecol.common.model.Map#search} until:
 * 
 * <ol>
 *   <li>The method returns {@code true} and there is
 *       {@link #hasSubGoals() no sub goals}.</li>
 *   <li>The maximum distance of the search has been reached.</li>
 * </ol>
 * 
 * The method {@link #getGoal()} will get called after this.
 */
public interface GoalDecider {

    /**
     * Gets the {@code PathNode} containing the goal.
     *
     * @return The {@code PathNode} where the {@code Tile}
     *     returned by {@code pathNode.getTile()} is the goal.
     */
    public PathNode getGoal();
    
    /**
     * Determines whether this {@code GoalDecider} has any
     * sub goals.
     * 
     * @return {@code true} if there are any sub goals
     *     and {@code false} otherwise.
     */
    public boolean hasSubGoals();
    
    /**
     * Checks whether the given {@code PathNode} is a
     * goal/sub-goal.
     * 
     * @param u The {@code Unit} which we are trying
     *     to find a path for.
     * @param pathNode The {@code PathNode} where the
     *     {@code Tile} returned by
     *     {@code pathNode.getTile()} is the tile to be checked.
     * @return {@code true} if the {@code PathNode} was
     *     either a goal or a sub goal and {@code false}
     *     otherwise. The goal should be returned by {@link #getGoal()}
     *     right after a call to this method, if this method returns
     *     {@code true} and {@link #hasSubGoals()} returns
     *     {@code false}.
     */
    public boolean check(Unit u, PathNode pathNode);
}
