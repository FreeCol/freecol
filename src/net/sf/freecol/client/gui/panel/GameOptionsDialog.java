
package net.sf.freecol.client.gui.panel;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileFilter;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.client.gui.option.OptionMapUI;


/**
* Dialog for changing the {@link net.sf.freecol.common.model.GameOptions}.
*/
public final class GameOptionsDialog extends FreeColDialog implements ActionListener {
    private static final Logger logger = Logger.getLogger(GameOptionsDialog.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    private static final int    OK = 0,
                                CANCEL = 1,
                                SAVE = 2,
                                LOAD = 3;

    private final Canvas        parent;
    private final FreeColClient freeColClient;

    private JButton ok, load, save, cancel;
    private JPanel buttons = new JPanel(new FlowLayout());
    private JLabel header;
    private OptionMapUI ui;


    /**
    * The constructor that will add the items to this panel.
    * @param parent The parent of this panel.
    * @param freeColClient The main controller object for the client.
    */
    public GameOptionsDialog(Canvas parent, FreeColClient freeColClient) {
        setLayout(new BorderLayout());

        this.parent = parent;
        this.freeColClient = freeColClient;

        ok = new JButton(Messages.message("ok"));
        ok.setActionCommand(String.valueOf(OK));
        ok.addActionListener(this);
        ok.setMnemonic('O');
        buttons.add(ok);
        
        load = new JButton(Messages.message("load"));
        load.setActionCommand(String.valueOf(LOAD));
        load.addActionListener(this);
        load.setMnemonic('L');
        buttons.add(load);

        save = new JButton(Messages.message("save"));
        save.setActionCommand(String.valueOf(SAVE));
        save.addActionListener(this);
        save.setMnemonic('S');
        buttons.add(save);

        cancel = new JButton(Messages.message("cancel"));
        cancel.setActionCommand(String.valueOf(CANCEL));
        cancel.addActionListener(this);
        cancel.setMnemonic('C');
        buttons.add(cancel);


        FreeColPanel.enterPressesWhenFocused(ok);
        setCancelComponent(cancel);

        setSize(640, 480);
    }


    public void initialize(boolean editable) {
        removeAll();

        // Header:
        header = new JLabel(freeColClient.getGame().getGameOptions().getName(), JLabel.CENTER);
        header.setFont(((Font) UIManager.get("HeaderFont")).deriveFont(0, 48));
        header.setBorder(new EmptyBorder(20, 0, 0, 0));
        add(header, BorderLayout.NORTH);

        // Options:
        JPanel uiPanel = new JPanel(new BorderLayout());
        uiPanel.setOpaque(false);
        ui = new OptionMapUI(freeColClient.getGame().getGameOptions(), editable);
        uiPanel.add(ui, BorderLayout.CENTER);
        uiPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(uiPanel, BorderLayout.CENTER);

        // Buttons:
        add(buttons, BorderLayout.SOUTH);
        
        ok.setEnabled(editable);
        save.setEnabled(editable);
        load.setEnabled(editable);
    }


    public void requestFocus() {
        if (ok.isEnabled()) {
            ok.requestFocus();
        } else {
            cancel.requestFocus();
        }
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
                    ui.unregister();
                    ui.updateOption();
                    freeColClient.getPreGameController().sendGameOptions();
                    parent.remove(this);
                    setResponse(new Boolean(true));
                    break;
                case CANCEL:
                    ui.unregister();
                    parent.remove(this);
                    setResponse(new Boolean(false));
                    break;
                case SAVE:
                    File saveFile = freeColClient.getCanvas().showSaveDialog(FreeCol.getSaveDirectory(), ".fgo", new FileFilter[] {FreeColDialog.getFGOFileFilter(), FreeColDialog.getFSGFileFilter(), FreeColDialog.getGameOptionsFileFilter()});
                    if (saveFile != null) {
                        try {
                            ui.updateOption();
                            freeColClient.getPreGameController().saveGameOptions(saveFile);
                        } catch (IOException e) {
                            freeColClient.getCanvas().errorMessage("unspecifiedIOException");
                        }
                    }
                    break;
                case LOAD:
                    File loadFile = freeColClient.getCanvas().showLoadDialog(FreeCol.getSaveDirectory(), new FileFilter[] {FreeColDialog.getFGOFileFilter(), FreeColDialog.getFSGFileFilter(), FreeColDialog.getGameOptionsFileFilter()});
                    if (loadFile != null) {
                        try {
                            freeColClient.getPreGameController().loadGameOptions(loadFile);
                        } catch (IOException e) {
                            freeColClient.getCanvas().errorMessage("unspecifiedIOException");
                        }
                    }
                    break;
                default:
                    logger.warning("Invalid ActionCommand: invalid number.");
            }
        } catch (NumberFormatException e) {
            logger.warning("Invalid Actioncommand: not a number.");
        }
    }
}
