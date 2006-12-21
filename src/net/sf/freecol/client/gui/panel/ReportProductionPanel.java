package net.sf.freecol.client.gui.panel;

import java.awt.Color;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.MatteBorder;

import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Unit;
import cz.autel.dmi.*;

/**
 * This panel displays the Production Report.
 */
public final class ReportProductionPanel extends JPanel {
    public static final String  COPYRIGHT = "Copyright (C) 2003-2006 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    /** How many additional rows are defined. */
    private final int extraRows = 4; 
    /** How much space to leave between labels. */
    private final int columnSeparatorWidth = 12;
    /** How wide the margins should be. */
    private final int marginWidth = 12;
    /** The widths of the columns. */
    private final int[] widths = {0, columnSeparatorWidth, 0, columnSeparatorWidth, 0};

    /** The heights of the rows. */
    private final int[] heights;

    private final JScrollPane scrollPane;
    private Canvas parent;
    private final int goodsType;

    /**
     * The constructor that will add the items to this panel.
     * @param parent The parent of this panel.
     */
    public ReportProductionPanel(int goodsType, Canvas parent) {
        this.goodsType = goodsType;
        this.parent = parent;

        heights = null;

        scrollPane = new JScrollPane(this,
                                     JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                     JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    }

    /**
     * Prepares this panel to be displayed.
     */
    public void initialize() {
        Player player = parent.getClient().getMyPlayer();

        int colonyColumn = 1;
        int productionColumn = 3;
        int unitColumn = 5;

        // Display Panel
        removeAll();

        Iterator colonyIterator = player.getColonyIterator();
        ArrayList colonies = new ArrayList();
        while (colonyIterator.hasNext()) {
            colonies.add(colonyIterator.next());
        }
        int[] heights = new int[colonies.size() + extraRows];
        
        // labels
        heights[0] = 0;
        heights[1] = marginWidth;
        // summary
        heights[2] = 0;
        heights[3] = marginWidth;

        for (int h = 4; h < heights.length; h++) {
            heights[h] = 0;
        }

        this.setLayout(new HIGLayout(widths, heights));
        HIGConstraints higConst = new HIGConstraints();

        // labels
        add(new JLabel(Messages.message("Colony")),
            higConst.rc(1, colonyColumn));

        add(new JLabel(Messages.message("report.production")),
            higConst.rc(1, productionColumn));

        add(new JLabel(Messages.message("report.units")),
            higConst.rc(1, unitColumn));


        int row = 5;
        int totalProduction = 0;
        int totalUnits = 0;

        colonyIterator = colonies.iterator();
        while (colonyIterator.hasNext()) {
            Colony colony = (Colony) colonyIterator.next();

            // colonyLabel
            JLabel colonyLabel = new JLabel(colony.getName());
            add(colonyLabel, higConst.rc(row, colonyColumn));

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
                Iterator unitIterator = building.getUnitIterator();
                while (unitIterator.hasNext()) {
                    Unit unit = (Unit) unitIterator.next();
                    UnitLabel label = new UnitLabel(unit, parent, true);
                    unitPanel.add(label);
                    totalUnits++;
                }
                add(unitPanel, higConst.rc(row, unitColumn));
            }

            row++;
        }

        // summary
        add(new JLabel(Messages.message("report.allColonies")),
            higConst.rc(3, colonyColumn));

        Goods allGoods = new Goods(goodsType);
        allGoods.setAmount(totalProduction);
        GoodsLabel allGoodsLabel = new GoodsLabel(allGoods, parent);
        allGoodsLabel.setHorizontalAlignment(JLabel.LEADING);
        add(allGoodsLabel, higConst.rc(3, productionColumn));

        add(new JLabel(String.valueOf(totalUnits)),
            higConst.rc(3, unitColumn));

    }
}

