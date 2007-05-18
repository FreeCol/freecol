
package net.sf.freecol.common.model;



import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;


public class ColonyTradeItem extends TradeItem {
    
    /**
     * The colony to change hands.
     */
    private Colony colony;
        
    public ColonyTradeItem(Game game, Player source, Player destination, Colony colony) {
        super(game, "tradeItem.colony", source, destination);
        this.colony = colony;
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
        super.readFromXMLImpl(in);
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

