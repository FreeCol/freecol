/**
 *  Copyright (C) 2002-2019   The FreeCol Team
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

package net.sf.freecol.client.gui.panel.colopedia;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.text.BadLocationException;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.tree.DefaultMutableTreeNode;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.*;
import net.sf.freecol.client.gui.action.ColopediaAction.PanelType;
import net.sf.freecol.client.gui.panel.*;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.AbstractUnit;
import net.sf.freecol.common.model.BuildableType;
import net.sf.freecol.common.model.FreeColSpecObjectType;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Modifier;
import net.sf.freecol.common.model.Nation;
import net.sf.freecol.common.model.ResourceType;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.UnitType;
import static net.sf.freecol.common.util.CollectionUtils.*;


/**
 * This panel displays details of game objects in the Colopedia.
 */
public abstract class ColopediaGameObjectTypePanel<T extends FreeColSpecObjectType>
    extends FreeColPanel implements ColopediaDetailPanel<T> {

    private static final Logger logger = Logger.getLogger(ColopediaGameObjectTypePanel.class.getName());

    /** The enclosing colopedia panel. */
    private ColopediaPanel colopediaPanel; 

    /** The specific panel id. */
    private final String id;


    /**
     * Create a new Colopedia game object type panel.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     * @param colopediaPanel The parent {@code ColopediaPanel}.
     * @param id The panel type identifier.
     */
    protected ColopediaGameObjectTypePanel(FreeColClient freeColClient,
                                           ColopediaPanel colopediaPanel,
                                           String id) {
        super(freeColClient);

        this.colopediaPanel = colopediaPanel;
        this.id = "colopediaAction." + id;
    }


    /**
     * Get the panel id.
     *
     * @return The panel id, usually "colopediaAction.*".
     */
    protected String getId() {
        return id;
    }

    /**
     * Get the name of this ColopediaDetailPanel, which is generally
     * used to label the root node of its sub-tree.
     *
     * @return The panel name.
     */
    @Override
    public String getName() {
        return Messages.getName(id);
    }

    /**
     * Builds a subtree including all the given objects.
     *
     * @param root a {@code DefaultMutableTreeNode}
     * @param types a List of FreeColSpecObjectTypes
     */
    protected void addSubTrees(DefaultMutableTreeNode root, List<T> types) {
        addSubTrees(root, id, types);
    }

    /**
     * Builds a subtree including all the given objects.
     *
     * @param root a {@code DefaultMutableTreeNode}
     * @param id The object identifier of the new branch node.
     * @param types a List of FreeColSpecObjectTypes
     */
    protected void addSubTrees(DefaultMutableTreeNode root, String id,
                               List<T> types) {
        final ImageLibrary lib = getImageLibrary();
        String name = getName();
        ColopediaTreeItem cti = new ColopediaTreeItem(this, id, name, null);
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(cti);
        int width = ImageLibrary.ICON_SIZE.width;
        int height = ImageLibrary.ICON_SIZE.height;
        for (FreeColSpecObjectType type : types) {
            Image image = lib.getObjectImage(type, ImageLibrary.ICON_SIZE);
            int x = (width - image.getWidth(null)) / 2;
            int y = (height - image.getHeight(null)) / 2;
            BufferedImage centeredImage
                = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = centeredImage.createGraphics();
            g.drawImage(image, x, y, null);
            g.dispose();
            node.add(buildItem(type, new ImageIcon(centeredImage)));
        }
        root.add(node);
    }

    protected DefaultMutableTreeNode buildItem(FreeColSpecObjectType type,
                                               ImageIcon icon) {
        String name = Messages.getName(type);
        return new DefaultMutableTreeNode(new ColopediaTreeItem(this, 
                type.getId(), name, icon));
    }

    protected JButton getButton(FreeColSpecObjectType type, String text,
                                ImageIcon icon) {
        JButton button = Utility.getLinkButton((text != null) ? text
            : Messages.getName(type), icon, type.getId());
        button.addActionListener(colopediaPanel);
        return button;
    }

    protected JButton getButton(PanelType panelType, String text,
                                ImageIcon icon) {
        JButton button = Utility.getLinkButton(text, icon,
            "colopediaAction." + panelType.getKey());
        button.addActionListener(colopediaPanel);
        return button;
    }

    protected JButton getButton(FreeColSpecObjectType type) {
        return getButton(type, null, null);
    }

    protected JButton getResourceButton(final ResourceType resourceType) {
        return getButton(resourceType, null, new ImageIcon(getImageLibrary()
                .getScaledResourceTypeImage(resourceType)));
    }

    protected JButton getGoodsButton(final GoodsType goodsType) {
        return getGoodsButton(goodsType, null);
    }

    protected JButton getGoodsButton(final GoodsType goodsType, int amount) {
        return getGoodsButton(goodsType, Integer.toString(amount));
    }

    protected JButton getGoodsButton(final AbstractGoods goods) {
        return getGoodsButton(goods.getType(), goods.getAmount());
    }

    protected JButton getGoodsButton(final GoodsType goodsType, String text) {
        JButton result = getButton(goodsType, text,
            new ImageIcon(getImageLibrary().getScaledGoodsTypeImage(goodsType)));
        result.setToolTipText(Messages.getName(goodsType));
        return result;
    }

    protected JButton getUnitButton(AbstractUnit au) {
        return getUnitButton(au.getType(getSpecification()), au.getRoleId());
    }

    protected JButton getUnitButton(final UnitType unitType, String roleId) {
        ImageIcon unitIcon = new ImageIcon(getImageLibrary()
            .getSmallUnitTypeImage(unitType, roleId, false));
        JButton unitButton = getButton(unitType, null, unitIcon);
        unitButton.setHorizontalAlignment(JButton.LEFT);
        return unitButton;
    }

    protected JButton getUnitButton(final UnitType unitType) {
        return getUnitButton(unitType, unitType.getDisplayRoleId());
    }

    public JComponent getModifierComponent(Modifier modifier) {
        try {
            GoodsType goodsType = getSpecification()
                .getGoodsType(modifier.getId());
            String bonus = ModifierFormat.getModifierAsString(modifier);
            return getGoodsButton(goodsType, bonus);
        } catch (Exception e) {
            // not a production bonus
            JLabel label = new JLabel(ModifierFormat.getFeatureAsString(modifier) + ": "
                + ModifierFormat.getModifierAsString(modifier));
            label.setToolTipText(Messages.getShortDescription(modifier));
            return label;
        }
    }

    public JLabel getAbilityComponent(Ability ability) {
        if (ability.getValue()) {
            JLabel label = new JLabel(ModifierFormat.getFeatureAsString(ability));
            label.setToolTipText(Messages.getShortDescription(ability));
            return label;
        } else {
            return null;
        }
    }

    public void appendRequiredAbility(StyledDocument doc, String key, boolean value) {
        final Specification spec = getSpecification();
        try {
            doc.insertString(doc.getLength(), Messages.getName(key),
                             doc.getStyle("regular"));
            List<JButton> requiredTypes
                 = transform(spec.getTypesProviding(key, value), alwaysTrue(),
                     t -> {
                         JButton typeButton = getButton(t);
                         typeButton.addActionListener(this);
                         return typeButton;
                     });
            JButton rt = first(requiredTypes);
            if (rt != null) {
                doc.insertString(doc.getLength(), " (",
                                 doc.getStyle("regular"));
                StyleConstants.setComponent(doc.getStyle("button"), rt);
                doc.insertString(doc.getLength(), " ", doc.getStyle("button"));
                for (int index = 1; index < requiredTypes.size(); index++) {
                    JButton button = requiredTypes.get(index);
                    doc.insertString(doc.getLength(), " / ",
                                     doc.getStyle("regular"));
                    StyleConstants.setComponent(doc.getStyle("button"), button);
                    doc.insertString(doc.getLength(), " ",
                                     doc.getStyle("button"));
                }
                doc.insertString(doc.getLength(), ")",
                                 doc.getStyle("regular"));
            }
            doc.insertString(doc.getLength(), "\n", doc.getStyle("regular"));
        } catch (BadLocationException ble) {
            logger.log(Level.WARNING, "Insert fail", ble);
        }
    }
        

    // Override Component

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeNotify() {
        super.removeNotify();

        colopediaPanel = null;
    }
}
