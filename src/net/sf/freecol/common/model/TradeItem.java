
package net.sf.freecol.common.model;



import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;


/**
 * One of the items a DiplomaticTrade consists of.
 *
 */
public abstract class TradeItem extends PersistentObject {

    /**
     * Describe game here.
     */
    private Game game;
    
    // the ID, used to get a name, etc.
    protected String ID;
    // the player offering something
    protected Player source;
    // the player who is to receive something
    protected Player destination;
        
    /**
     * Creates a new <code>TradeItem</code> instance.
     *
     * @param game a <code>Game</code> value
     * @param id a <code>String</code> value
     * @param source a <code>Player</code> value
     * @param destination a <code>Player</code> value
     */
    public TradeItem(Game game, String id, Player source, Player destination) {
        this.game = game;
        this.ID = id;
        this.source = source;
        this.destination = destination;
    }

    /**
     * Get the <code>Game</code> value.
     *
     * @return a <code>Game</code> value
     */
    public final Game getGame() {
        return game;
    }

    /**
     * Set the <code>Game</code> value.
     *
     * @param newGame The new Game value.
     */
    public final void setGame(final Game newGame) {
        this.game = newGame;
    }

    /**
     * Returns whether this TradeItem is valid.
     *
     * @return a <code>boolean</code> value
     */
    public abstract boolean isValid();

    /**
     * Concludes the trade.
     *
     */
    public abstract void makeTrade();

    /**
     * Initialize this object from an XML-representation of this object.
     * @param in The input stream with the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     */
    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        this.ID = in.getAttributeValue(null, "ID");
        String sourceID = in.getAttributeValue(null, "source");
        this.source = (Player) game.getFreeColGameObject(sourceID);
        String destinationID = in.getAttributeValue(null, "destination");
        this.destination = (Player) game.getFreeColGameObject(destinationID);
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
        out.writeAttribute("ID", this.ID);
        out.writeAttribute("source", this.source.getID());
        out.writeAttribute("destination", this.destination.getID());
    }
    

}

