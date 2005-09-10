package net.sf.freecol.common.model;

import java.util.Arrays;

/**
* Contains a message about a change in the model.
*/
public class ModelMessage {
    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";
    
    private final FreeColGameObject source;
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
    * @see Game#addModelMessage
    * @see FreeColGameObject#addModelMessage
    */
    public ModelMessage(FreeColGameObject source, String messageID, String[][] data) {
        this.source = source;
        this.messageID = messageID;
        this.data = data;
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
    */
    public void setBeenDisplayed(boolean beenDisplayed) {
        this.beenDisplayed = beenDisplayed;
    }


    /**
    * Gets the source of the message. This is what the message
    * should be associated with. In addition, the owner of the source is the
    * player getting the message.
    *
    * @see #getOwner
    */
    public FreeColGameObject getSource() {
        return source;
    }
    
    
    /**
    * Gets the ID of the message to display.    
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
    * Returns the owner of this message. The owner of this method
    * is the owner of the {@link #getSource source}.
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
        if (o instanceof ModelMessage) {
            ModelMessage m = (ModelMessage) o;
            if (getSource() == m.getSource() && getMessageID().equals(m.getMessageID()) && Arrays.equals(getData(), m.getData())) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }
}
