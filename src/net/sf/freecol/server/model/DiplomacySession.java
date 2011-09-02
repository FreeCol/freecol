/**
 *  Copyright (C) 2002-2011  The FreeCol Team
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
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.server.control.ChangeSet;


/**
 * A type of session to handle diplomacy.
 */
public class DiplomacySession extends TransactionSession {

    private static final Logger logger = Logger.getLogger(DiplomacySession.class.getName());

    /** The agreement under consideration. */
    private DiplomaticTrade agreement;


    public DiplomacySession(Unit unit, Settlement settlement) {
        super(makeSessionKey(DiplomacySession.class, unit, settlement));
        agreement = null;
    }

    public void complete(ChangeSet cs) {
        super.complete(cs);
    }

    public DiplomaticTrade getAgreement() {
        return agreement;
    }

    public void setAgreement(DiplomaticTrade agreement) {
        this.agreement = agreement;
    }
}
