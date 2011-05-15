/**
 *  Copyright (C) 2002-2011  The FreeCol Team
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


import net.sf.freecol.common.model.Locatable;
import net.sf.freecol.common.model.Location;


/**
* A single item in a carrier's transport list.
* Any {@link Locatable} which should be able to be transported
* by a carrier using the {@link TransportMission}, needs an
* AI object implementing this interface.
*
* @see TransportMission
*/
public interface Transportable {


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
    * This can either be the target {@link Tile} of the transport
    * or the target for the entire <code>Transportable</code>'s
    * mission. The target for the tansport is determined by
    * {@link TransportMission} in the latter case.
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
    * Gets the carrier responsible for transporting this <code>Transportable</code>.
    *
    * @return The <code>AIUnit</code> which has this <code>Transportable</code>
    *         in it's transport list. This <code>Transportable</code> has not been
    *         scheduled for transport if this value is <code>null</code>.
    *
    */
    public AIUnit getTransport();
    
    
    /**
    * Sets the carrier responsible for transporting this <code>Transportable</code>.
    * This method should also add this <code>Transportable</code> to the given
    * carrier's transport list.
    *
    * @param transport The <code>AIUnit</code> which has this <code>Transportable</code>
    *         in it's transport list. This <code>Transportable</code> has not been
    *         scheduled for transport if this value is <code>null</code>.
    *
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
     *         the ID of the {@link FreeColGameObject} that object
     *         represents.
     */    
    public String getId();
}
