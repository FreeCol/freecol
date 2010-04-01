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
        // public float evade;
        // public float capture;
        // public float demote;
        // public float promote;
        public CombatOdds(float win) {
            this.win = win;
        }
    }


    /**
     * Calculates the chance of the outcomes of combat between units.
     * 
     * @param attacker The attacking <code>Unit</code>.
     * @param defender The defending <code>Unit</code>.
     * @return The <code>CombatOdds</code>.
     */
    public CombatOdds calculateCombatOdds(Unit attacker, Unit defender);

    /**
     * Calculates the chance of the outcomes of a colony bombarding a unit.
     *
     * @param attacker The attacking <code>Colony</code>.
     * @param defender The defending <code>Unit</code>.
     * @return The <code>CombatOdds</code>.
     */
    public CombatOdds calculateCombatOdds(Colony attacker, Unit defender);


    /**
     * Generates a result of a unit attacking another.
     *
     * @param attacker The attacking <code>Unit</code>.
     * @param defender The defending <code>Unit</code>.
     * @return The <code>CombatResult</code>.
     */
    public CombatResult generateAttackResult(Unit attacker, Unit defender);

    /**
     * Generates the result of a colony bombarding a unit.
     *
     * @param attacker The attacking <code>Colony</code>.
     * @param defender The defending <code>Unit</code>.
     * @return The <code>CombatResult</code>.
     */
    public CombatResult generateAttackResult(Colony attacker, Unit defender);


    /**
     * Get the offensive power of a unit attacking another.
     *
     * Null can be passed for the defender when only the attacker unit
     * stats are required.
     * 
     * @param attacker The attacking <code>Unit</code>.
     * @param defender The defending <code>Unit</code>.
     * @return The offensive power.
     */
    public float getOffencePower(Unit attacker, Unit defender);

    /**
     * Get the offensive power of a colony bombarding a unit.
     * 
     * @param attacker The attacking <code>Colony</code>.
     * @param defender The defending <code>Unit</code>.
     * @return The offensive power.
     */
    public float getOffencePower(Colony attacker, Unit defender);

    /**
     * Get the defensive power of a unit defending against another.
     * 
     * @param attacker The attacking <code>Unit</code>.
     * @param defender The defending <code>Unit</code>.
     * @return The defensive power.
     */
    public float getDefencePower(Unit attacker, Unit defender);

    /**
     * Get the defensive power of a unit defending against a colony.
     * 
     * @param attacker The attacking <code>Colony</code>.
     * @param defender The defending <code>Unit</code>.
     * @return The defensive power.
     */
    public float getDefencePower(Colony attacker, Unit defender);


    /**
     * Collect all the offensive modifiers that apply to a unit
     * attacking another.
     * 
     * Null can be passed as the defender when only the attacker unit
     * stats are required.
     *
     * @param attacker The attacking <code>Unit</code>.
     * @param defender The defending <code>Unit</code>.
     * @return All the applicable offensive modifiers.
     */
    public Set<Modifier> getOffensiveModifiers(Unit attacker, Unit defender);

    /**
     * Collect all the offensive modifiers that apply to a colony
     * bombarding a unit.
     * 
     * @param attacker The attacking <code>Colony</code>.
     * @param defender The defending <code>Unit</code>.
     * @return All the applicable offensive modifiers.
     */
    public Set<Modifier> getOffensiveModifiers(Colony attacker, Unit defender);

    /**
     * Collect all defensive modifiers that apply to a unit defending
     * against another.
     *
     * @param attacker The attacking <code>Unit</code>.
     * @param defender The defending <code>Unit</code>.
     * @return All the applicable defensive modifiers.
     */
    public Set<Modifier> getDefensiveModifiers(Unit attacker, Unit defender);

    /**
     * Collect all defensive modifiers that apply to a unit defending
     * against a colony.
     * 
     * @param attacker The attacking <code>Colony</code>.
     * @param defender The defending <code>Unit</code>.
     * @return All the applicable defensive modifiers.
     */
    public Set<Modifier> getDefensiveModifiers(Colony attacker, Unit defender);


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
