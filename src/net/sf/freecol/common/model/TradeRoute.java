
package net.sf.freecol.common.model;

import java.util.ArrayList;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.Element;





/**
* A trade route.
*/
public class TradeRoute extends FreeColGameObject {

    public static final String  COPYRIGHT = "Copyright (C) 2003-2007 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    //private static final Logger logger = Logger.getLogger(TradeRoute.class.getName());

    // keeps track of where we are
    private int index = 0;

    /**
     * The name of this trade route.
     */
    private String name;

    /**
     * Describe game here.
     */
    private Game game;

    /**
     * Describe modified here.
     */
    private boolean modified;

    /**
     * A list of stops.
     */
    private ArrayList<Stop> stops = new ArrayList<Stop>();

    public TradeRoute(Game game, String name) {
        super(game);
        this.name = name;
    }


    public TradeRoute(Game game, XMLStreamReader in) throws XMLStreamException {
        super(game, in);
        readFromXML(in);
    }
    
    public TradeRoute(Game game, Element e) {
        super(game, e);
        readFromXMLElement(e);
    }

    /**
     * Get the <code>Modified</code> value.
     *
     * @return a <code>boolean</code> value
     */
    public final boolean isModified() {
        return modified;
    }

    /**
     * Set the <code>Modified</code> value.
     *
     * @param newModified The new Modified value.
     */
    public final void setModified(final boolean newModified) {
        this.modified = newModified;
    }

    /**
     * Get the <code>Name</code> value.
     *
     * @return a <code>String</code> value
     */
    public final String getName() {
        return name;
    }

    /**
     * Set the <code>Name</code> value.
     *
     * @param newName The new Name value.
     */
    public final void setName(final String newName) {
        this.name = newName;
    }


    public void addStop(Stop stop) {
        stops.add(stop);
    }


    public String toString() {
        return getName();
    }

    /**
     * Get the <code>Stops</code> value.
     *
     * @return an <code>ArrayList<Stop></code> value
     */
    public final ArrayList<Stop> getStops() {
        return stops;
    }

    /**
     * Set the <code>Stops</code> value.
     *
     * @param newStops The new Stops value.
     */
    public final void setStops(final ArrayList<Stop> newStops) {
        this.stops = newStops;
    }

    /**
     * Get the next stop.
     * 
     * @return the next stop.
     */
    public Stop nextStop() {
        if (stops.size() == 0) {
            return null;
        } else if (index >= stops.size()) {
            index = 0;
        }
        Stop result = stops.get(index);
        index++;
        return result;
    }

    public void newTurn() {}

    public TradeRoute clone() {
        TradeRoute result = new TradeRoute(getGame(), new String(getName()));
        for (Stop stop : getStops()) {
            result.addStop(stop.clone());
        }
        return result;
    }

    public class Stop {

        private Location location;
        private ArrayList<Integer> cargo;

        public Stop(Location location) {
            this.location = location;
            this.cargo = new ArrayList<Integer>();
        }

        public Stop(Location location, ArrayList<Integer> cargo) {
            this.location = location;
            this.cargo = cargo;
        }

        public Stop(XMLStreamReader in) throws XMLStreamException {
            readFromXML(in);
        }

        /**
         * Get the <code>Location</code> value.
         *
         * @return a <code>Location</code> value
         */
        public final Location getLocation() {
            return location;
        }

        /**
         * Get the <code>Cargo</code> value.
         *
         * @return an <code>ArrayList<Integer></code> value
         */
        public final ArrayList<Integer> getCargo() {
            return cargo;
        }

        public void addCargo(Integer newCargo) {
            cargo.add(newCargo);
        }

        public String toString() {
            return getLocation().getLocationName();
        }

        public Stop clone() {
            Stop result = new Stop(getLocation());
            for (Integer cargo : getCargo()) {
                result.addCargo(new Integer(cargo));
            }
            return result;
        }

        public void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
            out.writeStartElement(getXMLElementTagName());
            out.writeAttribute("location", getLocation().getID());
            int[] cargoArray = new int[cargo.size()];
            for (int index = 0; index < cargoArray.length; index++) {
                cargoArray[index] = cargo.get(index).intValue();
            }
            toArrayElement("cargo", cargoArray, out);
            out.writeEndElement();
        }

        /**
         * Initialize this object from an XML-representation of this object.
         * @param in The input stream with the XML.
         */
        public void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
            location = (Location) getGame().getFreeColGameObject(in.getAttributeValue(null, "location"));
            int[] cargoArray = readFromArrayElement("cargo", in, new int[0]);
        }



        /**
         * Returns the tag name of the root element representing this object.
         *
         * @return "tradeRouteStop".
         */
        public String getXMLElementTagName() {
            return "tradeRouteStop";
        }

            
    }

    public void toXMLImpl(XMLStreamWriter out, Player player, boolean showAll, boolean toSavedGame)
        throws XMLStreamException {
        // Start element:
        out.writeStartElement(getXMLElementTagName());

        out.writeAttribute("ID", getID());
        out.writeAttribute("name", getName());
        for (Stop stop : stops) {
            stop.toXMLImpl(out);
        }

        out.writeEndElement();
    }



    /**
     * Initialize this object from an XML-representation of this object.
     * @param in The input stream with the XML.
     */
    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        setID(in.getAttributeValue(null, "ID"));
        setName(in.getAttributeValue(null, "name"));

        // Read child elements:
        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            stops.add(new Stop(in));
        }
    }

    /**
    * Returns the tag name of the root element representing this object.
    *
    * @return "tradeRoute".
    */
    public static String getXMLElementTagName() {
        return "tradeRoute";
    }


}
