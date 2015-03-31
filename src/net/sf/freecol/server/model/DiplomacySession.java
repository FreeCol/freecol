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

package net.sf.freecol.server.model;

import java.util.logging.Logger;

import net.sf.freecol.common.model.DiplomaticTrade;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.server.control.ChangeSet;
import net.sf.freecol.server.control.ChangeSet.See;


/**
 * A type of session to handle diplomacy.
 */
public class DiplomacySession extends TransactionSession {

    private static final Logger logger = Logger.getLogger(DiplomacySession.class.getName());

    /** The agreement under consideration. */
    private DiplomaticTrade agreement;

    /** The initiating unit. */
    private final Unit unit;

    /** The other player's settlement. */
    private final Settlement settlement;

    /** The other player's unit (only non-null in first contact cases). */
    private final Unit otherUnit;


    public DiplomacySession(Unit unit, Settlement settlement) {
        super(makeSessionKey(DiplomacySession.class, unit, settlement));
        this.agreement = null;
        this.unit = unit;
        this.settlement = settlement;
        this.otherUnit = null;
    }

    public DiplomacySession(Unit unit, Unit otherUnit) {
        super(makeSessionKey(DiplomacySession.class, unit, otherUnit));
        this.agreement = null;
        this.unit = unit;
        this.settlement = null;
        this.otherUnit = otherUnit;
    }

    @Override
    public void complete(ChangeSet cs) {
        unit.setMovesLeft(0);
        cs.add(See.only((ServerPlayer)unit.getOwner()), unit);
        super.complete(cs);
    }

    public DiplomaticTrade getAgreement() {
        return agreement;
    }

    public void setAgreement(DiplomaticTrade agreement) {
        this.agreement = agreement;
    }
    
    public Unit getUnit() {
        return unit;
    }

    public Settlement getSettlement() {
        return settlement;
    }

    public Unit getOtherUnit() {
        return otherUnit;
    }
}
