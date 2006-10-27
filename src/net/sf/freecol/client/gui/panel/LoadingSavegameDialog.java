
package net.sf.freecol.client.gui.panel;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;


/**
* Dialog for setting some options when loading a game.
*/
public final class LoadingSavegameDialog extends FreeColDialog implements ActionListener {
    private static final Logger logger = Logger.getLogger(LoadingSavegameDialog.class.getName());

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
    
    private JRadioButton singleplayer;
    private JRadioButton privateMultiplayer;
    private JRadioButton publicMultiplayer;
    private JTextField serverNameField;
    private JSpinner portField;


    /**
    * The constructor that will add the items to this panel.
    * @param parent The parent of this panel.
    * @param freeColClient The main controller object for the client.
    */
    public LoadingSavegameDialog(Canvas parent, FreeColClient freeColClient) {
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

        setSize(320, 275);
        
        // Header:
        header = new JLabel(Messages.message("LoadingSavegame.title"), JLabel.CENTER);
        header.setFont(((Font) UIManager.get("HeaderFont")).deriveFont(0, 32));
        header.setBorder(new EmptyBorder(20, 0, 0, 0));
        add(header, BorderLayout.NORTH);

        // Panel:
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        
        JPanel p1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        p1.add(new JLabel(Messages.message("LoadingSavegame.serverName"), JLabel.LEFT));
        panel.add(p1);
        serverNameField = new JTextField();
        panel.add(serverNameField);
        
        JPanel p2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        p2.add(new JLabel(Messages.message("LoadingSavegame.port"), JLabel.LEFT));
        panel.add(p2);
        portField = new JSpinner(new SpinnerNumberModel(3541, 1, 65536, 1));
        panel.add(portField);
        
        ButtonGroup bg = new ButtonGroup();
        singleplayer = new JRadioButton(Messages.message("LoadingSavegame.singleplayer"));
        bg.add(singleplayer);
        panel.add(singleplayer);
        privateMultiplayer = new JRadioButton(Messages.message("LoadingSavegame.privateMultiplayer"));
        bg.add(privateMultiplayer);
        panel.add(privateMultiplayer);
        publicMultiplayer = new JRadioButton(Messages.message("LoadingSavegame.publicMultiplayer"));
        bg.add(publicMultiplayer);
        panel.add(publicMultiplayer);
        
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(panel, BorderLayout.CENTER);

        // Buttons:
        add(buttons, BorderLayout.SOUTH);
    }

    public boolean isSingleplayer() {
        return singleplayer.isSelected();
    }
    
    public boolean isPublic() {
        return publicMultiplayer.isSelected();
    }
    
    public int getPort() {
        return ((Integer) portField.getValue()).intValue();
    }
    
    public String getName() {
        return serverNameField.getName();
    }

    public void initialize(boolean publicServer, boolean singleplayer) {
        
        this.singleplayer.setSelected(false);
        this.privateMultiplayer.setSelected(false);
        this.publicMultiplayer.setSelected(false);
        
        if (singleplayer) {
            this.singleplayer.setSelected(true);    
        } else if (publicServer) {
            this.publicMultiplayer.setSelected(true);
        } else {
            this.privateMultiplayer.setSelected(true);
        }
        
        this.serverNameField.setText("");
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
                case CANCEL:
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
