package net.sf.freecol.client.gui.panel;

import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

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
        reportPanel.removeAll();

        int widths[] = new int[] { 0, 12, 0 };
        int heights[] = new int[colonies.size() * 2];
        for (int index = 1; index < heights.length; index += 2) {
            heights[index] = SEPARATOR;
        }

        reportPanel.setLayout(new HIGLayout(widths, heights));

        int row = 1;
        int colonyColumn = 1;
        int panelColumn = 3;

        unitCount = new int[colonies.size()][Unit.UNIT_COUNT];
        canTrain = new boolean[colonies.size()][Unit.UNIT_COUNT];

        // check which colonies can train which units
        for (int index = 0; index < colonies.size(); index++) {
            Colony colony = colonies.get(index);
            if (colony.getBuilding(Building.SCHOOLHOUSE).getLevel() != Building.NOT_BUILT) {
                for (Unit unit : colony.getUnitList()) {
                    unitCount[index][unit.getType()]++;
                    if (colony.canTrain(unit)) {
                        canTrain[index][unit.getType()] = true;
                    }
                }
            }
        }

        for (int index = 0; index < colonies.size(); index++) {

            Colony colony = colonies.get(index);

            // colonyLabel
            JButton colonyButton = new JButton(colony.getName());
            colonyButton.setActionCommand(String.valueOf(index));
            colonyButton.addActionListener(this);
            reportPanel.add(colonyButton, higConst.rc(row, colonyColumn, "lrt"));

            JTextArea textArea = new JTextArea();
            textArea.setColumns(45);
            textArea.setOpaque(false);
            textArea.setLineWrap(true);
            textArea.setWrapStyleWord(true);
            textArea.setFocusable(false);
            textArea.setFont(defaultFont);

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
                            addMessage(textArea, colony.getName(), Goods.getName(workType), expert);
                            expertWarning[expert] = true;
                        }
                    }
                } else {
                    int workType = ((Building) workLocation).getGoodsOutputType();
                    int expert = ((Building) workLocation).getExpertUnitType();
                    if (workType != -1 &&
                        ((Building) workLocation).getFirstUnit() != null && !expertWarning[expert]) {
                        if (unitCount[index][expert] == 0) {
                            addMessage(textArea, colony.getName(), Goods.getName(workType), expert);
                            expertWarning[expert] = true;
                        }
                    }
                }
            }

            // text area
            reportPanel.add(textArea, higConst.rc(row, panelColumn));
            row += 2;

        }
    }

    public void addMessage(JTextArea textArea, String colonyName, String goods, int workType) {
        String expertName = Unit.getName(workType);
        textArea.append(Messages.message("report.requirements.noExpert",
                                         new String[][] {
                                             {"%colony%", colonyName},
                                             {"%goods%", goods},
                                             {"%unit%", expertName}}));
        textArea.append("\n\n");

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
            textArea.append(Messages.message("report.requirements.severalExperts",
                                             new String[][] {
                                                 {"%unit%", expertName}}) + " ");
            for (int index = 0; index < severalExperts.size() - 1; index++) {
                textArea.append(severalExperts.get(index).getName() + ", ");
            }
            textArea.append(severalExperts.get(severalExperts.size() - 1).getName());
            textArea.append("\n\n");
        }



        if (!canTrainExperts.isEmpty()) {
            textArea.append(Messages.message("report.requirements.canTrainExperts",
                                             new String[][] {
                                                 {"%unit%", expertName}}) + " ");
            for (int index = 0; index < canTrainExperts.size() - 1; index++) {
                textArea.append(canTrainExperts.get(index).getName() + ", ");
            }
            textArea.append(canTrainExperts.get(canTrainExperts.size() - 1).getName());
            textArea.append("\n\n");
        }

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
