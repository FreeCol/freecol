
package net.sf.freecol.common.model;



import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;


public class ColonyTradeItem extends TradeItem {
    
    /**
     * The colony to change hands.
     */
    private Colony colony;
        
    /**
     * Creates a new <code>ColonyTradeItem</code> instance.
     *
     * @param game a <code>Game</code> value
     * @param source a <code>Player</code> value
     * @param destination a <code>Player</code> value
     * @param colony a <code>Colony</code> value
     */
    public ColonyTradeItem(Game game, Player source, Player destination, Colony colony) {
        super(game, "tradeItem.colony", source, destination);
        this.colony = colony;
    }

    /**
     * Creates a new <code>ColonyTradeItem</code> instance.
     *
     * @param game a <code>Game</code> value
     * @param in a <code>XMLStreamReader</code> value
     */
    public ColonyTradeItem(Game game, XMLStreamReader in) throws XMLStreamException {
        super(game, in);
        readFromXMLImpl(in);
    }

    /**
     * Get the <code>Colony</code> value.
     *
     * @return a <code>Colony</code> value
     */
    public final Colony getColony() {
        return colony;
    }

    /**
     * Set the <code>Colony</code> value.
     *
     * @param newColony The new Colony value.
     */
    public final void setColony(final Colony newColony) {
        this.colony = newColony;
    }

    public boolean isValid() {
        return (colony.getOwner() == getSource() &&
                getDestination().isEuropean());
    }

    public boolean isUnique() {
        return false;
    }
    
    public void makeTrade() {
        colony.setOwner(getDestination());
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
        String colonyID = in.getAttributeValue(null, "colony");
        this.colony = (Colony) getGame().getFreeColGameObject(colonyID);
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
        out.writeAttribute("colony", this.colony.getID());
        out.writeEndElement();
    }
    
    /**
     * Gets the tag name of the root element representing this object.
     * @return "goods".
     */
    public static String getXMLElementTagName() {
        return "colonyTradeItem";
    }

}

