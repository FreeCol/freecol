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

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.text.BadLocationException;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.tree.DefaultMutableTreeNode;

import net.sf.freecol.client.gui.action.ColopediaAction.PanelType;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.AbstractUnit;
import net.sf.freecol.common.model.BuildableType;
import net.sf.freecol.common.model.Feature;
import net.sf.freecol.common.model.FreeColGameObjectType;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Modifier;
import net.sf.freecol.common.model.ResourceType;
import net.sf.freecol.common.model.Scope;
import net.sf.freecol.common.model.Unit.Role;
import net.sf.freecol.common.model.UnitType;


/**
 * This panel displays the Colopedia.
 */
public abstract class ColopediaGameObjectTypePanel<T extends FreeColGameObjectType>
    extends FreeColPanel implements ColopediaDetailPanel<T> {

    private String id;
    private double scale = 1;
    private ColopediaPanel colopediaPanel;

    public ColopediaGameObjectTypePanel(ColopediaPanel colopediaPanel, String id, double scale) {
        super(colopediaPanel.getCanvas());
        this.colopediaPanel = colopediaPanel;
        this.id = "colopediaAction." + id;
        this.scale = scale;
    }

    /**
     * Returns the name of this ColopediaDetailPanel, which is
     * generally used to label the root node of its sub-tree.
     *
     * @return a String value
     */
    public String getName() {
        return Messages.message(id + ".name");
    }

    protected String getId() {
        return id;
    }

    /**
     * Returns the scale to use for icons.
     *
     * @return a double value
     */
    protected double getScale() {
        return scale;
    }

    /**
     * Builds a subtree including all the given objects.
     *
     * @param root a <code>DefaultMutableTreeNode</code>
     * @param types a List of FreeColGameObjectTypes
     */
    public void addSubTrees(DefaultMutableTreeNode root, List<T> types) {
        addSubTrees(root, id, types);
    }

    /**
     * Builds a subtree including all the given objects.
     *
     * @param root a <code>DefaultMutableTreeNode</code>
     * @param id the ID of the new branch node
     * @param types a List of FreeColGameObjectTypes
     */
    public void addSubTrees(DefaultMutableTreeNode root, String id, List<T> types) {
        String name = Messages.message(id + ".name");
        DefaultMutableTreeNode node =
            new DefaultMutableTreeNode(new ColopediaTreeItem(this, id, name, null));
        int width = 0;
        int height = 0;
        for (FreeColGameObjectType type : types) {
            Image image = getLibrary().getImage(type, scale);
            width = Math.max(image.getWidth(null), width);
            height = Math.max(image.getHeight(null), height);
        }
        for (FreeColGameObjectType type : types) {
            BufferedImage centeredImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Image image = getLibrary().getImage(type, scale);
            int x = (width - image.getWidth(null)) / 2;
            int y = (height - image.getHeight(null)) / 2;
            centeredImage.getGraphics().drawImage(image, x, y, null);
            node.add(buildItem(type, new ImageIcon(centeredImage)));
        }
        root.add(node);
    }

    protected DefaultMutableTreeNode buildItem(FreeColGameObjectType type, ImageIcon icon) {
        String name = Messages.getName(type);
        DefaultMutableTreeNode item =
            new DefaultMutableTreeNode(new ColopediaTreeItem(this, type.getId(), name, icon));
        return item;
    }


    protected JButton getButton(FreeColGameObjectType type, String text, ImageIcon icon) {
        JButton button = getLinkButton(text == null ? Messages.getName(type) : text, icon, type.getId());
        button.addActionListener(colopediaPanel);
        return button;
    }

    protected JButton getButton(PanelType panelType, String text, ImageIcon icon) {
        JButton button = getLinkButton(text, icon, "colopediaAction." + panelType);
        button.addActionListener(colopediaPanel);
        return button;
    }

    protected JButton getButton(FreeColGameObjectType type) {
        return getButton(type, null, null);
    }

    protected JButton getResourceButton(final ResourceType resourceType) {
        return getButton(resourceType, null, getLibrary().getBonusImageIcon(resourceType));
    }

    protected JButton getGoodsButton(final GoodsType goodsType) {
        return getGoodsButton(goodsType, null);
    }

    protected JButton getGoodsButton(final GoodsType goodsType, int amount) {
        return getGoodsButton(goodsType, Integer.toString(amount));
    }

    protected JButton getGoodsButton(final GoodsType goodsType, String text) {
        JButton result = getButton(goodsType, text, getLibrary().getGoodsImageIcon(goodsType));
        result.setToolTipText(Messages.getName(goodsType));
        return result;
    }

    protected JButton getUnitButton(AbstractUnit unit) {
        return getUnitButton(unit.getUnitType(getSpecification()), unit.getRole());
    }

    protected JButton getUnitButton(final UnitType unitType, Role role) {
        ImageIcon unitIcon = getLibrary().getUnitImageIcon(unitType, role, 0.66);
        JButton unitButton = getButton(unitType, null, unitIcon);
        unitButton.setHorizontalAlignment(JButton.LEFT);
        return unitButton;
    }

    protected JButton getUnitButton(final UnitType unitType) {
        return getUnitButton(unitType, Role.DEFAULT);
    }

    private String getFeatureName(Feature feature) {
        return Messages.message(feature.getNameKey());
    }

    private String getFeatureAsString(Feature feature) {
        String label = Messages.message(getFeatureName(feature)) + ":";
        if (feature.hasScope()) {
            for (Scope scope : feature.getScopes()) {
                String key = null;
                if (scope.getType() != null) {
                    key = scope.getType();
                } else if (scope.getAbilityID() != null) {
                    key = scope.getAbilityID();
                } else if (scope.getMethodName() != null) {
                    key = "model.scope." + scope.getMethodName();
                }
                if (key != null) {
                    label += (scope.isMatchNegated() ? " !" : " ")
                        + Messages.message(key + ".name") + ",";
                }
            }
        }
        return label.substring(0, label.length() - 1);
    }

    public String getModifierAsString(Modifier modifier) {
        String bonus = modifierFormat.format(modifier.getValue());
        switch(modifier.getType()) {
        case ADDITIVE:
            if (modifier.getValue() > 0) {
                bonus = "+" + bonus;
            }
            break;
        case PERCENTAGE:
            if (modifier.getValue() > 0) {
                bonus = "+" + bonus;
            }
            bonus = bonus + "%";
            break;
        case MULTIPLICATIVE:
            bonus = "\u00D7" + bonus;
            break;
        default:
        }
        return bonus;
    }

    public JComponent getModifierComponent(Modifier modifier) {
        try {
            GoodsType goodsType = getSpecification()
                .getGoodsType(modifier.getId());
            String bonus = getModifierAsString(modifier);
            return getGoodsButton(goodsType, bonus);
        } catch(Exception e) {
            // not a production bonus
            JLabel label = new JLabel(getFeatureAsString(modifier) + ": "
                                      + getModifierAsString(modifier));
            label.setToolTipText(Messages.message(modifier.getId() + ".shortDescription"));
            return label;
        }
    }

    public JLabel getAbilityComponent(Ability ability) {
        if (ability.getValue()) {
            JLabel label = new JLabel(getFeatureAsString(ability));
            label.setToolTipText(Messages.message(ability.getId() + ".shortDescription"));
            return label;
        } else {
            return null;
        }
    }

    public void appendRequiredAbilities(StyledDocument doc, BuildableType buildableType)
        throws BadLocationException {
        for (Entry<String, Boolean> entry : buildableType.getAbilitiesRequired().entrySet()) {
            doc.insertString(doc.getLength(),
                             Messages.message(entry.getKey() + ".name"),
                             doc.getStyle("regular"));
            List<JButton> requiredTypes = new ArrayList<JButton>();
            for (FreeColGameObjectType type : getSpecification()
                     .getTypesProviding(entry.getKey(), entry.getValue())) {
                JButton typeButton = getButton(type);
                typeButton.addActionListener(this);
                requiredTypes.add(typeButton);
            }
            if (!requiredTypes.isEmpty()) {
                doc.insertString(doc.getLength(), " (", doc.getStyle("regular"));
                StyleConstants.setComponent(doc.getStyle("button"), requiredTypes.get(0));
                doc.insertString(doc.getLength(), " ", doc.getStyle("button"));
                for (int index = 1; index < requiredTypes.size(); index++) {
                    JButton button = requiredTypes.get(index);
                    doc.insertString(doc.getLength(), " / ", doc.getStyle("regular"));
                    StyleConstants.setComponent(doc.getStyle("button"), button);
                    doc.insertString(doc.getLength(), " ", doc.getStyle("button"));
                }
                doc.insertString(doc.getLength(), ")", doc.getStyle("regular"));
            }
            doc.insertString(doc.getLength(), "\n", doc.getStyle("regular"));
        }
    }


}
