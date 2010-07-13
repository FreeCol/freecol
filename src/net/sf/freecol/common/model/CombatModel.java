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

package net.sf.freecol.common.model;

import java.util.List;
import java.util.Random;
import java.util.Set;


public interface CombatModel {

    public static enum CombatResultType {
        GREAT_LOSS(false),
        LOSS(false),
        EVADES(false),
        WIN(true),
        GREAT_WIN(true),
        DONE_SETTLEMENT(true);
        
        private final boolean success;
        
        private CombatResultType(boolean success) {
            this.success = success;
        }
        
        /**
         * Returns if this combat result was a success
         * for the attacking unit.
         * 
         * @return <code>true</code> if the attack was a
         *      success and <code>false</code> otherwise.
         */
        public boolean isSuccess() {
            return success;
        }
    }

    public class CombatResult {
        public CombatResultType type;
        public int damage;
        public CombatResult(CombatResultType type, int damage) {
            this.type = type;
            this.damage = damage;
        }
    }

    /**
     * Odds a particular outcome will occur in combat.
     */
    public class CombatOdds {
        public static final float UNKNOWN_ODDS = -1.0f;
        
        public float win;

        public CombatOdds(float win) {
            this.win = win;
        }
    }


    /**
     * Calculates the chance of the outcomes of a combat.
     * 
     * @param attacker The attacker.
     * @param defender The defender.
     * @return The <code>CombatOdds</code>.
     */
    public CombatOdds calculateCombatOdds(FreeColGameObject attacker,
                                          FreeColGameObject defender);

    /**
     * Get the offensive power of a attacker wrt a defender.
     *
     * Null can be passed for the defender when only the attacker
     * stats are required.
     * 
     * @param attacker The attacker.
     * @param defender The defender.
     * @return The offensive power.
     */
    public float getOffencePower(FreeColGameObject attacker,
                                 FreeColGameObject defender);

    /**
     * Get the defensive power of a defender wrt an attacker.
     * 
     * @param attacker The attacker.
     * @param defender The defender.
     * @return The defensive power.
     */
    public float getDefencePower(FreeColGameObject attacker,
                                 FreeColGameObject defender);

    /**
     * Collect all the offensive modifiers that apply to an attack.
     * 
     * Null can be passed as the defender when only the attacker unit
     * stats are required.
     *
     * @param attacker The attacker.
     * @param defender The defender.
     * @return All the applicable offensive modifiers.
     */
    public Set<Modifier> getOffensiveModifiers(FreeColGameObject attacker,
                                               FreeColGameObject defender);

    /**
     * Collect all defensive modifiers that apply to a unit defending
     * against another.
     *
     * @param attacker The attacker.
     * @param defender The defender.
     * @return All the applicable defensive modifiers.
     */
    public Set<Modifier> getDefensiveModifiers(FreeColGameObject attacker,
                                               FreeColGameObject defender);

    /**
     * Generates a result of an attack.
     * To be called by the server only.
     *
     * @param random A pseudo-random number source.
     * @param attacker The attacker.
     * @param defender The defender.
     * @return The results of the combat.
     */
    public List<CombatResult> generateAttackResult(Random random,
                                                   FreeColGameObject attacker,
                                                   FreeColGameObject defender);

    /**
     * Attack a unit with the given outcome.
     * 
     * @param attacker The attacking <code>Unit</code>.
     * @param defender The defending <code>Unit</code>.
     * @param result The result of the attack.
     * @param plunderGold The amount of gold plundered.
     * @param repairLocation A <code>Location</code> to send damaged
     *            naval units.
     */
    public void attack(Unit attacker, Unit defender, CombatResult result,
                       int plunderGold, Location repairLocation);

    /**
     * Bombard a unit with the given outcome.
     * 
     * @param colony The attacking <code>Colony</code>.
     * @param defender The defending <code>Unit</code>.
     * @param result The result of the bombardment.
     * @param repairLocation A <code>Location</code> to send damaged
     *            naval units.
     */
    public void bombard(Colony colony, Unit defender, CombatResult result,
                        Location repairLocation);

}
