
package net.sf.freecol.common.model;


import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;


public class GoodsTradeItem extends TradeItem {
    
    /**
     * The goods to change hands.
     */
    private Goods goods;

    /**
     * The settlement where the trade is to take place.
     */
    private Settlement settlement;
        
    /**
     * Creates a new <code>GoodsTradeItem</code> instance.
     *
     * @param game a <code>Game</code> value
     * @param source a <code>Player</code> value
     * @param destination a <code>Player</code> value
     * @param goods a <code>Goods</code> value
     * @param settlement a <code>Settlement</code> value
     */
    public GoodsTradeItem(Game game, Player source, Player destination, Goods goods, Settlement settlement) {
        super(game, "tradeItem.goods", source, destination);
        this.goods = goods;
        this.settlement = settlement;
    }

    /**
     * Creates a new <code>GoodsTradeItem</code> instance.
     *
     * @param game a <code>Game</code> value
     * @param in a <code>XMLStreamReader</code> value
     */
    public GoodsTradeItem(Game game, XMLStreamReader in) throws XMLStreamException {
        super(game, in);
        readFromXMLImpl(in);
    }

    /**
     * Get the <code>Goods</code> value.
     *
     * @return a <code>Goods</code> value
     */
    public final Goods getGoods() {
        return goods;
    }

    /**
     * Set the <code>Goods</code> value.
     *
     * @param newGoods The new Goods value.
     */
    public final void setGoods(final Goods newGoods) {
        this.goods = newGoods;
    }

    /**
     * Get the <code>Settlement</code> value.
     *
     * @return a <code>Settlement</code> value
     */
    public final Settlement getSettlement() {
        return settlement;
    }

    /**
     * Set the <code>Settlement</code> value.
     *
     * @param newSettlement The new Settlement value.
     */
    public final void setSettlement(final Settlement newSettlement) {
        this.settlement = newSettlement;
    }

    /**
     * Returns whether this TradeItem is valid.
     *
     * @return a <code>boolean</code> value
     */
    public boolean isValid() {
        if (!(goods.getLocation() instanceof Unit)) {
            return false;
        }
        Unit unit = (Unit) goods.getLocation();
        if (unit.getOwner() != getSource()) {
            return false;
        }
        if (settlement != null && settlement.getOwner() == getDestination()) {
            return true;
        } else {
            return false;
        }

    }
    
    /**
     * Returns whether this TradeItem must be unique. This is true for
     * the StanceTradeItem and the GoldTradeItem, and false for all
     * others.
     *
     * @return a <code>boolean</code> value
     */
    public boolean isUnique() {
        return false;
    }

    /**
     * Concludes the trade.
     *
     */
    public void makeTrade() {
        goods.getLocation().remove(goods);
        settlement.add(goods);
    }

    /**
     * Initialize this object from an XML-representation of this object.
     * @param in The input stream with the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     */
    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        super.readFromXMLImpl(in);
        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            if (in.getLocalName().equals(Goods.getXMLElementTagName())) {
                this.goods = new Goods(getGame(), in);
            }
        }
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
        this.goods.toXML(out, player);
        out.writeEndElement();
    }
    
    /**
     * Gets the tag name of the root element representing this object.
     * @return "goods".
     */
    public static String getXMLElementTagName() {
        return "goodsTradeItem";
    }

}


