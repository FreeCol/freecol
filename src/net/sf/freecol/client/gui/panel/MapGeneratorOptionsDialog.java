package net.sf.freecol.client.gui.panel;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.client.gui.option.OptionMapUI;
import net.sf.freecol.server.generator.MapGeneratorOptions;

/**
 * Dialog for changing the
 * {@link net.sf.freecol.server.generator.MapGeneratorOptions}.
 */
public final class MapGeneratorOptionsDialog extends FreeColDialog implements ActionListener {
    private static final Logger logger = Logger.getLogger(MapGeneratorOptionsDialog.class.getName());

    public static final String COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";

    public static final String LICENSE = "http://www.gnu.org/licenses/gpl.html";

    public static final String REVISION = "$Revision$";

    private static final int OK = 0, CANCEL = 1;

    private final Canvas parent;

    private final FreeColClient freeColClient;

    private JButton ok, cancel;

    private JPanel buttons = new JPanel(new FlowLayout());

    private JLabel header;

    private OptionMapUI ui;


    /**
     * The constructor that will add the items to this panel.
     * 
     * @param parent The parent of this panel.
     * @param freeColClient The main controller object for the client.
     */
    public MapGeneratorOptionsDialog(Canvas parent, FreeColClient freeColClient) {
        setLayout(new BorderLayout());

        this.parent = parent;
        this.freeColClient = freeColClient;

        ok = new JButton(Messages.message("ok"));
        ok.setActionCommand(String.valueOf(OK));
        ok.addActionListener(this);
        ok.setMnemonic('O');
        buttons.add(ok);

        cancel = new JButton(Messages.message("cancel"));
        cancel.setActionCommand(String.valueOf(CANCEL));
        cancel.addActionListener(this);
        cancel.setMnemonic('C');
        buttons.add(cancel);

        FreeColPanel.enterPressesWhenFocused(ok);
        setCancelComponent(cancel);

        setSize(750, 500);
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(750, 500);
    }
    
    @Override
    public Dimension getPreferredSize() {
        return getMinimumSize();
    }
    
    public void initialize(boolean editable) {
        removeAll();

        final MapGeneratorOptions mgo = freeColClient.getPreGameController().getMapGeneratorOptions();

        // Header:
        header = getDefaultHeader(mgo.getName());
        add(header, BorderLayout.NORTH);

        // Options:
        JPanel uiPanel = new JPanel(new BorderLayout());
        uiPanel.setOpaque(false);
        ui = new OptionMapUI(mgo, editable);
        uiPanel.add(ui, BorderLayout.CENTER);
        uiPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(uiPanel, BorderLayout.CENTER);

        // Buttons:
        add(buttons, BorderLayout.SOUTH);

        ok.setEnabled(editable);
    }

    public void requestFocus() {
        if (ok.isEnabled()) {
            ok.requestFocus();
        } else {
            cancel.requestFocus();
        }
    }

    /**
     * This function analyses an event and calls the right methods to take care
     * of the user's requests.
     * 
     * @param event The incoming ActionEvent.
     */
    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();
        try {
            switch (Integer.valueOf(command).intValue()) {
            case OK:
                ui.unregister();
                ui.updateOption();
                parent.remove(this);
                freeColClient.getPreGameController().sendMapGeneratorOptions();
                freeColClient.getCanvas().getStartGamePanel().updateMapGeneratorOptions();
                setResponse(new Boolean(true));
                break;
            case CANCEL:
                ui.unregister();
                parent.remove(this);
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
