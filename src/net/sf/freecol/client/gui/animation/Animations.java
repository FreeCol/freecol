/**
 *  Copyright (C) 2002-2022   The FreeCol Team
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

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.common.model.Direction;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;


/**
 * A facade for animations. 
 */
public class Animations {

    private static final Logger logger = Logger.getLogger(Animations.class.getName());

    /**
     * Trivial wrapper for zero-argument-zero-return function, 
     * used as a callback for an animation to trigger painting.
     */
    public interface Procedure {
        public void execute();
    };
    
     /**
     * Collect animations for a unit move.
     *
     * @param freeColClient The enclosing {@code FreeColClient}.
     * @param unit The {@code Unit} to be animated.
     * @param source The source {@code Tile} for the unit.
     * @param destination The destination {@code Tile} for the unit.
     * @param scale The scale factor for the unit image.
     * @return A list of {@code Animation}s.
     */
    public static List<Animation> unitMove(FreeColClient freeColClient,
                                           Unit unit,
                                           Tile source, Tile destination,
                                           float scale) {
        List<Animation> ret = new ArrayList<>();
        int speed = freeColClient.getAnimationSpeed(unit.getOwner());
        if (speed > 0) {
            UnitMoveAnimation a
                = new UnitMoveAnimation(unit,
                    source, destination, speed, scale);
            if (a == null) {
                logger.warning("No move animation for: " + unit);
            } else {
                ret.add(a);
            }
        }
        return ret;
    }
    
    /**
     * Get the base resource identifier for an attack animation.
     *
     * @param unit The attacking {@code Unit}.
     * @return The resource base exclusive of direction.
     */
    private static String getAttackAnimationBase(Unit unit) {
        String roleStr = (unit.hasDefaultRole()) ? ""
            : "." + unit.getRoleSuffix();
        return "animation.unit." + unit.getType().getId() + roleStr
            + ".attack.";
    }
        
    /**
     * Collection animations for a unit attack.
     * 
     * @param freeColClient The enclosing {@code FreeColClient}.
     * @param attacker The {@code Unit} that is attacking.
     * @param defender The {@code Unit} that is defending.
     * @param attackerTile The {@code Tile} the attack comes from.
     * @param defenderTile The {@code Tile} the attack goes to.
     * @param success Did the attack succeed?
     * @param scale The scale factor for the unit image.
     * @return A list of {@code Animation}s.
     */
    public static List<Animation> unitAttack(FreeColClient freeColClient,
        Unit attacker, Unit defender,
        Tile attackerTile, Tile defenderTile,
        boolean success, float scale) {
        List<Animation> ret = new ArrayList<>();
        Direction dirn = attackerTile.getDirection(defenderTile);
        if (dirn == null) {
            logger.warning("Attack animation null direction: " + attacker
                + " v " + defender);
            return ret; // Fail fast
        }

        if (freeColClient.getAnimationSpeed(attacker.getOwner()) > 0) {
            UnitImageAnimation a
                = UnitImageAnimation.build(attacker, attackerTile, dirn,
                    getAttackAnimationBase(attacker), scale);
            if (a == null) {
                logger.warning("No attack animation for: "
                    + attacker + " (" + dirn + ")");
            } else {
                ret.add(a);
            }
        }

        if (!success
            && freeColClient.getAnimationSpeed(defender.getOwner()) > 0) {
            Direction revd = dirn.getReverseDirection();
            UnitImageAnimation a
                = UnitImageAnimation.build(defender, defenderTile, revd,
                    getAttackAnimationBase(defender), scale);
            if (a == null) {
                logger.warning("No attack animation for: "
                    + defender + " (" + revd + ")");
            } else {
                ret.add(a);
            }
        }

        return ret;
    }
}
