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

public interface CombatModel {

    public static enum CombatResult { GREAT_LOSS, LOSS, EVADES, WIN,
            GREAT_WIN, DONE_SETTLEMENT }; // The last defender of the settlement has died.

    /**
     * Return the offensive power of the attacker versus the defender.
     * 
     * @param attacker an <code>Unit</code> value
     * @param defender an <code>Unit</code> value
     * @return a <code>float</code> value
     */
    public float getOffensePower(Unit attacker, Unit defender);

    /**
     * Return the defensive power of the defender versus the attacker.
     * 
     * @param attacker an <code>Unit</code> value
     * @param defender an <code>Unit</code> value
     * @return an <code>float</code> value
     */
    public float getDefensePower(Unit attacker, Unit defender);

    /**
     * Return a list of all offensive modifiers that apply to the attacker
     * versus the defender.
     * 
     * @param attacker an <code>Unit</code> value
     * @param defender an <code>Unit</code> value
     * @return a <code>List</code> of Modifiers
     */
    public List<Modifier> getOffensiveModifiers(Unit attacker, Unit defender);

    /**
     * Return a list of all defensive modifiers that apply to the defender
     * versus the attacker.
     * 
     * @param attacker an <code>Unit</code> value
     * @param defender an <code>Unit</code> value
     * @return a <code>List</code> of Modifiers
     */
    public List<Modifier> getDefensiveModifiers(Unit attacker, Unit defender);

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
     * @param damage an <code>int</code> value
     * @param plunderGold an <code>int</code> value
     */
    public void attack(Unit attacker, Unit defender, CombatResult result, int damage, int plunderGold);

}


