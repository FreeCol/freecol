/**
 *  Copyright (C) 2002-2011  The FreeCol Team
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
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.common.io.sza.SimpleZippedAnimation;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Unit.Role;
import net.sf.freecol.common.resources.ResourceManager;


/**
 * Class for the animation of units attacks.
 */
final class UnitAttackAnimation {

    private final Canvas canvas;
    private final Unit attacker;
    private final Unit defender;
    private final boolean success;
    private GUI gui;
    private FreeColClient freeColClient;

    /**
     * Build a new attack animation.
     *
     * @param canvas The <code>Canvas</code> to draw the animation on.
     * @param attacker The <code>Unit</code> that is attacking.
     * @param defender The <code>Unit</code> that is defending.
     * @param success Does the attack succeed?
     */
    public UnitAttackAnimation(FreeColClient freeColClient, GUI gui, Canvas canvas, Unit attacker, Unit defender,
                               boolean success) {
        this.freeColClient = freeColClient;
        this.gui = gui;
        this.canvas = canvas;
        this.attacker = attacker;
        this.defender = defender;
        this.success = success;
    }

    /**
     * Find the animation for a unit attack.
     *
     * @param canvas The <code>Canvas</code> to draw the animation on.
     * @param unit The <code>Unit</code> to animate.
     * @param direction The <code>Direction</code> of the attack.
     * @return An animation, if available.
     */
    private SimpleZippedAnimation getAnimation(Unit unit,
                                               Direction direction) {
        float scale = gui.getMapScale();
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
        Direction direction = attacker.getTile()
            .getDirection(defender.getTile());
        SimpleZippedAnimation sza;

        if (Animations.getAnimationSpeed(freeColClient, attacker) > 0) {
            if ((sza = getAnimation(attacker, direction)) != null) {
                new UnitImageAnimation(gui, canvas, attacker, sza).animate();
            }
        }

        if (!success && Animations.getAnimationSpeed(freeColClient, defender) > 0) {
            direction = direction.getReverseDirection();
            if ((sza = getAnimation(defender, direction)) != null) {
                new UnitImageAnimation(gui, canvas, defender, sza).animate();
            }
        }
    }
}
