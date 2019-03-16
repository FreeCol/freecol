/**
 *  Copyright (C) 2002-2019   The FreeCol Team
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

import java.util.Random;
import java.util.logging.Logger;

import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.ColonyTile;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.ModelMessage;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Resource;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.networking.ChangeSet;
import net.sf.freecol.common.networking.ChangeSet.See;
import net.sf.freecol.common.util.LogBuilder;


/**
 * The server version of a colony tile.
 */
public class ServerColonyTile extends ColonyTile implements TurnTaker {

    private static final Logger logger = Logger.getLogger(ServerColonyTile.class.getName());


    /**
     * Trivial constructor for Game.newInstance.
     *
     * @param game The {@code Game} this object belongs to.
     * @param id The object identifier.
     */
    public ServerColonyTile(Game game, String id) {
        super(game, id);
    }

    /**
     * Creates at new ServerColonyTile.
     *
     * @param game The {@code Game} this object belongs to.
     * @param colony The {@code Colony} this object belongs to.
     * @param workTile The tile in which this {@code ColonyTile}
     *                 represents a {@code WorkLocation} for.
     */
    public ServerColonyTile(Game game, Colony colony, Tile workTile) {
        super(game, colony, workTile);
    }


    /**
     * This method is called only when a new turn is beginning.  It
     * will reduce the quantity of the bonus {@code Resource}
     * that is on the given tile, if any and if applicable.
     *
     * @param tile The {@code Tile} to check for a resource.
     * @param goodsType The {@code GoodsType} the goods type to expend.
     * @param unitType The {@code UnitType} doing the production.
     * @return The {@code Resource} if it is exhausted by this
     *     call (so it can be used in a message), otherwise null.
     */
    private Resource expendResource(Tile tile, GoodsType goodsType,
                                    UnitType unitType) {
        if (!tile.hasResource()) return null;

        Resource resource = tile.getResource();
        if (resource.isUnlimited()) return null;

        if (resource.useQuantity(goodsType, unitType,
                                 tile.getPotentialProduction(goodsType, unitType)) == 0) {
            tile.cacheUnseen();//+til
            tile.removeResource();//-til
            return resource;
        }
        return null;
    }


    // Implement TurnTaker

    /**
     * New turn for this colony tile.
     *
     * @param random A {@code Random} number source.
     * @param lb A {@code LogBuilder} to log to.
     * @param cs A {@code ChangeSet} to update.
     */
    @Override
    public void csNewTurn(Random random, LogBuilder lb, ChangeSet cs) {
        final Colony colony = getColony();
        final Player owner = colony.getOwner();

        Tile workTile = getWorkTile();
        if (!isColonyCenterTile() && !isEmpty() && canBeWorked()) {
            for (Unit unit : getUnitList()) {
                Resource resource = expendResource(workTile, unit.getWorkType(),
                                                   unit.getType());
                if (resource != null) {
                    lb.add(" ", getId(), " exhausted resource ",
                           resource.getType().getSuffix());
                    cs.addMessage(owner,
                        new ModelMessage(ModelMessage.MessageType.WARNING,
                                         "model.colonyTile.resourceExhausted",
                                         colony)
                            .addNamed("%resource%", resource)
                            .addName("%colony%", colony.getName()));
                    cs.add(See.perhaps(), workTile);
                    break;
                }
            }
        }
    }
}
