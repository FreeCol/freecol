package net.sf.freecol.client.gui.panel;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;
import java.util.Comparator;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Unit;

/**
 * This panel displays a report.
 */
public class ReportPanel extends FreeColPanel implements ActionListener {
    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";
    
    protected static final Logger logger = Logger.getLogger(ReportPanel.class.getName());
    protected static final int    OK = -1;

    protected final Canvas parent;
    protected JPanel reportPanel;
    private JLabel header;
    private JButton ok;
    private JScrollPane scrollPane;
    
    private static ImageLibrary library;

    private static final Comparator unitTypeComparator = new Comparator<Unit> () {
        public int compare(Unit unit1, Unit unit2) {
            return unit2.getType() - unit1.getType();
        }
    };

    /**
     * The constructor that will add the items to this panel.
     * @param parent The parent of this panel.
     * @param title The title to display on the panel.
     */
    public ReportPanel(Canvas parent, String title) {
        super(new FlowLayout(FlowLayout.CENTER, 1000, 10));
        this.parent = parent;
        this.library = (ImageLibrary) parent.getImageProvider();

        setLayout(new BorderLayout());
        
        header = getDefaultHeader(title);
        add(header, BorderLayout.NORTH);

        reportPanel = new JPanel();
        reportPanel.setOpaque(true);
        reportPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        scrollPane = new JScrollPane(reportPanel,
                                     JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                     JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        add(scrollPane, BorderLayout.CENTER);

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
     * Returns a unit type comparator.
     * @return A unit type comparator.
     */
    public Comparator getUnitTypeComparator() {
        return unitTypeComparator;
    }

    /**
     * Builds the button for the given unit.
     * @param unit
     * @param unitIcon
     * @param scale
     */
    public JLabel buildUnitLabel(int unitIcon, float scale) {
        return new JLabel(library.getScaledUnitImageIcon(unitIcon, scale));
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
