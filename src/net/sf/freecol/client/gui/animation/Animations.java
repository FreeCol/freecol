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

package net.sf.freecol.client.gui.animation;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;


/**
 * A facade for animations. 
 */
public class Animations {

    /**
     * Animates a unit move.
     *
     * @param freeColClient The enclosing {@code FreeColClient}.
     * @param unit The {@code Unit} to be animated.
     * @param source The source {@code Tile} for the unit.
     * @param destination The destination {@code Tile} for the unit.
     * @return False if there was a problem.
     */
    public static boolean unitMove(FreeColClient freeColClient, Unit unit,
                                   Tile source, Tile destination) {
        return new UnitMoveAnimation(freeColClient, unit, source, destination)
            .animate();
    }
    
    /**
     * Animates a unit attack.
     * 
     * @param freeColClient The enclosing {@code FreeColClient}.
     * @param attacker The {@code Unit} that is attacking.
     * @param defender The {@code Unit} that is defending.
     * @param attackerTile The {@code Tile} the attack comes from.
     * @param defenderTile The {@code Tile} the attack goes to.
     * @param success Did the attack succeed?
     * @return False if there was a problem.
     */
    public static boolean unitAttack(FreeColClient freeColClient,
                                     Unit attacker, Unit defender,
                                     Tile attackerTile, Tile defenderTile,
                                     boolean success) {
        return new UnitAttackAnimation(freeColClient, attacker, defender,
                                       attackerTile, defenderTile, success)
            .animate();
    }
}
