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
        GREAT_LOSS, LOSS, EVADES, WIN,
            GREAT_WIN, DONE_SETTLEMENT };

    public class CombatResult {
        public CombatResultType type;
        public int damage;
        public CombatResult(CombatResultType type, int damage) {
            this.type = type;
            this.damage = damage;
        }
    }

    /**
     * Generates a result of an attack.
     * 
     * @param attacker The <code>Unit</code> attacking.
     * @param defender The defending unit.
     * @return a <code>CombatResult</code> value
     */
    public CombatResult generateAttackResult(Unit attacker, Unit defender);

    /**
     * Generates the result of a colony bombarding a Unit.
     *
     * @param colony the bombarding <code>Colony</code>
     * @param defender the defending <code>Unit</code>
     * @return a <code>CombatResult</code> value
     */
    public CombatResult generateAttackResult(Colony colony, Unit defender);

    /**
     * Returns the power for bombarding
     * 
     * @param colony a <code>Colony</code> value
     * @param defender an <code>Unit</code> value
     * @return the power for bombarding
     */
    public float getOffencePower(Colony colony, Unit defender);

    /**
     * Return the offensive power of the attacker versus the defender.
     * 
     * @param attacker an <code>Unit</code> value
     * @param defender an <code>Unit</code> value
     * @return a <code>float</code> value
     */
    public float getOffencePower(Unit attacker, Unit defender);

    /**
     * Return the defensive power of the defender versus the
     * bombarding colony.
     * 
     * @param colony a <code>Colony</code> value
     * @param defender a <code>Unit</code> value
     * @return an <code>float</code> value
     */
    public float getDefencePower(Colony colony, Unit defender);

    /**
     * Return the defensive power of the defender versus the attacker.
     * 
     * @param attacker an <code>Unit</code> value
     * @param defender an <code>Unit</code> value
     * @return an <code>float</code> value
     */
    public float getDefencePower(Unit attacker, Unit defender);

    /**
     * Return a list of all offensive modifiers that apply to the attacker
     * versus the defender.
     * 
     * @param colony an <code>Colony</code> value
     * @param defender an <code>Unit</code> value
     * @return a <code>List</code> of Modifiers
     */
    public Set<Modifier> getOffensiveModifiers(Colony colony, Unit defender);

    /**
     * Return a list of all offensive modifiers that apply to the attacker
     * versus the defender.
     * 
     * @param attacker an <code>Unit</code> value
     * @param defender an <code>Unit</code> value
     * @return a <code>List</code> of Modifiers
     */
    public Set<Modifier> getOffensiveModifiers(Unit attacker, Unit defender);

    /**
     * Return a list of all defensive modifiers that apply to the defender
     * versus the attacker.
     * 
     * @param attacker an <code>Unit</code> value
     * @param defender an <code>Unit</code> value
     * @return a <code>List</code> of Modifiers
     */
    public Set<Modifier> getDefensiveModifiers(Unit attacker, Unit defender);

    /**
     * Return a list of all defensive modifiers that apply to the defender
     * versus the bombarding colony.
     * 
     * @param colony a <code>Colony</code> value
     * @param defender an <code>Unit</code> value
     * @return a <code>List</code> of Modifiers
     */
    public Set<Modifier> getDefensiveModifiers(Colony colony, Unit defender);

    /**
     * Return the defensive modifier that applies to defenders in the given
     * settlement versus the attacker.
     * 
     * @param attacker an <code>Unit</code> value
     * @param settlement a <code>Settlement</code> value
     * @return a <code>Modifier</code>
     */
    public Modifier getSettlementModifier(Unit attacker, Settlement settlement);

    /**
     * Attack a unit with the given outcome.
     * 
     * @param attacker an <code>Unit</code> value
     * @param defender The <code>Unit</code> defending against attack.
     * @param result The result of the attack.
     * @param plunderGold an <code>int</code> value
     */
    public void attack(Unit attacker, Unit defender, CombatResult result, int plunderGold);

    /**
     * Bombard a unit with the given outcome.
     * 
     * @param colony a <code>Colony</code> value
     * @param defender The <code>Unit</code> defending against bombardment.
     * @param result The result of the bombardment.
     */
    public void bombard(Colony colony, Unit defender, CombatResult result);

}


