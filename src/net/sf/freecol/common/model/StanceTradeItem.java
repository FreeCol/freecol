
package net.sf.freecol.common.model;



import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;


public class StanceTradeItem extends TradeItem {
    
    /**
     * The stance between source and destination.
     */
    private int stance;
        
    /**
     * Creates a new <code>StanceTradeItem</code> instance.
     *
     * @param game a <code>Game</code> value
     * @param source a <code>Player</code> value
     * @param destination a <code>Player</code> value
     * @param stance an <code>int</code> value
     */
    public StanceTradeItem(Game game, Player source, Player destination, int stance) {
        super(game, "tradeItem.stance", source, destination);
        this.stance = stance;
    }

    /**
     * Creates a new <code>StanceTradeItem</code> instance.
     *
     * @param game a <code>Game</code> value
     * @param in a <code>XMLStreamReader</code> value
     */
    public StanceTradeItem(Game game, XMLStreamReader in) throws XMLStreamException {
        super(game, in);
        readFromXMLImpl(in);
    }

    /**
     * Get the <code>Stance</code> value.
     *
     * @return an <code>int</code> value
     */
    public final int getStance() {
        return stance;
    }

    /**
     * Set the <code>Stance</code> value.
     *
     * @param newStance The new Stance value.
     */
    public final void setStance(final int newStance) {
        this.stance = newStance;
    }

    /**
     * Returns whether this TradeItem is valid.
     *
     * @return a <code>boolean</code> value
     */
    public boolean isValid() {
        return (stance == Player.WAR ||
                stance == Player.CEASE_FIRE ||
                stance == Player.PEACE ||
                stance == Player.ALLIANCE);
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
        getSource().setStance(getDestination(), stance);
        getDestination().setStance(getSource(), stance);
    }


    /**
     * Initialize this object from an XML-representation of this object.
     * @param in The input stream with the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     */
    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        super.readFromXMLImpl(in);
        this.stance = Integer.parseInt(in.getAttributeValue(null, "stance"));
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
        out.writeAttribute("stance", Integer.toString(this.stance));
        out.writeEndElement();
    }
    
    /**
     * Gets the tag name of the root element representing this object.
     * @return "goods".
     */
    public static String getXMLElementTagName() {
        return "stanceTradeItem";
    }

}

