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
import net.sf.freecol.common.io.sza.SimpleZippedAnimation;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Role;
import net.sf.freecol.common.resources.ResourceManager;


/**
 * Class for the animation of units attacks.
 */
final class UnitAttackAnimation {

    private final Unit attacker;
    private final Unit defender;
    private final Tile attackerTile;
    private final Tile defenderTile;
    private final boolean success;
    private GUI gui;


    /**
     * Build a new attack animation.
     *
     * @param gui The <code>GUI</code> to display on.
     * @param attacker The <code>Unit</code> that is attacking.
     * @param defender The <code>Unit</code> that is defending.
     * @param attackerTile The <code>Tile</code> the attack comes from.
     * @param defenderTile The <code>Tile</code> the attack goes to.
     * @param success Does the attack succeed?
     */
    public UnitAttackAnimation(GUI gui, Unit attacker, Unit defender,
                               Tile attackerTile, Tile defenderTile,
                               boolean success) {
        this.gui = gui;
        this.attacker = attacker;
        this.defender = defender;
        this.attackerTile = attackerTile;
        this.defenderTile = defenderTile;
        this.success = success;
    }

    /**
     * Find the animation for a unit attack.
     *
     * @param unit The <code>Unit</code> to animate.
     * @param direction The <code>Direction</code> of the attack.
     * @return An animation, if available.
     */
    private SimpleZippedAnimation getAnimation(Unit unit,
                                               Direction direction) {
        float scale = gui.getMapScale();
        String roleStr = ("model.role.default".equals(unit.getRole().getId()))
            ? ""
            : "." + unit.getRoleSuffix();
        String startStr = unit.getType().getId() + roleStr + ".attack.";
        String specialId = startStr + direction.toString().toLowerCase()
            + ".animation";

        SimpleZippedAnimation sza;
        sza = ResourceManager.getSimpleZippedAnimation(specialId, scale);
        if (sza == null) {
            String genericDirection;
            switch (direction) {
            case SW: case W: case NW: genericDirection = "w"; break;
            default:                  genericDirection = "e"; break;
            }
            String genericId = startStr + genericDirection + ".animation";
            sza = ResourceManager.getSimpleZippedAnimation(genericId, scale);
        }
        return sza;
    }

    /**
     * Do the animation.
     */
    public void animate() {
        Direction direction = attackerTile.getDirection(defenderTile);
        SimpleZippedAnimation sza;

        if (gui.getAnimationSpeed(attacker) > 0) {
            if ((sza = getAnimation(attacker, direction)) != null) {
                new UnitImageAnimation(gui, attacker, attackerTile, sza)
                    .animate();
            }
        }

        if (!success
            && gui.getAnimationSpeed(defender) > 0) {
            direction = direction.getReverseDirection();
            if ((sza = getAnimation(defender, direction)) != null) {
                new UnitImageAnimation(gui, defender, defenderTile, sza)
                    .animate();
            }
        }
    }
}
