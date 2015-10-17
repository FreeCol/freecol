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

package net.sf.freecol.client.gui.animation;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.SwingGUI;
import net.sf.freecol.common.io.sza.SimpleZippedAnimation;
import net.sf.freecol.common.model.Direction;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.resources.ResourceManager;


/**
 * Class for the animation of units attacks.
 */
final class UnitAttackAnimation {

    private final FreeColClient freeColClient;
    private final Unit attacker;
    private final Unit defender;
    private final Tile attackerTile;
    private final Tile defenderTile;
    private final boolean success;
    private boolean mirror = false;

    /**
     * Build a new attack animation.
     *
     * @param freeColClient The enclosing <code>FreeColClient</code>.
     * @param attacker The <code>Unit</code> that is attacking.
     * @param defender The <code>Unit</code> that is defending.
     * @param attackerTile The <code>Tile</code> the attack comes from.
     * @param defenderTile The <code>Tile</code> the attack goes to.
     * @param success Does the attack succeed?
     */
    public UnitAttackAnimation(FreeColClient freeColClient,
                               Unit attacker, Unit defender,
                               Tile attackerTile, Tile defenderTile,
                               boolean success) {
        this.freeColClient = freeColClient;
        this.attacker = attacker;
        this.defender = defender;
        this.attackerTile = attackerTile;
        this.defenderTile = defenderTile;
        this.success = success;
    }

    private SimpleZippedAnimation getAnimation(String startStr,
                                               float scale,
                                               Direction direction) {
        SimpleZippedAnimation sza;
        String specialId = startStr + direction.toString().toLowerCase();
        sza = ResourceManager.getSimpleZippedAnimation(specialId, scale);
        if(sza != null) {
            mirror = false;
            return sza;
        }

        specialId = startStr + direction.getEWMirroredDirection().toString().toLowerCase();
        sza = ResourceManager.getSimpleZippedAnimation(specialId, scale);
        if(sza != null) {
            mirror = true;
            return sza;
        }
        return null;
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
        float scale = ((SwingGUI)freeColClient.getGUI()).getMapScale();
        String roleStr = (unit.hasDefaultRole()) ? ""
            : "." + unit.getRoleSuffix();
        String startStr = "animation.unit." + unit.getType().getId() + roleStr
                        + ".attack.";

        SimpleZippedAnimation sza;
        sza = getAnimation(startStr, scale, direction);
        if(sza != null) return sza;

        sza = getAnimation(startStr, scale, direction.getNextDirection());
        if(sza != null) return sza;
        sza = getAnimation(startStr, scale, direction.getPreviousDirection());
        if(sza != null) return sza;

        sza = getAnimation(startStr, scale, direction.getNextDirection()
                                                     .getNextDirection());
        if(sza != null) return sza;
        sza = getAnimation(startStr, scale, direction.getPreviousDirection()
                                                     .getPreviousDirection());
        if(sza != null) return sza;

        sza = getAnimation(startStr, scale, direction.getNextDirection()
                                                     .getNextDirection()
                                                     .getNextDirection());
        if(sza != null) return sza;
        sza = getAnimation(startStr, scale, direction.getPreviousDirection()
                                                     .getPreviousDirection()
                                                     .getPreviousDirection());
        if(sza != null) return sza;

        sza = getAnimation(startStr, scale, direction.getReverseDirection());
        return sza;
    }

    /**
     * Do the animation.
     */
    public void animate() {
        final SwingGUI gui = (SwingGUI)freeColClient.getGUI();
        Direction direction = attackerTile.getDirection(defenderTile);
        SimpleZippedAnimation sza;

        if (freeColClient.getAnimationSpeed(attacker.getOwner()) > 0) {
            if ((sza = getAnimation(attacker, direction)) != null) {
                new UnitImageAnimation(gui, attacker, attackerTile, sza, mirror)
                    .animate();
            }
        }

        if (!success
            && freeColClient.getAnimationSpeed(defender.getOwner()) > 0) {
            direction = direction.getReverseDirection();
            if ((sza = getAnimation(defender, direction)) != null) {
                new UnitImageAnimation(gui, defender, defenderTile, sza, mirror)
                    .animate();
            }
        }
    }
}
