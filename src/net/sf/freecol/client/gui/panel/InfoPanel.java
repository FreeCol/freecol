
package net.sf.freecol.client.gui.panel;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Iterator;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.Unit;

import cz.autel.dmi.HIGLayout;

/**
 * This is the panel that shows more details about the currently selected unit
 * and the tile it stands on. It also shows the amount of gold the player has left
 * and stuff like that.
 */
public final class InfoPanel extends FreeColPanel {
    public static final String  COPYRIGHT = "Copyright (C) 2003-2007 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    private static final Logger logger = Logger.getLogger(InfoPanel.class.getName());

    private static final int PANEL_WIDTH = 256;
    private static final int PANEL_HEIGHT = 128;

    private final FreeColClient freeColClient;
    private final Game          game;
    private final ImageLibrary  library;
    private final EndTurnPanel  endTurnPanel = new EndTurnPanel();
    private final UnitInfoPanel unitInfoPanel;
    

    /**
    * The constructor that will add the items to this panel.
    * 
    * @param freeColClient The main controller object for the client.
    * @param game The Game object that has all kinds of useful information
    *       that we want to display here.
    * @param imageProvider The ImageProvider that can provide us with images
    *       to display on this panel.
    */
    public InfoPanel(FreeColClient freeColClient, Game game, ImageProvider imageProvider) {
        this.freeColClient = freeColClient;
        this.game = game;
        this.library = (ImageLibrary) imageProvider;

        unitInfoPanel = new UnitInfoPanel();
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
                                unitToolsLabel,
                                goldLabel;
        private final JPanel unitCargoPanel,
                             labelPanel;
        private Unit unit;

        private final int[] widths = {0, 0, 0, 0};
        private final int[] heights = {0, 0};

        public UnitInfoPanel() {
            super(null);

            Image tools = library.getGoodsImageIcon(Goods.TOOLS).getImage();
            ImageIcon toolsIcon = new ImageIcon(tools.getScaledInstance(tools.getWidth(null) * 2/3,
                                                                        tools.getHeight(null) * 2/3,
                                                                        Image.SCALE_SMOOTH));
            unitToolsLabel = new JLabel(toolsIcon);            

            unitCargoPanel = new JPanel(new HIGLayout(widths, heights));
            unitCargoPanel.setOpaque(false);

            JPanel picturePanel = new JPanel(new BorderLayout());
            picturePanel.setOpaque(false);
            picturePanel.setSize(110, 100);
            picturePanel.setLocation(0, 0);
            unitLabel = new JLabel();
            unitLabel.setHorizontalAlignment(JLabel.CENTER);
            unitLabel.setVerticalAlignment(JLabel.CENTER);
            picturePanel.add(unitLabel, BorderLayout.CENTER);
            add(picturePanel);

            int[] widths = {0};
            int[] heights = {0, 0, 0};
            labelPanel = new JPanel(new HIGLayout(widths, heights));
            labelPanel.setOpaque(false);

            unitNameLabel = new JLabel();
            unitMovesLabel = new JLabel();
            goldLabel = new JLabel();

            labelPanel.add(unitNameLabel, higConst.rc(1, 1));
            labelPanel.add(unitMovesLabel, higConst.rc(2, 1));
            labelPanel.add(unitCargoPanel, higConst.rc(3, 1));

            unitLabel.setFocusable(false);
            unitNameLabel.setFocusable(false);
            unitMovesLabel.setFocusable(false);
            unitToolsLabel.setFocusable(false);
            labelPanel.setSize(130, 100);
            labelPanel.setLocation(100, 10);//(100-labelPanel.getHeight())/2);
            add(labelPanel);

            setSize(226, 100);
            setOpaque(false);
        }


        /**
        * Paints this component.
        * @param graphics The Graphics context in which to draw this component.
        */
        public void paintComponent(Graphics graphics) {
            unitCargoPanel.removeAll();
            if (unit != null) {
                unitNameLabel.setText(unit.getName());
                unitLabel.setIcon(library.getUnitImageIcon(library.getUnitGraphicsType(unit)));
                unitMovesLabel.setText(Messages.message("moves") + " " + unit.getMovesAsString());
                if (unit.isPioneer()) {
                    unitToolsLabel.setText(String.valueOf(unit.getNumberOfTools()));
                    unitCargoPanel.add(unitToolsLabel, higConst.rc(1, 1));
                } else if (unit.getType() == Unit.TREASURE_TRAIN) {
                    goldLabel.setText(unit.getTreasureAmount() + " " +
                                      Messages.message("gold"));
                    unitCargoPanel.add(goldLabel, higConst.rc(1, 1));
                } else if (unit.isCarrier()) {
                    int counter = 1;
                    int row = 1;
                    Iterator goodsIterator = unit.getGoodsIterator();
                    while (goodsIterator.hasNext()) {
                        Goods goods = (Goods) goodsIterator.next();
                        JLabel goodsLabel = new JLabel(library.getScaledGoodsImageIcon(goods.getType(), 0.66f));
                        unitCargoPanel.add(goodsLabel, higConst.rc(row, counter));
                        if (counter == 4) {
                            row++;
                            counter = 1;
                        } else {
                            counter++;
                        }
                    }
                    Iterator unitIterator = unit.getUnitIterator();
                    while (unitIterator.hasNext()) {
                        Unit unit = (Unit) unitIterator.next();
                        int graphicsType = library.getUnitGraphicsType(unit);
                        JLabel unitLabel = new JLabel(library.getScaledUnitImageIcon(graphicsType, 0.5f));
                        unitCargoPanel.add(unitLabel, higConst.rc(row, counter));
                        if (counter == 4) {
                            row++;
                            counter = 1;
                        } else {
                            counter++;
                        }
                    }                    
                    unitCargoPanel.validate();
                }
            } else {
                unitLabel.setIcon(null);
            }
            labelPanel.validate();
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
