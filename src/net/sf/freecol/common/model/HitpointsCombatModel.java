/**
 *  Copyright (C) 2002-2023   The FreeCol Team
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

import static net.sf.freecol.common.util.RandomUtils.randomDouble;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

import net.sf.freecol.common.util.LogBuilder;

public class HitpointsCombatModel extends SimpleCombatModel {
    private static final Logger logger = Logger.getLogger(HitpointsCombatModel.class.getName());

    /**
     * Deliberately empty constructor.
     */
    public HitpointsCombatModel() {}

    /**
     * Generates a result of a unit attacking.
     * Takes care to only call the pseudo-random source *once*.
     *
     * @param random A pseudo-random number source.
     * @param attacker The attacker.
     * @param defender The defender.
     * @return The results of the combat.
     */
    @Override
    public CombatResult generateAttackResult(Random random,
        FreeColGameObject attacker, FreeColGameObject defender) {
        LogBuilder lb = new LogBuilder(256);
        lb.add("Combat");
        List<CombatEffectType> crs = new ArrayList<>();
        CombatOdds odds = calculateCombatOdds(attacker, defender, lb);
        double r = randomDouble(logger, "AttackResult", random);
        lb.add(" random(1.0)=", r);

        if (combatIsAttack(attacker, defender)) {
            Unit attackerUnit = (Unit) attacker;
            Unit defenderUnit = (Unit) defender;

            final int defenderDamage = random.nextInt((int) Math.ceil(getOffencePower(attacker, defender)) + 1);
            final int newDefenderHitpoints = defenderUnit.getHitPoints() - defenderDamage;
            if (newDefenderHitpoints > 0) {
                int newAttackerHitpoints = -1;
                if (attackerUnit.getType().getAttackRange() <= defenderUnit.getType().getAttackRange()
                        || defenderUnit.getTile().getDistanceTo(attackerUnit.getTile()) <= defenderUnit.getType().getAttackRange()) {
                    final int attackerDamage = random.nextInt((int) Math.ceil(getDefencePower(attacker, defender)) + 1);
                    newAttackerHitpoints = attackerUnit.getHitPoints() - attackerDamage;
                    if (newAttackerHitpoints <= 0) {
                        crs.add(CombatEffectType.LOSE);
                        resolveAttack(defenderUnit, attackerUnit, true,
                                // Rescaling to 0 <= r < 1
                                // (rearrange: 0.8 * odds.win + 0.2 <= r < 1.0)
                                (1.25 * r - 0.25 - odds.win)/(1.0 - odds.win), crs);
                        return new CombatResult(crs, 1, newDefenderHitpoints);
                    }
                }
                
                crs.add(CombatEffectType.WIN);
                return new CombatResult(crs, newAttackerHitpoints, newDefenderHitpoints);
            } else {
                crs.add(CombatEffectType.WIN);
                resolveAttack(attackerUnit, defenderUnit, true,
                        // Rescale to 0 <= r < 1
                        r / (0.1 * odds.win), crs);
                return new CombatResult(crs, -1, 1);
            }
        } else if (combatIsBombard(attacker, defender)) {
            Unit defenderUnit = (Unit) defender;
            if (!defenderUnit.isNaval()) {
                // One day we might want:
                //   crs.add(CombatResult.SLAUGHTER_UNIT_BOMBARD);
                throw new RuntimeException("Bombard of non-naval: " + attacker
                    + " v " + defender);
            }
            
            final int defenderDamage = random.nextInt((int) Math.ceil(getOffencePower(attacker, defender)) + 1);
            if (defenderDamage == 0) {
                crs.add(CombatEffectType.NO_RESULT);
                crs.add(CombatEffectType.EVADE_BOMBARD);
                return new CombatResult(crs, -1, -1);
            }
            final int newDefenderHitpoints = defenderUnit.getHitPoints() - defenderDamage;
            if (newDefenderHitpoints > 0) {
                crs.add(CombatEffectType.WIN);
                return new CombatResult(crs, -1, newDefenderHitpoints);
            } else {
                crs.add(CombatEffectType.WIN);
                crs.add(CombatEffectType.SINK_SHIP_BOMBARD);
                return new CombatResult(crs, -1, -1);
            }
        } else {
            throw new RuntimeException("Bogus combat: " + attacker
                + " v " + defender);
        }
    }

}
