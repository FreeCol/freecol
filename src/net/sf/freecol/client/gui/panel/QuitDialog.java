
package net.sf.freecol.client.gui.panel;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JLabel;

import net.sf.freecol.client.gui.Canvas;
import cz.autel.dmi.HIGLayout;

/**
* Asks the user if he's sure he wants to quit.
*/
public final class QuitDialog extends FreeColDialog implements ActionListener {
    private static final Logger logger = Logger.getLogger(QuitDialog.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";
    
    private static final int    OK = 0,
                                CANCEL = 1;

    private final Canvas    parent;
    private JButton         ok = new JButton("Yes");
    private JButton         cancel;
    
    
    
    /**
    * The constructor that will add the items to this panel.
    * @param parent The parent of this panel.
    */
    public QuitDialog(Canvas parent) {
        this.parent = parent;

        int[] w = {0, 0, 0};
        int[] h = {0, 10, 0};
        HIGLayout l = new HIGLayout(w,h);
        l.setColumnWeight(2, 1);
        setLayout(l);

        cancel = new JButton("No");
        JLabel qLabel = new JLabel("Are you sure you want to Quit?");
        qLabel.setHorizontalAlignment(JLabel.CENTER);

//        qLabel.setSize(200, 20);
//        ok.setSize(60, 20);
//        cancel.setSize(60, 20);

//        qLabel.setLocation(10, 10);
//        ok.setLocation(30, 40);
//        cancel.setLocation(130, 40);

//        setLayout(null);
        
        ok.setActionCommand(String.valueOf(OK));
        cancel.setActionCommand(String.valueOf(CANCEL));
        
        ok.addActionListener(this);
        cancel.addActionListener(this);

        ok.setMnemonic('y');
        cancel.setMnemonic('n');

        FreeColPanel.enterPressesWhenFocused(cancel);
        FreeColPanel.enterPressesWhenFocused(ok);

        setCancelComponent(cancel);

        add(qLabel, higConst.rcwh(1,1, 3,1));
        higConst.setVCorrection(0,-4);
        add(ok, higConst.rc(3,1));
        add(cancel, higConst.rc(3,3));
        higConst.clearCorrection();

//        try {
//            BevelBorder border = new BevelBorder(BevelBorder.RAISED);
//            setBorder(border);
//        }
//        catch(Exception e) {
//        }

        setSize(220, 70);
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
                    //parent.quit();
                    break;
                case CANCEL:
                    parent.remove(this);
                    setResponse(new Boolean(false));
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
