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

package net.sf.freecol.server.control;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.HistoryEvent;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.ModelMessage;
import net.sf.freecol.common.model.Ownable;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Player.Stance;
import net.sf.freecol.common.model.Region;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Document;
import org.w3c.dom.Element;


/**
 * Changes to be sent to the client.
 */
public class ChangeSet {
    private ArrayList<Change> changes;

    private static Comparator<Change> changeComparator
        = new Comparator<Change>() {
        public int compare(final Change c1, final Change c2) {
            return c1.sortPriority() - c2.sortPriority();
        }
    };

    public static class See {
        private static final int ALL = 1;
        private static final int PERHAPS = 0;
        private static final int ONLY = -1;
        private ServerPlayer seeAlways;
        private ServerPlayer seePerhaps;
        private ServerPlayer seeNever;
        private int type;

        private See(int type) {
            this.seeAlways = this.seePerhaps = this.seeNever = null;
            this.type = type;
        }

        /**
         * Check this visibility with respect to a player.
         *
         * @param player The <code>ServerPlayer</code> to consider.
         * @param perhapsResult The result if the visibility is ambiguous.
         * @return True if the player satisfies the visibility test.
         */
        public boolean check(ServerPlayer player, boolean perhapsResult) {
            return (seeNever == player) ? false
                : (seeAlways == player) ? true
                : (type == ONLY) ? false
                : (type == ALL) ? true
                : perhapsResult;
        }

        // Use these public constructor-like functions to define the
        // visibility of changes.

        public static See all() {
            return new See(ALL);
        }

        public static See perhaps() {
            return new See(PERHAPS);
        }

        public static See only(ServerPlayer player) {
            return new See(ONLY).always(player);
        }

        // Use these to modify a See visibility.

        public See always(ServerPlayer player) {
            seeAlways = player;
            return this;
        }

        public See perhaps(ServerPlayer player) {
            seePerhaps = player;
            return this;
        }

        public See except(ServerPlayer player) {
            seeNever = player;
            return this;
        }

    }

    // Abstract template for all types of Change.
    private abstract static class Change {

        protected See vis;

        /**
         * Make a new Change.
         */
        Change(See vis) {
            this.vis = vis;
        }

        /**
         * The sort priority of a change, to be used by the
         * changeComparator.
         */
        public abstract int sortPriority();

        /**
         * Should a player be notified of this Change?
         *
         * @param serverPlayer The <code>ServerPlayer</code> to consider.
         * @return True if this <code>Change</code> should be sent.
         */
        public boolean isNotifiable(ServerPlayer serverPlayer) {
            return vis.check(serverPlayer, isPerhapsNotifiable(serverPlayer));
        }

        /**
         * Should a player be notified of a Change for which the
         * visibility is delegated to the change type, allowing
         * special change-specific overrides.
         *
         * @param serverPlayer The <code>ServerPlayer</code> to consider.
         * @return False.  This is the default, to be overridden as required.
         */
        public boolean isPerhapsNotifiable(ServerPlayer serverPlayer) {
            return false;
        }

        /**
         * Are the secondary changes consequent to this Change?
         *
         * @param serverPlayer The <code>ServerPlayer</code> to consider.
         * @return A list of secondary <code>Change</code>s or the
         *     empty list if there are none, which is usually the case.
         */
        public List<Change> consequences(ServerPlayer serverPlayer) {
            return Collections.emptyList();
        }

        /**
         * Can this Change be directly converted to an Element?
         *
         * @return True if this change can be directly converted to an Element.
         */
        public boolean convertsToElement() {
            return true;
        }

        /**
         * Specialize a Change for a particular player.
         *
         * @param serverPlayer The <code>ServerPlayer</code> to update.
         * @param doc The owner <code>Document</code> to build the element in.
         * @return An <code>Element</code> encapsulating this change.
         */
        public abstract Element toElement(ServerPlayer serverPlayer,
                                          Document doc);

        /**
         * Some changes can not be directly specialized, but need to be
         * directly attached to an element.
         *
         * @param element The <code>Element</code> to attach to.
         */
        public void attachToElement(Element element) {
        }
    }

    /**
     * Encapsulate an attack.
     */
    private static class AttackChange extends Change {
        private Unit attacker;
        private Unit defender;
        private boolean success;

        /**
         * Build a new AttackChange.
         *
         * @param see The visibility of this change.
         * @param attacker The <code>Unit</code> that is attacking.
         * @param defender The <code>Unit</code> that is defending.
         * @param success Did the attack succeed.
         */
        AttackChange(See vis, Unit attacker, Unit defender, boolean success) {
            super(vis);
            this.attacker = attacker;
            this.defender = defender;
            this.success = success;
        }

        /**
         * The sort priority.
         *
         * @return 0.  Animations are first.
         */
        public int sortPriority() {
            return 0;
        }

        /**
         * Should a player perhaps be notified of this attack?
         *
         * @param serverPlayer The <code>ServerPlayer</code> to notify.
         * @return True if the player should be notified.
         */
        @Override
        public boolean isPerhapsNotifiable(ServerPlayer serverPlayer) {
            return serverPlayer == attacker.getOwner()
                || serverPlayer == defender.getOwner()
                || (attacker.isVisibleTo(serverPlayer)
                    && defender.isVisibleTo(serverPlayer));
        }

        /**
         * Specialize a AttackChange into an "animateAttack" element
         * for a particular player.
         *
         * @param serverPlayer The <code>ServerPlayer</code> to update.
         * @param doc The owner <code>Document</code>.
         * @return An "animateAttack" element.
         */
        public Element toElement(ServerPlayer serverPlayer, Document doc) {
            Element element = doc.createElement("animateAttack");
            element.setAttribute("attacker", attacker.getId());
            element.setAttribute("defender", defender.getId());
            element.setAttribute("success", Boolean.toString(success));
            if (!attacker.isVisibleTo(serverPlayer)) {
                element.appendChild(attacker.toXMLElement(serverPlayer, doc,
                                                          false, false));
            } else if (!defender.isVisibleTo(serverPlayer)) {
                element.appendChild(defender.toXMLElement(serverPlayer, doc,
                                                          false, false));
            }
            return element;
        }
    }

    /**
     * Encapsulate an attribute change.
     */
    private static class AttributeChange extends Change {
        private String key;
        private String value;

        /**
         * Build a new AttributeChange.
         *
         * @param see The visibility of this change.
         * @param key A key <code>String</code>.
         * @param value The corresponding value as a <code>String</code>.
         */
        AttributeChange(See vis, String key, String value) {
            super(vis);
            this.key = key;
            this.value = value;
        }

        /**
         * The sort priority.
         *
         * @return -1, attributes are special.
         */
        public int sortPriority() {
            return -1;
        }

        /**
         * AttributeChanges are tacked onto the final Element, not converted
         * directly.
         *
         * @return false.
         */
        @Override
            public boolean convertsToElement() {
            return false;
        }

        /**
         * We do not specialize AttributeChanges.
         *
         * @param serverPlayer The <code>ServerPlayer</code> to update.
         * @param doc The owner <code>Document</code>.
         * @return Null.
         */
        public Element toElement(ServerPlayer serverPlayer, Document doc) {
            return null;
        }

        /**
         * Tack attributes onto the element.
         *
         * @param element The <code>Element</code> to attach to.
         */
        @Override
            public void attachToElement(Element element) {
            element.setAttribute(key, value);
        }
    }

    /**
     * Encapsulate a setDead event.
     */
    private static class DeadChange extends Change {
        private Player player;

        /**
         * Build a new DeadChange.
         *
         * @param see The visibility of this change.
         * @param player The <code>Player</code> to kill.
         */
        DeadChange(See vis, Player player) {
            super(vis);
            this.player = player;
        }

        /**
         * The sort priority.
         *
         * @return 1.  Right after the animations.
         */
        public int sortPriority() {
            return 1;
        }

        /**
         * Specialize a DeadChange into an "setDead" element for a
         * particular player.
         *
         * @param serverPlayer The <code>ServerPlayer</code> to update.
         * @param doc The owner <code>Document</code>.
         * @return A "setDead" element.
         */
        public Element toElement(ServerPlayer serverPlayer, Document doc) {
            Element element = doc.createElement("setDead");
            element.setAttribute("player", player.getId());
            return element;
        }
    }

    /**
     * Encapsulate a move.
     */
    private static class MoveChange extends Change {
        private Unit unit;
        private Location oldLocation;
        private Tile newTile;

        private boolean seeOld(ServerPlayer serverPlayer) {
            Tile oldTile = oldLocation.getTile();
            return unit.getOwner() == serverPlayer
                || (oldLocation instanceof Tile && serverPlayer.canSee(oldTile)
                    && oldTile.getSettlement() == null);
        }

        private boolean seeNew(ServerPlayer serverPlayer) {
            return unit.getOwner() == serverPlayer
                || unit.isVisibleTo(serverPlayer);
        }


        /**
         * Build a new MoveChange.
         *
         * @param see The visibility of this change.
         * @param unit The <code>Unit</code> that is moving.
         * @param oldLocation The location from which the unit is moving.
         * @param newTile The <code>Tile</code> to which the unit is moving.
         */
        MoveChange(See vis, Unit unit, Location oldLocation,
                   Tile newTile) {
            super(vis);
            this.unit = unit;
            this.oldLocation = oldLocation;
            this.newTile = newTile;
        }

        /**
         * The sort priority.
         *
         * @return 0.  Animations are first.
         */
        public int sortPriority() {
            return 0;
        }

        /**
         * Should a player perhaps be notified of this move?
         *
         * @param serverPlayer The <code>ServerPlayer</code> to notify.
         * @return True if the player should be notified.
         */
        @Override
        public boolean isPerhapsNotifiable(ServerPlayer serverPlayer) {
            return seeOld(serverPlayer) || seeNew(serverPlayer);
        }

        /**
         * There are consequences to a move.  If the player can not
         * vis the unit after the move, it should be removed.
         *
         * @param serverPlayer The <code>ServerPlayer</code> to notify.
         * @return A RemoveChange if the unit disappears.
         */
        @Override
        public List<Change> consequences(ServerPlayer serverPlayer) {
            if (seeOld(serverPlayer) && !seeNew(serverPlayer)) {
                List<Change> changes = new ArrayList<Change>();
                List<FreeColGameObject> objects = new ArrayList<FreeColGameObject>();
                objects.add(unit);
                changes.add(new RemoveChange(See.only(serverPlayer),
                                             unit.getLocation(), objects));
                return changes;
            }
            return Collections.emptyList();
        }

        /**
         * Specialize a MoveChange into an "animateMove" element for a
         * particular player.
         *
         * @param serverPlayer The <code>ServerPlayer</code> to update.
         * @param doc The owner <code>Document</code>.
         * @return An "animateMove" element.
         */
        public Element toElement(ServerPlayer serverPlayer, Document doc) {
            Element element = doc.createElement("animateMove");
            element.setAttribute("unit", unit.getId());
            element.setAttribute("oldTile", oldLocation.getTile().getId());
            element.setAttribute("newTile", newTile.getId());
            if (!seeOld(serverPlayer)) {
                // We can not rely on the unit that is about to move
                // being present on the client side, and it is needed
                // before we can run the animation, so it is attached
                // to animateMove.
                element.appendChild(unit.toXMLElement(serverPlayer, doc,
                                                      false, false));
            }
            return element;
        }
    }

    /**
     * Encapsulate a FreeColGameObject update.
     */
    private static class ObjectChange extends Change {
        protected FreeColGameObject fcgo;

        /**
         * Build a new ObjectChange for a single object.
         *
         * @param see The visibility of this change.
         * @param fcgo The <code>FreeColGameObject</code> to update.
         */
        ObjectChange(See vis, FreeColGameObject fcgo) {
            super(vis);
            this.fcgo = fcgo;
        }

        /**
         * The sort priority.
         *
         * @return 2.  Vanilla update.
         */
        public int sortPriority() {
            return 2;
        }

        /**
         * Should a player perhaps be notified of this update?
         *
         * @param serverPlayer The <code>ServerPlayer</code> to notify.
         * @return True if the object update can is notifiable.
         */
        @Override
        public boolean isPerhapsNotifiable(ServerPlayer serverPlayer) {
            return (fcgo instanceof Ownable
                    && ((Ownable) fcgo).getOwner() == (Player) serverPlayer)
                || (fcgo instanceof Unit
                    && ((Unit) fcgo).isVisibleTo(serverPlayer))
                || (fcgo instanceof Location
                    && ((Location) fcgo).getTile() != null
                    && serverPlayer.canSee(((Location) fcgo).getTile()))
                ;
        }

        /**
         * Specialize a ObjectChange to a particular player.
         *
         * @param serverPlayer The <code>ServerPlayer</code> to update.
         * @param doc The owner <code>Document</code>.
         * @return An "update" element, or null if the update should not
         *     be visible to the player.
         */
        public Element toElement(ServerPlayer serverPlayer, Document doc) {
            Element element = doc.createElement("update");
            element.appendChild(fcgo.toXMLElement(serverPlayer, doc,
                                                  false, false));
            return element;
        }
    }

    /**
     * Encapsulate a partial update of a FreeColGameObject.
     */
    private static class PartialObjectChange extends ObjectChange {
        private String[] fields;

        /**
         * Build a new PartialObjectChange for a single object.
         *
         * @param see The visibility of this change.
         * @param fcgo The <code>FreeColGameObject</code> to update.
         * @param fields The fields to update.
         */
        PartialObjectChange(See vis, FreeColGameObject fcgo,
                            String... fields) {
            super(vis, fcgo);
            this.fields = fields;
        }

        /**
         * The sort priority.
         *
         * @return 2.  Special update, but still an update.
         */
        public int sortPriority() {
            return 2;
        }

        /**
         * Should a player perhaps be notified of this update?
         *
         * @param serverPlayer The <code>ServerPlayer</code> to notify.
         * @return False.  Revert to default from ObjectChange special case.
         */
        @Override
        public boolean isPerhapsNotifiable(ServerPlayer serverPlayer) {
            return false;
        }

        /**
         * Specialize a PartialObjectChange to a particular player.
         *
         * @param serverPlayer The <code>ServerPlayer</code> to update.
         * @param doc The owner <code>Document</code>.
         * @return An "update" element.
         */
        @Override
        public Element toElement(ServerPlayer serverPlayer, Document doc) {
            Element element = doc.createElement("update");
            element.appendChild(fcgo.toXMLElement(serverPlayer, doc,
                                                  false, false, fields));
            return element;
        }
    }

    private static class RemoveChange extends Change {
        private Tile tile;
        private FreeColGameObject fcgo;
        private List<FreeColGameObject> contents;

        /**
         * Build a new RemoveChange for an object that is disposed.
         *
         * @param see The visibility of this change.
         * @param loc The <code>Location</code> where the object was.
         * @param objects The <code>FreeColGameObject</code>s to remove.
         */
        RemoveChange(See vis, Location loc,
                     List<FreeColGameObject> objects) {
            super(vis);
            this.tile = (loc instanceof Tile) ? (Tile) loc : null;
            this.fcgo = objects.remove(objects.size() - 1);
            this.contents = objects;
        }

        /**
         * The sort priority.
         *
         * @return 100.  Removes are last.
         */
        public int sortPriority() {
            return 100;
        }

        /**
         * Should a player perhaps be notified of this removal?
         *
         * @param serverPlayer The <code>ServerPlayer</code> to notify.
         * @return True if the player should be notified.
         */
        @Override
        public boolean isPerhapsNotifiable(ServerPlayer serverPlayer) {
            return tile != null && serverPlayer.canSee(tile);
        }

        /**
         * Specialize a RemoveChange to a particular player.
         *
         * @param serverPlayer The <code>ServerPlayer</code> to update.
         * @param doc The owner <code>Document</code>.
         * @return A "remove" element.
         */
        public Element toElement(ServerPlayer serverPlayer, Document doc) {
            Element element = doc.createElement("remove");
            // The main object may be visible, but the contents are by
            // only visible if the deeper ownership test succeeds.
            if (fcgo instanceof Ownable
                && ((Ownable) fcgo).getOwner() == serverPlayer) {
                for (FreeColGameObject o : contents) {
                    element.appendChild(o.toXMLElementPartial(doc));
                }
                element.setAttribute("divert", (tile != null) ? tile.getId()
                                     : serverPlayer.getId());
            }
            element.appendChild(fcgo.toXMLElementPartial(doc));
            return element;
        }
    }

    /**
     * Encapsulate a stance change.
     */
    private static class StanceChange extends Change {
        private Player first;
        private Stance stance;
        private Player second;

        /**
         * Build a new StanceChange.
         *
         * @param see The visibility of this change.
         * @param first The <code>Player</code> changing stance.
         * @param stance The <code>Stance</code> to change to.
         * @param second The <code>Player</code> wrt with to change.
         */
        StanceChange(See vis, Player first, Stance stance, Player second) {
            super(vis);
            this.first = first;
            this.stance = stance;
            this.second = second;
        }

        /**
         * The sort priority.
         *
         * @return 1.  Before the updates.
         */
        public int sortPriority() {
            return 1;
        }

        /**
         * Should a player perhaps be notified of this stance change?
         * Yes, if they are one of the players involved in the stance
         * change, or it is a war.  TODO: more cases.
         *
         * @param serverPlayer The <code>ServerPlayer</code> to notify.
         * @return True if the player should be notified.
         */
        @Override
        public boolean isPerhapsNotifiable(ServerPlayer serverPlayer) {
            return (Player) serverPlayer == first
                || (Player) serverPlayer == second
                || stance == Stance.WAR;
        }

        /**
         * Specialize a StanceChange to a particular player.
         *
         * @param serverPlayer The <code>ServerPlayer</code> to update.
         * @param doc The owner <code>Document</code>.
         * @return A "setStance" element.
         */
        @Override
        public Element toElement(ServerPlayer serverPlayer, Document doc) {
            Element element = doc.createElement("setStance");
            element.setAttribute("stance", stance.toString());
            element.setAttribute("first", first.getId());
            element.setAttribute("second", second.getId());
            return element;
        }
    }

    /**
     * Encapsulate a message or history event.
     */
    private static class StringChange extends Change {
        StringTemplate template;

        /**
         * Build a new HistoryChange.
         *
         * @param see The visibility of this change.
         * @param h The <code>HistoryEvent</code> that occurred.
         */
        StringChange(See vis, StringTemplate template) {
            super(vis);
            this.template = template;
        }

        /**
         * The sort priority.
         *
         * @return 3.  Messages and history are after the updates.
         */
        public int sortPriority() {
            return 3;
        }

        /**
         * Specialize a StringChange into an "addObject" element for a
         * particular player.
         *
         * @param serverPlayer The <code>ServerPlayer</code> to update.
         * @param doc The owner <code>Document</code>.
         * @return An "addObject" element.
         */
        public Element toElement(ServerPlayer serverPlayer, Document doc) {
            Element element = doc.createElement("addObject");
            Element child = template.toXMLElement(serverPlayer, doc,
                                                  false, false);
            child.setAttribute("owner", serverPlayer.getId());
            element.appendChild(child);
            return element;
        }
    }

    /**
     * Simple constructor.
     */
    ChangeSet() {
        changes = new ArrayList<Change>();
    }

    /**
     * Copying constructor.
     *
     * @param other The other <code>ChangeSet</code> to copy.
     */
    ChangeSet(ChangeSet other) {
        changes = new ArrayList<Change>(other.changes);
    }


    // Helper routines that should be used to construct a change set.

    /**
     * Helper function to add updates for multiple objects to a ChangeSet.
     *
     * @param see The visibility of this change.
     * @param objects The <code>FreeColGameObject</code>s that changed.
     * @return The updated <code>ChangeSet</code>.
     */
    public ChangeSet add(See vis, FreeColGameObject... objects) {
        for (FreeColGameObject o : objects) {
            changes.add(new ObjectChange(vis, o));
        }
        return this;
    }

    /**
     * Helper function to add updates for multiple objects to a ChangeSet.
     *
     * @param see The visibility of this change.
     * @param objects The <code>FreeColGameObject</code>s that changed.
     * @return The updated <code>ChangeSet</code>.
     */
    public ChangeSet add(See vis, List<FreeColGameObject> objects) {
        for (FreeColGameObject o : objects) {
            changes.add(new ObjectChange(vis, o));
        }
        return this;
    }

    /**
     * Helper function to add an attack to a ChangeSet.
     *
     * @param see The visibility of this change.
     * @param unit The <code>Unit</code> that is attacking.
     * @param defender The <code>Unit</code> that is defending.
     * @param success Did the attack succeed?
     * @return The updated <code>ChangeSet</code>.
     */
    public ChangeSet addAttack(See vis, Unit unit, Unit defender,
                               boolean success) {
        changes.add(new AttackChange(vis, unit, defender, success));
        return this;
    }

    /**
     * Helper function to add an attribute setting to a ChangeSet.
     *
     * @param see The visibility of this change.
     * @param key A key <code>String</code>.
     * @param value The corresponding value as a <code>String</code>.
     * @return The updated <code>ChangeSet</code>.
     */
    public ChangeSet addAttribute(See vis, String key, String value) {
        changes.add(new AttributeChange(vis, key, value));
        return this;
    }

    /**
     * Helper function to add a dead player event to a ChangeSet.
     * Deaths are public knowledge.
     *
     * @param serverPlayer The <code>ServerPlayer</code> that died.
     * @return The updated <code>ChangeSet</code>.
     */
    public ChangeSet addDead(ServerPlayer serverPlayer) {
        changes.add(new DeadChange(See.all(), serverPlayer));
        return this;
    }

    /**
     * Helper function to add a removal for a disposal list to a ChangeSet.
     *
     * @param owner The <code>ServerPlayer</code> that owns this object.
     * @param loc The <code>Location</code> where the object was.
     * @param objects The <code>FreeColGameObject</code>s to remove.
     * @return The updated <code>ChangeSet</code>.
     */
    public ChangeSet addDispose(ServerPlayer owner, Location loc,
                                FreeColGameObject obj) {
        changes.add(new RemoveChange(See.perhaps().always(owner), loc,
                                     obj.disposeList()));
        return this;
    }

    /**
     * Helper function to add a removal for an object that disappears
     * (that is, moves where it can not be seen) to a ChangeSet.
     *
     * @param owner The <code>ServerPlayer</code> that owns this object.
     * @param tile The <code>Tile</code> where the object was.
     * @param fcgo The <code>FreeColGameObject</code> that disappears.
     * @return The updated <code>ChangeSet</code>.
     */
    public ChangeSet addDisappear(ServerPlayer owner, Tile tile,
                                  FreeColGameObject fcgo) {
        List<FreeColGameObject> objects = new ArrayList<FreeColGameObject>();
        objects.add(fcgo);
        changes.add(new ObjectChange(See.perhaps().except(owner), tile));
        changes.add(new RemoveChange(See.perhaps().except(owner), tile,
                                     objects));
        return this;
    }

    /**
     * Helper function to add a history event to a ChangeSet.
     * Also adds the history to the owner.
     *
     * @param serverPlayer The <code>ServerPlayer</code> making history
     *     or null if all players should see this event.
     * @param history The <code>HistoryEvent</code> to add.
     * @return The updated <code>ChangeSet</code>.
     */
    public ChangeSet addHistory(ServerPlayer serverPlayer,
                                HistoryEvent history) {
        changes.add(new StringChange(See.only(serverPlayer), history));
        serverPlayer.addHistory(history);
        return this;
    }

    /**
     * Helper function to add a message to a ChangeSet.
     *
     * @param see The visibility of this change.
     * @param message The <code>ModelMessage</code> to add.
     * @return The updated <code>ChangeSet</code>.
     */
    public ChangeSet addMessage(See vis, ModelMessage message) {
        changes.add(new StringChange(vis, message));
        return this;
    }

    /**
     * Helper function to add a move to a ChangeSet.
     *
     * @param see The visibility of this change.
     * @param unit The <code>Unit</code> that is moving.
     * @param oldLocation The location from which the unit is moving.
     * @param newTile The <code>Tile</code> to which the unit is moving.
     * @return The updated <code>ChangeSet</code>.
     */
    public ChangeSet addMove(See vis, Unit unit, Location loc, Tile tile) {
        changes.add(new MoveChange(vis, unit, loc, tile));
        return this;
    }

    /**
     * Helper function to add a partial update change for an object to
     * a ChangeSet.
     *
     * @param see The visibility of this change.
     * @param fcgo The <code>FreeColGameObject</code> to update.
     * @param fields The fields to update.
     * @return The updated <code>ChangeSet</code>.
     */
    public ChangeSet addPartial(See vis, FreeColGameObject fcgo,
                                String... fields) {
        changes.add(new PartialObjectChange(vis, fcgo, fields));
        return this;
    }

    /**
     * Helper function to add a region discovery to a ChangeSet.
     * Also adds the history to all Europeans.
     *
     * @param serverPlayer The <code>ServerPlayer</code> discovering the region.
     * @param region The <code>Region</code> to discover.
     * @param name The name of the region.
     * @return The updated <code>ChangeSet</code>.
     */
    public ChangeSet addRegion(ServerPlayer serverPlayer, Region region,
                               String name) {
        Game game = serverPlayer.getGame();
        HistoryEvent h = region.discover(serverPlayer, game.getTurn(), name);
        changes.add(new ObjectChange(See.all(), region));
        changes.add(new StringChange(See.all(), h));
        for (Player p : game.getPlayers()) {
            if (p.isEuropean()) p.addHistory(h);
        }
        return this;
    }

    /**
     * Helper function to add a stance change to a ChangeSet.
     *
     * @param see The visibility of this change.
     * @param first The <code>Player</code> changing stance.
     * @param stance The <code>Stance</code> to change to.
     * @param second The <code>Player</code> wrt with to change.
     * @return The updated <code>ChangeSet</code>.
     */
    public ChangeSet addStance(See see, Player first, Stance stance,
                               Player second) {
        changes.add(new StanceChange(see, first, stance, second));
        return this;
    }

    // Conversion of a change set to a corresponding element.

    /**
     * Collapse one element into another.
     *
     * @param head The <code>Element</code> to collapse into.
     * @param tail The <code>Element</code> to extract nodes from.
     */
    private static void collapseElements(Element head, Element tail) {
        while (tail.hasChildNodes()) {
            head.appendChild(tail.removeChild(tail.getFirstChild()));
        }
    }

    /**
     * Collapse adjacent elements in a list with the same tag.
     *
     * @param elements The list of <code>Element</code>s to consider.
     * @return A collapsed list of elements.
     */
    private static List<Element> collapseElementList(List<Element> elements) {
        List<Element> results = new ArrayList<Element>();
        if (!elements.isEmpty()) {
            Element head = elements.remove(0);
            while (!elements.isEmpty()) {
                Element e = elements.remove(0);
                if (e.getTagName() == head.getTagName()) {
                    collapseElements(head, e);
                } else {
                    results.add(head);
                    head = e;
                }
            }
            results.add(head);
        }
        return results;
    }

    /**
     * Build a generalized update.
     * Beware that removing an object does not necessarily update
     * its tile correctly on the client side--- if a tile update
     * is needed the tile should be supplied in the objects list.
     *
     * @param serverPlayer The <code>ServerPlayer</code> to send the
     *            update to.
     * @param originalChanges A list of <code>Change</code>s to consider.
     * @return An element encapsulating an update of the objects to
     *         consider, or null if there is nothing to report.
     */
    public Element build(ServerPlayer serverPlayer) {
        List<Change> c = new ArrayList<Change>(changes);
        List<Element> elements = new ArrayList<Element>();
        List<Change> diverted = new ArrayList<Change>();
        Document doc = Message.createNewDocument();

        // For all sorted changes, if it is notifiable to the target
        // player then convert it to an Element, or divert for later
        // attachment.  Then add all consequence changes to the list.
        Collections.sort(c, changeComparator);
        while (!c.isEmpty()) {
            Change change = c.remove(0);
            if (change.isNotifiable(serverPlayer)) {
                if (change.convertsToElement()) {
                    elements.add(change.toElement(serverPlayer, doc));
                } else {
                    diverted.add(change);
                }
                c.addAll(change.consequences(serverPlayer));
            }
        }
        elements = collapseElementList(elements);

        // Decide what to return.  If there are several parts with
        // children then return multiple, if there is one viable part,
        // return that, if there is none return null unless there are
        // attributes in which case they become viable as an update.
        Element result;
        switch (elements.size()) {
        case 0:
            if (diverted.isEmpty()) return null;
            result = doc.createElement("update");
            break;
        case 1:
            result = elements.get(0);
            break;
        default:
            result = doc.createElement("multiple");
            for (Element e : elements) result.appendChild(e);
            break;
        }
        doc.appendChild(result);
        for (Change change : diverted) change.attachToElement(result);
        return result;
    }

}
