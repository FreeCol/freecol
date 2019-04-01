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

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;

import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.Feature;
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
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Stance;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.WorkLocation;
import static net.sf.freecol.common.util.CollectionUtils.*;
import static net.sf.freecol.common.util.StringUtils.*;


/**
 * Changes to be sent to the client.
 */
public class ChangeSet {

    /** Result of a visibility check. */
    private enum SeeCheck {
        VISIBLE,
        INVISIBLE,
        SPECIAL;
    }
    
    /** The changes to send. */
    private final List<Change> changes;


    /**
     * Class to control the visibility of a change.
     */
    public static class See {
        private static final int ALL = 1;
        private static final int PERHAPS = 0;
        private static final int ONLY = -1;
        private Player seeAlways;
        private Player seePerhaps;
        private Player seeNever;
        private final int type;

        private See(int type) {
            this.seeAlways = this.seePerhaps = this.seeNever = null;
            this.type = type;
        }

        /**
         * Check this visibility with respect to a player.
         *
         * @param player The {@code Player} to consider.
         * @return If the player satisfies the visibility test return VISIBLE,
         *     or INVISIBLE on failure, or SPECIAL if indeterminate.
         */
        public SeeCheck check(Player player) {
            return 
                (player != null && player == seeNever) ? SeeCheck.INVISIBLE
                : (player != null && player == seeAlways) ? SeeCheck.VISIBLE
                : (player != null && player == seePerhaps) ? SeeCheck.SPECIAL
                : (type == ONLY) ? SeeCheck.INVISIBLE
                : (type == ALL) ? SeeCheck.VISIBLE
                : SeeCheck.SPECIAL;
        }

        // Use these public constructor-like functions to define the
        // visibility of changes.

        /**
         * Make this change visible to all players.
         *
         * @return See(ALL).
         */
        public static See all() {
            return new See(ALL);
        }

        /**
         * Make this change visible to all players, provided they can
         * see the objects that are being changed.
         *
         * @return See(PERHAPS).
         */
        public static See perhaps() {
            return new See(PERHAPS);
        }

        /**
         * Make this change visible only to the given player.
         *
         * @param player The {@code Player} to see this change.
         * @return A {@code See}(ONLY) with attached player.
         */
        public static See only(Player player) {
            return new See(ONLY).always(player);
        }

        // Use these to modify a See visibility.

        /**
         * Make this change visible to the given player.
         *
         * @param player The {@code Player} to see this change.
         * @return A {@code See} with attached player.
         */
        public See always(Player player) {
            seeAlways = player;
            return this;
        }

        /**
         * Make this change visible to the given player, provided the
         * player can see the objects being changed.
         *
         * @param player The {@code Player} to perhaps see this change.
         * @return A {@code See} with attached player.
         */
        public See perhapsOnly(Player player) {
            seePerhaps = player;
            return this;
        }

        /**
         * Make this change invisible to the given player.
         *
         * @param player The {@code Player} that can not see this change.
         * @return A {@code See} with attached player.
         */
        public See except(Player player) {
            seeNever = player;
            return this;
        }


        // Override Object

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(64);
            sb.append((type == ALL) ? "ALL" : (type == PERHAPS) ? "PERHAPS"
                : (type == ONLY) ? "ONLY" : "BADTYPE");
            if (seeAlways != null) {
                sb.append(",always(").append(seeAlways.getId()).append(')');
            }
            if (seePerhaps != null) {
                sb.append(",perhaps(").append(seePerhaps.getId()).append(')');
            }
            if (seeNever != null) {
                sb.append(",never(").append(seeNever.getId()).append(')');
            }
            return sb.toString();
        }
    }

    /**
     * Abstract template for all types of Change.
     */
    public abstract static class Change<T extends Message> {

        /**
         * The visibility of the change.
         */
        protected final See see;


        /**
         * Make a new Change.
         *
         * @param see The visibility.
         */
        public Change(See see) {
            this.see = see;
        }


        /**
         * Check this changes visibility to a given player.
         *
         * @param player The {@code player} to check.
         * @return The visibility result.
         */
        protected SeeCheck check(Player player) {
            return this.see.check(player);
        }

        /**
         * Does this Change operate on the given object?
         *
         * @param fcgo The {@code FreeColGameObject} to check.
         * @return True if the object is a subject of this change.
         */
        public boolean matches(FreeColGameObject fcgo) {
            return false;
        }

        /**
         * Should a player be notified of this Change?
         *
         * Override in subclasses with special cases.
         *
         * @param player The {@code Player} to consider.
         * @return True if this {@code Change} should be sent.
         */
        public boolean isNotifiable(Player player) {
            return check(player) == SeeCheck.VISIBLE;
        }

        /**
         * Are the secondary changes consequent to this Change?
         *
         * @param player The {@code Player} to consider.
         * @return The consequent {@code Change}, or null if none.
         */
        public Change consequence(Player player) {
            return null;
        }

        /**
         * Specialize a Change for a particular player.
         *
         * @param player The {@code Player} to update.
         * @return A specialized {@code Message}.
         */
        public abstract T toMessage(Player player);
    }

    /**
     * Encapsulate an attack.
     */
    private static class AttackChange extends Change<AnimateAttackMessage> {

        private final Unit attacker;
        private final Unit defender;
        private final boolean success;
        private final boolean defenderInSettlement;


        /**
         * Build a new AttackChange.
         *
         * Note that we must copy attackers and defenders because a
         * successful attacker can move, and an unsuccessful
         * participant can die, and unsuccessful defenders can be
         * captured.  Furthermore for defenders, insufficient
         * information is serialized when a unit is inside a
         * settlement, but if unscoped too much is disclosed.  So we
         * make a copy and neuter it.
         *
         * We have to remember if the defender was in a settlement
         * because by the time serialization occurs the settlement
         * might have been destroyed.
         *
         * We just have to accept that combat animation is an
         * exception to the normal visibility rules.
         *
         * @param see The visibility of this change.
         * @param attacker The {@code Unit} that is attacking.
         * @param defender The {@code Unit} that is defending.
         * @param success Did the attack succeed.
         */
        public AttackChange(See see, Unit attacker, Unit defender,
                            boolean success) {
            super(see);
            Game game = attacker.getGame();
            this.attacker = attacker.copy(game, Unit.class);
            this.defender = defender.copy(game, Unit.class);
            this.success = success;
            this.defenderInSettlement = defender.getTile().hasSettlement();
        }

        /**
         * Is the attacker visible to a player?
         *
         * @param player The {@code Player} to test.
         * @return The attacker visibility.
         */
        private boolean attackerVisible(Player player) {
            return player.canSeeUnit(this.attacker);
        }

        /**
         * Is the defender visible to a player?
         *
         * A false positive can occur as the defender may *start*
         * invisible because it is in a settlement, but the settlement
         * falls, exposing the defender (the defender dies).
         * Defenders in settlements must always be considered to be
         * invisible to other players as the animation happens while
         * the settlement stands.
         *
         * @param player The {@code Player} to test.
         * @return The defender visibility.
         */
        private boolean defenderVisible(Player player) {
            return player.canSeeUnit(this.defender)
                && (!this.defenderInSettlement || player.owns(this.defender));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isNotifiable(Player player) {
            switch (check(player)) {
            case VISIBLE: return true;
            case INVISIBLE: return false;
            case SPECIAL: break;
            }
            // Do not just use canSeeUnit because that gives a false
            // negative for units in settlements, which should be
            // animated.
            return player.owns(attacker)
                || player.owns(defender)
                || (player.canSee(attacker.getTile())
                    && player.canSee(defender.getTile()));
        }

        /**
         * {@inheritDoc}
         */
        public AnimateAttackMessage toMessage(Player player) {
            if (!isNotifiable(player)) return null;
            Unit a = (player.owns(attacker)) ? attacker
                : attacker.reduceVisibility(attacker.getTile(), player);
            Unit d = (player.owns(defender)) ? defender
                : defender.reduceVisibility(defender.getTile(), player);
            return new AnimateAttackMessage(a, d, success,
                !attackerVisible(player), !defenderVisible(player));
        }


        // Override Object

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(64);
            sb.append('[').append(getClass().getName())
                .append(' ').append(see)
                .append(' ').append(attacker.getId())
                .append('@').append(attacker.getTile().getId())
                .append(' ').append(success)
                .append(' ').append(defender.getId())
                .append('@').append(defender.getTile().getId())
                .append(']');
            return sb.toString();
        }
    }

    /**
     * Encapsulate an attribute change.
     */
    private static class AttributeChange extends Change<AttributeMessage> {

        private final String key;
        private final String value;


        /**
         * Build a new AttributeChange.
         *
         * @param see The visibility of this change.
         * @param key A key {@code String}.
         * @param value The corresponding value as a {@code String}.
         */
        public AttributeChange(See see, String key, String value) {
            super(see);
            this.key = key;
            this.value = value;
        }


        /**
         * {@inheritDoc}
         */
        public AttributeMessage toMessage(Player player) {
            return new AttributeMessage(AttributeMessage.TAG,
                                        key, value).setMergeable(true);
        }


        // Override Object

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(32);
            sb.append('[').append(getClass().getName())
                .append(' ').append(see)
                .append(' ').append(key)
                .append('=').append(value)
                .append(']');
            return sb.toString();
        }
    }

    /**
     * Encapsulate a feature change.
     */
    private static class FeatureChange extends Change<FeatureChangeMessage> {

        private final FreeColGameObject parent;
        private final FreeColObject child;
        private final boolean add;


        /**
         * Build a new FeatureChange.
         *
         * @param see The visibility of this change.
         * @param parent The {@code FreeColGameObject} to update.
         * @param child The {@code FreeColObject} value to add or remove.
         * @param add If true, add the child, if not, remove it.
         */
        public FeatureChange(See see, FreeColGameObject parent,
                             FreeColObject child, boolean add) {
            super(see);
            this.parent = parent;
            this.child = child;
            this.add = add;
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isNotifiable(Player player) {
            return check(player) == SeeCheck.VISIBLE;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public FeatureChangeMessage toMessage(Player player) {
            return (!isNotifiable(player)) ? null
                : new FeatureChangeMessage(this.parent, this.child, this.add);
        }


        // Override Object

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(32);
            sb.append('[').append(getClass().getName())
                .append(' ').append(see)
                .append(' ').append((this.add) ? "add" : "remove")
                .append(' ').append(this.child)
                .append(' ').append((this.add) ? "to" : "from")
                .append(' ').append(this.parent.getId())
                .append(']');
            return sb.toString();
        }
    }

    /**
     * Encapsulate a Message.
     */
    private static class MessageChange<T extends Message> extends Change<T> {

        private final T message;


        /**
         * Build a new MessageChange.
         *
         * @param see The visibility of this change.
         * @param message The {@code Message} to add.
         */
        public MessageChange(See see, T message) {
            super(see);

            this.message = message;
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public T toMessage(Player player) {
            return (isNotifiable(player)) ? this.message : null;
        }


        // Override Object

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(32);
            sb.append('[').append(lastPart(getClass().getName(), "."))
                .append(' ').append(see)
                .append(' ').append(message)
                .append(']');
            return sb.toString();
        }
    }

    /**
     * Encapsulate a move.
     */
    private static class MoveChange extends Change<AnimateMoveMessage> {

        private final Unit unit;
        private final Location oldLocation;
        private final Tile newTile;


        /**
         * Build a new MoveChange.
         *
         * @param see The visibility of this change.
         * @param unit The {@code Unit} that is moving.
         * @param oldLocation The location from which the unit is moving.
         * @param newTile The {@code Tile} to which the unit is moving.
         */
        public MoveChange(See see, Unit unit, Location oldLocation,
                          Tile newTile) {
            super(see);
            this.unit = unit;
            this.oldLocation = oldLocation;
            this.newTile = newTile;
        }

        /**
         * Can a player see the old tile?
         *
         * @param player The {@code Player} to test.
         * @return True if the old tile is visible.
         */
        private boolean seeOld(Player player) {
            Tile oldTile = oldLocation.getTile();
            return player.owns(unit)
                || (oldTile != null
                    && player.canSee(oldTile)
                    && !(oldTile.hasSettlement()
                        || (oldLocation instanceof Unit)));
        }

        /**
         * Can a player see the new tile?
         *
         * @param player The {@code Player} to test.
         * @return True if the new tile is visible.
         */
        private boolean seeNew(Player player) {
            return player.canSeeUnit(unit);
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isNotifiable(Player player) {
            switch (check(player)) {
            case VISIBLE: return true;
            case INVISIBLE: return false;
            case SPECIAL: break;
            }
            return seeOld(player) || seeNew(player);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Change consequence(Player player) {
            return (seeOld(player) && !seeNew(player) && !unit.isDisposed())
                ? new RemoveChange(See.only(player), unit.getLocation(),
                                   Stream.of(unit))
                : null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public AnimateMoveMessage toMessage(Player player) {
            if (!isNotifiable(player)) return null;
            final Tile oldTile = oldLocation.getTile();
            final Unit u = (player.owns(unit)) ? unit
                : unit.reduceVisibility(oldLocation.getTile(), player);
            return new AnimateMoveMessage(u, oldTile, newTile, !seeOld(player));
        }


        // Override Object

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(32);
            sb.append('[').append(getClass().getName())
                .append(' ').append(see)
                .append(' ').append(unit.getId())
                .append(' ').append(oldLocation.getId())
                .append(' ').append(newTile.getId())
                .append(']');
            return sb.toString();
        }
    }

    /**
     * Encapsulate a FreeColGameObject update.
     */
    private static class ObjectChange extends Change<UpdateMessage> {

        protected final FreeColGameObject fcgo;


        /**
         * Build a new ObjectChange for a single object.
         *
         * @param see The visibility of this change.
         * @param fcgo The {@code FreeColGameObject} to update.
         */
        public ObjectChange(See see, FreeColGameObject fcgo) {
            super(see);

            this.fcgo = fcgo;
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public boolean matches(FreeColGameObject fcgo) {
            return this.fcgo == fcgo;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isNotifiable(Player player) {
            switch (check(player)) {
            case VISIBLE: return true;
            case INVISIBLE: return false;
            case SPECIAL: break;
            }
            if (fcgo == null) return false;
            if (fcgo instanceof Unit) {
                // Units have a precise test, use that rather than
                // the more general interface-based tests.
                return player.canSeeUnit((Unit)fcgo);
            }
            // If we own it, we can see it.
            if (fcgo instanceof Ownable && player.owns((Ownable)fcgo)) {
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
            if (fcgo instanceof Location) {
                Tile tile = ((Location)fcgo).getTile();
                return player.canSee(tile);
            }
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public UpdateMessage toMessage(Player player) {
            return (!isNotifiable(player)) ? null
                : new UpdateMessage(player, Collections.singletonList(this.fcgo));
        }


        // Override Object

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(32);
            sb.append('[').append(getClass().getName())
                .append(' ').append(see)
                .append(' ').append((this.fcgo == null) ? "<null>"
                    : this.fcgo.getId())
                .append(']');
            return sb.toString();
        }
    }

    /**
     * Encapsulate a partial update of a FreeColGameObject.
     */
    private static class PartialObjectChange<T extends FreeColGameObject>
        extends Change<PartialMessage> {

        private final Map<String, String> map;


        /**
         * Build a new PartialObjectChange for a single object.
         *
         * @param see The visibility of this change.
         * @param fcgo The {@code FreeColGameObject} to update.
         * @param map A map of the field to update to the value to use.
         */
        public PartialObjectChange(See see, T fcgo, Map<String,String> map) {
            super(see);

            this.map = map;
            this.map.put(PartialMessage.ID_TAG, fcgo.getId());
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public PartialMessage toMessage(Player player) {
            return new PartialMessage(this.map);
        }


        // Override Object

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(32);
            sb.append('[').append(getClass().getName())
                .append(' ').append(see);
            for (Entry<String, String> e : this.map.entrySet()) {
                sb.append(' ').append(e.getKey())
                    .append('=').append(e.getValue());
            }
            sb.append(']');
            return sb.toString();
        }
    }

    /**
     * Encapsulate a new player change.
     */
    private static class PlayerChange extends Change<AddPlayerMessage> {

        private final List<Player> players = new ArrayList<>();


        /**
         * Build a new PlayerChange.
         *
         * @param see The visibility of this change.
         * @param newPlayers The {@code Player}s to add.
         */
        public PlayerChange(See see, List<? extends Player> newPlayers) {
            super(see);
            this.players.clear();
            this.players.addAll(newPlayers);
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public AddPlayerMessage toMessage(Player player) {
            if (!isNotifiable(player)) return null;

            return new AddPlayerMessage(player, this.players);
        }


        // Override Object

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(32);
            sb.append('[').append(getClass().getName()).append(' ').append(see);
            for (Player p : this.players) sb.append(' ').append(p.getId());
            sb.append(']');
            return sb.toString();
        }
    }

    /**
     * Encapsulates removing some objects.
     *
     * -vis: If removing settlements or units, visibility changes.
     */
    private static class RemoveChange extends Change<RemoveMessage> {

        private final Tile tile;
        private final List<? extends FreeColGameObject> contents;


        /**
         * Build a new RemoveChange for an object that is disposed.
         *
         * @param see The visibility of this change.
         * @param loc The {@code Location} where the object was.
         * @param objects The {@code FreeColGameObject}s to remove.
         */
        public RemoveChange(See see, Location loc,
                            Stream<? extends FreeColGameObject> objects) {
            super(see);
            this.tile = (loc instanceof Tile) ? (Tile)loc : null;
            this.contents = toList(objects);
        }


        /**
         * By convention, the main object is last.  All other objects are
         * internal to it and should be removed first if visible.
         *
         * @return The main object.
         */
        private FreeColGameObject getMainObject() {
            return this.contents.get(this.contents.size() - 1);
        }

        /**
         * Should we tell a player to remove all the objects or just
         * the main one?
         *
         * @param player A {@code Player} to check.
         * @return True if all the objects must go.
         */
        private boolean fullRemoval(Player player) {
            FreeColGameObject fcgo = getMainObject();
            return fcgo instanceof Ownable && player.owns((Ownable)fcgo);
        }            


        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isNotifiable(Player player) {
            switch (check(player)) {
            case VISIBLE: return true;
            case INVISIBLE: return false;
            case SPECIAL: break;
            }
            // Notifiable if the player can see the tile, and there is no
            // other-player settlement present.
            Settlement settlement;
            return tile != null
                && player.canSee(tile)
                && ((settlement = tile.getSettlement()) == null
                    || settlement.isDisposed()
                    || player.owns(settlement));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public RemoveMessage toMessage(Player player) {
            final String divertId = (tile != null) ? tile.getId()
                : player.getId();
            // The main object may be visible, but the contents are
            // only visible if the deeper ownership test succeeds.
            return new RemoveMessage(divertId,
                ((fullRemoval(player)) ? this.contents
                    : Collections.singletonList(getMainObject())));
        }


        // Override Object

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(32);
            sb.append('[').append(getClass().getName())
                .append(' ').append(see)
                .append(' ').append(((tile == null) ? "<null>" : tile.getId()));
            for (FreeColGameObject f : contents) {
                sb.append(' ').append(f.getId());
            }
            sb.append(']');
            return sb.toString();
        }
    }

    /**
     * Encapsulates a spying action.
     */
    private static class SpyChange extends Change<SpySettlementMessage> {

        private final Unit unit;
        private final Settlement settlement;


        /**
         * Build a new SpyChange.
         *
         * @param see The visibility of this change.
         * @param unit The {@code Unit} that is spying.
         * @param settlement The {@code Settlement} to spy on.
         */
        public SpyChange(See see, Unit unit, Settlement settlement) {
            super(see);
            this.unit = unit;
            this.settlement = settlement;
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public SpySettlementMessage toMessage(Player player) {
            return (!isNotifiable(player)) ? null
                : new SpySettlementMessage(unit, settlement);
        }


        // Override Object

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(32);
            sb.append('[').append(getClass().getName())
                .append(' ').append(see)
                .append(' ').append(settlement.getId())
                .append(']');
            return sb.toString();
        }
    }

    /**
     * Encapsulate a stance change.
     */
    private static class StanceChange extends Change<SetStanceMessage> {

        private final Player first;
        private final Stance stance;
        private final Player second;


        /**
         * Build a new StanceChange.
         *
         * @param see The visibility of this change.
         * @param first The {@code Player} changing stance.
         * @param stance The {@code Stance} to change to.
         * @param second The {@code Player} wrt with to change.
         */
        public StanceChange(See see, Player first, Stance stance,
                            Player second) {
            super(see);
            this.first = first;
            this.stance = stance;
            this.second = second;
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public SetStanceMessage toMessage(Player player) {
            return (!isNotifiable(player)) ? null
                : new SetStanceMessage(stance, first, second);
        }


        // Override Object

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(32);
            sb.append('[').append(getClass().getName())
                .append(' ').append(see)
                .append(' ').append(first.getId())
                .append(' ').append(stance)
                .append(' ').append(second.getId())
                .append(']');
            return sb.toString();
        }
    }

    /**
     * Simple constructor.
     */
    public ChangeSet() {
        changes = new ArrayList<>();
    }

    /**
     * Copying constructor.
     *
     * @param other The other {@code ChangeSet} to copy.
     */
    public ChangeSet(ChangeSet other) {
        changes = new ArrayList<>(other.changes);
    }


    /**
     * Are there changes present?
     *
     * @return True if there is no changes present.
     */
    public boolean isEmpty() {
        return this.changes.isEmpty();
    }

    /**
     * Clear the current changes.
     */
    public void clear() {
        this.changes.clear();
    }


    // Helper routines that should be used to construct a change set.

    /**
     * Sometimes we need to backtrack on making a change.
     *
     * @param fcgo A {@code FreeColGameObject} to remove a matching
     *     change for.
     */
    public void remove(FreeColGameObject fcgo) {
        removeInPlace(changes, c -> c.matches(fcgo));
    }

    /**
     * Helper function to add updates for multiple objects to a ChangeSet.
     *
     * @param see The visibility of this change.
     * @param objects The {@code FreeColGameObject}s that changed.
     * @return The updated {@code ChangeSet}.
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
     * @param objects The {@code FreeColGameObject}s that changed.
     * @return The updated {@code ChangeSet}.
     */
    public ChangeSet add(See see, Collection<? extends FreeColGameObject> objects) {
        for (FreeColGameObject o : objects) {
            changes.add(new ObjectChange(see, o));
        }
        return this;
    }

    /**
     * Helper function to add a Message to a ChangeSet.
     *
     * @param <T> The actual message type.
     * @param see The visibility of this change.
     * @param message The {@code Message} to add.
     * @return The updated {@code ChangeSet}.
     */
    public <T extends Message> ChangeSet add(See see, T message) {
        changes.add(new MessageChange<T>(see, message));
        return this;
    }

    /**
     * Helper function to add an attack to a ChangeSet.
     *
     * @param see The visibility of this change.
     * @param attacker The {@code Unit} that is attacking.
     * @param defender The {@code Unit} that is defending.
     * @param success Did the attack succeed?
     * @return The updated {@code ChangeSet}.
     */
    public ChangeSet addAttack(See see, Unit attacker, Unit defender,
                               boolean success) {
        changes.add(new AttackChange(see, attacker, defender, success));
        return this;
    }

    /**
     * Helper function to add a mergeable attribute setting to a ChangeSet.
     *
     * @param see The visibility of this change.
     * @param key A key {@code String}.
     * @param value The corresponding value as a {@code String}.
     * @return The updated {@code ChangeSet}.
     */
    public ChangeSet addAttribute(See see, String key, String value) {
        changes.add(new AttributeChange(see, key, value));
        return this;
    }

    /**
     * Helper function to add a removal for an object that disappears
     * (that is, moves where it can not be seen) to a ChangeSet.
     *
     * @param owner The {@code Player} that owns this object.
     * @param tile The {@code Tile} where the object was.
     * @param fcgo The {@code FreeColGameObject} that disappears.
     * @return The updated {@code ChangeSet}.
     */
    public ChangeSet addDisappear(Player owner, Tile tile,
                                  FreeColGameObject fcgo) {
        changes.add(new RemoveChange(See.perhaps().except(owner), tile,
                                     Stream.of(fcgo)));
        changes.add(new ObjectChange(See.perhaps().except(owner), tile));
        return this;
    }

    /**
     * Helper function to add or remove an Ability to a FreeColGameObject.
     *
     * @param player The owning {@code Player}.
     * @param object The {@code FreeColGameObject} to add to.
     * @param ability The {@code Ability} to add/remove.
     * @param add If true, add the ability.
     * @return The updated {@code ChangeSet}.
     */
    public ChangeSet addAbility(Player player, FreeColGameObject object,
                                Ability ability, boolean add) {
        changes.add(new FeatureChange(See.only(player), object, ability, add));
        if (add) {
            object.addAbility(ability);
        } else {
            object.removeAbility(ability);
        }
        return this;
    }

    /**
     * Helper function to add or remove a Modifier to a FreeColGameObject.
     *
     * @param player The owning {@code Player}.
     * @param object The {@code FreeColGameObject} to add to.
     * @param modifier The {@code Modifier} to add/remove.
     * @param add If true, add the modifier.
     * @return The updated {@code ChangeSet}.
     */
    public ChangeSet addModifier(Player player, FreeColGameObject object,
                                 Modifier modifier, boolean add) {
        changes.add(new FeatureChange(See.only(player), object, modifier, add));
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
     * @param game The {@code Game} to find players in.
     * @param history The {@code HistoryEvent} to add.
     * @return The updated {@code ChangeSet}.
     */
    public ChangeSet addGlobalHistory(Game game, HistoryEvent history) {
        for (Player p : game.getLiveEuropeanPlayerList()) {
            addHistory(p, history);
        }
        return this;
    }

    /**
     * Helper function to add a message to all the European players.
     *
     * @param game The {@code Game} to find players in.
     * @param omit An optional {@code Player} to omit.
     * @param message The {@code ModelMessage} to add.
     * @return The updated {@code ChangeSet}.
     */
    public ChangeSet addGlobalMessage(Game game, Player omit,
                                      ModelMessage message) {
        for (Player p : game.getLiveEuropeanPlayerList()) {
            if (p == omit) continue;
            addMessage(p, message);
        }
        return this;
    }

    /**
     * Helper function to add a history event to a ChangeSet.
     * Also adds the history to the owner.
     *
     * @param player The {@code Player} making history.
     * @param history The {@code HistoryEvent} to add.
     * @return The updated {@code ChangeSet}.
     */
    public ChangeSet addHistory(Player player, HistoryEvent history) {
        changes.add(new FeatureChange(See.only(player), player,
                                      history, true));
        player.addHistory(history);
        return this;
    }

    /**
     * Helper function to add a message to a ChangeSet.
     *
     * @param player The {@code Player} to send the message to.
     * @param message The {@code ModelMessage} to add.
     * @return The updated {@code ChangeSet}.
     */
    public ChangeSet addMessage(Player player, ModelMessage message) {
        changes.add(new FeatureChange(See.only(player), player, message, true));
        return this;
    }

    /**
     * Helper function to add a move to a ChangeSet.
     *
     * @param see The visibility of this change.
     * @param unit The {@code Unit} that is moving.
     * @param loc The location from which the unit is moving.
     * @param tile The {@code Tile} to which the unit is moving.
     * @return The updated {@code ChangeSet}.
     */
    public ChangeSet addMove(See see, Unit unit, Location loc, Tile tile) {
        changes.add(new MoveChange(see, unit, loc, tile));
        return this;
    }

    /**
     * Helper function to add a partial update change for an object to
     * a ChangeSet.
     *
     * @param <T> The actual type of object to add.
     * @param see The visibility of this change.
     * @param fcgo The {@code FreeColGameObject} to update.
     * @param fields The fields to update.
     * @return The updated {@code ChangeSet}.
     */
    public <T extends FreeColGameObject> ChangeSet addPartial(See see, T fcgo,
                                                              String... fields) {
        changes.add(new PartialObjectChange<T>(see, fcgo, asMap(fields)));
        return this;
    }

    /**
     * Helper function to add a new player to a ChangeSet.
     *
     * The added player should not be visible, as this is called
     * pre-game to update other players, but the actual player may not
     * yet even have a Game.
     *
     * @param player The new {@code Player} to add.
     * @return The updated {@code ChangeSet}.
     */
    public ChangeSet addNewPlayer(Player player) {
        changes.add(new PlayerChange(See.all().except(player),
                                     Collections.singletonList(player)));
        return this;
    }

    /**
     * Helper function to add new players to a ChangeSet.
     *
     * Used when adding the AI players en masse, or when adding the REF.
     * No care need be taken with visibility wrt AIs.
     *
     * @param players A list of new {@code Player}s to add.
     * @return The updated {@code ChangeSet}.
     */
    public ChangeSet addPlayers(List<? extends Player> players) {
        changes.add(new PlayerChange(See.all(), players));
        return this;
    }

    /**
     * Helper function to add a removal to a ChangeSet.
     *
     * -vis: If disposing of units or colonies, this routine changes
     * player visibility.
     *
     * @param see The visibility of this change.
     * @param loc The {@code Location} where the object was.
     * @param obj The {@code FreeColGameObject} to remove.
     * @return The updated {@code ChangeSet}.
     */
    public ChangeSet addRemove(See see, Location loc, FreeColGameObject obj) {
        changes.add(new RemoveChange(see, loc, obj.getDisposables()));//-vis
        return this;
    }

    /**
     * Helper function to add removals for several objects to a ChangeSet.
     *
     * @param see The visibility of this change.
     * @param loc The {@code Location} where the object was.
     * @param objects A list of {@code FreeColGameObject}s to remove.
     * @return The updated {@code ChangeSet}.
     */
    public ChangeSet addRemoves(See see, Location loc,
                                List<? extends FreeColGameObject> objects) {
        for (FreeColGameObject fcgo : objects) {
            changes.add(new RemoveChange(see, loc, fcgo.getDisposables()));
        }
        return this;
    }

    /**
     * Helper function to add a sale change to a ChangeSet.
     *
     * @param player The {@code Player} making the sale.
     * @param settlement The {@code Settlement} that is buying.
     * @param type The {@code GoodsType} bought.
     * @param price The per unit price.
     * @return The updated {@code ChangeSet}.
     */
    public ChangeSet addSale(Player player, Settlement settlement,
                             GoodsType type, int price) {
        Game game = settlement.getGame();
        LastSale sale = new LastSale(settlement, type, game.getTurn(), price);
        changes.add(new FeatureChange(See.only(player), player, sale, true));
        player.addLastSale(sale);
        return this;
    }

    /**
     * Helper function to add a spying change to a ChangeSet.
     *
     * @param unit The {@code Unit} that is spying.
     * @param settlement The {@code Settlement} to spy on.
     * @return The updated {@code ChangeSet}.
     */
    public ChangeSet addSpy(Unit unit, Settlement settlement) {
        changes.add(new SpyChange(See.only(unit.getOwner()), unit, settlement));
        return this;
    }

    /**
     * Helper function to add a stance change to a ChangeSet.
     *
     * @param see The visibility of this change.
     * @param first The {@code Player} changing stance.
     * @param stance The {@code Stance} to change to.
     * @param second The {@code Player} wrt with to change.
     * @return The updated {@code ChangeSet}.
     */
    public ChangeSet addStance(See see, Player first, Stance stance,
                               Player second) {
        changes.add(new StanceChange(see, first, stance, second));
        return this;
    }


    // Conversion of a change set to a corresponding message to a
    // specific plater.

    /**
     * Merge a change set into this one.
     *
     * @param other The other {@code ChangeSet}.
     */
    public void merge(ChangeSet other) {
        this.changes.addAll(other.changes);
    }

    /**
     * Build an update message.
     *
     * @param player The {@code Player} to send the update to.
     * @return A {@code Message} encapsulating an update of the objects to
     *     consider, or null if there is nothing to report.
     */
    public Message build(Player player) {
        if (this.changes.isEmpty()) return null;

        // Convert eligible changes to messages and sort by priority,
        // splitting out trivial mergeable attribute changes.
        List<Message> messages = new ArrayList<>();
        List<Message> diverted = new ArrayList<>();
        for (Change c : this.changes) {
            if (!c.isNotifiable(player)) continue;
            Message m = c.toMessage(player);
            List<Message> onto = (m.canMerge()) ? diverted : messages;
            onto.add(m);
            if ((c = c.consequence(player)) != null) {
                m = c.toMessage(player);
                onto = (m.canMerge()) ? diverted : messages;
                onto.add(m);
            }
        }
        messages.sort(Message.messagePriorityComparator);
        diverted.sort(Message.messagePriorityComparator);
            
        // Merge the messages where possible
        if (messages.size() > 1) {
            List<Message> more = new ArrayList<>();
            Message head = messages.remove(0);
            while (!messages.isEmpty()) {
                Message m = messages.remove(0);
                if (!head.merge(m)) {
                    more.add(head);
                    head = m;
                }
            }
            more.add(head);
            messages = more;
        }

        // Collapse to one message
        MultipleMessage mm = new MultipleMessage(messages);
        mm.setMergeable(true);
            
        // Merge in the diverted messages.
        for (Message m : diverted) mm.merge(m);

        // Do not return degenerate multiple messages
        return mm.simplify();
    }


    // Convenience functions to create change sets
    
    /**
     * Convenience function to create an i18n client error message and
     * wrap it into a change set.
     *
     * @param player An optional {@code Player} to restrict visibility to.
     * @param template An i18n template.
     * @return A new {@code ChangeSet}.
     */
    public static ChangeSet clientError(Player player, StringTemplate template) {
        See see = (player == null) ? See.all() : See.only(player);
        return clientError(see, template);
    }

    /**
     * Convenience function to create an i18n client error message and
     * wrap it into a change set.
     *
     * @param see The message visibility.
     * @param template An i18n template.
     * @return A new {@code ChangeSet}.
     */
    public static ChangeSet clientError(See see, StringTemplate template) {
        ChangeSet cs = new ChangeSet();
        if (see == null) see = See.all();
        cs.add(see, new ErrorMessage(template));
        return cs;
    }

    /**
     * Convenience function to create a non-i18n client error message
     * and wrap it into a change set.
     *
     * @param player An optional {@code Player} to restrict visibility to.
     * @param message The message.
     * @return A new {@code ChangeSet}.
     */
    public static ChangeSet clientError(Player player, String message) {
        See see = (player == null) ? See.all() : See.only(player);
        return clientError(see, message);
    }

    /**
     * Convenience function to create a non-i18n client error message
     * and wrap it into a change set.
     *
     * @param see The message visibility.
     * @param message A non-i18n message.
     * @return A new {@code ChangeSet}.
     */
    public static ChangeSet clientError(See see, String message) {
        ChangeSet cs = new ChangeSet();
        if (see == null) see = See.all();
        cs.add(see, new ErrorMessage(message));
        return cs;
    }

    /**
     * Convenience function to create a change set containing a message.
     *
     * @param player An optional {@code Player} to restrict visibility to.
     * @param message The {@code Message} to wrap.
     * @return A new {@code ChangeSet}.
     */
    public static ChangeSet simpleChange(Player player, Message message) {
        See see = (player == null) ? See.all() : See.only(player);
        return simpleChange(see, message);
    }

    /**
     * Convenience function to create a change set containing a message.
     *
     * @param see The message visibility.
     * @param message The {@code Message} to wrap.
     * @return A new {@code ChangeSet}.
     */
    public static ChangeSet simpleChange(See see, Message message) {
        ChangeSet cs = new ChangeSet();
        cs.add((see == null) ? See.all() : see, message);
        return cs;
    }
    
    /**
     * Get a new ChangeSet that changes a player AI state.
     *
     * @param player The {@code Player} to change.
     * @param ai The new AI state.
     * @return The new {@code ChangeSet}.
     */
    public static ChangeSet aiChange(Player player, boolean ai) {
        return simpleChange(See.all(), new SetAIMessage(player, ai));
    }


    // Override Object

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Change c : this.changes) {
            sb.append(c).append('\n');
        }
        return sb.toString();
    }
}
