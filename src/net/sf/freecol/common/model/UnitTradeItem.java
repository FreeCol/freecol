
package net.sf.freecol.common.model;



import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;


public class UnitTradeItem extends TradeItem {
    
    private Unit unit;
        
    public UnitTradeItem(Game game, Player source, Player destination, Unit unit) {
        super(game, "tradeItem.unit", source, destination);
        this.unit = unit;
    }

    public boolean isValid() {
        return (unit.getOwner() == source);
    }

    public void makeTrade() {
        unit.setOwner(destination);
    }


    /**
     * Initialize this object from an XML-representation of this object.
     * @param in The input stream with the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     */
    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        super.readFromXMLImpl(in);
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

