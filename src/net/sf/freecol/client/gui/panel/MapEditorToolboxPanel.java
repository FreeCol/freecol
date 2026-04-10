/**
 *  Copyright (C) 2002-2024   The FreeCol Team
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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.control.MapEditorController;
import net.sf.freecol.client.control.MapEditorTool;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.SwingGUI.PopupPosition;
import net.sf.freecol.client.gui.panel.WrapLayout.HorizontalAlignment;
import net.sf.freecol.client.gui.panel.WrapLayout.HorizontalGap;
import net.sf.freecol.common.i18n.Messages;


/**
 * A panel for choosing the current tool in the map editor.
 *
 * This panel is only used when running in
 * {@link net.sf.freecol.client.FreeColClient#isMapEditor() map editor mode}.
 *
 * @see MapEditorTransformPanel
 */
public final class MapEditorToolboxPanel extends FreeColPanel {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(MapEditorToolboxPanel.class.getName());

    private final JPanel listPanel;
    private final ButtonGroup group;


    /**
     * Creates a panel for choosing the current tool in the map editor.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     */
    public MapEditorToolboxPanel(FreeColClient freeColClient) {
        super(freeColClient, null, new BorderLayout());

        final Dimension terrainSize = ImageLibrary.scaleDimension(getImageLibrary().scale(ImageLibrary.TILE_OVERLAY_SIZE), ImageLibrary.SMALLER_SCALE);
        listPanel = new JPanel(new WrapLayout()
                .withForceComponentSize(terrainSize)
                .withHorizontalAlignment(HorizontalAlignment.LEFT)
                .withHorizontalGap(HorizontalGap.AUTO));

        group = new ButtonGroup();
        //Add an invisible, move button to de-select all others
        group.add(new JToggleButton());
        buildList();

        JScrollPane sl = new JScrollPane(listPanel,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        sl.getViewport().setOpaque(false);
        listPanel.setSize(new Dimension(terrainSize.width * 3, 0));
        add(sl, BorderLayout.CENTER);
        setBorder(BorderFactory.createEmptyBorder());
        revalidate();
        repaint();
    }

    @Override
    public String getFrameTitle() {
        return Messages.message("mapEditor.toolBoxPanel.title");
    }
    
    @Override
    public PopupPosition getFramePopupPosition() {
        return PopupPosition.UPPER_RIGHT;
    }

    /**
     * Builds the buttons for all the tools.
     */
    private void buildList() {
        final MapEditorController ctlr = getFreeColClient().getMapEditorController();
        final MapEditorTool defaultTool = ctlr.getCurrentTool();
        for (MapEditorTool mapEditorTool : MapEditorTool.values()) {
            final boolean isDefaultTool = (mapEditorTool == defaultTool);
            listPanel.add(buildButton(mapEditorTool, isDefaultTool));
        }
    }

    private JToggleButton buildButton(MapEditorTool mapEditorTool, boolean defaultTool) {
        final String text = Messages.getName(mapEditorTool.getId());
        final MapEditorController ctlr = getFreeColClient().getMapEditorController();
        /* TODO: Update description panel?
        JPanel descriptionPanel = new JPanel(new BorderLayout());
        descriptionPanel.add(new JLabel(new ImageIcon(image)),
                             BorderLayout.CENTER);
        descriptionPanel.add(new JLabel(text, JLabel.CENTER),
                BorderLayout.PAGE_END);
        descriptionPanel.setBackground(Color.RED);
        mt.setDescriptionPanel(descriptionPanel);
        */
        final Dimension riverSize = ImageLibrary.scaleDimension(getImageLibrary().scale(ImageLibrary.TILE_SIZE), ImageLibrary.SMALLER_SCALE);
        ImageIcon icon = new ImageIcon(getImageLibrary().getScaledImage("image.ui.includesSpecification")); // TODO: Add icon
        final JToggleButton button = new JToggleButton(icon);
        button.setToolTipText(text);
        button.setOpaque(false);
        button.setSelected(defaultTool);
        group.add(button);
        button.addActionListener((ActionEvent ae) -> {

            });
        button.setBorder(null);
        return button;
    }
}
