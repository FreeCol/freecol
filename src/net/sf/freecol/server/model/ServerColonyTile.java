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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.ColonyTile;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsContainer;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.ModelMessage;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Resource;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileType;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.util.Utils;
import net.sf.freecol.server.control.ChangeSet;
import net.sf.freecol.server.control.ChangeSet.ChangePriority;
import net.sf.freecol.server.control.ChangeSet.See;
import net.sf.freecol.server.model.ServerGame;
import net.sf.freecol.server.model.ServerModelObject;


/**
 * The server version of a colony tile.
 */
public class ServerColonyTile extends ColonyTile implements ServerModelObject {

    private static final Logger logger = Logger.getLogger(ServerColonyTile.class.getName());


    /**
     * Trivial constructor required for all ServerModelObjects.
     */
    public ServerColonyTile(Game game, String id) {
        super(game, id);
    }

    /**
     * Creates at new ServerColonyTile.
     *
     * @param game The <code>Game</code> this object belongs to.
     * @param colony The <code>Colony</code> this object belongs to.
     * @param workTile The tile in which this <code>ColonyTile</code>
     *                 represents a <code>WorkLocation</code> for.
     */
    public ServerColonyTile(Game game, Colony colony, Tile workTile) {
        super(game);

        this.colony = colony;
        this.workTile = workTile;
        colonyCenterTile = colony.getTile() == workTile;
        unit = null;
    }


    /**
     * New turn for this colony tile.
     *
     * @param random A <code>Random</code> number source.
     * @param cs A <code>ChangeSet</code> to update.
     */
    public void csNewTurn(Random random, ChangeSet cs) {
        logger.finest("ServerColonyTile.csNewTurn, for " + toString());
        Colony colony = getColony();
        ServerPlayer owner = (ServerPlayer) colony.getOwner();
        Specification spec = getSpecification();

        Tile workTile = getWorkTile();
        TileType workType = workTile.getType();
        Unit unit = getUnit();
        if (isColonyCenterTile()) {
            GoodsType goodsType;
            if (workType.getPrimaryGoods() != null) {
                colony.addGoods(workType.getPrimaryGoods().getType(),
                                workTile.getPrimaryProduction());
            }
            if (workType.getSecondaryGoods() != null) {
                colony.addGoods(workType.getSecondaryGoods().getType(),
                                workTile.getSecondaryProduction());
            }
        } else if (unit != null && canBeWorked()) {
            int amount = getProductionOf(unit.getWorkType());
            if (amount > 0) {
                colony.addGoods(unit.getWorkType(), amount);
                unit.setExperience(amount + unit.getExperience());
                cs.addPartial(See.only(owner), unit, "experience");
            }
            Resource resource
                = workTile.expendResource(unit.getWorkType(),
                                          unit.getType(), colony);
            if (resource != null) {
                cs.addMessage(See.only(owner),
                    new ModelMessage(ModelMessage.MessageType.WARNING,
                                     "model.tile.resourceExhausted",
                                     colony)
                              .add("%resource%", resource.getNameKey())
                              .addName("%colony%", colony.getName()));
                cs.add(See.perhaps(), workTile);
            }
        }
    }


    /**
     * Returns the tag name of the root element representing this object.
     *
     * @return "serverColonyTile"
     */
    public String getServerXMLElementTagName() {
        return "serverColonyTile";
    }
}
