package net.sf.freecol.client.gui.panel;

import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
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

    /**
     * A list of all the player's colonies.
     */
    private List<Colony> colonies;

    /**
     * Records the number of units indexed by colony and unit type.
     */
    private int[][] unitCount;

    /**
     * Records whether a colony can train a type of unit.
     */
    private boolean[][] canTrain; 

    /**
     * Records surplus production indexed by colony and goods type.
     */
    private int[][] surplus;


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
        reportPanel.setLayout(new HIGLayout(new int[] {780}, new int[] {0}));

        //Create a text pane.
        JTextPane textPane = new JTextPane();
        textPane.setOpaque(false);
	textPane.setEditable(false);

        StyledDocument doc = textPane.getStyledDocument();
        defineStyles(doc);

        unitCount = new int[colonies.size()][Unit.UNIT_COUNT];
        canTrain = new boolean[colonies.size()][Unit.UNIT_COUNT];
        surplus = new int[colonies.size()][Goods.NUMBER_OF_TYPES];

        // check which colonies can train which units
        for (int index = 0; index < colonies.size(); index++) {
            Colony colony = colonies.get(index);
            for (Unit unit : colony.getUnitList()) {
                unitCount[index][unit.getType()]++;
                canTrain[index][unit.getType()] = colony.canTrain(unit);
            }
            for (int goodsType = 0; goodsType < Goods.NUMBER_OF_TYPES; goodsType++) {
                surplus[index][goodsType] = colony.getProductionNetOf(goodsType);
            }
        }

        for (int index = 0; index < colonies.size(); index++) {

            Colony colony = colonies.get(index);

            // colonyLabel
            try {
                StyleConstants.setComponent(doc.getStyle("button"), createColonyButton(colony, true));
                doc.insertString(doc.getLength(), " ", doc.getStyle("button"));
                doc.insertString(doc.getLength(), "\n\n", doc.getStyle("regular"));
            } catch(Exception e) {
                logger.warning(e.toString());
            }

            boolean[] expertWarning = new boolean[Unit.UNIT_COUNT];
            boolean[] productionWarning = new boolean[Goods.NUMBER_OF_ALL_TYPES];
            boolean hasWarning = false;

            // check if all unit requirements are met
            Iterator<WorkLocation> workLocationIterator = colony.getWorkLocationIterator();
            while (workLocationIterator.hasNext()) {
                // check colony tiles
                WorkLocation workLocation = workLocationIterator.next();
                if (workLocation instanceof ColonyTile) {
                    Unit unit = ((ColonyTile) workLocation).getUnit();
                    if (unit != null) {
                        int workType = unit.getWorkType();
                        int expert = ((ColonyTile) workLocation).getExpertForProducing(workType);
                        if (unitCount[index][expert] == 0 && !expertWarning[expert]) {
                            addExpertWarning(doc, index, Goods.getName(workType), expert);
                            expertWarning[expert] = true;
                            hasWarning = true;
                        }
                    }
                } else {
                    // check buildings
                    Building building = (Building) workLocation;
                    int goodsType = building.getGoodsOutputType();
                    int expert = building.getExpertUnitType();
                    if (goodsType != -1) {
                        // no expert
                        if (building.getFirstUnit() != null &&
                            !expertWarning[expert] &&
                            unitCount[index][expert] == 0) {
                            addExpertWarning(doc, index, Goods.getName(goodsType), expert);
                            expertWarning[expert] = true;
                            hasWarning = true;
                        }
                        // not enough input
                        if (building.getProductionNextTurn() < building.getMaximumProduction() &&
                            !productionWarning[goodsType]) {
                            addProductionWarning(doc, index, goodsType, building.getGoodsInputType());
                            productionWarning[goodsType] = true;
                            hasWarning = true;
                        }
                    }
                }
            }

            if (!hasWarning) {
                try {
                    doc.insertString(doc.getLength(), Messages.message("report.requirements.met") + "\n\n",
                                     doc.getStyle("regular"));
                } catch(Exception e) {
                    logger.warning(e.toString());
                }
            }

            // text area
            reportPanel.add(textPane, higConst.rc(1, 1));

        }
    }

    private void addExpertWarning(StyledDocument doc, int colonyIndex, String goods, int workType) {
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
                    Colony colony = severalExperts.get(index);
                    StyleConstants.setComponent(doc.getStyle("button"), createColonyButton(colony, false));
                    doc.insertString(doc.getLength(), " ", doc.getStyle("button"));
                    doc.insertString(doc.getLength(), ", ", doc.getStyle("regular"));
                }
                Colony colony = severalExperts.get(severalExperts.size() - 1);
                StyleConstants.setComponent(doc.getStyle("button"), createColonyButton(colony, false));
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
                    Colony colony = canTrainExperts.get(index);
                    StyleConstants.setComponent(doc.getStyle("button"), createColonyButton(colony, false));
                    doc.insertString(doc.getLength(), " ", doc.getStyle("button"));
                    doc.insertString(doc.getLength(), ", ", doc.getStyle("regular"));
                }
                Colony colony = canTrainExperts.get(canTrainExperts.size() - 1);
                StyleConstants.setComponent(doc.getStyle("button"), createColonyButton(colony, false));
                doc.insertString(doc.getLength(), " ", doc.getStyle("button"));
                doc.insertString(doc.getLength(), "\n\n", doc.getStyle("regular"));
            }
  
        } catch(Exception e) {
            logger.warning(e.toString());
        }
        
    }

    private void addProductionWarning(StyledDocument doc, int colonyIndex, int output, int input) {
        String colonyName = colonies.get(colonyIndex).getName();
        String newMessage = Messages.message("report.requirements.missingGoods",
                                             new String[][] {
                                                 {"%colony%", colonyName},
                                                 {"%goods%", Goods.getName(output)},
                                                 {"%input%", Goods.getName(input)}});

        try {
            doc.insertString(doc.getLength(), newMessage + "\n\n", doc.getStyle("regular"));

            ArrayList<Colony> withSurplus = new ArrayList<Colony>();
            ArrayList<Integer> theSurplus = new ArrayList<Integer>();
            for (int index = 0; index < colonies.size(); index++) {
                if (surplus[index][input] > 0) {
                    withSurplus.add(colonies.get(index));
                    theSurplus.add(new Integer(surplus[index][input]));
                }
            }

            if (!withSurplus.isEmpty()) {
                doc.insertString(doc.getLength(),
                                 (Messages.message("report.requirements.surplus",
                                                   new String[][] {
                                                       {"%goods%", Goods.getName(input)}}) + " "),
                                 doc.getStyle("regular"));
                for (int index = 0; index < withSurplus.size() - 1; index++) {
                    Colony colony = withSurplus.get(index);
                    String amount = " (" + theSurplus.get(index) + ")";
                    StyleConstants.setComponent(doc.getStyle("button"), createColonyButton(colony, amount, false));
                    doc.insertString(doc.getLength(), " ", doc.getStyle("button"));
                    doc.insertString(doc.getLength(), ", ", doc.getStyle("regular"));
                }
                Colony colony = withSurplus.get(withSurplus.size() - 1);
                String amount = " (" + theSurplus.get(theSurplus.size() - 1) + ")";
                StyleConstants.setComponent(doc.getStyle("button"), createColonyButton(colony, amount, false));
                doc.insertString(doc.getLength(), " ", doc.getStyle("button"));
                doc.insertString(doc.getLength(), "\n\n", doc.getStyle("regular"));
            }


        } catch(Exception e) {
            logger.warning(e.toString());
        }
        
    }

    private JButton createColonyButton(Colony colony, boolean headline) {
        return createColonyButton(colony, "", headline);
    }

    private JButton createColonyButton(Colony colony, String info, boolean headline) {
        JButton button = new JButton(colony.getName() + info);
        if (headline) {
            button.setFont(smallHeaderFont);
        }
        button.setMargin(new Insets(0,0,0,0));
        button.setOpaque(false);
        button.setForeground(LINK_COLOR);
        button.setAlignmentY(0.8f);
        button.setBorder(BorderFactory.createEmptyBorder());
        button.setActionCommand(colony.getID());
        button.addActionListener(this);
        return button;
    }

    private void defineStyles(StyledDocument doc) {

        //Initialize some styles.
        Style def = StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE);
	
        Style regular = doc.addStyle("regular", def);
        StyleConstants.setFontFamily(def, "Dialog");
        StyleConstants.setBold(def, true);
	StyleConstants.setFontSize(def, 12);

        Style buttonStyle = doc.addStyle("button", regular);
        StyleConstants.setForeground(buttonStyle, LINK_COLOR);
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
        if (command.equals(String.valueOf(OK))) {
            super.actionPerformed(event);
        } else {
            Colony colony = (Colony) getCanvas().getClient().getGame().getFreeColGameObject(command);
            if (colony instanceof Colony) {
                getCanvas().showColonyPanel((Colony) colony);
            }
        }
    }

}
