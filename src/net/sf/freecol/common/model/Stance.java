/**
 *  Copyright (C) 2002-2021   The FreeCol Team
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
import net.sf.freecol.common.model.Named;
import net.sf.freecol.common.model.Tension;
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
     * A stance change is about to happen.  Get the appropriate tension
     * modifier.
     *
     * @param newStance The new {@code Stance}.
     * @return A modifier to the current tension.
     */
    @SuppressFBWarnings(value="SF_SWITCH_FALLTHROUGH")
    public int getTensionModifier(Stance newStance) {
        switch (newStance) {
        case UNCONTACTED:     badTransition(newStance);
            break;
        case ALLIANCE:
            switch (this) {
            case UNCONTACTED: badTransition(newStance);
            case ALLIANCE:    return 0;
            case PEACE:       return Tension.ALLIANCE_MODIFIER;
            case CEASE_FIRE:  return Tension.ALLIANCE_MODIFIER + Tension.PEACE_TREATY_MODIFIER;
            case WAR:         return Tension.ALLIANCE_MODIFIER + Tension.CEASE_FIRE_MODIFIER + Tension.PEACE_TREATY_MODIFIER;
            default:          break;
            }
            break;
        case PEACE:
            switch (this) {
            case UNCONTACTED: return Tension.CONTACT_MODIFIER;
            case ALLIANCE:    return Tension.DROP_ALLIANCE_MODIFIER;
            case PEACE:       return 0;
            case CEASE_FIRE:  return Tension.PEACE_TREATY_MODIFIER;
            case WAR:         return Tension.CEASE_FIRE_MODIFIER + Tension.PEACE_TREATY_MODIFIER;
            default:          break;
            }
            break;
        case CEASE_FIRE:
            switch (this) {
            case UNCONTACTED: badTransition(newStance);
            case ALLIANCE:    badTransition(newStance);
            case PEACE:       badTransition(newStance);
            case CEASE_FIRE:  return 0;
            case WAR:         return Tension.CEASE_FIRE_MODIFIER;
            default:          break;
            }
            break;
        case WAR:
            switch (this) {
            case UNCONTACTED: return Tension.WAR_MODIFIER;
            case ALLIANCE:    return Tension.WAR_MODIFIER;
            case PEACE:       return Tension.WAR_MODIFIER;
            case CEASE_FIRE:  return Tension.RESUME_WAR_MODIFIER;
            case WAR:         return 0;
            default:          break;
            }
            break;
        default:
            throw new RuntimeException("Bogus newStance: " + newStance);
        }
        throw new RuntimeException("Bogus stance: " + this);
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
