/**
 *  Copyright (C) 2002-2024   The FreeCol Team
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

package net.sf.freecol.common.model;

import net.sf.freecol.common.i18n.Messages;
import static net.sf.freecol.common.util.StringUtils.*;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;


/**
 * The stance one player has towards another player.
 *   Valid transitions:
 *
 *   [FROM] \  [TO]  U  A  P  C  W        Reasons
 *   ----------------------------------   a = attack
 *   UNCONTACTED  |  -  x  c  x  i    |   c = contact
 *   ALLIANCE     |  x  -  d  x  adit |   d = act of diplomacy
 *   PEACE        |  x  d  -  x  adit |   i = incitement/rebellion
 *   CEASE_FIRE   |  x  d  t  -  adit |   t = change of tension
 *   WAR          |  x  d  ds dt -    |   s = surrender
 *   ----------------------------------   x = invalid
 */
public enum Stance implements Named {
    UNCONTACTED,
    ALLIANCE,
    PEACE,
    CEASE_FIRE,
    WAR;


    // Helpers to enforce valid transitions
    private void badStance() {
    }
    private void badTransition(Stance newStance) {
        throw new RuntimeException("Bad transition: " + this
            + " -> " + newStance);
    }

    /**
     * Check whether tension has changed enough to merit a stance
     * change.  Do not simply check for crossing tension
     * thresholds, add in Tension.DELTA to provide a bit of
     * hysteresis to dampen ringing.
     *
     * @param tension The {@code Tension} to check.
     * @return The {@code Stance} appropriate to the tension level.
     */
    public Stance getStanceFromTension(Tension tension) {
        int value = tension.getValue();
        switch (this) {
        case WAR: // Cease fire if tension decreases
            if (value <= Tension.Level.CONTENT.getLimit()-Tension.DELTA) {
                return Stance.CEASE_FIRE;
            }
            break;
        case CEASE_FIRE: // Peace if tension decreases
            if (value <= Tension.Level.HAPPY.getLimit()-Tension.DELTA) {
                return Stance.PEACE;
            }
            // Fall through
        case ALLIANCE: case PEACE: // War if tension increases
            if (value > Tension.Level.HATEFUL.getLimit()+Tension.DELTA) {
                return Stance.WAR;
            }
            break;
        case UNCONTACTED:
            break;
        default:
            this.badStance();
        }
        return this;
    }

    /**
     * Get the tension modifier for a transition to a new stance.
     *
     * @param newStance The new {@code Stance}.
     * @return The tension modifier value.
     */
    @SuppressFBWarnings(value="SF_SWITCH_FALLTHROUGH")
    public int getTensionModifier(Stance newStance) {
        if (this == newStance) return 0;
        if (!isValidTransition(newStance)) badTransition(newStance);

        switch (newStance) {
        case ALLIANCE:
            if (this == PEACE) return Tension.ALLIANCE_MODIFIER;
            if (this == CEASE_FIRE) return Tension.ALLIANCE_MODIFIER + Tension.PEACE_TREATY_MODIFIER;
            return Tension.ALLIANCE_MODIFIER + Tension.CEASE_FIRE_MODIFIER + Tension.PEACE_TREATY_MODIFIER;
        case PEACE:
            if (this == UNCONTACTED) return Tension.CONTACT_MODIFIER;
            if (this == ALLIANCE) return Tension.DROP_ALLIANCE_MODIFIER;
            if (this == CEASE_FIRE) return Tension.PEACE_TREATY_MODIFIER;
            return Tension.CEASE_FIRE_MODIFIER + Tension.PEACE_TREATY_MODIFIER;
        case CEASE_FIRE:
            return Tension.CEASE_FIRE_MODIFIER;
        case WAR:
            return (this == CEASE_FIRE) ? Tension.RESUME_WAR_MODIFIER : Tension.WAR_MODIFIER;
        default:
            throw new RuntimeException("Bogus newStance: " + newStance);
        }
    }

    /**
     * Is this stance one that can be incited to war?
     *
     * @return True if this stance is incitable.
     */
    public boolean isIncitable() {
        switch (this) {
        case CEASE_FIRE: case PEACE: return true;
        default: break;
        }
        return false;
    }

    /**
     * Evaluate the value of this stance for a trade.
     *
     * @param ratio The strength ratio between the players.
     * @return The value of the stance change.
     */
    public int getTradeValue(double ratio) {
        int value = (int)Math.round(100 * ratio);
        switch (this) {
        case WAR:
            if (ratio < 0.33) return -1000; // INVALID_TRADE_ITEM
            return (ratio < 0.5) ? -value : value;
        case PEACE: case CEASE_FIRE: case ALLIANCE:
            if (ratio > 0.66) return -1000; // INVALID_TRADE_ITEM
            if (ratio > 0.5) return -value;
            return (ratio < 0.33) ? 1000 : value;
        default:
            return -1000; // INVALID_TRADE_ITEM
        }
    }

    /**
     * Determine if a transition to a new stance is valid.
     *
     * @param newStance The potential new {@code Stance}.
     * @return True if the transition is allowed.
     */
    public boolean isValidTransition(Stance newStance) {
        if (this == newStance) return true;
        switch (this) {
        case UNCONTACTED:
            return newStance == PEACE || newStance == WAR;
        case ALLIANCE:
            return newStance == PEACE || newStance == WAR;
        case PEACE:
            return newStance == ALLIANCE || newStance == WAR;
        case CEASE_FIRE:
            return newStance == ALLIANCE || newStance == PEACE || newStance == WAR;
        case WAR:
            return newStance == ALLIANCE || newStance == PEACE || newStance == CEASE_FIRE;
        default:
            return false;
        }
    }

    /**
     * Get the stem key.
     *
     * @return The stance stem key.
     */
    public String getKey() {
        return "stance." + getEnumKey(this);
    }

    /**
     * Get the message key to use for player messages when stance changes.
     *
     * @return A suitable message key.
     */
    public String getStanceChangeKey() {
        return "model.player." + getKey() + ".declared";
    }
    
    /**
     * Get the message key to use for player messages when stance
     * changes between other players.
     *
     * @return A suitable message key.
     */
    public String getOtherStanceChangeKey() {
        return "model.player." + getKey() + ".others";
    }
    
    // Implement Named

    /**
     * {@inheritDoc}
     */
    public String getNameKey() {
        return Messages.nameKey("model." + getKey());
    }
}
