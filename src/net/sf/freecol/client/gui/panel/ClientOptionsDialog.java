
package net.sf.freecol.client.gui.panel;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.client.gui.option.OptionMapUI;


/**
* Dialog for changing the {@link net.sf.freecol.client.ClientOptions}.
*/
public final class ClientOptionsDialog extends FreeColDialog implements ActionListener {
    private static final Logger logger = Logger.getLogger(ClientOptionsDialog.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    private static final int    OK = 0,
                                CANCEL = 1;

    private final Canvas        parent;
    private final FreeColClient freeColClient;

    private JButton ok;
    private JPanel buttons = new JPanel(new FlowLayout());
    private JLabel header;
    private OptionMapUI ui;


    /**
    * The constructor that will add the items to this panel.
    * @param parent The parent of this panel.
    * @param freeColClient The main controller object for the
    *       client.
    */
    public ClientOptionsDialog(Canvas parent, FreeColClient freeColClient) {
        setLayout(new BorderLayout());

        this.parent = parent;
        this.freeColClient = freeColClient;

        ok = new JButton(Messages.message("ok"));
        ok.setActionCommand(String.valueOf(OK));
        ok.addActionListener(this);
        ok.setMnemonic('O');
        buttons.add(ok);

        JButton cancel = new JButton(Messages.message("cancel"));
        cancel.setActionCommand(String.valueOf(CANCEL));
        cancel.addActionListener(this);
        cancel.setMnemonic('C');
        buttons.add(cancel);


        FreeColPanel.enterPressesWhenFocused(ok);
        setCancelComponent(cancel);

        setSize(640, 480);
    }


    public void initialize() {
        removeAll();

        // Header:
        header = new JLabel(freeColClient.getClientOptions().getName(), JLabel.CENTER);
        header.setFont(((Font) UIManager.get("HeaderFont")).deriveFont(0, 48));
        header.setBorder(new EmptyBorder(20, 0, 0, 0));
        add(header, BorderLayout.NORTH);

        // Options:
        JPanel uiPanel = new JPanel(new BorderLayout());
        uiPanel.setOpaque(false);
        ui = new OptionMapUI(freeColClient.getClientOptions());
        uiPanel.add(ui, BorderLayout.CENTER);
        uiPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(uiPanel, BorderLayout.CENTER);

        // Buttons:
        add(buttons, BorderLayout.SOUTH);
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
                    ui.updateOption();
                    freeColClient.getCanvas().resetFreeColMenuBar();    // TODO: Find a better method to reset set accelerators.
                    parent.remove(this);
                    freeColClient.getActionManager().update();
                    freeColClient.saveClientOptions();
                    setResponse(new Boolean(true));
                    break;
                case CANCEL:
                    parent.remove(this);
                    freeColClient.getActionManager().update();
                    setResponse(new Boolean(false));
                    break;
                default:
                    logger.warning("Invalid ActionCommand: invalid number.");
            }
        } catch (NumberFormatException e) {
            logger.warning("Invalid Actioncommand: not a number.");
        }
    }
}
