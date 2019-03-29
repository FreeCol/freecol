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

package net.sf.freecol.common.networking;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Role;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;


/**
 * The message sent when the client requests rearrangeing of a colony.
 */
public class RearrangeColonyMessage extends AttributeMessage {

    public static final String TAG = "rearrangeColony";
    private static final String COLONY_TAG = "colony";

    /** Container for the unit change information. */
    public static class Arrangement {

        public Unit unit;
        public Location loc;
        public GoodsType work;
        public Role role;
        public int roleCount;

        public Arrangement() {} // deliberately empty

        public Arrangement(Unit unit, Location loc, GoodsType work,
                          Role role, int roleCount) {
            this.unit = unit;
            this.loc = loc;
            this.work = work;
            this.role = role;
            this.roleCount = roleCount;
        }

        public Arrangement(Game game, String unitId,
                          String locId, String workId,
                          String roleId, String roleCount) {
            init(game, unitId, locId, workId, roleId, roleCount);
        }

        public final void init(Game game, String unitId, 
                               String locId, String workId, 
                               String roleId, String roleCount) {
            this.unit = game.getFreeColGameObject(unitId, Unit.class);
            this.loc = game.findFreeColLocation(locId);
            this.work = (workId == null || workId.isEmpty()) ? null
                : game.getSpecification().getGoodsType(workId);
            this.role = game.getSpecification().getRole(roleId);
            try {
                this.roleCount = Integer.parseInt(roleCount);
            } catch (NumberFormatException nfe) {
                this.roleCount = 0;
            }
        }

        public static String unitKey(int i) {
            return FreeColObject.arrayKey(i) + "unit";
        }

        public static String locKey(int i) {
            return FreeColObject.arrayKey(i) + "loc";
        }

        public static String workKey(int i) {
            return FreeColObject.arrayKey(i) + "work";
        }

        public static String roleKey(int i) {
            return FreeColObject.arrayKey(i) + "role";
        }

        public static String roleCountKey(int i) {
            return FreeColObject.arrayKey(i) + "count";
        }

        /**
         * Create new arrangements for a given list of worker units on the
         * basis of a scratch colony configuration.
         *
         * @param colony The original {@code Colony}.
         * @param workers A list of worker {@code Unit}s to arrange.
         * @param scratch The scratch {@code Colony}.
         * @return A list of {@code Arrangement}s.
         */
        public static List<Arrangement> getArrangements(Colony colony,
                                                        List<Unit> workers,
                                                        Colony scratch) {
            List<Arrangement> ret = new ArrayList<>(workers.size());
            for (Unit u : workers) {
                Unit su = scratch.getCorresponding(u);
                if (u.getLocation().getId().equals(su.getLocation().getId())
                    && u.getWorkType() == su.getWorkType()
                    && u.getRole() == su.getRole()
                    && u.getRoleCount() == su.getRoleCount()) continue;
                ret.add(new Arrangement(u,
                        (Location)colony.getCorresponding((FreeColObject)su.getLocation()),
                        su.getWorkType(), su.getRole(), su.getRoleCount()));
            }
            return ret;
        }

        /**
         * Role comparison for use in rearrangeColony.
         *
         * @param other The {@code Arrangement} to compare to.
         * @return A comparison value.
         */
        public int roleComparison(Arrangement other) {
            int cmp = this.role.compareTo(other.role);
            if (cmp == 0) cmp = this.roleCount - other.roleCount;
            return cmp;
        }

        // Override Object

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return "[Arrangement " + unit.getId() + " at " + loc.getId()
                + " " + role.getRoleSuffix() + "." + roleCount
                + ((work == null) ? "" : " work " + work.getId()) + "]";
        }
    }


    /**
     * Create a new {@code RearrangeColonyMessage} with the
     * supplied colony.  Add changes with addChange().
     *
     * @param colony The {@code Colony} that is rearranging.
     * @param workers A list of worker {@code Unit}s to rearrange.
     * @param scratch A scratch {@code Colony} laid out as required.
     */
    public RearrangeColonyMessage(Colony colony, List<Unit> workers,
                                  Colony scratch) {
        super(TAG, COLONY_TAG, colony.getId());

        setArrangementAttributes(Arrangement.getArrangements(colony, workers, scratch));
    }

    /**
     * Create a new {@code RearrangeColonyMessage} from a stream.
     *
     * @param game The {@code Game} to read within.
     * @param xr The {@code FreeColXMLReader} to read from.
     */
    public RearrangeColonyMessage(Game game, FreeColXMLReader xr) {
        super(TAG, getAttributeMap(xr));
    }

    /**
     * Read the attributes from the stream.
     * 
     * @param xr The {@code FreeColXMLReader} to read from.
     * @return An attribute map.
     */
    private static Map<String, String> getAttributeMap(FreeColXMLReader xr) {
        int n = xr.getAttribute(FreeColObject.ARRAY_SIZE_TAG, 0);
        Map<String, String> ret = new HashMap<>(5 * n + 1);
        ret.put(COLONY_TAG, xr.getAttribute(COLONY_TAG, (String)null));
        for (int i = 0; i < n; i++) {
            ret.put(Arrangement.unitKey(i),
                xr.getAttribute(Arrangement.unitKey(i), (String)null));
            ret.put(Arrangement.locKey(i),
                xr.getAttribute(Arrangement.locKey(i), (String)null));
            ret.put(Arrangement.workKey(i),
                xr.getAttribute(Arrangement.workKey(i), (String)null));
            ret.put(Arrangement.roleKey(i),
                xr.getAttribute(Arrangement.roleKey(i), (String)null));
            ret.put(Arrangement.roleCountKey(i),
                xr.getAttribute(Arrangement.roleCountKey(i), (String)null));
        }
        return ret;
    }

    /**
     * Set the attributes consequent to a list of arrangements.
     *
     * @param arrangements The list of {@code Arrangement}.
     */
    private void setArrangementAttributes(List<Arrangement> arrangements) {
        int i = 0;
        for (Arrangement a : arrangements) {
            setStringAttribute(Arrangement.unitKey(i), a.unit.getId());
            setStringAttribute(Arrangement.locKey(i), a.loc.getId());
            if (a.work != null) {
                setStringAttribute(Arrangement.workKey(i), a.work.getId());
            }
            setStringAttribute(Arrangement.roleKey(i), a.role.toString());
            setStringAttribute(Arrangement.roleCountKey(i),
                               String.valueOf(a.roleCount));
            i++;
        }
        setIntegerAttribute(FreeColObject.ARRAY_SIZE_TAG, i);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean currentPlayerMessage() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MessagePriority getPriority() {
        return Message.MessagePriority.NORMAL;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ChangeSet serverHandler(FreeColServer freeColServer,
                                   ServerPlayer serverPlayer) {
        final String colonyId = getStringAttribute(COLONY_TAG);
        final Game game = serverPlayer.getGame();
        final List<Arrangement> arrangements = getArrangements(game);
        
        Colony colony;
        try {
            colony = serverPlayer.getOurFreeColGameObject(colonyId, Colony.class);
        } catch (Exception e) {
            return serverPlayer.clientError(e.getMessage());
        }

        if (arrangements.isEmpty()) {
            return serverPlayer.clientError("Empty rearrangement list.");
        }
        int i = 0;
        for (Arrangement uc : arrangements) {
            if (uc.unit == null) {
                return serverPlayer.clientError("Invalid unit " + i);
            }
            if (uc.loc == null) {
                return serverPlayer.clientError("Invalid location " + i);
            }
            if (uc.role == null) {
                return serverPlayer.clientError("Invalid role " + i);
            }
            if (uc.roleCount < 0) {
                return serverPlayer.clientError("Invalid role count " + i);
            }
        }

        // Rearrange can proceed.
        return igc(freeColServer)
            .rearrangeColony(serverPlayer, colony, arrangements);
    }


    // Public interface

    /**
     * Check if the are no arrangements present.
     *
     * @return True if there are no arrangements.
     */
    public boolean isEmpty() {
        return getIntegerAttribute(FreeColObject.ARRAY_SIZE_TAG, 0) == 0;
    }

    /**
     * Get arrangements from the attributes.
     *
     * @param game The {@code Game} to create arrangements in.
     * @return A list of {@code Arrangement}s.
     */
    public List<Arrangement> getArrangements(Game game) {
        int n = getIntegerAttribute(FreeColObject.ARRAY_SIZE_TAG, 0);
        List<Arrangement> ret = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            ret.add(new Arrangement(game,
                                    getStringAttribute(Arrangement.unitKey(i)),
                                    getStringAttribute(Arrangement.locKey(i)),
                                    getStringAttribute(Arrangement.workKey(i)),
                                    getStringAttribute(Arrangement.roleKey(i)),
                                    getStringAttribute(Arrangement.roleCountKey(i))));
        }
        return ret;
    }
}
