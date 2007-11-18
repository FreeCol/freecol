/**
 *  Copyright (C) 2002-2007  The FreeCol Team
 *
 *  This file is part of FreeCol.
 *
 *  FreeCol is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  FreeCol is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FreeCol.  If not, see <http://www.gnu.org/licenses/>.
 */


package net.sf.freecol.client.gui.panel;

import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.BoxLayout;
import javax.swing.ComponentInputMap;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.i18n.Messages;

import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileType;

/**
 * This panel is used to show information about a tile.
 */
public final class TilePanel extends FreeColDialog implements ActionListener {

    private static final Logger logger = Logger.getLogger(TilePanel.class.getName());

    private static final int OK = 0;
    private static final int COLOPEDIA = 1;
    private final Canvas canvas;

    private final GoodsType[] goodsTypes;
    private final int number;
    
    private final JPanel goodsPanel;
    private final JLabel tileNameLabel;
    private final JLabel ownerLabel;
    private final JButton okButton;
    private final JButton colopediaButton;

    private TileType tileType;

    /**
     * The constructor that will add the items to this panel.
     * @param parent The parent panel.
     */
    public TilePanel(Canvas parent) {
        canvas = parent;

        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        
        tileNameLabel = new JLabel("", JLabel.CENTER);
        tileNameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(tileNameLabel);

        ownerLabel = new JLabel("", JLabel.CENTER);
        ownerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(ownerLabel);        

        List<GoodsType> farmedGoods = FreeCol.getSpecification().getFarmedGoodsTypeList();
        number = farmedGoods.size();

        goodsPanel = new JPanel();
        goodsPanel.setLayout(new FlowLayout());

        goodsTypes = new GoodsType[number];
        JLabel[] labels = new JLabel[number];
        for (int k = 0; k < number; k++) {
            goodsTypes[k] = farmedGoods.get(k);
            labels[k] = new JLabel(canvas.getGUI().getImageLibrary().getGoodsImageIcon(goodsTypes[k]));
        }
        
        goodsPanel.setSize(goodsPanel.getPreferredSize());
        add(goodsPanel);

        colopediaButton = new JButton(Messages.message("menuBar.colopedia"));
        colopediaButton.setActionCommand(String.valueOf(COLOPEDIA));
        colopediaButton.addActionListener(this);
        enterPressesWhenFocused(colopediaButton);

        okButton = new JButton(Messages.message("ok"));
        okButton.setActionCommand(String.valueOf(OK));
        okButton.addActionListener(this);
        okButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        enterPressesWhenFocused(okButton);

        // Use ESCAPE for closing the panel:
        InputMap inputMap = new ComponentInputMap(okButton);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false), "pressed");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, true), "released");
        SwingUtilities.replaceUIInputMap(okButton, JComponent.WHEN_IN_FOCUSED_WINDOW, inputMap);        

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(colopediaButton);
        buttonPanel.add(okButton);
        add(buttonPanel);

    }


    public void requestFocus() {
        okButton.requestFocus();
    }


    /**
     * Initializes the information that is being displayed on this panel.
     * The information displayed will be based on the given tile.
     *
     * @param tile The Tile whose information should be displayed.
     */
    public void initialize(Tile tile) {
        this.tileType = tile.getType();
        tileNameLabel.setText(tile.getName() + " (" + tile.getX() + ", " +
                              tile.getY() + ")");
        if (tile.getOwner() == null) {
            ownerLabel.setText("");
        } else {
            String ownerName = tile.getOwner().getNationAsString();
            if (ownerName == null) {
                ownerLabel.setText("");
            } else {
                ownerLabel.setText(ownerName);
            }
        }
        
        goodsPanel.removeAll();
        if (tileType == null) {
            colopediaButton.setEnabled(false);
        } else {
            List<AbstractGoods> production = tileType.getProduction();
            for (AbstractGoods goods : production) {
                JLabel label = new JLabel(canvas.getGUI().getImageLibrary().getGoodsImageIcon(goods.getType()));
                label.setText(String.valueOf(tile.potential(goods.getType())));
                goodsPanel.add(label);
            }
        }
        setSize(getPreferredSize());

    }


    /**
     * This function analyses an event and calls the right methods to take
     * care of the user's requests.
     * @param event The incoming ActionEvent.
     */
    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();
        try {
            switch (Integer.valueOf(command).intValue()) {
            case OK:
                setResponse(new Boolean(true));
                break;
            case COLOPEDIA:
                setResponse(new Boolean(true));
                canvas.showColopediaPanel(ColopediaPanel.COLOPEDIA_TERRAIN, tileType.getIndex());
                break;
            default:
                logger.warning("Invalid Actioncommand: invalid number.");
            }
        } catch (NumberFormatException e) {
            logger.warning("Invalid Actioncommand: not a number.");
        }
    }
}
