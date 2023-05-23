/**
 *  Copyright (C) 2002-2022   The FreeCol Team
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

package net.sf.freecol.client.gui.plaf;

import java.awt.AlphaComposite;
import java.awt.Component;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.Serializable;

import javax.swing.AbstractButton;
import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicRadioButtonUI;
import javax.swing.plaf.metal.MetalLookAndFeel;

import net.sf.freecol.client.gui.FontLibrary;
import net.sf.freecol.common.resources.ResourceManager;


/**
 * Sets the default opaque attribute to <i>false</i>.
 */
public class FreeColRadioButtonUI extends BasicRadioButtonUI {

    public static ComponentUI createUI(@SuppressWarnings("unused") JComponent c) {
        return new FreeColRadioButtonUI();
    }


    @Override
    public void installUI(JComponent c) {
        super.installUI(c);

        c.setOpaque(false);
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        LAFUtilities.setProperties(g, c);
        super.paint(g, c);
    }
    
    public static Icon createRadioButtonIcon() {
        return new RadioButtonIcon();
    }
    
    private static class RadioButtonIcon implements Icon, UIResource, Serializable {
        
        protected int getWidgetSize() {
            return FontLibrary.getMainFont().getSize() * 3 / 2;
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            final Graphics2D g2d = (Graphics2D) g;
            final ButtonModel model = ((AbstractButton) c).getModel();
            final int widgetSize = getWidgetSize();

            if (!model.isEnabled()) {
                g.setColor(MetalLookAndFeel.getControlDisabled());
                g.fillRect(x, y, widgetSize - 1, widgetSize - 1);
            } else if (model.isPressed() && model.isArmed() ) {
                g.setColor(MetalLookAndFeel.getControlShadow());
                g.fillRect(x, y, widgetSize - 1, widgetSize - 1);
            }

            if (c.hasFocus()) {
                g.setColor(MetalLookAndFeel.getControlHighlight());
                g.drawRect(x, y, widgetSize - 1, widgetSize - 1);
            }
            
            if (model.isSelected()) {
                final BufferedImage radioButtonImage =  ResourceManager.getImage("image.ui.button.radio.selected", 
                        new Dimension(getIconWidth(), getIconHeight()), false);

                final Composite origComposite = g2d.getComposite();
                if (!model.isEnabled()) {
                    g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
                }
                
                if (radioButtonImage != null) {
                    g2d.drawImage(radioButtonImage, x, y, null);
                }

                g2d.setComposite(origComposite);
            } else {
                final BufferedImage radioButtonImage =  ResourceManager.getImage("image.ui.button.radio", 
                        new Dimension(getIconWidth(), getIconHeight()), false);
                if (radioButtonImage != null) {
                    g2d.drawImage(radioButtonImage, x, y, null);
                }
            }
        }

        public int getIconWidth() {
            return getWidgetSize();
        }

        public int getIconHeight() {
            return getWidgetSize();
        }
    }
}
