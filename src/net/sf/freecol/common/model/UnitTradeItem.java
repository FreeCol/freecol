
package net.sf.freecol.common.model;



import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;


public class UnitTradeItem extends TradeItem {
    
    /**
     * The unit to change hands.
     */
    private Unit unit;
        
    /**
     * Creates a new <code>UnitTradeItem</code> instance.
     *
     * @param game a <code>Game</code> value
     * @param source a <code>Player</code> value
     * @param destination a <code>Player</code> value
     * @param unit an <code>Unit</code> value
     */
    public UnitTradeItem(Game game, Player source, Player destination, Unit unit) {
        super(game, "tradeItem.unit", source, destination);
        this.unit = unit;
    }

    /**
     * Creates a new <code>UnitTradeItem</code> instance.
     *
     * @param game a <code>Game</code> value
     * @param in a <code>XMLStreamReader</code> value
     */
    public UnitTradeItem(Game game, XMLStreamReader in) throws XMLStreamException {
        super(game, in);
        readFromXMLImpl(in);
    }

    /**
     * Get the <code>Unit</code> value.
     *
     * @return an <code>Unit</code> value
     */
    public final Unit getUnit() {
        return unit;
    }

    /**
     * Set the <code>Unit</code> value.
     *
     * @param newUnit The new Unit value.
     */
    public final void setUnit(final Unit newUnit) {
        this.unit = newUnit;
    }

    public boolean isValid() {
        return (unit.getOwner() == getSource());
    }

    public boolean isUnique() {
        return false;
    }
    
    public void makeTrade() {
        unit.setOwner(getDestination());
    }


    /**
     * Initialize this object from an XML-representation of this object.
     * @param in The input stream with the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     */
    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        setID(in.getAttributeValue(null, "ID"));
        String sourceID = in.getAttributeValue(null, "source");
        setSource((Player) getGame().getFreeColGameObject(sourceID));
        String destinationID = in.getAttributeValue(null, "destination");
        setDestination((Player) getGame().getFreeColGameObject(destinationID));
        //super.readFromXMLImpl(in);
        String unitID = in.getAttributeValue(null, "unit");
        this.unit = (Unit) getGame().getFreeColGameObject(unitID);
        in.nextTag();
    }

    /**
     * This method writes an XML-representation of this object to
     * the given stream.
     * 
     * <br><br>
     * 
     * Only attributes visible to the given <code>Player</code> will 
     * be added to that representation if <code>showAll</code> is
     * set to <code>false</code>.
     *  
     * @param out The target stream.
     * @param player The <code>Player</code> this XML-representation 
     *      should be made for, or <code>null</code> if
     *      <code>showAll == true</code>.
     * @throws XMLStreamException if there are any problems writing
     *      to the stream.
     */
    public void toXML(XMLStreamWriter out, Player player) throws XMLStreamException {
        out.writeStartElement(getXMLElementTagName());
        super.toXML(out, player);
        out.writeAttribute("unit", this.unit.getID());
        out.writeEndElement();
    }
    
    /**
     * Gets the tag name of the root element representing this object.
     * @return "goods".
     */
    public static String getXMLElementTagName() {
        return "unitTradeItem";
    }

}

