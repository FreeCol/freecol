package net.sf.freecol.client.gui.panel;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;

/**
 * This panel displays a report.
 */
public class ReportPanel extends FreeColPanel implements ActionListener {
    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";
    
    protected static final Logger logger = Logger.getLogger(ReportPanel.class.getName());
    private static final int    OK = -1;

    protected final Canvas parent;
    protected JPanel reportPanel;
    private JLabel header;
    private JButton ok;
    
    /**
     * The constructor that will add the items to this panel.
     * @param parent The parent of this panel.
     * @param title The title to display on the panel.
     */
    public ReportPanel(Canvas parent, String title) {
        super(new FlowLayout(FlowLayout.CENTER, 1000, 10));
        this.parent = parent;
        
        setLayout(new BorderLayout());
        
        header = new JLabel(title, JLabel.CENTER);
        header.setFont(((Font) UIManager.get("HeaderFont")).deriveFont(0, 48));
        header.setBorder(new EmptyBorder(20, 0, 0, 0));
        add(header, BorderLayout.NORTH);

        reportPanel = new JPanel();
        reportPanel.setOpaque(false);
        reportPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        add(reportPanel, BorderLayout.CENTER);

        ok = new JButton(Messages.message("ok"));
        ok.setActionCommand(String.valueOf(OK));
        ok.addActionListener(this);
        enterPressesWhenFocused(ok);
        setCancelComponent(ok);
        add(ok, BorderLayout.SOUTH);

        setSize(850, 600);
    }

    
    /**
     * Prepares this panel to be displayed.
     */
    public void initialize()
    {
      reportPanel.removeAll();
      reportPanel.doLayout();
    }

    /**
     * 
     */
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
        int action = Integer.valueOf(command).intValue();
        if (action == OK) {
            parent.remove(this);
        } else {
            logger.warning("Invalid ActionCommand: " + action);
        }
    }
}
