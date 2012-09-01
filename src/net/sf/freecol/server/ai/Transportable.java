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


package net.sf.freecol.server.ai;

import java.util.Comparator;

import net.sf.freecol.common.model.Locatable;
import net.sf.freecol.common.model.Location;


/**
 * A single item in a carrier's transport list.  Any {@link Locatable}
 * which should be able to be transported by a carrier using the {@link
 * net.sf.freecol.server.ai.mission.TransportMission}, needs an AI
 * object implementing this interface.
 *
 * @see net.sf.freecol.server.ai.mission.TransportMission
 */
public interface Transportable {

    /**
     * The priority for a goods that are hitting the warehouse limit.
     */
    public static final int IMPORTANT_DELIVERY = 110;

    /**
     * The priority for goods that provide at least a full cargo load.
     */
    public static final int FULL_DELIVERY = 100;

    /**
     * The priority of tools intended for a Colony with none stored
     * at the present (and with no special needs).
     */
    public static final int TOOLS_FOR_COLONY_PRIORITY = 10;

    /**
     * The extra priority value added to the base value of
     * {@link #TOOLS_FOR_COLONY_PRIORITY}
     * for each ColonyTile needing a terrain improvement.
     */
    public static final int TOOLS_FOR_IMPROVEMENT = 10;

    /**
     * The extra priority value added to the base value of
     * {@link #TOOLS_FOR_COLONY_PRIORITY}
     * if a Pioneer is lacking tools
     */
    public static final int TOOLS_FOR_PIONEER = 90;

    /**
     * The extra priority value added to the base value of
     * {@link #TOOLS_FOR_COLONY_PRIORITY}
     * if a building is lacking tools. The number of tools
     * is also added to the total amount.
     */
    public static final int TOOLS_FOR_BUILDING = 100;

    /**
     * A comparator that sorts by transport priority.
     */
    public static final Comparator<Transportable> transportableComparator
        = new Comparator<Transportable>() {
            public int compare(Transportable t1, Transportable t2) {
                return t2.getTransportPriority() - t1.getTransportPriority();
            }
        };


    /**
     * Gets the number of cargo slots taken by this transportable.
     *
     * @return The number of cargo slots taken.
     */
    public int getSpaceTaken();

    /**
     * Returns the source for this <code>Transportable</code>.
     * This is normally the location of the
     * {@link #getTransportLocatable locatable}.
     *
     * @return The source for this <code>Transportable</code>.
     */
    public Location getTransportSource();

    /**
     * Returns the destination for this <code>Transportable</code>.
     * This can either be the target {@link
     * net.sf.freecol.common.model.Tile} of the transport or the target
     * for the entire <code>Transportable</code>'s mission. The target
     * for the tansport is determined by {@link
     * net.sf.freecol.server.ai.mission.TransportMission} in the latter
     * case.
     *
     * @return The destination for this <code>Transportable</code>.
     */
    public Location getTransportDestination();

    /**
     * Gets the priority of transporting this <code>Transportable</code>
     * to it's destination.
     *
     * @return The priority of the transport.
     */
    public int getTransportPriority();

    /**
     * Sets the priority of getting the goods to the {@link
     * #getTransportDestination}.
     *
     * @param transportPriority The priority.
     */
    public void setTransportPriority(int transportPriority);

    /**
     * Increases the transport priority of this <code>Transportable</code>.
     * This method gets called every turn the <code>Transportable</code>
     * have not been put on a carrier's transport list.
     */
    public void increaseTransportPriority();

    /**
     * Gets the <code>Locatable</code> which should be transported.
     * @return The <code>Locatable</code>.
     */
    public Locatable getTransportLocatable();

    /**
     * Gets the carrier responsible for transporting this
     * <code>Transportable</code>.
     *
     * @return The <code>AIUnit</code> which has this
     *     <code>Transportable</code> in it's transport list. This
     *     <code>Transportable</code> has not been scheduled for
     *     transport if this value is <code>null</code>.
     */
    public AIUnit getTransport();

    /**
     * Sets the carrier responsible for transporting this
     * <code>Transportable</code>.  This method should also add this
     * <code>Transportable</code> to the given carrier's transport
     * list.
     *
     * @param transport The <code>AIUnit</code> which has this
     *     <code>Transportable</code> in it's transport list. This
     *     <code>Transportable</code> has not been scheduled for
     *     transport if this value is <code>null</code>.
     */
    public void setTransport(AIUnit transport);

    /**
     * Aborts the given <code>Wish</code>.
     * @param w The <code>Wish</code> to be aborted.
     */
    public void abortWish(Wish w);

    /**
     * Returns the ID of the <code>AIObject</code> implementing
     * this interface.
     *
     * @return The ID of the <code>AIObject</code>. This is normally
     *     the ID of the
     *     {@link net.sf.freecol.common.model.FreeColGameObject} that
     *     object represents.
     */
    public String getId();
}
