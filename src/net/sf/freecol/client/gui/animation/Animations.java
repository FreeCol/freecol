/**
 *  Copyright (C) 2002-2013   The FreeCol Team
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

package net.sf.freecol.client.gui.animation;

import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;


/**
 * A facade for animations. 
 */
public class Animations {

    /**
     * Animates a unit move.
     *
     * @param gui The <code>GUI</code> to display on.
     * @param unit The <code>Unit</code> to be animated.
     * @param source The source <code>Tile</code> for the unit.
     * @param destination The destination <code>Tile</code> for the unit.
     */
    public static void unitMove(GUI gui, Unit unit,
                                Tile source, Tile destination) {
        new UnitMoveAnimation(gui, unit, source, destination)
            .animate();
    }
    
    /**
     * Animates a unit attack.
     * 
     * @param gui The <code>GUI</code> to display on.
     * @param attacker The <code>Unit</code> that is attacking.
     * @param defender The <code>Unit</code> that is defending.
     * @param attackerTile The <code>Tile</code> the attack comes from.
     * @param defenderTile The <code>Tile</code> the attack goes to.
     * @param success Did the attack succeed?
     */
    public static void unitAttack(GUI gui,
                                  Unit attacker, Unit defender,
                                  Tile attackerTile, Tile defenderTile,
                                  boolean success) {
        new UnitAttackAnimation(gui, attacker, defender,
                                attackerTile, defenderTile, success)
            .animate();
    }

}
