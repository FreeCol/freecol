
package net.sf.freecol.client.gui;


import javax.swing.JLabel;

import net.sf.freecol.client.gui.i18n.Messages;


public final class MessageLabel extends JLabel {

    public static final  String  COPYRIGHT = "Copyright (C) 2003-2006 The FreeCol Team";
    public static final  String  LICENSE   = "http://www.gnu.org/licenses/gpl.html";
    public static final  String  REVISION  = "$Revision$";


    public MessageLabel( String messageId ) {

        super( Messages.message(messageId) );
    }

}
