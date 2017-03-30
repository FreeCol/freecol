/**
 *  Copyright (C) 2002-2017   The FreeCol Team
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
import java.util.List;

import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Role;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.util.DOMUtils;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


/**
 * The message sent when the client requests rearrangeing of a colony.
 */
public class RearrangeColonyMessage extends AttributeMessage {

    public static final String TAG = "rearrangeColony";
    private static final String COLONY_TAG = "colony";

    /** Container for the unit change information. */
    public static class Arrangement implements Comparable<Arrangement> {

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

        public Arrangement readFromElement(Game game, Element e, int i) {
            init(game,
                 getStringAttribute(e, unitKey(i)),
                 getStringAttribute(e, locKey(i)),
                 getStringAttribute(e, workKey(i)),
                 getStringAttribute(e, roleKey(i)),
                 getStringAttribute(e, roleCountKey(i)));
            return this;
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
            List<Arrangement> ret = new ArrayList<>();
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
         * Create new arrangements from an element.
         *
         * @param game The {@code Game} to create arrangements in.
         * @param element The {@code Element} to read from.
         * @return A list of {@code Arrangement}s found.
         */
        public static List<Arrangement> readArrangements(Game game,
                                                         Element element) {
            List<Arrangement> ret = new ArrayList<>();
            int n = getIntegerAttribute(element, FreeColObject.ARRAY_SIZE_TAG, 0);
            for (int i = 0; i < n; i++) {
                ret.add(new Arrangement().readFromElement(game, element, i));
            }
            return ret;
        }

        // Interface Comparable<Arrangement>

        /**
         * {@inheritDoc}
         */
        public int compareTo(Arrangement other) {
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
     * Create a new {@code RearrangeColonyMessage} from a
     * supplied element.
     *
     * @param game The {@code Game} this message belongs to.
     * @param element The {@code Element} to use to create the message.
     */
    public RearrangeColonyMessage(Game game, Element element) {
        super(TAG, COLONY_TAG, getStringAttribute(element, COLONY_TAG));

        setArrangementAttributes(Arrangement.readArrangements(game, element));
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
     * Set the attributes consequent to a list of arrangements.
     *
     * @param arrangements The list of {@code Arrangement}.
     */
    private void setArrangementAttributes(List<Arrangement> arrangements) {
        int i = 0;
        for (Arrangement a : arrangements) {
            setStringAttribute(a.unitKey(i), a.unit.getId());
            setStringAttribute(a.locKey(i), a.loc.getId());
            if (a.work != null) {
                setStringAttribute(a.workKey(i), a.work.getId());
            }
            setStringAttribute(a.roleKey(i), a.role.toString());
            setStringAttribute(a.roleCountKey(i), String.valueOf(a.roleCount));
            i++;
        }
        setIntegerAttribute(FreeColObject.ARRAY_SIZE_TAG, i);
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
        List<Arrangement> ret = new ArrayList<>();
        int n = getIntegerAttribute(FreeColObject.ARRAY_SIZE_TAG, 0);
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
        return freeColServer.getInGameController()
            .rearrangeColony(serverPlayer, colony, arrangements);
    }
}
