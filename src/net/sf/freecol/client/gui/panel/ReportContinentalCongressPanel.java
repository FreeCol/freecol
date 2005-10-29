package net.sf.freecol.client.gui.panel;

import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;
import javax.swing.JLabel;

import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.FoundingFather;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.Player;

/**
 * This panel displays the Founding Fathers Report.
 */
public final class ReportContinentalCongressPanel extends ReportPanel implements ActionListener {
    public static final String COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String REVISION = "$Revision$";

    private int bellsNextTurn = 0;
    private int playerLibertyBells = 0;
    private int bellsNeededTotal = 0;
    private String currentFFather = "";

    static final String title = Messages.message("report.continentalCongress.title");

    /**
     * The constructor that will add the items to this panel.
     * 
     * @param parent The parent of this panel.
     */
    public ReportContinentalCongressPanel(Canvas parent) {
        super(parent, title);
    }

    /**
     * Prepares this panel to be displayed.
     * 
     * NOTE: This is called every time the panel is shown, however it is always
     * the same panel.
     */
    public void initialize() {
        final String none = Messages.message("report.continentalCongress.none");

        // Display Panel
        reportPanel.removeAll();
        reportPanel.setLayout(new GridLayout(0, 1));

        Player player = parent.getClient().getMyPlayer();
        final int fatherCount = player.getFatherCount();
        final int currFatherId = player.getCurrentFather();
        playerLibertyBells = player.getBells();

        int displayed = 0;

        StringBuffer buffer = new StringBuffer("<html><p align=center><b>").append(
                player.getNationAsString()).append(" ").append(title).append("</b>");
        if (fatherCount < 1) {
            buffer.append("<p>").append(none);
        } else {
            for (int fatherId = 0; fatherId < FoundingFather.FATHER_COUNT
                    && displayed < fatherCount; fatherId++) {
                if (player.hasFather(fatherId)) {
                    final String name = Messages.message(FoundingFather.getName(fatherId));
                    buffer.append("<p>").append(name);
                    displayed++;
                }
            }
        }
        
        if (currFatherId == FoundingFather.NONE) {
            currentFFather = none;
        } else {
            final String nameKey = FoundingFather.getName(currFatherId);
            currentFFather = Messages.message(nameKey);
        }
        buffer.append("</html>");
        final String report = buffer.toString();
        JLabel label;
        label = new JLabel(report);
        label.setVerticalAlignment(JLabel.TOP);
        label.setVerticalTextPosition(JLabel.TOP);
        reportPanel.add(label);

        bellsNextTurn = player.getBellsProductionNextTurn();
        bellsNeededTotal = player.getTotalFoundingFatherCost();

        reportPanel.doLayout();
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        final int insetHorizontal = 5;
        final int insetVertical = 50;
        final int drawWidth = reportPanel.getWidth() - insetHorizontal * 2;
        final int halfWidth = drawWidth / 2;
        ImageIcon goodsIcon = parent.getImageProvider().getGoodsImageIcon(Goods.BELLS);
        final int iconHeight = goodsIcon.getIconHeight();
        final int iconWidth = goodsIcon.getIconWidth();
        final int y2 = this.getHeight() - iconHeight - insetVertical;
        final int y1 = y2 - iconHeight / 4;
        final int y0 = y1 - iconHeight;
        BufferedImage productionImage;

        final String recruiting = Messages.message("report.continentalCongress.recruiting");
        final String bellCurrent = Messages.message("report.continentalCongress.bellsCurrent");
        final String bellChange = Messages.message("report.continentalCongress.bellsIncrease");
        final String bellReq = Messages.message("report.continentalCongress.bellsRequired");

        final String displayRecruit = recruiting + ": " + currentFFather;

        int x = insetHorizontal;
        Font origFont = g.getFont();
        g.setFont(new Font(origFont.getName(), Font.BOLD, origFont.getSize()));
        g.drawString(displayRecruit, x, y0);

        // The right-half of the row is total production
        productionImage = parent.getGUI().createProductionImage(goodsIcon, playerLibertyBells,
                halfWidth, iconHeight, 100, 100, -1, false, false, halfWidth);
        g.drawString(bellCurrent, x, y1);
        g.drawImage(productionImage, x, y2, null);

        // The 3rd quarter is the number of bells for next turn
        // TODO NOT actually shown in Colonization
        final int eighth = halfWidth / 4;
        productionImage = parent.getGUI().createProductionImage(goodsIcon, bellsNextTurn, eighth,
                iconHeight, 50, 50, -1, true, false, eighth);
        x += halfWidth + eighth;
        g.drawString(bellChange, x, y1);
        g.drawImage(productionImage, x, y2, null);

        // The last image (far right) shows how many more are required.
        productionImage = parent.getGUI().createProductionImage(goodsIcon, this.bellsNeededTotal,
                iconWidth * 3, iconHeight, 5, 5, -1, false, false, iconWidth * 3);
        x = insetHorizontal + drawWidth - productionImage.getWidth();
        g.drawString(bellReq, x, y1);
        g.drawImage(productionImage, x, y2, null);

        g.setFont(origFont);
    }
}
