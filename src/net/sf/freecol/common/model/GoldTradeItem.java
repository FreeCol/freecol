
package net.sf.freecol.common.model;



import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;


public class GoldTradeItem extends TradeItem {
    
    /**
     * The amount of gold to change hands.
     */
    private int gold;
        
    /**
     * Creates a new <code>GoldTradeItem</code> instance.
     *
     * @param game a <code>Game</code> value
     * @param source a <code>Player</code> value
     * @param destination a <code>Player</code> value
     * @param gold an <code>int</code> value
     */
    public GoldTradeItem(Game game, Player source, Player destination, int gold) {
        super(game, "tradeItem.gold", source, destination);
        this.gold = gold;
    }

    /**
     * Creates a new <code>GoldTradeItem</code> instance.
     *
     * @param game a <code>Game</code> value
     * @param in a <code>XMLStreamReader</code> value
     */
    public GoldTradeItem(Game game, XMLStreamReader in) throws XMLStreamException {
        super(game, in);
        readFromXMLImpl(in);
    }


    /**
     * Get the <code>Gold</code> value.
     *
     * @return an <code>int</code> value
     */
    public final int getGold() {
        return gold;
    }

    /**
     * Set the <code>Gold</code> value.
     *
     * @param newGold The new Gold value.
     */
    public final void setGold(final int newGold) {
        this.gold = newGold;
    }

    /**
     * Returns whether this TradeItem is valid.
     *
     * @return a <code>boolean</code> value
     */
    public boolean isValid() {
        return ((gold >= 0) && (getSource().getGold() >= gold));
    }

    /**
     * Returns whether this TradeItem must be unique. This is true for
     * the StanceTradeItem and the GoldTradeItem, and false for all
     * others.
     *
     * @return a <code>boolean</code> value
     */
    public boolean isUnique() {
        return true;
    }

    /**
     * Concludes the trade.
     *
     */
    public void makeTrade() {
        getSource().modifyGold(-gold);
        getDestination().modifyGold(gold);
    }


    /**
     * Initialize this object from an XML-representation of this object.
     * @param in The input stream with the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     */
    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        super.readFromXMLImpl(in);
        this.gold = Integer.parseInt(in.getAttributeValue(null, "gold"));
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
    public void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        out.writeStartElement(getXMLElementTagName());
        super.toXML(out);
        out.writeAttribute("gold", Integer.toString(this.gold));
        out.writeEndElement();
    }
    
    /**
     * Gets the tag name of the root element representing this object.
     * @return "goods".
     */
    public static String getXMLElementTagName() {
        return "goldTradeItem";
    }

}
