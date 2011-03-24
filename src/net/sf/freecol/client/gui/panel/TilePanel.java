/**
 *  Copyright (C) 2002-2011  The FreeCol Team
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

import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;

import java.util.logging.Logger;

import javax.swing.ComponentInputMap;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileType;
import net.sf.freecol.common.model.UnitType;

import net.miginfocom.swing.MigLayout;


/**
 * This panel is used to show information about a tile.
 */
public final class TilePanel extends FreeColPanel {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(TilePanel.class.getName());

    private static final String COLOPEDIA = "COLOPEDIA";

    private TileType tileType;


    /**
     * The constructor that will add the items to this panel.
     * @param parent The parent panel.
     * @param tile a <code>Tile</code> value
     */
    public TilePanel(Canvas parent, Tile tile) {
        super(parent);

        tileType = tile.getType();

        setLayout(new MigLayout("wrap 1, insets 20 30 10 30", "[center]", ""));

        JButton colopediaButton = new JButton(Messages.message("menuBar.colopedia"));
        colopediaButton.setActionCommand(String.valueOf(COLOPEDIA));
        colopediaButton.addActionListener(this);
        enterPressesWhenFocused(colopediaButton);

        // Use ESCAPE for closing the panel:
        InputMap inputMap = new ComponentInputMap(okButton);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false), "pressed");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, true), "released");
        SwingUtilities.replaceUIInputMap(okButton, JComponent.WHEN_IN_FOCUSED_WINDOW, inputMap);        

        String name = Messages.message(tile.getLabel()) + " (" + tile.getX() + ", " + tile.getY() + ")";
        add(new JLabel(name));

        int width = getLibrary().getTerrainImageWidth(tileType);
        int height = getLibrary().getCompoundTerrainImageHeight(tileType);
        int baseHeight = getLibrary().getTerrainImageHeight(tileType);
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = (Graphics2D) image.getGraphics();
        g.translate(0, height - baseHeight);
        getCanvas().getGUI().displayColonyTile(g, tile, null);
        add(new JLabel(new ImageIcon(image)));

        if (tile.getRegion() != null) {
            add(localizedLabel(tile.getRegion().getLabel()));
        }
        if (tile.getOwner() != null) {
            StringTemplate ownerName = tile.getOwner().getNationName();
            if (ownerName != null) {
                add(localizedLabel(ownerName));
            }
        }

        if (tileType != null) {
            // TODO: make this more generic
            UnitType colonist = getSpecification().getUnitType("model.unit.freeColonist");

            JLabel label = null;
            boolean first = true;
            for (GoodsType goodsType : getSpecification().getFarmedGoodsTypeList()) {
                int potential = tile.potential(goodsType, colonist);
                UnitType expert = getSpecification().getExpertForProducing(goodsType);
                int expertPotential = tile.potential(goodsType, expert);
                if (potential > 0) {
                    label = new JLabel(String.valueOf(potential),
                                       getLibrary().getGoodsImageIcon(goodsType),
                                       JLabel.CENTER);
                    if (first) {
                        add(label, "split");
                        first = false;
                    } else {
                        add(label);
                    }
                }
                if (expertPotential > potential) {
                    if (label == null) {
                        // this could happen if a resource were exploitable
                        // only by experts, for example
                        label = new JLabel(String.valueOf(expertPotential),
                                           getLibrary().getGoodsImageIcon(goodsType),
                                           JLabel.CENTER);
                        label.setToolTipText(Messages.message(expert.getNameKey()));
                        if (first) {
                            add(label, "split");
                            first = false;
                        } else {
                            add(new JLabel("/"));
                            add(label);
                        }
                    } else {
                        label.setText(String.valueOf(potential) + "/" +
                                      String.valueOf(expertPotential));
                        label.setToolTipText(Messages.message(colonist.getNameKey()) + "/" +
                                             Messages.message(expert.getNameKey()));
                    }
                }
            }
        }

        add(okButton, "newline 30, split 2, align center, tag ok");
        add(colopediaButton, "tag help");

        setSize(getPreferredSize());

    }


    /**
     * This function analyses an event and calls the right methods to take
     * care of the user's requests.
     * @param event The incoming ActionEvent.
     */
    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();
        if (OK.equals(command)) {
            getCanvas().remove(this);
        } else {
            getCanvas().showPanel(new ColopediaPanel(getCanvas(),
                                                     ColopediaPanel.PanelType.TERRAIN, tileType));
        }
    }

    @Override
    public String getUIClassID() {
        return "TilePanelUI";
    }
}
