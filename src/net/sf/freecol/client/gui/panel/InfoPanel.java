
package net.sf.freecol.client.gui.panel;

import java.awt.Graphics;
import java.util.logging.Logger;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.BevelBorder;
import javax.swing.SwingConstants;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.model.Unit;

/**
 * This is the panel that shows more details about the currently selected unit
 * and the tile it stands on. It also shows the amount of gold the player has left
 * and stuff like that.
 */
public final class InfoPanel extends JPanel {
    public static final String  COPYRIGHT = "Copyright (C) 2003 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";
    
    private static final Logger logger = Logger.getLogger(InfoPanel.class.getName());
    
    private final JLabel        unitLabel,
                                unitNameLabel,
                                unitMovesLabel,
                                unitToolsLabel,
                                goldLabel,
                                turnLabel;

    private FreeColClient freeColClient;
    private final Game          game;
    private final ImageProvider imageProvider;
    private Unit unit;

    /**
    * The constructor that will add the items to this panel.
    * @param game The Game object that has all kinds of useful information
    * that we want to display here.
    * @param imageProvider The ImageProvider that can provide us with images to
    * display on this panel.
    */
    public InfoPanel(FreeColClient freeColClient, Game game, ImageProvider imageProvider) {
        this.freeColClient = freeColClient;
        this.game = game;
        this.imageProvider = imageProvider;

        unitLabel = new JLabel();
        unitNameLabel = new JLabel();
        unitMovesLabel = new JLabel();
        unitToolsLabel = new JLabel();
        goldLabel = new JLabel();
        turnLabel = new JLabel();

        turnLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        unitLabel.setSize(118, 96);
        unitNameLabel.setSize(116, 20);
        unitMovesLabel.setSize(116, 20);
        unitToolsLabel.setSize(116, 20);
        goldLabel.setSize(100, 20);
        turnLabel.setSize(146, 20);

        unitLabel.setLocation(10, 4);
        unitNameLabel.setLocation(130, 15);
        unitMovesLabel.setLocation(130, 40);
        unitToolsLabel.setLocation(130, 65);
        goldLabel.setLocation(10, 102);
        turnLabel.setLocation(100, 102);

        setLayout(null);

        add(unitLabel);
        add(unitNameLabel);
        add(unitMovesLabel);
        add(unitToolsLabel);
        add(goldLabel);
        add(turnLabel);

        unitLabel.setFocusable(false);
        unitNameLabel.setFocusable(false);
        unitMovesLabel.setFocusable(false);
        unitToolsLabel.setFocusable(false);
        goldLabel.setFocusable(false);
        turnLabel.setFocusable(false);

        try {
            BevelBorder border = new BevelBorder(BevelBorder.RAISED);
            setBorder(border);
        } catch(Exception e) {}

        setSize(256, 128);
    }

    /**
    * Paints this component.
    * @param graphics The Graphics context in which to draw this component.
    */
    public void paintComponent(Graphics graphics) {
        //Unit unit = freeColClient.getGUI().getActiveUnit();
        if (unit != null) {
            unitLabel.setIcon(imageProvider.getUnitImageIcon(imageProvider.getUnitGraphicsType(unit)));
            unitNameLabel.setText(unit.getName());
            unitMovesLabel.setText("Moves: " + unit.getMovesLeft() + "/" + unit.getInitialMovesLeft());
            if (unit.isPioneer()) {
                unitToolsLabel.setText("Tools: " + unit.getNumberOfTools());
            } else {
                unitToolsLabel.setText("");
            }
        } else {
            unitLabel.setIcon(null);
            unitNameLabel.setText("");
            unitMovesLabel.setText("");
            unitToolsLabel.setText("");
        }

        goldLabel.setText("Gold: " + freeColClient.getMyPlayer().getGold());
        turnLabel.setText("Year: " + freeColClient.getGame().getTurn().toString());

        super.paintComponent(graphics);
    }

    /**
    * Updates the moves of the displayed unit (or erases it if null provided).
    * @param unit The displayed unit (or null if none)
    */
    public void updateMoves(Unit unit) {
        this.unit = unit;

        if (unit == null) {
            unitLabel.setIcon(null);
            unitNameLabel.setText("");
            unitMovesLabel.setText("");
        } else {
            unitMovesLabel.setText("Moves: " + unit.getMovesLeft() + "/" +
                                   unit.getInitialMovesLeft());
        }
    }
}
