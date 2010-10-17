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

package net.sf.freecol.server.model;

import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.server.control.ChangeSet;
import net.sf.freecol.server.control.ChangeSet.ChangePriority;
import net.sf.freecol.server.control.ChangeSet.See;
import net.sf.freecol.server.model.ServerModelObject;
import net.sf.freecol.server.model.ServerPlayer;
import net.sf.freecol.server.model.ServerUnit;


/**
 * The server version of Europe.
 */
public class ServerEurope extends Europe implements ServerModelObject {

    private static final Logger logger = Logger.getLogger(ServerEurope.class.getName());


    /**
     * Trivial constructor required for all ServerModelObjects.
     */
    public ServerEurope(Game game, String id) {
        super(game, id);
    }

    /**
     * Creates a new ServerEurope.
     *
     * @param game The <code>Game</code> in which this object belong.
     * @param owner The <code>Player</code> that will be using this object of
     *            <code>Europe</code>.
     */
    public ServerEurope(Game game, Player owner) {
        super(game, owner);
    }

    /**
     * Increases the price for a unit.
     *
     * @param unitType The <code>UnitType</code>, trained or purchased
     * @param price The current price of the unit
     */
    public void increasePrice(UnitType unitType, int price) {
        Specification spec = getSpecification();
        String baseOption = "model.option.priceIncreasePerType";
        String name
            = unitType.getId().substring(unitType.getId().lastIndexOf('.'));
        String option = (spec.getBooleanOption(baseOption).getValue())
            ? "model.option.priceIncrease" + name
            : "model.option.priceIncrease";
        int increase = (spec.hasOption(option))
            ? spec.getIntegerOption(option).getValue()
            : 0;
        if (increase != 0) {
            unitPrices.put(unitType, new Integer(price + increase));
        }
    }

    /**
     * New turn for this colony tile.
     * TODO: give Europe a shipyard and remove this
     *
     * @param random A <code>Random</code> number source.
     * @param cs A <code>ChangeSet</code> to update.
     */
    public void csNewTurn(Random random, ChangeSet cs) {
        logger.finest("ServerEurope.csNewTurn, for " + toString());

        for (Unit unit : getUnitList()) {
            if (unit.isNaval() && unit.isUnderRepair()) {
                ((ServerUnit) unit).csRepairUnit(cs);
            }
        }
    }


    /**
     * Returns the tag name of the root element representing this object.
     *
     * @return "serverEurope"
     */
    public String getServerXMLElementTagName() {
        return "serverEurope";
    }
}
