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
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.HistoryEvent;
import net.sf.freecol.common.model.LastSale;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.ModelMessage;
import net.sf.freecol.common.model.ModelMessage.MessageType;
import net.sf.freecol.common.model.Ownable;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Player.Stance;
import net.sf.freecol.common.model.Region;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 * Changes to be sent to the client.
 */
public class ChangeSet {

    // Convenient way to specify the relative priorities of the fixed
    // change types in one place.
    public static enum ChangePriority {
        CHANGE_ATTRIBUTE(-1), // N/A
        CHANGE_ANIMATION(0),  // Do animations first
        CHANGE_REMOVE(100),   // Do removes last
        CHANGE_STANCE(5),     // Do stance before updates
        CHANGE_OWNED(20),     // Do owned changes after updates
        CHANGE_UPDATE(10),    // There are a lot of updates
        // Symbolic priorities used by various non-fixed types
        CHANGE_EARLY(1),
        CHANGE_NORMAL(15),
        CHANGE_LATE(90);

        private int level;

        ChangePriority(int level) {
            this.level = level;
        }

        public int getPriority() {
            return level;
        }
    }

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

        protected See see;

        /**
         * Make a new Change.
         */
        Change(See see) {
            this.see = see;
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
            return see.check(serverPlayer, isPerhapsNotifiable(serverPlayer));
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
        AttackChange(See see, Unit attacker, Unit defender, boolean success) {
            super(see);
            this.attacker = attacker;
            this.defender = defender;
            this.success = success;
        }

        /**
         * The sort priority.
         *
         * @return "CHANGE_ANIMATION".
         */
        public int sortPriority() {
            return ChangePriority.CHANGE_ANIMATION.getPriority();
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
        AttributeChange(See see, String key, String value) {
            super(see);
            this.key = key;
            this.value = value;
        }

        /**
         * The sort priority.
         *
         * @return "CHANGE_ATTRIBUTE", attributes are special.
         */
        public int sortPriority() {
            return ChangePriority.CHANGE_ATTRIBUTE.getPriority();
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
     * Encapsulate a Message.
     */
    private static class MessageChange extends Change {
        private ChangePriority priority;
        private Message message;

        /**
         * Build a new MessageChange.
         *
         * @param see The visibility of this change.
         * @param priority The priority of the change.
         * @param message The <code>Message</code> to add.
         */
        MessageChange(See see, ChangePriority priority, Message message) {
            super(see);
            this.priority = priority;
            this.message = message;
        }

        /**
         * The sort priority.
         *
         * @return The priority.
         */
        public int sortPriority() {
            return priority.getPriority();
        }

        /**
         * Specialize a MessageChange to a particular player.
         *
         * @param serverPlayer The <code>ServerPlayer</code> to update.
         * @param doc The owner <code>Document</code>.
         * @return An element.
         */
        public Element toElement(ServerPlayer serverPlayer, Document doc) {
            Element element = message.toXMLElement();
            return (Element) doc.importNode(element, true);
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
        MoveChange(See see, Unit unit, Location oldLocation,
                   Tile newTile) {
            super(see);
            this.unit = unit;
            this.oldLocation = oldLocation;
            this.newTile = newTile;
        }

        /**
         * The sort priority.
         *
         * @return "CHANGE_ANIMATION"
         */
        public int sortPriority() {
            return ChangePriority.CHANGE_ANIMATION.getPriority();
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
         * see the unit after the move, it should be removed.
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
        ObjectChange(See see, FreeColGameObject fcgo) {
            super(see);
            this.fcgo = fcgo;
        }

        /**
         * The sort priority.
         *
         * @return "CHANGE_UPDATE"
         */
        public int sortPriority() {
            return ChangePriority.CHANGE_UPDATE.getPriority();
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
        PartialObjectChange(See see, FreeColGameObject fcgo,
                            String... fields) {
            super(see, fcgo);
            this.fields = fields;
        }

        /**
         * The sort priority.
         *
         * @return CHANGE_UPDATE.  Special update, but still an update.
         */
        public int sortPriority() {
            return ChangePriority.CHANGE_UPDATE.getPriority();
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
        RemoveChange(See see, Location loc,
                     List<FreeColGameObject> objects) {
            super(see);
            this.tile = (loc instanceof Tile) ? (Tile) loc : null;
            this.fcgo = objects.remove(objects.size() - 1);
            this.contents = objects;
        }

        /**
         * The sort priority.
         *
         * @return "CHANGE_REMOVE"
         */
        public int sortPriority() {
            return ChangePriority.CHANGE_REMOVE.getPriority();
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
     * Encapsulate an owned object change.
     */
    private static class OwnedChange extends Change {

        private FreeColObject fco;

        /**
         * Build a new OwnedChange.
         *
         * @param see The visibility of this change.
         * @param fco The <code>FreeColObject</code> to update.
         */
        OwnedChange(See vis, FreeColObject fco) {
            super(vis);
            this.fco = fco;
        }

        /**
         * The sort priority.
         *
         * @return "CHANGE_OWNER"
         */
        public int sortPriority() {
            return ChangePriority.CHANGE_OWNED.getPriority();
        }

        /**
         * Specialize a OwnedChange into an "addObject" element for a
         * particular player.
         *
         * @param serverPlayer The <code>ServerPlayer</code> to update.
         * @param doc The owner <code>Document</code>.
         * @return An "addObject" element.
         */
        public Element toElement(ServerPlayer serverPlayer, Document doc) {
            Element element = doc.createElement("addObject");
            Element child = fco.toXMLElement(serverPlayer, doc, false, false);
            child.setAttribute("owner", serverPlayer.getId());
            element.appendChild(child);
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
        StanceChange(See see, Player first, Stance stance, Player second) {
            super(see);
            this.first = first;
            this.stance = stance;
            this.second = second;
        }

        /**
         * The sort priority.
         *
         * @return "CHANGE_STANCE"
         */
        public int sortPriority() {
            return ChangePriority.CHANGE_STANCE.getPriority();
        }

        /**
         * Should a player perhaps be notified of this stance change?
         * Yes, if they are the player that initiated the change, the
         * player the stance change applies to, or it is a war, or
         * they have enhaced diplomacy reporting.
         *
         * @param serverPlayer The <code>ServerPlayer</code> to notify.
         * @return True if the player should be notified.
         */
        @Override
        public boolean isPerhapsNotifiable(ServerPlayer serverPlayer) {
            return (ServerPlayer) first == serverPlayer
                || (ServerPlayer) second == serverPlayer
                || stance == Stance.WAR
                || serverPlayer.hasAbility("model.ability.betterForeignAffairsReport");
        }

        /**
         * There are consequences to a stance change.  If it is
         * visible to a player they should see a message about it,
         * unless they initiated the change and already know.
         *
         * @param serverPlayer The <code>ServerPlayer</code> to notify.
         * @return A list of changes if there are messages to send.
         */
        @Override
        public List<Change> consequences(ServerPlayer serverPlayer) {
            List<Change> changes = new ArrayList<Change>();
            if (!serverPlayer.isAI()
                && (ServerPlayer) first != serverPlayer) {
                String sta = stance.toString();
                ModelMessage m = ((ServerPlayer) second == serverPlayer)
                    ? new ModelMessage(MessageType.FOREIGN_DIPLOMACY,
                                       "model.diplomacy." + sta + ".declared",
                                       first)
                        .addStringTemplate("%nation%", first.getNationName())
                    : new ModelMessage(MessageType.FOREIGN_DIPLOMACY,
                                       "model.diplomacy." + sta + ".others",
                                       first)
                        .addStringTemplate("%attacker%", first.getNationName())
                        .addStringTemplate("%defender%", second.getNationName());
                changes.add(new OwnedChange(See.only(serverPlayer), m));
            }
            return changes;
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
     * Encapsulate trivial element, which will only have attributes apart
     * from its name.
     */
    private static class TrivialChange extends Change {
        int priority;
        String name;
        String[] attributes;

        /**
         * Build a new TrivialChange.
         *
         * @param see The visibility of this change.
         * @param name The name of the element.
         * @param priority The sort priority of this change.
         */
        TrivialChange(See see, String name, int priority, String[] attributes) {
            super(see);
            if ((attributes.length & 1) == 1) {
                throw new IllegalArgumentException("Attributes must be even sized");
            }
            this.name = name;
            this.priority = priority;
            this.attributes = attributes;
        }

        /**
         * The sort priority.
         *
         * @return priority.
         */
        public int sortPriority() {
            return priority;
        }

        /**
         * Specialize a TrivialChange into an element with the supplied name.
         *
         * @param serverPlayer The <code>ServerPlayer</code> to update.
         * @param doc The owner <code>Document</code>.
         * @return An element.
         */
        public Element toElement(ServerPlayer serverPlayer, Document doc) {
            Element element = doc.createElement(name);
            for (int i = 0; i < attributes.length; i += 2) {
                element.setAttribute(attributes[i], attributes[i+1]);
            }
            return element;
        }
    }

    /**
     * Simple constructor.
     */
    public ChangeSet() {
        changes = new ArrayList<Change>();
    }

    /**
     * Copying constructor.
     *
     * @param other The other <code>ChangeSet</code> to copy.
     */
    public ChangeSet(ChangeSet other) {
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
    public ChangeSet add(See see, FreeColGameObject... objects) {
        for (FreeColGameObject o : objects) {
            changes.add(new ObjectChange(see, o));
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
    public ChangeSet add(See see, List<FreeColGameObject> objects) {
        for (FreeColGameObject o : objects) {
            changes.add(new ObjectChange(see, o));
        }
        return this;
    }

    /**
     * Helper function to add a Message to a ChangeSet.
     *
     * @param see The visibility of this change.
     * @param cp The priority of this change.
     * @param message The <code>Message</code> to add.
     * @return The updated <code>ChangeSet</code>.
     */
    public ChangeSet add(See see, ChangePriority cp, Message message) {
        changes.add(new MessageChange(see, cp, message));
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
    public ChangeSet addAttack(See see, Unit unit, Unit defender,
                               boolean success) {
        changes.add(new AttackChange(see, unit, defender, success));
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
    public ChangeSet addAttribute(See see, String key, String value) {
        changes.add(new AttributeChange(see, key, value));
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
        addTrivial(See.all(), "setDead", ChangePriority.CHANGE_EARLY,
                   "player", serverPlayer.getId());
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
        changes.add(new OwnedChange(See.only(serverPlayer), history));
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
    public ChangeSet addMessage(See see, ModelMessage message) {
        changes.add(new OwnedChange(see, message));
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
    public ChangeSet addMove(See see, Unit unit, Location loc, Tile tile) {
        changes.add(new MoveChange(see, unit, loc, tile));
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
    public ChangeSet addPartial(See see, FreeColGameObject fcgo,
                                String... fields) {
        changes.add(new PartialObjectChange(see, fcgo, fields));
        return this;
    }

    /**
     * Helper function to add a sale change to a ChangeSet.
     *
     * @param serverPlayer The <code>ServerPlayer</code> making the sale.
     * @param settlement The <code>Settlement</code> that is buying.
     * @param type The <code>GoodsType</code> bought.
     * @param price The per unit price.
     * @return The updated <code>ChangeSet</code>.
     */
    public ChangeSet addSale(ServerPlayer serverPlayer, Settlement settlement,
                             GoodsType type, int price) {
        Game game = settlement.getGame();
        LastSale sale = new LastSale(settlement, type, game.getTurn(), price);
        changes.add(new OwnedChange(See.only(serverPlayer), sale));
        serverPlayer.saveSale(sale);
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
        changes.add(new OwnedChange(See.all(), h));
        for (Player p : game.getPlayers()) {
            if (p.isEuropean()) p.addHistory(h);
        }
        return this;
    }

    /**
     * Helper function to add a stance change to a ChangeSet.
     *
     * @param vis The visibility of this change.
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

    /**
     * Helper function to add a trivial element to a ChangeSet.
     *
     * @param see The visibility of this change.
     * @param name The name of the element.
     * @param cp The <code>ChangePriority</code> for this change.
     * @param attributes Attributes to add to this trivial change.
     * @return The updated <code>ChangeSet</code>.
     */
    public ChangeSet addTrivial(See see, String name, ChangePriority cp,
                                String... attributes) {
        changes.add(new TrivialChange(see, name, cp.getPriority(), attributes));
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
     * Can two elements be collapsed?
     * They need to have the same name and attributes.
     *
     * @param e1 The first <code>Element</code>.
     * @param e2 The second <code>Element</code>.
     * @return True if they can be collapsed.
     */
    private static boolean collapseOK(Element e1, Element e2) {
        if (e1.getTagName() != e2.getTagName()) return false;
        NamedNodeMap nnm1 = e1.getAttributes();
        NamedNodeMap nnm2 = e2.getAttributes();
        if (nnm1.getLength() != nnm2.getLength()) return false;
        for (int i = 0; i < nnm1.getLength(); i++) {
            if (nnm1.item(i).getNodeType() != nnm2.item(i).getNodeType()) {
                return false;
            }
            if (nnm1.item(i).getNodeName() != nnm2.item(i).getNodeName()) {
                return false;
            }
            if (nnm1.item(i).getNodeValue() != nnm2.item(i).getNodeValue()) {
                return false;
            }
        }
        return true;
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
                if (collapseOK(head, e)) {
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
