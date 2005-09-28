
package net.sf.freecol.server.ai;

import net.sf.freecol.common.model.Locatable;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.server.ai.mission.TransportMission;


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
    * @return The source for this <codeTransportable</code>.
    */
    public Location getTransportSource();
    

    /**
    * Returns the destination for this <code>Transportable</code>.
    * This can either be the target {@link Tile} of the transport
    * or the target for the entire <code>Transportable</code>'s
    * mission. The target for the tansport is determined by
    * {@link TransportMission} in the latter case.
    *
    * @return The destination for this <codeTransportable</code>.
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
    
    public String getID();
}
