/**
 *  Copyright (C) 2002-2024   The FreeCol Team
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

package net.sf.freecol.common.model;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.logging.Logger;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.sf.freecol.common.model.Colony.NoBuildReason;
import net.sf.freecol.common.model.Constants.IntegrityType;
import net.sf.freecol.common.option.GameOptions;
import net.sf.freecol.common.util.LogBuilder;

import static net.sf.freecol.common.util.CollectionUtils.*;
import static net.sf.freecol.common.util.RandomUtils.*;


/**
 * A queue of things for a colony to build.
 */
public class BuildQueue<T extends BuildableType> implements Consumer {

    public static enum CompletionAction {
        /**
         * Always remove the completed item. Not used by any build
         * queue at the moment.
         */
        REMOVE,
        /**
         * Remove the completed item unless it is the last item and
         * several instances of the item can co-exist (which is true
         * for units, but not buildings). This is the strategy used by
         * the colony build queue.
         */
        REMOVE_EXCEPT_LAST,
        /**
         * Shuffle the items rather than remove the completed
         * item. This is the strategy used by the colony population
         * queue.
         */
        SHUFFLE,
        /**
         * Remove the completed item and add a random new item. This
         * is the strategy of the immigration queue (which is not
         * implemented as a build queue at the moment, however).
         */
        ADD_RANDOM
    };

    private static final Logger logger = Logger.getLogger(BuildQueue.class.getName());

    /** A list of Buildable items. */
    private final Deque<T> queue = new ArrayDeque<>();

    /** What to do when an item has been completed. */
    private CompletionAction completionAction = CompletionAction.REMOVE;

    /** The build priority. */
    private int priority = COLONY_PRIORITY;

    /** The colony to queue buildables for. */
    private final Colony colony;


    /**
     * Create a new build queue.
     *
     * @param colony The {@code Colony} to build for.
     * @param action The action on build completion.
     * @param priority The build priority.
     */
    public BuildQueue(Colony colony, CompletionAction action, int priority) {
        this.colony = colony;
        this.completionAction = action;
        this.priority = priority;
    }


    public void clear() {
        queue.clear();
    }

    public void add(T buildable) {
        queue.addLast(buildable);
    }

    public List<T> getValues() {
        return Collections.unmodifiableList(new ArrayList<>(queue));
    }

    public void setValues(List<T> values) {
        clear();
        values.forEach(queue::addLast);
    }

    public void remove(int index) {
        List<T> list = new ArrayList<>(queue);
        list.remove(index);
        queue.clear();
        queue.addAll(list);
    }

    public int size() {
        return queue.size();
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    public final CompletionAction getCompletionAction() {
        return completionAction;
    }

    public final void setCompletionAction(final CompletionAction newCompletionAction) {
        completionAction = newCompletionAction;
    }


    /**
     * Get the type of building currently being built.
     *
     * @return The type of building currently being built.
     */
    public T getCurrentlyBuilding() {
        return queue.peekFirst();
    }

    /**
     * Sets the current type of buildable to be built and if it is a building
     * insist that there is only one in the queue.
     *
     * @param buildable The {@code T} to build.
     */
    public void setCurrentlyBuilding(T buildable) {
        if (buildable == null) {
            clear();
            return;
        }

        if (buildable.isUniqueInQueue() && queue.contains(buildable)) {
            queue.remove(buildable);
        }

        queue.addFirst(buildable);
    }

    /**
     * Get the {@code ProductionInfo} for this BuildQueue.
     *
     * @param input A list of input {@code AbstractGoods}.
     * @return The {@code ProductionInfo} for this BuildQueue.
     */
    public ProductionInfo getProductionInfo(List<AbstractGoods> input) {
        ProductionInfo result = new ProductionInfo();
        T current = getCurrentlyBuilding();

        if (current == null) {
            return result;
        }

        final boolean overflow = colony.getSpecification()
                                       .getBoolean(GameOptions.SAVE_PRODUCTION_OVERFLOW);

        List<AbstractGoods> consumption = new ArrayList<>();

        for (AbstractGoods required : current.getRequiredGoodsList()) {

            AbstractGoods available = find(input, AbstractGoods.matches(required.getType()));

            if (available == null || available.getAmount() < required.getAmount()) {
                return result; // Not enough goods → no production
            }

            int amount = computeConsumptionAmount(required, available, overflow);
            consumption.add(new AbstractGoods(required.getType(), amount));
        }

        result.setConsumption(consumption);
        return result;
    }

    /**
     * Determines how many goods to consume for production,
     * using overflow rules and storable‑goods behavior.
     *
     * @return the amount of goods to consume
     */
    private int computeConsumptionAmount(AbstractGoods required, AbstractGoods available, boolean overflow) {
        if (overflow || required.getType().isStorable()) {
            return required.getAmount();
        }
        return available.getAmount();
    }

    /**
     * Apply the completion action to this queue.
     * Only mutates the queue; no colony side effects.
     */
    public void applyCompletionAction(Random random) {
        switch (completionAction) {

        case SHUFFLE:
            // Shuffle queue items (except the one just completed)
            if (queue.size() > 1) {
                List<T> list = new ArrayList<>(queue);
                Collections.shuffle(list, random);
                queue.clear();
                queue.addAll(list);
            }
            break;

        case REMOVE_EXCEPT_LAST:
            // Keep last unit if repeating; otherwise remove first
            if (!(queue.size() == 1
                  && queue.peekFirst() instanceof UnitType)) {
                queue.removeFirst();
            }
            break;

        case ADD_RANDOM:
            queue.removeFirst();
            T randomItem = pickRandomBuildable(random);
            if (randomItem != null) {
                queue.addLast(randomItem);
            }
            break;

        case REMOVE:

        default:
            // Default: remove completed item
            queue.removeFirst();
            break;
        }
    }

    /**
     * Selects a random valid buildable for ADD_RANDOM queues.
     * Used mainly by population queues to pick a recruitable UnitType.
     * Returns null if no candidates exist.
     */
    @SuppressWarnings("unchecked")
    private T pickRandomBuildable(Random random) {
        // Only meaningful for UnitType queues
        if (!(colony instanceof Colony)) return null;

        List<UnitType> candidates = colony.getSpecification()
            .getUnitTypeList().stream()
            .filter(UnitType::isRecruitable)
            .collect(Collectors.toList());

        if (candidates.isEmpty()) return null;

        return (T) getRandomMember(
                logger,
                "ADD_RANDOM",
                candidates,
                random
            );
    }

    /**
     * Return the next buildable item, removing invalid ones.
     * No messages or side effects; pure queue logic.
     */
    public T getNextBuildable(Colony colony) {

        List<BuildableType> assumeBuilt = new ArrayList<>();

        for (T bt : new ArrayList<>(queue)) {

            NoBuildReason reason = colony.getNoBuildReason(bt, assumeBuilt);

            switch (reason) {

            case NONE:
            case LIMIT_EXCEEDED:
                // Valid or semi-valid → return it
                return bt;

            case NOT_BUILDABLE:
                // Redundant building (e.g. schoolhouse already built)
                assumeBuilt.add(bt);
                queue.remove(bt);
                break;

            default:
                // Invalid → remove but do NOT add to assumeBuilt
                queue.remove(bt);
                break;
            }
        }

        return null;
    }

    /**
     * Check queue integrity in order.
     * If fix=true, remove invalid items.
     */
    public IntegrityType checkIntegrity(Colony colony,
                                        boolean fix,
                                        LogBuilder lb) {

        IntegrityType result = IntegrityType.INTEGRITY_GOOD;
        List<BuildableType> assumeBuilt = new ArrayList<>();

        List<T> items = new ArrayList<>(queue);

        for (int i = 0; i < items.size(); i++) {
            T bt = items.get(i);
            NoBuildReason reason = colony.getNoBuildReason(bt, assumeBuilt);

            if (reason == NoBuildReason.NONE) {
                assumeBuilt.add(bt);
            } else if (fix) {
                if (lb != null) lb.add("\n  Invalid queue item removed: ", bt.getId());
                queue.remove(bt);
                result = result.fix();
            } else {
                if (lb != null) lb.add("\n  Invalid queue item: ", bt.getId());
                result = result.fail();
            }
        }

        return result;
    }

    // Interface Consumer

    /**
     * {@inheritDoc}
     */
    @Override
    public List<AbstractGoods> getConsumedGoods() {
        T current = getCurrentlyBuilding();
        return (current == null) ? Collections.<AbstractGoods>emptyList()
            : current.getRequiredGoodsList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getPriority() {
        return priority;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Stream<Modifier> getConsumptionModifiers(String id) {
        return Stream.<Modifier>empty();
    }


    // Override Object

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        String items = queue.stream()
                            .map(BuildableType::getId)
                            .collect(Collectors.joining(" "));
        return "[BuildQueue (" + colony.getName() + ") " + items + "]";
    }
}
