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

import net.sf.freecol.common.model.Settlement.SettlementType;

/**
 * Represents one of the nations present in the game.
 */
public abstract class NationType extends FreeColGameObjectType {

    private static int nextIndex = 0;

    /**
     * The type of settlement this Nation has.
     */
    private SettlementType typeOfSettlement;

    /**
     * The radius of this nation type's settlements.
     */
    private int settlementRadius = 1;

    /**
     * The radius of this nation type's capital.
     */
    private int capitalRadius = 2;

    /**
     * Sole constructor.
     */
    public NationType() {
        setIndex(nextIndex++);
    }

    /**
     * Get the <code>TypeOfSettlement</code> value.
     *
     * @return an <code>SettlementType</code> value
     */
    public final SettlementType getTypeOfSettlement() {
        return typeOfSettlement;
    }

    /**
     * Set the <code>TypeOfSettlement</code> value.
     *
     * @param newTypeOfSettlement The new TypeOfSettlement value.
     */
    public final void setTypeOfSettlement(final SettlementType newTypeOfSettlement) {
        this.typeOfSettlement = newTypeOfSettlement;
    }

    /**
     * Whether this is a EuropeanNation, i.e. a player or a REF.
     *
     */
    public abstract boolean isEuropean();

    /**
     * Whether this is a EuropeanREFNation.
     *
     */
    public abstract boolean isREF();

    /**
     * Get the <code>SettlementRadius</code> value.
     *
     * @return an <code>int</code> value
     */
    public final int getSettlementRadius() {
        return settlementRadius;
    }

    /**
     * Set the <code>SettlementRadius</code> value.
     *
     * @param newSettlementRadius The new SettlementRadius value.
     */
    public final void setSettlementRadius(final int newSettlementRadius) {
        this.settlementRadius = newSettlementRadius;
    }

    /**
     * Get the <code>CapitalRadius</code> value.
     *
     * @return an <code>int</code> value
     */
    public final int getCapitalRadius() {
        return capitalRadius;
    }

    /**
     * Set the <code>CapitalRadius</code> value.
     *
     * @param newCapitalRadius The new CapitalRadius value.
     */
    public final void setCapitalRadius(final int newCapitalRadius) {
        this.capitalRadius = newCapitalRadius;
    }

    public String toString() {
        return getName();
    }

}
