/**
 *  Copyright (C) 2002-2012   The FreeCol Team
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

import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.Feature;
import net.sf.freecol.common.model.FoundingFather;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.HistoryEvent;
import net.sf.freecol.common.model.LastSale;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.ModelMessage;
import net.sf.freecol.common.model.Modifier;
import net.sf.freecol.common.model.Ownable;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Player.Stance;
import net.sf.freecol.common.model.Region;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TradeRoute;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.WorkLocation;
import net.sf.freecol.common.networking.DOMMessage;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;


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
            return c1.getPriority() - c2.getPriority();
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

        /**
         * Make this change visible to all players.
         *
         * @return a <code>See</code> value
         */
        public static See all() {
            return new See(ALL);
        }

        /**
         * Make this change visible to all players, provided they can
         * see the objects that are being changed.
         *
         * @return a <code>See</code> value
         */
        public static See perhaps() {
            return new See(PERHAPS);
        }

        /**
         * Make this change visible only to the given player.
         *
         * @param player a <code>ServerPlayer</code> value
         * @return a <code>See</code> value
         */
        public static See only(ServerPlayer player) {
            return new See(ONLY).always(player);
        }

        // Use these to modify a See visibility.

        /**
         * Make this change visible to the given player.
         *
         * @param player a <code>ServerPlayer</code> value
         * @return a <code>See</code> value
         */
        public See always(ServerPlayer player) {
            seeAlways = player;
            return this;
        }

        /**
         * Make this change visible to the given player, provided the
         * player can see the objects being changed.
         *
         * @param player a <code>ServerPlayer</code> value
         * @return a <code>See</code> value
         */
        public See perhaps(ServerPlayer player) {
            seePerhaps = player;
            return this;
        }

        /**
         * Make this change invisible to the given player.
         *
         * @param player a <code>ServerPlayer</code> value
         * @return a <code>See</code> value
         */
        public See except(ServerPlayer player) {
            seeNever = player;
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(64);
            sb.append((type == ALL) ? "ALL" : (type == PERHAPS) ? "PERHAPS"
                : (type == ONLY) ? "ONLY" : "BADTYPE");
            if (seeAlways != null) {
                sb.append(",always(").append(seeAlways.getId()).append(")");
            }
            if (seePerhaps != null) {
                sb.append(",perhaps(").append(seePerhaps.getId()).append(")");
            }
            if (seeNever != null) {
                sb.append(",never(").append(seeNever.getId()).append(")");
            }
            return sb.toString();
        }
    }

    // Abstract template for all types of Change.
    private abstract static class Change {

        /**
         * The visibility of the change.
         */
        protected See see;


        /**
         * Make a new Change.
         */
        Change(See see) {
            this.see = see;
        }

        /**
         * Gets the sort priority of a change, to be used by the
         * changeComparator.
         */
        public abstract int getPriority();

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
         * This is false by default, subclasses should override when
         * special case handling is required.
         *
         * @param serverPlayer The <code>ServerPlayer</code> to consider.
         * @return False.
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

        /**
         * {@inheritDoc}
         */
        public abstract String toString();
    }

    /**
     * Encapsulate an attack.
     */
    private static class AttackChange extends Change {
        private Unit attacker;
        private Unit defender;
        private Tile attackerTile;
        private Tile defenderTile;
        private boolean success;

        /**
         * Build a new AttackChange.
         *
         * Note that we must record attacker and defender tiles
         * now, because a successful attacker can move, and an unsuccessful
         * participant can die.
         *
         * @param see The visibility of this change.
         * @param attacker The <code>Unit</code> that is attacking.
         * @param defender The <code>Unit</code> that is defending.
         * @param attackerTile The <code>Tile</code> the attack comes from.
         * @param defenderTile The <code>Tile</code> the attack goes to.
         * @param success Did the attack succeed.
         */
        public AttackChange(See see, Unit attacker, Unit defender,
                            Tile attackerTile, Tile defenderTile,
                            boolean success) {
            super(see);
            this.attacker = attacker;
            this.defender = defender;
            this.attackerTile = attackerTile;
            this.defenderTile = defenderTile;
            this.success = success;
        }

        /**
         * Gets the sort priority.
         *
         * @return "CHANGE_ANIMATION".
         */
        public int getPriority() {
            return ChangePriority.CHANGE_ANIMATION.getPriority();
        }

        /**
         * Should a player perhaps be notified of this attack?
         * Do not use Unit.isVisibleTo because that gives a false
         * negative for units in settlements, which should be animated.
         *
         * @param serverPlayer The <code>ServerPlayer</code> to notify.
         * @return True if the player should be notified.
         */
        @Override
        public boolean isPerhapsNotifiable(ServerPlayer serverPlayer) {
            return serverPlayer == attacker.getOwner()
                || serverPlayer == defender.getOwner()
                || (serverPlayer.canSee(attackerTile)
                    && serverPlayer.canSee(defenderTile));
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
            element.setAttribute("attackerTile", attackerTile.getId());
            element.setAttribute("defenderTile", defenderTile.getId());
            element.setAttribute("success", Boolean.toString(success));
            if (!attacker.isVisibleTo(serverPlayer)) {
                element.appendChild(attacker.toXMLElement(serverPlayer, doc,
                                                          false, false));
                if (attacker.getLocation() instanceof Unit) {
                    Unit loc = (Unit)attacker.getLocation();
                    element.appendChild(loc.toXMLElement(serverPlayer, doc,
                                                         false, false));
                }
            }
            if (!defender.isVisibleTo(serverPlayer)) {
                element.appendChild(defender.toXMLElement(serverPlayer, doc,
                                                          false, false));
            }
            return element;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(64);
            sb.append("[").append(getClass().getName())
                .append(" ").append(see.toString())
                .append(" #").append(getPriority())
                .append(" ").append(attacker.getId())
                .append("@").append(attackerTile.getId())
                .append(" ").append(success)
                .append(" ").append(defender.getId())
                .append("@").append(defenderTile.getId())
                .append("]");
            return sb.toString();
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
        public AttributeChange(See see, String key, String value) {
            super(see);
            this.key = key;
            this.value = value;
        }

        /**
         * Gets the sort priority.
         *
         * @return "CHANGE_ATTRIBUTE", attributes are special.
         */
        public int getPriority() {
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

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(32);
            sb.append("[").append(getClass().getName())
                .append(" ").append(see.toString())
                .append(" #").append(getPriority())
                .append(" ").append(key)
                .append("=").append(value)
                .append("]");
            return sb.toString();
        }
    }

    /**
     * Encapsulate a Message.
     */
    private static class MessageChange extends Change {
        private ChangePriority priority;
        private DOMMessage message;

        /**
         * Build a new MessageChange.
         *
         * @param see The visibility of this change.
         * @param priority The priority of the change.
         * @param message The <code>Message</code> to add.
         */
        public MessageChange(See see, ChangePriority priority,
                             DOMMessage message) {
            super(see);
            this.priority = priority;
            this.message = message;
        }

        /**
         * Gets the sort priority.
         *
         * @return The priority.
         */
        public int getPriority() {
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

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(32);
            sb.append("[").append(getClass().getName())
                .append(" ").append(see.toString())
                .append(" #").append(getPriority())
                .append(" ").append(message)
                .append("]");
            return sb.toString();
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
                || (oldTile != null
                    && serverPlayer.canSee(oldTile)
                    && oldTile.getSettlement() == null
                    && !(oldLocation instanceof Unit));
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
        public MoveChange(See see, Unit unit, Location oldLocation,
                          Tile newTile) {
            super(see);
            this.unit = unit;
            this.oldLocation = oldLocation;
            this.newTile = newTile;
        }

        /**
         * Gets the sort priority.
         *
         * @return "CHANGE_ANIMATION"
         */
        public int getPriority() {
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
                List<Unit> units = new ArrayList<Unit>();
                units.add(unit);
                List<Change> changes = new ArrayList<Change>();
                changes.add(new RemoveChange(See.only(serverPlayer),
                                             unit.getLocation(), units));
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

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(32);
            sb.append("[").append(getClass().getName())
                .append(" ").append(see.toString())
                .append(" #").append(getPriority())
                .append(" ").append(unit.getId())
                .append(" ").append(((FreeColGameObject)oldLocation).getId())
                .append(" ").append(newTile.getId())
                .append("]");
            return sb.toString();
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
        public ObjectChange(See see, FreeColGameObject fcgo) {
            super(see);
            this.fcgo = fcgo;
        }

        /**
         * Gets the sort priority.
         *
         * @return "CHANGE_UPDATE"
         */
        public int getPriority() {
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
            if (fcgo instanceof Unit) {
                // Units have a precise test, use that rather than
                // the more general interface-based tests.
                return ((Unit) fcgo).isVisibleTo(serverPlayer);
            }
            // If we own it, we can see it.
            if (fcgo instanceof Ownable
                && ((Ownable) fcgo).getOwner() == (Player) serverPlayer) {
                return true;
            }
            // We do not own it, so the only way we could see it is if
            // it is on the map.  Would like to use getTile() to
            // decide that, but this will include ColonyTiles, which
            // report the colony center tile, yet should never be visible.
            // So just brutally disallow WorkLocations which should always
            // be invisible inside colonies.
            if (fcgo instanceof WorkLocation) {
                return false;
            }
            return fcgo instanceof Location
                && ((Location) fcgo).getTile() != null
                && serverPlayer.canSee(((Location) fcgo).getTile());
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

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(32);
            sb.append("[").append(getClass().getName())
                .append(" ").append(see.toString())
                .append(" #").append(getPriority())
                .append(" ").append(fcgo.getId())
                .append("]");
            return sb.toString();
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
        public PartialObjectChange(See see, FreeColGameObject fcgo,
                                   String... fields) {
            super(see, fcgo);
            this.fields = fields;
        }

        /**
         * Gets the sort priority.
         *
         * @return CHANGE_UPDATE.  Special update, but still an update.
         */
        public int getPriority() {
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

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(32);
            sb.append("[").append(getClass().getName())
                .append(" ").append(see.toString())
                .append(" #").append(getPriority())
                .append(" ").append(fcgo.getId());
            for (String f : fields) sb.append(" ").append(f);
            sb.append("]");
            return sb.toString();
        }
    }

    /**
     * Encapsulates removing some objects.
     */
    private static class RemoveChange extends Change {
        private Tile tile;
        private FreeColGameObject fcgo;
        private List<? extends FreeColGameObject> contents;

        /**
         * Build a new RemoveChange for an object that is disposed.
         *
         * @param see The visibility of this change.
         * @param loc The <code>Location</code> where the object was.
         * @param objects The <code>FreeColGameObject</code>s to remove.
         */
        public RemoveChange(See see, Location loc,
                            List<? extends FreeColGameObject> objects) {
            super(see);
            this.tile = (loc instanceof Tile) ? (Tile) loc : null;
            this.fcgo = objects.remove(objects.size() - 1);
            this.contents = objects;
        }

        /**
         * Gets the sort priority.
         *
         * @return "CHANGE_REMOVE"
         */
        public int getPriority() {
            return ChangePriority.CHANGE_REMOVE.getPriority();
        }

        /**
         * Should a player perhaps be notified of this removal?
         * They should if they can see the tile, and there is no
         * other-player settlement present.
         *
         * @param serverPlayer The <code>ServerPlayer</code> to notify.
         * @return True if the player should be notified.
         */
        @Override
        public boolean isPerhapsNotifiable(ServerPlayer serverPlayer) {
            Settlement settlement;
            return tile != null
                && serverPlayer.canSee(tile)
                && ((settlement = tile.getSettlement()) == null
                    || settlement.isDisposed()
                    || (ServerPlayer)settlement.getOwner() == serverPlayer);
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
                && ((Ownable)fcgo).getOwner() == serverPlayer) {
                for (FreeColGameObject o : contents) {
                    element.appendChild(o.toXMLElementPartial(doc));
                }
                element.setAttribute("divert", (tile != null) ? tile.getId()
                                     : serverPlayer.getId());
            }
            element.appendChild(fcgo.toXMLElementPartial(doc));
            return element;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(32);
            sb.append("[").append(getClass().getName())
                .append(" ").append(see.toString())
                .append(" #").append(getPriority())
                .append(" ").append(((tile == null) ? "<null>" : tile.getId()));
            for (FreeColGameObject f : contents) {
                sb.append(" ").append(f.getId());
            }
            sb.append(" ").append(fcgo.getId()).append("]");
            return sb.toString();
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
        public OwnedChange(See see, FreeColObject fco) {
            super(see);
            this.fco = fco;
        }

        /**
         * Gets the sort priority.
         *
         * @return "CHANGE_OWNER"
         */
        public int getPriority() {
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

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(32);
            sb.append("[").append(getClass().getName())
                .append(" ").append(see.toString())
                .append(" #").append(getPriority())
                .append(" ").append(fco.getId())
                .append("]");
            return sb.toString();
        }
    }

    /**
     * Encapsulate a feature change.
     */
    private static class FeatureChange extends Change {
        private FreeColGameObject object;
        private Feature feature;
        private boolean add;

        /**
         * Build a new OwnedChange.
         *
         * @param see The visibility of this change.
         * @param object The <code>FreeColGameObject</code> to update.
         * @param feature a <code>Feature</code> value to add or remove.
         * @param add a <code>boolean</code> value
         */
        public FeatureChange(See see, FreeColGameObject object,
                             Feature feature, boolean add) {
            super(see);
            this.object = object;
            this.feature = feature;
            this.add = add;
        }

        /**
         * Gets the sort priority.
         *
         * @return "CHANGE_OWNER"
         */
        public int getPriority() {
            return ChangePriority.CHANGE_OWNED.getPriority();
        }

        /**
         * Specialize a feature change into an element for a
         * particular player.
         *
         * @param serverPlayer The <code>ServerPlayer</code> to update.
         * @param doc The owner <code>Document</code>.
         * @return An "addObject" element.
         */
        public Element toElement(ServerPlayer serverPlayer, Document doc) {
            Element element = doc.createElement("featureChange");
            element.setAttribute("add", Boolean.toString(add));
            element.setAttribute(FreeColObject.ID_ATTRIBUTE_TAG, object.getId());
            Element child = feature.toXMLElement(doc);
            element.appendChild(child);
            return element;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(32);
            sb.append("[").append(getClass().getName())
                .append(" ").append(see.toString())
                .append(" #").append(getPriority())
                .append(" ").append((add) ? "add" : "remove")
                .append(" ").append(feature)
                .append(" ").append((add) ? "to" : "from")
                .append(" ").append(object.getId())
                .append("]");
            return sb.toString();
        }
    }

    /**
     * Encapsulates a spying action.
     */
    private static class SpyChange extends Change {
        private Tile tile;

        /**
         * Build a new SpyChange.
         *
         * @param see The visibility of this change.
         * @param settlement The <code>Settlement</code> to spy on.
         */
        public SpyChange(See see, Settlement settlement) {
            super(see);
            tile = settlement.getTile();
        }

        /**
         * Gets the sort priority.
         *
         * @return priority.
         */
        public int getPriority() {
            return ChangePriority.CHANGE_NORMAL.getPriority();
        }

        /**
         * Specialize a SpyChange into an element with the supplied name.
         *
         * @param serverPlayer The <code>ServerPlayer</code> to update.
         * @param doc The owner <code>Document</code>.
         * @return An element.
         */
        public Element toElement(ServerPlayer serverPlayer, Document doc) {
            Element element = doc.createElement("spyResult");
            element.setAttribute("tile", tile.getId());
            // Have to tack on two copies of the settlement tile.
            // One full version, one ordinary version to restore.
            element.appendChild(tile.toXMLElement(serverPlayer, doc,
                                                  true, false));
            element.appendChild(tile.toXMLElement(serverPlayer, doc,
                                                  false, false));
            return element;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(32);
            sb.append("[").append(getClass().getName())
                .append(" ").append(see.toString())
                .append(" #").append(getPriority())
                .append(" ").append(tile.getId())
                .append("]");
            return sb.toString();
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
        public StanceChange(See see, Player first, Stance stance,
                            Player second) {
            super(see);
            this.first = first;
            this.stance = stance;
            this.second = second;
        }

        /**
         * Gets the sort priority.
         *
         * @return "CHANGE_STANCE"
         */
        public int getPriority() {
            return ChangePriority.CHANGE_STANCE.getPriority();
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

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(32);
            sb.append("[").append(getClass().getName())
                .append(" ").append(see.toString())
                .append(" #").append(getPriority())
                .append(" ").append(first.getId())
                .append(" ").append(stance)
                .append(" ").append(second.getId())
                .append("]");
            return sb.toString();
        }
    }

    /**
     * Encapsulate trivial element, which will only have attributes apart
     * from its name.
     */
    private static class TrivialChange extends Change {
        private int priority;
        private String name;
        private String[] attributes;

        /**
         * Build a new TrivialChange.
         *
         * @param see The visibility of this change.
         * @param name The name of the element.
         * @param priority The sort priority of this change.
         */
        public TrivialChange(See see, String name, int priority,
                             String[] attributes) {
            super(see);
            if ((attributes.length & 1) == 1) {
                throw new IllegalArgumentException("Attributes must be even sized");
            }
            this.name = name;
            this.priority = priority;
            this.attributes = attributes;
        }

        /**
         * Gets the sort priority.
         *
         * @return priority.
         */
        public int getPriority() {
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

        /**
         * Debug helper.
         */
        public String toString() {
            String ret = "[" + getClass().getName() + " " + see.toString()
                + " #" + getPriority()
                + " " + name;
            for (String a : attributes) ret += " " + a;
            return ret + "]";
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
    public ChangeSet add(See see, ChangePriority cp, DOMMessage message) {
        changes.add(new MessageChange(see, cp, message));
        return this;
    }

    /**
     * Helper function to add an attack to a ChangeSet.
     *
     * @param see The visibility of this change.
     * @param attacker The <code>Unit</code> that is attacking.
     * @param defender The <code>Unit</code> that is defending.
     * @param attackerTile The <code>Tile</code> the attack comes from.
     * @param defenderTile The <code>Tile</code> the attack goes to.
     * @param success Did the attack succeed?
     * @return The updated <code>ChangeSet</code>.
     */
    public ChangeSet addAttack(See see, Unit attacker, Unit defender,
                               Tile attackerTile, Tile defenderTile,
                               boolean success) {
        changes.add(new AttackChange(see, attacker, defender,
                                     attackerTile, defenderTile, success));
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
     * @param see The visibility of this change.
     * @param loc The <code>Location</code> where the object was.
     * @param obj The <code>FreeColGameObject</code> to remove.
     * @return The updated <code>ChangeSet</code>.
     */
    public ChangeSet addDispose(See see, Location loc, FreeColGameObject obj) {
        changes.add(new RemoveChange(see, loc, obj.disposeList()));
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
        changes.add(new RemoveChange(See.perhaps().except(owner), tile, objects));
        changes.add(new ObjectChange(See.perhaps().except(owner), tile));
        return this;
    }

    /**
     * Helper function to add a founding father addition event to a ChangeSet.
     * Also adds the father to the owner.
     *
     * @param serverPlayer The <code>ServerPlayer</code> adding the father.
     * @param father The <code>FoundingFather</code> to add.
     * @return The updated <code>ChangeSet</code>.
     */
    public ChangeSet addFather(ServerPlayer serverPlayer,
                               FoundingFather father) {
        changes.add(new OwnedChange(See.only(serverPlayer), father));
        serverPlayer.addFather(father);
        return this;
    }

    /**
     * Helper function to add an Ability to a FreeColGameObject, or remove it.
     *
     * @param serverPlayer a <code>ServerPlayer</code> value
     * @param object a <code>FreeColGameObject</code> value
     * @param ability an <code>Ability</code> value
     * @param add a <code>boolean</code> value
     * @return a <code>ChangeSet</code> value
     */
    public ChangeSet addFeatureChange(ServerPlayer serverPlayer, FreeColGameObject object,
                                      Ability ability, boolean add) {
        changes.add(new FeatureChange(See.only(serverPlayer), object, ability, add));
        if (add) {
            object.addAbility(ability);
        } else {
            object.removeAbility(ability);
        }
        return this;
    }

    /**
     * Helper function to add a Modifier to a FreeColGameObject, or remove it.
     *
     * @param serverPlayer a <code>ServerPlayer</code> value
     * @param object a <code>FreeColGameObject</code> value
     * @param modifier a <code>Modifier</code> value
     * @param add a <code>boolean</code> value
     * @return a <code>ChangeSet</code> value
     */
    public ChangeSet addFeatureChange(ServerPlayer serverPlayer, FreeColGameObject object,
                                      Modifier modifier, boolean add) {
        changes.add(new FeatureChange(See.only(serverPlayer), object, modifier, add));
        if (add) {
            object.addModifier(modifier);
        } else {
            object.removeModifier(modifier);
        }
        return this;
    }

    /**
     * Helper function to add a global history event to a ChangeSet.
     * Also adds the history to all the European players.
     *
     * @param game The <code>Game</code> to find players in.
     * @param history The <code>HistoryEvent</code> to add.
     * @return The updated <code>ChangeSet</code>.
     */
    public ChangeSet addGlobalHistory(Game game, HistoryEvent history) {
        changes.add(new OwnedChange(See.all(), history));
        for (Player p : game.getLiveEuropeanPlayers()) {
            p.addHistory(history);
        }
        return this;
    }

    /**
     * Helper function to add a history event to a ChangeSet.
     * Also adds the history to the owner.
     *
     * @param serverPlayer The <code>ServerPlayer</code> making history.
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
     * @param loc The location from which the unit is moving.
     * @param tile The <code>Tile</code> to which the unit is moving.
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
        addGlobalHistory(game, h);
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
        serverPlayer.addLastSale(sale);
        return this;
    }

    /**
     * Helper function to add removals for several objects to a ChangeSet.
     *
     * @param see The visibility of this change.
     * @param loc The <code>Location</code> where the object was.
     * @param objects A list of <code>FreeColGameObject</code>s to remove.
     * @return The updated <code>ChangeSet</code>.
     */
    public ChangeSet addRemoves(See see, Location loc,
                                List<? extends FreeColGameObject> objects) {
        List<FreeColGameObject> fcgos = new ArrayList<FreeColGameObject>();
        for (FreeColGameObject fcgo : objects) {
            fcgos.clear(); fcgos.add(fcgo);
            changes.add(new RemoveChange(see, loc, fcgos));
        }
        return this;
    }

    /**
     * Helper function to add a spying change to a ChangeSet.
     *
     * @param see The visibility of this change.
     * @param settlement The <code>Settlement</code> to spy on.
     * @return The updated <code>ChangeSet</code>.
     */
    public ChangeSet addSpy(See see, Settlement settlement) {
        changes.add(new SpyChange(see, settlement));
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

    /**
     * Helper function to add a new trade route change to a ChangeSet.
     * Also adds the trade route to the player.
     *
     * @param serverPlayer The <code>ServerPlayer</code> adding the route.
     * @param tradeRoute The new <code>TradeRoute</code>.
     * @return The updated <code>ChangeSet</code>.
     */
    public ChangeSet addTradeRoute(ServerPlayer serverPlayer,
                                   TradeRoute tradeRoute) {
        changes.add(new OwnedChange(See.only(serverPlayer), tradeRoute));
        serverPlayer.getTradeRoutes().add(tradeRoute);
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
     * @return An element encapsulating an update of the objects to
     *         consider, or null if there is nothing to report.
     */
    public Element build(ServerPlayer serverPlayer) {
        List<Change> c = new ArrayList<Change>(changes);
        List<Element> elements = new ArrayList<Element>();
        List<Change> diverted = new ArrayList<Change>();
        Document doc = DOMMessage.createNewDocument();

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

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        Collections.sort(changes, changeComparator);
        for (Change c : changes) {
            sb.append(c.toString()).append("\n");
        }
        return sb.toString();
    }
}
