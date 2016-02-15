/**
 *  Copyright (C) 2002-2016   The FreeCol Team
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
import java.util.Comparator;
import java.util.List;

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

import org.w3c.dom.Element;


/**
 * The message sent when the client requests rearrangeing of a colony.
 */
public class RearrangeColonyMessage extends DOMMessage {

    public static final String TAG = "rearrangeColony";
    private static final String COLONY_TAG = "colony";

    /** Container for the unit change information. */
    public static class UnitChange implements Comparable<UnitChange> {

        public Unit unit;
        public Location loc;
        public GoodsType work;
        public Role role;
        public int roleCount;

        public UnitChange() {} // deliberately empty

        public UnitChange(Unit unit, Location loc, GoodsType work,
                          Role role, int roleCount) {
            this.unit = unit;
            this.loc = loc;
            this.work = work;
            this.role = role;
            this.roleCount = roleCount;
        }

        public UnitChange(Game game, String unitId,
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

        public UnitChange readFromElement(Game game, Element e, int i) {
            init(game,
                getStringAttribute(e, unitKey(i)),
                getStringAttribute(e, locKey(i)),
                getStringAttribute(e, workKey(i)),
                getStringAttribute(e, roleKey(i)),
                getStringAttribute(e, roleCountKey(i)));
            return this;
        }

        public String unitKey(int i) {
            return "x" + i + "unit";
        }

        public String locKey(int i) {
            return "x" + i + "loc";
        }

        public String workKey(int i) {
            return "x" + i + "work";
        }

        public String roleKey(int i) {
            return "x" + i + "role";
        }

        public String roleCountKey(int i) {
            return "x" + i + "count";
        }

        @Override
        public String toString() {
            return "[UnitChange " + unit.getId() + " at " + loc.getId()
                + " " + role.getRoleSuffix() + "." + roleCount
                + ((work == null) ? "" : " work " + work.getId()) + "]";
        }

        // Interface Comparable<UnitChange>

        /**
         * {@inheritDoc}
         */
        public int compareTo(UnitChange other) {
            int cmp = this.role.compareTo(other.role);
            if (cmp == 0) cmp = this.roleCount - other.roleCount;
            return cmp;
        }
    }

    /** The id of the colony requesting the rearrangement. */
    private final String colonyId;

    /** A list of unitChanges to make. */
    private final List<UnitChange> unitChanges = new ArrayList<>();


    /**
     * Create a new <code>RearrangeColonyMessage</code> with the
     * supplied colony.  Add changes with addChange().
     *
     * @param colony The <code>Colony</code> that is rearranging.
     * @param workers A list of worker <code>Unit</code>s to rearrange.
     * @param scratch A scratch <code>Colony</code> laid out as required.
     */
    public RearrangeColonyMessage(Colony colony, List<Unit> workers,
                                  Colony scratch) {
        super(getTagName());

        this.colonyId = colony.getId();
        this.unitChanges.clear();
        for (Unit u : workers) {
            Unit su = scratch.getCorresponding(u);
            if (u.getLocation().getId().equals(su.getLocation().getId())
                && u.getWorkType() == su.getWorkType()
                && u.getRole() == su.getRole()
                && u.getRoleCount() == su.getRoleCount()) continue;
            addChange(u,
                (Location)colony.getCorresponding((FreeColObject)su.getLocation()),
                su.getWorkType(), su.getRole(), su.getRoleCount());
        }
    }

    /**
     * Create a new <code>RearrangeColonyMessage</code> from a
     * supplied element.
     *
     * @param game The <code>Game</code> this message belongs to.
     * @param element The <code>Element</code> to use to create the message.
     */
    public RearrangeColonyMessage(Game game, Element element) {
        super(getTagName());

        this.colonyId = getStringAttribute(element, COLONY_TAG);
        int n;
        try {
            n = Integer.parseInt(element.getAttribute(FreeColObject.ARRAY_SIZE_TAG));
        } catch (NumberFormatException nfe) {
            n = 0;
        }
        this.unitChanges.clear();
        for (int i = 0; i < n; i++) {
            this.unitChanges.add(new UnitChange().readFromElement(game, element, i));
        }
    }


    // Public interface

    /**
     * Are there no changes present?
     *
     * @return True if no changes have been added.
     */
    public boolean isEmpty() {
        return this.unitChanges.isEmpty();
    }

    /**
     * Add a change to this message.
     *
     * @param unit The <code>Unit</code> that is to change.
     * @param loc The destination <code>Location</code> for the unit.
     * @param work The <code>GoodsType</code> to produce (may be null).
     * @param role The unit <code>Role</code>.
     * @param roleCount The role count.
     */
    public void addChange(Unit unit, Location loc, GoodsType work,
                          Role role, int roleCount) {
        this.unitChanges.add(new UnitChange(unit, loc, work, role, roleCount));
    }

    
    /**
     * Handle a "rearrangeColony"-message.
     *
     * @param server The <code>FreeColServer</code> handling the request.
     * @param player The <code>Player</code> rearrangeing the colony.
     * @param connection The <code>Connection</code> the message is from.
     * @return An update <code>Element</code> with the rearranged colony,
     *     or an error <code>Element</code> on failure.
     */
    public Element handle(FreeColServer server, Player player,
                          Connection connection) {
        final ServerPlayer serverPlayer = server.getPlayer(connection);

        Colony colony;
        try {
            colony = player.getOurFreeColGameObject(this.colonyId, Colony.class);
        } catch (Exception e) {
            return serverPlayer.clientError(e.getMessage())
                .build(serverPlayer);
        }

        if (isEmpty()) {
            return serverPlayer.clientError("Empty rearrangement list.")
                .build(serverPlayer);
        }
        int i = 0;
        for (UnitChange uc : unitChanges) {
            if (uc.unit == null) {
                return serverPlayer.clientError("Invalid unit " + i)
                    .build(serverPlayer);
            }
            if (uc.loc == null) {
                return serverPlayer.clientError("Invalid location " + i)
                    .build(serverPlayer);
            }
            if (uc.role == null) {
                return serverPlayer.clientError("Invalid role " + i)
                    .build(serverPlayer);
            }
            if (uc.roleCount < 0) {
                return serverPlayer.clientError("Invalid role count " + i)
                    .build(serverPlayer);
            }
        }

        // Rearrange can proceed.
        return server.getInGameController()
            .rearrangeColony(serverPlayer, colony, this.unitChanges)
            .build(serverPlayer);
    }

    /**
     * Convert this RearrangeColonyMessage to XML.
     *
     * @return The XML representation of this message.
     */
    @Override
    public Element toXMLElement() {
        DOMMessage result = new DOMMessage(getTagName(),
            COLONY_TAG, this.colonyId,
            FreeColObject.ARRAY_SIZE_TAG, Integer.toString(unitChanges.size()));
        int i = 0;
        for (UnitChange uc : unitChanges) {
            result.setAttribute(uc.unitKey(i), uc.unit.getId());
            result.setAttribute(uc.locKey(i), uc.loc.getId());
            if (uc.work != null) {
                result.setAttribute(uc.workKey(i), uc.work.getId());
            }
            result.setAttribute(uc.roleKey(i), uc.role.toString());
            result.setAttribute(uc.roleCountKey(i), String.valueOf(uc.roleCount));
            i++;
        }
        return result.toXMLElement();
    }

    /**
     * The tag name of the root element representing this object.
     *
     * @return "rearrangeColony".
     */
    public static String getTagName() {
        return TAG;
    }
}
