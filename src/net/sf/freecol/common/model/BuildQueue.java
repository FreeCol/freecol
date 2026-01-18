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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.sf.freecol.common.option.GameOptions;
import static net.sf.freecol.common.util.CollectionUtils.*;


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
