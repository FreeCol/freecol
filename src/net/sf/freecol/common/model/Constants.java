/**
 *  Copyright (C) 2002-2022   The FreeCol Team
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


/**
 * Contains the constants.  These constants are used by the
 * controllers and input handlers when they are communicating.
 */
public interface Constants {

    /** Generic "huge" value. */
    int INFINITY = Integer.MAX_VALUE;

    /** Generic "unsure" value. */
    int UNDEFINED = Integer.MIN_VALUE;

    /** The result of checkIntegrity. */
    enum IntegrityType {
        INTEGRITY_FAIL(-1),
        INTEGRITY_FIXED(0),
        INTEGRITY_GOOD(1);

        /** The magic value. */
        private int val;

        /**
         * Build an integrity type with the given value.
         *
         * @param val The value.
         */
        IntegrityType(int val) {
            this.val = val;
        }

        /**
         * Is this integrity safe (i.e. non-failed).
         *
         * @return True if the integrity is safe.
         */
        public boolean safe() {
            return this != INTEGRITY_FAIL;
        }

        /**
         * Combine this integrity with another.
         *
         * @param it The other {@code IntegrityType}.
         * @return The combined integrity.
         */
        public IntegrityType combine(IntegrityType it) {
            return values()[1 + Math.min(this.val, it.val)];
        }

        /**
         * Get the fixed version of this integrity.
         *
         * If the integrity is broken, nothing can happen.
         *
         * @return The fixed {@code IntegrityType}.
         */
        public IntegrityType fix() {
            return combine(INTEGRITY_FIXED);
        }

        /**
         * Get the failed version of this integrity.
         *
         * @return INTEGRITY_FAIL.
         */
        public IntegrityType fail() {
            return INTEGRITY_FAIL;
        }
    };
    
    /** Actions when an armed unit contacts a settlement. */
    enum ArmedUnitSettlementAction {
        SETTLEMENT_ATTACK,
        SETTLEMENT_TRIBUTE,
    }

    /** Actions when dealing with a boycott. */
    enum BoycottAction {
        BOYCOTT_PAY_ARREARS,
        BOYCOTT_DUMP_CARGO
    }

    /** Actions when claiming land. */
    enum ClaimAction {
        CLAIM_ACCEPT,
        CLAIM_STEAL
    }

    /** Actions surrounding native demands at colonies. */
    enum IndianDemandAction {
        INDIAN_DEMAND_ACCEPT,
        INDIAN_DEMAND_REJECT,
        INDIAN_DEMAND_DONE
    }
    
    /** Actions with a missionary at a native settlement. */
    enum MissionaryAction {
        MISSIONARY_ESTABLISH_MISSION,
        MISSIONARY_DENOUNCE_HERESY,
        MISSIONARY_INCITE_INDIANS
    }

    /** Actions in scouting a colony. */
    enum ScoutColonyAction {
        SCOUT_COLONY_NEGOTIATE,
        SCOUT_COLONY_SPY,
        SCOUT_COLONY_ATTACK
    }

    /** Actions in scouting a native settlement. */
    enum ScoutIndianSettlementAction {
        SCOUT_SETTLEMENT_SPEAK,
        SCOUT_SETTLEMENT_TRIBUTE,
        SCOUT_SETTLEMENT_ATTACK
    }

    /** Price used to denote claiming land by stealing it. */
    int STEAL_LAND = -1;

    /** Choice of sales action at a native settlement. */
    enum TradeAction {
        BUY,
        SELL,
        GIFT
    }

    /** Actions when buying from the natives. */
    enum TradeBuyAction {
        BUY,
        HAGGLE
    }

    /** Actions when selling to the natives. */
    enum TradeSellAction {
        SELL,
        HAGGLE,
        GIFT
    }
}
