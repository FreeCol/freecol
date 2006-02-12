
package net.sf.freecol.common.model;


import java.util.Arrays;


/**
* Contains a message about a change in the model.
*/
public class ModelMessage {

    public static final String  COPYRIGHT = "Copyright (C) 2003-2006 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    /** Constants describing the type of message. */
    public static final int DEFAULT = 0;
    public static final int SONS_OF_LIBERTY = 1;
    public static final int GOVERNMENT_EFFICIENCY = 2;
    public static final int WAREHOUSE_CAPACITY = 3;
    public static final int UNIT_IMPROVEMENT = 4;
    public static final int UNIT_PROMOTION = 5;
    public static final int UNIT_DEMOTION = 6;
    public static final int BUILDING_COMPLETION = 7;
    public static final int NEW_COLONIST = 8;
    public static final int FOREIGN_DIPLOMACY = 9;
    public static final int MARKET_PRICES = 10;

    private final FreeColGameObject source;
    private final int type;
    private final String messageID;
    private final String[][] data;
    private boolean beenDisplayed = false;


    /**
    * Creates a new <code>ModelMessage</code>.
    *
    * @param source The source of the message. This is what the message should be
    *               associated with. In addition, the owner of the source is the
    *               player getting the message.
    * @param messageID The ID of the message to display.
    * @param data Contains data to be displayed in the message or <i>null</i>.
    * @param type The type of this model message.
    * @see Game#addModelMessage
    * @see FreeColGameObject#addModelMessage
    */
    public ModelMessage(FreeColGameObject source, String messageID, String[][] data, int type) {
        this.source = source;
        this.messageID = messageID;
        this.data = data;
        this.type = type;
    }

    /**
    * Creates a new <code>ModelMessage</code>.
    *
    * @param source The source of the message. This is what the message should be
    *               associated with. In addition, the owner of the source is the
    *               player getting the message.
    * @param messageID The ID of the message to display.
    * @param data Contains data to be displayed in the message or <i>null</i>.
    * @see Game#addModelMessage
    * @see FreeColGameObject#addModelMessage
    */
    public ModelMessage(FreeColGameObject source, String messageID, String[][] data) {
        this.source = source;
        this.messageID = messageID;
        this.data = data;
        this.type = DEFAULT;
    }

    /**
    * Checks if this <code>ModelMessage</code> has been displayed.
    * @return <i>true</i> if this <code>ModelMessage</code> has been displayed.
    * @see #setBeenDisplayed
    */
    public boolean hasBeenDisplayed() {
        return beenDisplayed;
    }


    /**
    * Sets the <code>beenDisplayed</code> value of this <code>ModelMessage</code>.
    * This is used to avoid showing the same message twice.
    * 
    * @param beenDisplayed Should be set to <code>true</code> after the
    *       message has been displayed.
    */
    public void setBeenDisplayed(boolean beenDisplayed) {
        this.beenDisplayed = beenDisplayed;
    }


    /**
    * Gets the source of the message. This is what the message
    * should be associated with. In addition, the owner of the source is the
    * player getting the message.
    *
    * @return The source of the message.
    * @see #getOwner
    */
    public FreeColGameObject getSource() {
        return source;
    }
    
    
    /**
    * Gets the ID of the message to display.   
    * @return The ID. 
    */
    public String getMessageID() {
        return messageID;
    }
    
    
    /**
    * Returns the data to be displayed in the message.
    * @return The data as a <code>String[][]</code> or <i>null</i>
    *         if no data applies.
    */
    public String[][] getData() {
        return data;
    }

    /**
     * Gets the type of the message to display.   
     * @return The type. 
     */
    public int getType() {
        return type;
    }
    

    /**
    * Returns the owner of this message. The owner of this method
    * is the owner of the {@link #getSource source}.
    * 
    * @return The owner of the message. This is the <code>Player</code>
    *       who should receive the message.
    */
    public Player getOwner() {
        if (source instanceof Unit) {
            return ((Unit) source).getOwner();
        } else if (source instanceof Settlement) {
            return ((Settlement) source).getOwner();
        } else if (source instanceof Europe) {
            return ((Europe) source).getOwner();
        } else if (source instanceof Player) {
            return (Player) source;
        } else if (source instanceof Ownable) {
            return ((Ownable) source).getOwner();
        }

        return null;
    }


    /**
    * Checks if this <code>ModelMessage</code> is equal to another <code>ModelMessage</code>.
    * @return <i>true</i> if the sources, message IDs and data are equal.
    */
    public boolean equals(Object o) {

        if ( ! (o instanceof ModelMessage) ) { return false; }

        ModelMessage m = (ModelMessage) o;
        return ( getSource() == m.getSource()
                 && getMessageID().equals(m.getMessageID())
                 && Arrays.equals(getData(), m.getData())
                 && getType() == m.getType() );
    }
}
