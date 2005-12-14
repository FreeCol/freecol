
package net.sf.freecol.client.gui;


import javax.swing.JLabel;

import net.sf.freecol.client.gui.i18n.Messages;


public final class MessageLabel extends JLabel {

    public MessageLabel( String messageId ) {

        super( Messages.message(messageId) );
    }

}
