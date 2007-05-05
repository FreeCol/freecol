package net.sf.freecol.client.gui.panel;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.Insets;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Unit;
import cz.autel.dmi.HIGConstraints;
import cz.autel.dmi.HIGLayout;

/**
 * This panel displays the Production Report.
 */
public final class ReportProductionPanel extends JPanel implements ActionListener {
    public static final String COPYRIGHT = "Copyright (C) 2003-2006 The FreeCol Team";

    public static final String LICENSE = "http://www.gnu.org/licenses/gpl.html";

    public static final String REVISION = "$Revision$";

    /** How many additional rows are defined. */
    private final int extraRows = 4;

    /** How much space to leave between labels. */
    private final int columnSeparatorWidth = 12;

    /** How wide the margins should be. */
    private final int marginWidth = 12;

    /** The widths of the columns. */
    private final int[] widths;

    /** The heights of the rows. */
    // private final int[] heights;
    private static final HIGConstraints higConst = new HIGConstraints();

    private Canvas parent;

    private List<Colony> colonies;

    private final int goodsType;

    private final ReportPanel reportPanel;

    private int totalProduction;


    /**
     * The constructor that will add the items to this panel.
     * 
     * @param parent The parent of this panel.
     */
    public ReportProductionPanel(int goodsType, Canvas parent, ReportPanel reportPanel) {
        this.goodsType = goodsType;
        this.parent = parent;
        this.reportPanel = reportPanel;

        if (goodsType == Goods.BELLS) {
            widths = new int[] { 0, columnSeparatorWidth, 0, columnSeparatorWidth, 0, columnSeparatorWidth, 0,
                    columnSeparatorWidth, 0, columnSeparatorWidth, 0, columnSeparatorWidth, 0, columnSeparatorWidth, 0 };
        } else if (goodsType == Goods.CROSSES) {
            widths = new int[] { 0, columnSeparatorWidth, 0, columnSeparatorWidth, 0, columnSeparatorWidth, 0,
                    columnSeparatorWidth, 0 };
        } else {
            widths = new int[] { 0, columnSeparatorWidth, 0, columnSeparatorWidth, 0 };
        }
        // heights = null;
        totalProduction = 0;
        setOpaque(false);
    }

    /**
     * Prepares this panel to be displayed.
     */
    public void initialize() {
        Player player = parent.getClient().getMyPlayer();

        int colonyColumn = 1;
        int productionColumn = 3;
        int unitColumn = 5;
        int level1Column = 7;
        int level2Column = 9;
        int percentageColumn = 11;
        int solColumn = 13;
        int toryColumn = 15;

        // Display Panel
        removeAll();

        colonies = player.getColonies();
        int numberOfColonies = colonies.size();
        int[] heights = new int[numberOfColonies + extraRows];

        // labels
        heights[0] = 0;
        heights[1] = marginWidth;
        // summary
        heights[2] = 0;
        heights[3] = marginWidth;

        
        for (int index = 4; index < heights.length; index++) {
            /** TODO: replace this magic number by some value from the
             * ImageLibrary. At the moment this is difficult, as the
             * unit images in the library are not sorted according to
             * type. For this purpose, however, one would need to
             * consider only ships for the naval report and only units
             * able to defend colonies for the military report. The
             * value of 64 is large enough to accommodate the 2/3
             * scale version of all unit graphics.
             */
            heights[index] = 64;
        }
        

        setLayout(new HIGLayout(widths, heights));

        // labels
        add(new JLabel(Messages.message("Colony")), higConst.rc(1, colonyColumn));

        add(new JLabel(Messages.message("report.production")), higConst.rc(1, productionColumn));

        add(new JLabel(Messages.message("report.units")), higConst.rc(1, unitColumn));

        if (goodsType == Goods.BELLS) {

            add(new JLabel(Messages.message("colopedia.buildings.name.PrintingPress")), higConst.rc(1, level1Column));

            add(new JLabel(Messages.message("colopedia.buildings.name.Newspaper")), higConst.rc(1, level2Column));

            add(new JLabel(Messages.message("sonsOfLiberty") + "%"), higConst.rc(1, percentageColumn));

            add(new JLabel(Messages.message("sonsOfLiberty")), higConst.rc(1, solColumn));

            add(new JLabel(Messages.message("tory")), higConst.rc(1, toryColumn));

        } else if (goodsType == Goods.CROSSES) {

            add(new JLabel(Messages.message("colopedia.buildings.name.Church")), higConst.rc(1, level1Column));

            add(new JLabel(Messages.message("colopedia.buildings.name.Cathedral")), higConst.rc(1, level2Column));

        }
            

        int row = 5;
        totalProduction = 0;
        int totalUnits = 0;

        int pressCount = 0;
        int paperCount = 0;
        int churchCount = 0;
        int cathedralCount = 0;
        int percentageCount = 0;
        int solCount = 0;
        int toryCount = 0;

        Collections.sort(colonies, parent.getClient().getClientOptions().getColonyComparator());

        for (int colonyIndex = 0; colonyIndex < colonies.size(); colonyIndex++) {

            Colony colony = colonies.get(colonyIndex);

            // colonyButton
            add(createColonyButton(colonyIndex), higConst.rc(row, colonyColumn, "l"));

            // production
            Goods goods = new Goods(goodsType);
            int newValue = colony.getProductionOf(goodsType);
            goods.setAmount(newValue);
            totalProduction += newValue;
            GoodsLabel goodsLabel = new GoodsLabel(goods, parent);
            goodsLabel.setHorizontalAlignment(JLabel.LEADING);
            add(goodsLabel, higConst.rc(row, productionColumn));

            // units
            Building building = colony.getBuildingForProducing(goodsType);
            if (building != null) {
                JPanel unitPanel = new JPanel();
                unitPanel.setOpaque(false);
                Iterator<Unit> unitIterator = building.getUnitIterator();
                while (unitIterator.hasNext()) {
                    Unit unit = unitIterator.next();
                    UnitLabel label = new UnitLabel(unit, parent, true);
                    unitPanel.add(label);
                    totalUnits++;
                }
                add(unitPanel, higConst.rc(row, unitColumn));
            }

            // special
            if (goodsType == Goods.BELLS) {

                int level = colony.getBuilding(Building.PRINTING_PRESS).getLevel();
                if (level == Building.HOUSE) {
                    add(new JLabel("X"), higConst.rc(row, level1Column));
                    pressCount++;
                } else if (level == Building.SHOP) {
                    add(new JLabel("X"), higConst.rc(row, level2Column));
                    paperCount++;
                }

                int percentage = colony.getSoL();
                percentageCount += percentage;
                int count = colony.getUnitCount();
                int sol = percentage * count / 100;
                solCount += sol;
                int tories = count - sol;
                toryCount += tories;

                add(new JLabel(String.valueOf(percentage), JLabel.TRAILING), higConst.rc(row, percentageColumn));
                add(new JLabel(String.valueOf(sol), JLabel.TRAILING), higConst.rc(row, solColumn));
                add(new JLabel(String.valueOf(tories), JLabel.TRAILING), higConst.rc(row, toryColumn));

            } else if (goodsType == Goods.CROSSES) {

                int level = colony.getBuilding(Building.CHURCH).getLevel();
                if (level == Building.HOUSE) {
                    add(new JLabel("X"), higConst.rc(row, level1Column));
                    churchCount++;
                } else if (level == Building.SHOP) {
                    add(new JLabel("X"), higConst.rc(row, level2Column));
                    cathedralCount++;
                }
            }


            row++;
        }

        row = 3;
        // summary
        JLabel allColonies = new JLabel(Messages.message("report.allColonies", new String[][] { { "%number%",
                String.valueOf(numberOfColonies) } }));
        allColonies.setForeground(Color.BLUE);
        add(allColonies, higConst.rc(row, colonyColumn));

        Goods allGoods = new Goods(goodsType);
        allGoods.setAmount(totalProduction);
        GoodsLabel allGoodsLabel = new GoodsLabel(allGoods, parent);
        allGoodsLabel.setHorizontalAlignment(JLabel.LEADING);
        add(allGoodsLabel, higConst.rc(row, productionColumn));

        add(new JLabel(String.valueOf(totalUnits)), higConst.rc(row, unitColumn));

        if (goodsType == Goods.BELLS) {
            add(new JLabel(String.valueOf(pressCount), JLabel.TRAILING), higConst.rc(row, level1Column));
            add(new JLabel(String.valueOf(paperCount), JLabel.TRAILING), higConst.rc(row, level2Column));
            int percentage = 0;
            if (numberOfColonies > 0) {
                percentage = percentageCount / numberOfColonies;
            }
            add(new JLabel(String.valueOf(percentage), JLabel.TRAILING), higConst.rc(row, percentageColumn));
            add(new JLabel(String.valueOf(solCount), JLabel.TRAILING), higConst.rc(row, solColumn));
            add(new JLabel(String.valueOf(toryCount), JLabel.TRAILING), higConst.rc(row, toryColumn));
        } else if (goodsType == Goods.CROSSES) {
            add(new JLabel(String.valueOf(churchCount), JLabel.TRAILING), higConst.rc(row, level1Column));
            add(new JLabel(String.valueOf(cathedralCount), JLabel.TRAILING), higConst.rc(row, level2Column));
        }
    }


    private JButton createColonyButton(int index) {

        JButton button = new JButton(colonies.get(index).getName());
        //button.setFont(FreeColPanel.smallHeaderFont);
        button.setMargin(new Insets(0,0,0,0));
        button.setOpaque(false);
        button.setForeground(FreeColPanel.LINK_COLOR);
        button.setAlignmentY(0.8f);
        button.setBorder(BorderFactory.createEmptyBorder());
        button.setActionCommand(String.valueOf(index));
        button.addActionListener(this);
        return button;
    }

    /**
     * This function analyses an event and calls the right methods to take care
     * of the user's requests.
     * 
     * @param event The incoming ActionEvent.
     */
    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();
        int action = Integer.valueOf(command).intValue();
        if (action == ReportPanel.OK) {
            reportPanel.actionPerformed(event);
        } else {
            parent.showColonyPanel(colonies.get(action));
        }
    }

    public int getTotalProduction() {
        return totalProduction;
    }

}
