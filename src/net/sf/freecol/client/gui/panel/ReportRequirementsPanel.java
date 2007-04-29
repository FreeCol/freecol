package net.sf.freecol.client.gui.panel;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;

import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.ColonyTile;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.WorkLocation;
import cz.autel.dmi.HIGLayout;

/**
 * This panel displays the Advanced Colony Report.
 */
public final class ReportRequirementsPanel extends ReportPanel implements ActionListener {
    public static final String COPYRIGHT = "Copyright (C) 2003-2007 The FreeCol Team";

    public static final String LICENSE = "http://www.gnu.org/licenses/gpl.html";

    public static final String REVISION = "$Revision$";

    private List<Colony> colonies;

    private final int ROWS_PER_COLONY = 4;
    private final int SEPARATOR = 24;

    private int[][] unitCount;
    private boolean[][] canTrain; 


    /**
     * The constructor that will add the items to this panel.
     * 
     * @param parent The parent of this panel.
     */
    public ReportRequirementsPanel(Canvas parent) {
        super(parent, Messages.message("menuBar.report.requirements"));
    }

    /**
     * Prepares this panel to be displayed.
     */
    @Override
    public void initialize() {

        Player player = getCanvas().getClient().getMyPlayer();
        colonies = player.getColonies();
        Collections.sort(colonies, getCanvas().getClient().getClientOptions().getColonyComparator());

        // Display Panel
        //reportPanel.removeAll();
        reportPanel.setLayout(new HIGLayout(new int[] {780}, new int[] {0}));

        //Create a text pane.
        JTextPane textPane = new JTextPane();
        textPane.setOpaque(false);
	textPane.setEditable(false);

        StyledDocument doc = textPane.getStyledDocument();
        defineStyles(doc);

        unitCount = new int[colonies.size()][Unit.UNIT_COUNT];
        canTrain = new boolean[colonies.size()][Unit.UNIT_COUNT];

        // check which colonies can train which units
        for (int index = 0; index < colonies.size(); index++) {
            Colony colony = colonies.get(index);
            for (Unit unit : colony.getUnitList()) {
                unitCount[index][unit.getType()]++;
                if (colony.canTrain(unit)) {
                    canTrain[index][unit.getType()] = true;
                }
            }
        }

        for (int index = 0; index < colonies.size(); index++) {

            Colony colony = colonies.get(index);

            // colonyLabel
            try {
                StyleConstants.setComponent(doc.getStyle("button"), createColonyButton(index, true));
                doc.insertString(doc.getLength(), " ", doc.getStyle("button"));
                doc.insertString(doc.getLength(), "\n\n", doc.getStyle("regular"));
            } catch(Exception e) {
                logger.warning(e.toString());
            }

            boolean[] expertWarning = new boolean[Unit.UNIT_COUNT];

            Iterator<WorkLocation> workLocationIterator = colony.getWorkLocationIterator();
            while (workLocationIterator.hasNext()) {
                WorkLocation workLocation = workLocationIterator.next();
                if (workLocation instanceof ColonyTile) {
                    Unit unit = ((ColonyTile) workLocation).getUnit();
                    if (unit != null) {
                        int workType = unit.getWorkType();
                        int expert = ((ColonyTile) workLocation).getExpertForProducing(workType);
                        if (unitCount[index][expert] == 0 && !expertWarning[expert]) {
                            addMessage(doc, index, Goods.getName(workType), expert);
                            expertWarning[expert] = true;
                        }
                    }
                } else {
                    int workType = ((Building) workLocation).getGoodsOutputType();
                    int expert = ((Building) workLocation).getExpertUnitType();
                    if (workType != -1 &&
                        ((Building) workLocation).getFirstUnit() != null && !expertWarning[expert]) {
                        if (unitCount[index][expert] == 0) {
                            addMessage(doc, index, Goods.getName(workType), expert);
                            expertWarning[expert] = true;
                        }
                    }
                }
            }

            // text area
            reportPanel.add(textPane, higConst.rc(1, 1));

        }
    }

    private void addMessage(StyledDocument doc, int colonyIndex, String goods, int workType) {
        String expertName = Unit.getName(workType);
        String colonyName = colonies.get(colonyIndex).getName();
        String newMessage = Messages.message("report.requirements.noExpert",
                                             new String[][] {
                                                 {"%colony%", colonyName},
                                                 {"%goods%", goods},
                                                 {"%unit%", expertName}});

        try {
            doc.insertString(doc.getLength(), newMessage + "\n\n", doc.getStyle("regular"));

            ArrayList<Colony> severalExperts = new ArrayList<Colony>();
            ArrayList<Colony> canTrainExperts = new ArrayList<Colony>();
            for (int index = 0; index < colonies.size(); index++) {
                if (unitCount[index][workType] > 1) {
                    severalExperts.add(colonies.get(index));
                }
                if (canTrain[index][workType]) {
                    canTrainExperts.add(colonies.get(index));
                }
            }

            if (!severalExperts.isEmpty()) {
                doc.insertString(doc.getLength(),
                                 (Messages.message("report.requirements.severalExperts",
                                                   new String[][] {
                                                       {"%unit%", expertName}}) + " "),
                                 doc.getStyle("regular"));
                for (int index = 0; index < severalExperts.size() - 1; index++) {
                    StyleConstants.setComponent(doc.getStyle("button"), createColonyButton(index, false));
                    doc.insertString(doc.getLength(), " ", doc.getStyle("button"));
                    doc.insertString(doc.getLength(), ", ", doc.getStyle("regular"));
                }
                StyleConstants.setComponent(doc.getStyle("button"), 
                                            createColonyButton(severalExperts.size() - 1, false));
                doc.insertString(doc.getLength(), " ", doc.getStyle("button"));
                doc.insertString(doc.getLength(), "\n\n", doc.getStyle("regular"));
            }

            if (!canTrainExperts.isEmpty()) {
                doc.insertString(doc.getLength(),
                                 (Messages.message("report.requirements.canTrainExperts",
                                                   new String[][] {
                                                       {"%unit%", expertName}}) + " "),
                                 doc.getStyle("regular"));
                for (int index = 0; index < canTrainExperts.size() - 1; index++) {
                    StyleConstants.setComponent(doc.getStyle("button"), createColonyButton(index, false));
                    doc.insertString(doc.getLength(), " ", doc.getStyle("button"));
                    doc.insertString(doc.getLength(), ", ", doc.getStyle("regular"));
                }
                StyleConstants.setComponent(doc.getStyle("button"), 
                                            createColonyButton(canTrainExperts.size() - 1, false));
                doc.insertString(doc.getLength(), " ", doc.getStyle("button"));
                doc.insertString(doc.getLength(), "\n\n", doc.getStyle("regular"));
            }
  
        } catch(Exception e) {
            logger.warning(e.toString());
        }
        
    }


    private JButton createColonyButton(int index, boolean headline) {

        JButton button = new JButton(colonies.get(index).getName());
        if (headline) {
            button.setFont(smallHeaderFont);
        }
        button.setCursor(Cursor.getDefaultCursor());
        button.setMargin(new Insets(0,0,0,0));
        button.setOpaque(false);
        button.setForeground(Color.BLUE);
        //button.setBackground(Color.WHITE);
        button.setAlignmentY(0.8f);
        button.setBorder(BorderFactory.createEmptyBorder());
        button.setActionCommand(String.valueOf(index));
        button.addActionListener(this);
        return button;
    }

    private void defineStyles(StyledDocument doc) {

        //Initialize some styles.
        Style def = StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE);
	
        Style regular = doc.addStyle("regular", def);
        StyleConstants.setFontFamily(def, "Dialog");
	StyleConstants.setFontSize(def, 12);

        Style buttonStyle = doc.addStyle("button", regular);
        StyleConstants.setForeground(buttonStyle, Color.BLUE);
    }


    /**
     * This function analyses an event and calls the right methods to take care
     * of the user's requests.
     * 
     * @param event The incoming ActionEvent.
     */
    @Override
    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();
        int action = Integer.valueOf(command).intValue();
        if (action == OK) {
            super.actionPerformed(event);
        } else {
            getCanvas().showColonyPanel(colonies.get(action));
        }
    }

}
