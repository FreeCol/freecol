
package net.sf.freecol.client.gui.panel;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Unit;


/**
 * This is the panel that shows more details about the currently selected unit
 * and the tile it stands on. It also shows the amount of gold the player has left
 * and stuff like that.
 */
public final class InfoPanel extends FreeColPanel {
    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    private static final Logger logger = Logger.getLogger(InfoPanel.class.getName());

    private static final int PANEL_WIDTH = 256;
    private static final int PANEL_HEIGHT = 128;

    private final FreeColClient freeColClient;
    private final Game          game;
    private final ImageProvider imageProvider;
    private final EndTurnPanel  endTurnPanel = new EndTurnPanel();
    private final UnitInfoPanel unitInfoPanel = new UnitInfoPanel();


    

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

        setLayout(null);

        int internalPanelTop = 0;
        int internalPanelHeight = 128;
        Image skin = (Image) UIManager.get("InfoPanel.skin");
        if (skin == null) {
            setSize(PANEL_WIDTH, PANEL_HEIGHT);
        } else {
            setBorder(null);
            setSize(skin.getWidth(null), skin.getHeight(null));
            setOpaque(false);
            internalPanelTop = 75;
            internalPanelHeight = 100;
        }

        unitInfoPanel.setVisible(false);
        endTurnPanel.setVisible(false);

        unitInfoPanel.setLocation((getWidth()-unitInfoPanel.getWidth())/2,
                                  internalPanelTop + (internalPanelHeight-unitInfoPanel.getHeight())/2);
        endTurnPanel.setLocation((getWidth()-endTurnPanel.getWidth())/2,
                                  internalPanelTop + (internalPanelHeight-endTurnPanel.getHeight())/2);

        add(unitInfoPanel);
        add(endTurnPanel);
    }



    /**
    * Updates this <code>InfoPanel</code>.
    * @param unit The displayed unit (or null if none)
    */
    public void update(Unit unit) {
        unitInfoPanel.update(unit);
    }


    /**
    * Gets the <code>Unit</code> in which this <code>InfoPanel</code> is
    * displaying information about.
    *
    * @return The <code>Unit</code> or <i>null</i> if no <code>Unit</code> applies.
    */
    public Unit getUnit() {
        return unitInfoPanel.getUnit();
    }


    /**
    * Paints this component.
    * @param graphics The Graphics context in which to draw this component.
    */
    public void paintComponent(Graphics graphics) {
        if (unitInfoPanel.getUnit() != null) {
            if (!unitInfoPanel.isVisible()) {
                unitInfoPanel.setVisible(true);
                endTurnPanel.setVisible(false);
            }
        } else if (!freeColClient.getMyPlayer().hasNextActiveUnit()) {
            if (!endTurnPanel.isVisible()) {
                endTurnPanel.setVisible(true);
                unitInfoPanel.setVisible(false);
            }
        }

        Image skin = (Image) UIManager.get("InfoPanel.skin");
        if (skin != null) {
            graphics.drawImage(skin, 0, 0, null);
        }
        
        super.paintComponent(graphics);
    }


    /**
    * Panel for displaying <code>Unit</code>-information.
    */
    public class UnitInfoPanel extends JPanel {

        private final JLabel    unitLabel,
                                unitNameLabel,
                                unitMovesLabel,
                                unitToolsLabel;
        private Unit unit;

        public UnitInfoPanel() {
            super(null);
/*
            unitLabel = new JLabel();
            unitNameLabel = new JLabel();
            unitMovesLabel = new JLabel();
            unitToolsLabel = new JLabel();

            unitLabel.setSize(118, 96);
            unitNameLabel.setSize(116, 20);
            unitMovesLabel.setSize(116, 20);
            unitToolsLabel.setSize(116, 20);

            unitLabel.setLocation(10, 4);
            unitNameLabel.setLocation(130, 15);
            unitMovesLabel.setLocation(130, 40);
            unitToolsLabel.setLocation(130, 65);

            setLayout(null);

            add(unitLabel);
            add(unitNameLabel);
            add(unitMovesLabel);
            add(unitToolsLabel);

            unitLabel.setFocusable(false);
            unitNameLabel.setFocusable(false);
            unitMovesLabel.setFocusable(false);
            unitToolsLabel.setFocusable(false);

            setSize(226, 100);
            setOpaque(false);
            */

            JPanel picturePanel = new JPanel(new BorderLayout());
            picturePanel.setOpaque(false);
            picturePanel.setSize(110, 100);
            picturePanel.setLocation(0, 0);
            unitLabel = new JLabel();
            unitLabel.setHorizontalAlignment(JLabel.CENTER);
            unitLabel.setVerticalAlignment(JLabel.CENTER);
            picturePanel.add(unitLabel, BorderLayout.CENTER);
            add(picturePanel);

            JPanel labelPanel = new JPanel(new GridLayout(3, 1, 10, 10));
            labelPanel.setOpaque(false);
            unitNameLabel = new JLabel();
            unitMovesLabel = new JLabel();
            unitToolsLabel = new JLabel();
            labelPanel.add(unitNameLabel);
            labelPanel.add(unitMovesLabel);
            labelPanel.add(unitToolsLabel);
            unitLabel.setFocusable(false);
            unitNameLabel.setFocusable(false);
            unitMovesLabel.setFocusable(false);
            unitToolsLabel.setFocusable(false);
            labelPanel.setSize(130, 60);
            labelPanel.setLocation(100, (100-labelPanel.getHeight())/2);
            add(labelPanel);

            setSize(226, 100);
            setOpaque(false);
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
                unitMovesLabel.setText("Moves: " + unit.getMovesAsString());
                if (unit.isPioneer()) {
                    unitToolsLabel.setText("Tools: " + unit.getNumberOfTools());
                } else if (unit.getType() == Unit.TREASURE_TRAIN) {
                    unitToolsLabel.setText("Gold: " + unit.getTreasureAmount());
                } else {
                    unitToolsLabel.setText("");
                }
            } else {
                unitLabel.setIcon(null);
                unitNameLabel.setText("");
                unitMovesLabel.setText("");
                unitToolsLabel.setText("");
            }

            super.paintComponent(graphics);
        }


        /**
        * Updates this <code>InfoPanel</code>.
        * @param unit The displayed unit (or null if none)
        */
        public void update(Unit unit) {
            this.unit = unit;
        }


        /**
        * Gets the <code>Unit</code> in which this <code>InfoPanel</code> is
        * displaying information about.
        *
        * @return The <code>Unit</code> or <i>null</i> if no <code>Unit</code> applies.
        */
        public Unit getUnit() {
            return unit;
        }
    }


    /**
    * Panel for ending the turn.
    */
    public class EndTurnPanel extends JPanel {

        private JLabel endTurnLabel = new JLabel(Messages.message("infoPanel.endTurnPanel.text"), JLabel.CENTER);
        private JButton endTurnButton = new JButton(Messages.message("infoPanel.endTurnPanel.endTurnButton"));

        public EndTurnPanel() {
            super(new FlowLayout(FlowLayout.CENTER, 10, 10));

            add(endTurnLabel);
            add(endTurnButton);
            setOpaque(false);
            setSize(230, endTurnLabel.getPreferredSize().height + endTurnButton.getPreferredSize().height + 30);

            /* TODO:
              The action listener does not work, because this button looses it's focus.
              The reason why the focus gets lost should be found, in order to use the actionlistener.
            */
            /*endTurnButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    freeColClient.getInGameController().endTurn();
                }
            });*/

            endTurnButton.addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    freeColClient.getInGameController().endTurn();
                }
            });
        }
    }
}
