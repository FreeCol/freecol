
package net.sf.freecol.client.gui.panel;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

import java.awt.*;
import javax.swing.border.*;
import javax.swing.*;


/**
* Dialog for changing the {@link GameOption}s.
*/
public final class GameOptionsDialog extends FreeColDialog implements ActionListener {
    private static final Logger logger = Logger.getLogger(GameOptionsDialog.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2004 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    private static final int    OK = 0;

    private final Canvas    parent;
    private final FreeColClient freeColClient;

    private JButton         ok = new JButton(Messages.message("ok"));
    private JLabel header;


    /**
    * The constructor that will add the items to this panel.
    * @param parent The parent of this panel.
    */
    public GameOptionsDialog(Canvas parent, FreeColClient freeColClient) {
        this.parent = parent;
        this.freeColClient = freeColClient;

        ok.setActionCommand(String.valueOf(OK));
        ok.addActionListener(this);
        ok.setMnemonic('O');

        FreeColPanel.enterPressesWhenFocused(ok);
        setCancelComponent(ok);

        add(ok, BorderLayout.SOUTH);

        setSize(640, 480);
    }


    public void initialize() {
        header = new JLabel(Messages.message(freeColClient.getGame().getGameOptions().getName()), JLabel.CENTER);
        header.setFont(((Font) UIManager.get("HeaderFont")).deriveFont(0, 48));
        header.setBorder(new EmptyBorder(20, 0, 0, 0));
        add(header, BorderLayout.NORTH);
    }


    public void requestFocus() {
        ok.requestFocus();
    }


    /**
    * This function analyses an event and calls the right methods to take
    * care of the user's requests.
    * @param event The incoming ActionEvent.
    */
    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();
        try {
            switch (Integer.valueOf(command).intValue()) {
                case OK:
                    parent.remove(this);
                    setResponse(new Boolean(true));
                    break;
                default:
                    logger.warning("Invalid ActionCommand: invalid number.");
            }
        }
        catch (NumberFormatException e) {
            logger.warning("Invalid Actioncommand: not a number.");
        }
    }
}
