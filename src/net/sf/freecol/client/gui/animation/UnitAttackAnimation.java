/**
 *  Copyright (C) 2002-2007  The FreeCol Team
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

import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.common.io.sza.SimpleZippedAnimation;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.CombatModel.CombatResultType;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.Unit.Role;
import net.sf.freecol.common.resources.ResourceManager;


/**
 * Class for the animation of units attacks.
 */
final class UnitAttackAnimation {

    private final Canvas canvas;
    private final Unit attacker;
    private final Unit defender;
    private final CombatResultType result;

    /**
     * Build a new attack animation.
     *
     * @param canvas The <code>Canvas</code> to draw the animation on.
     * @param attacker The <code>Unit</code> that is attacking.
     * @param defender The <code>Unit</code> that is defending.
     * @param result The <code>CombatResultType</code> to animate.
     */
    public UnitAttackAnimation(Canvas canvas, Unit attacker, Unit defender,
                               CombatResultType result) {
        this.canvas = canvas;
        this.attacker = attacker;
        this.defender = defender;
        this.result = result;
    }

    /**
     * Find the animation for a unit attack.
     *
     * @param canvas The <code>Canvas</code> to draw the animation on.
     * @param unit The <code>Unit</code> to animate.
     * @param direction The <code>Direction</code> of the attack.
     * @return An animation, if available.
     */
    private SimpleZippedAnimation getAnimation(Canvas canvas, Unit unit,
                                               Direction direction) {
        float scale = canvas.getGUI().getMapScale();
        String roleStr = (unit.getRole() == Role.DEFAULT) ? ""
            : "." + unit.getRole().getId();
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
        Map map = attacker.getGame().getMap();
        Direction direction = map.getDirection(attacker.getTile(),
                                               defender.getTile());
        SimpleZippedAnimation sza;

        if (Animations.getAnimationSpeed(canvas, attacker) > 0) {
            if ((sza = getAnimation(canvas, attacker, direction)) != null) {
                new UnitImageAnimation(canvas, attacker, sza).animate();
            }
        }

        if (!result.isSuccess()
            && Animations.getAnimationSpeed(canvas, defender) > 0) {
            direction = direction.getReverseDirection();
            if ((sza = getAnimation(canvas, defender, direction)) != null) {
                new UnitImageAnimation(canvas, defender, sza).animate();
            }
        }
    }
}
