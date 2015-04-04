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

    /** Container for the unit change information. */
    public static class UnitChange {

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

        public void writeToElement(Element e, int i) {
            e.setAttribute(unitKey(i), this.unit.getId());
            e.setAttribute(locKey(i), this.loc.getId());
            if (this.work != null) {
                e.setAttribute(workKey(i), this.work.getId());
            }
            e.setAttribute(roleKey(i), this.role.toString());
            e.setAttribute(roleCountKey(i), String.valueOf(this.roleCount));
        }

        public UnitChange readFromElement(Game game, Element e, int i) {
            init(game,
                e.getAttribute(unitKey(i)),
                e.getAttribute(locKey(i)),
                e.getAttribute(workKey(i)),
                e.getAttribute(roleKey(i)),
                e.getAttribute(roleCountKey(i)));
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
    }

    /** A comparator for UnitChanges, favouring simplest roles first. */
    public static final Comparator<UnitChange> roleComparator
        = new Comparator<UnitChange>() {
            @Override
            public int compare(UnitChange uc1, UnitChange uc2) {
                int cmp = uc1.role.compareTo(uc2.role);
                if (cmp == 0) cmp = uc1.roleCount - uc2.roleCount;
                return cmp;
            }
        };

    /** The id of the colony requesting the rearrangement. */
    private final String colonyId;

    /** A list of unitChanges to make. */
    private List<UnitChange> unitChanges = null;


    /**
     * Create a new <code>RearrangeColonyMessage</code> with the
     * supplied colony.  Add changes with addChange().
     *
     * @param colony The <code>Colony</code> that is rearranging.
     */
    public RearrangeColonyMessage(Colony colony) {
        super(getXMLElementTagName());

        this.colonyId = colony.getId();
        this.unitChanges = new ArrayList<>();
    }

    /**
     * Create a new <code>RearrangeColonyMessage</code> from a
     * supplied element.
     *
     * @param game The <code>Game</code> this message belongs to.
     * @param element The <code>Element</code> to use to create the message.
     */
    public RearrangeColonyMessage(Game game, Element element) {
        super(getXMLElementTagName());

        this.colonyId = element.getAttribute("colony");
        int n;
        try {
            n = Integer.parseInt(element.getAttribute(FreeColObject.ARRAY_SIZE_TAG));
        } catch (NumberFormatException nfe) {
            n = 0;
        }
        this.unitChanges = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            unitChanges.add(new UnitChange().readFromElement(game, element, i));
        }
    }


    // Public interface

    /**
     * Are there no changes present?
     *
     * @return True if no changes have been added.
     */
    public boolean isEmpty() {
        return unitChanges == null || unitChanges.isEmpty();
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
        unitChanges.add(new UnitChange(unit, loc, work, role, roleCount));
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
            colony = player.getOurFreeColGameObject(colonyId, Colony.class);
        } catch (Exception e) {
            return DOMMessage.clientError(e.getMessage());
        }

        if (unitChanges.isEmpty()) {
            return DOMMessage.clientError("Empty rearrangement list.");
        }
        int i = 0;
        for (UnitChange uc : unitChanges) {
            if (uc.unit == null) {
                return DOMMessage.clientError("Invalid unit " + i);
            }
            if (uc.loc == null) {
                return DOMMessage.clientError("Invalid location " + i);
            }
            if (uc.role == null) {
                return DOMMessage.clientError("Invalid role " + i);
            }
            if (uc.roleCount < 0) {
                return DOMMessage.clientError("Invalid role count " + i);
            }
        }

        // Rearrange can proceed.
        return server.getInGameController()
            .rearrangeColony(serverPlayer, colony, unitChanges);
    }

    /**
     * Convert this RearrangeColonyMessage to XML.
     *
     * @return The XML representation of this message.
     */
    @Override
    public Element toXMLElement() {
        Element result = createMessage(getXMLElementTagName(),
            "colony", colonyId,
            FreeColObject.ARRAY_SIZE_TAG, Integer.toString(unitChanges.size()));
        int i = 0;
        for (UnitChange uc : unitChanges) {
            uc.writeToElement(result, i);
            i++;
        }
        return result;
    }

    /**
     * The tag name of the root element representing this object.
     *
     * @return "rearrangeColony".
     */
    public static String getXMLElementTagName() {
        return "rearrangeColony";
    }
}
