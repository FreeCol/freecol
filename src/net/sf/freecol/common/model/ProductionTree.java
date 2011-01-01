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

package net.sf.freecol.common.model;

import java.util.ArrayList;
import java.util.List;

public class ProductionTree {

    /**
     * The abstract goods all other types of goods in this tree are
     * finally stored as.
     */
    private AbstractGoods root;

    /**
     * The abstract goods that are actually produced.
     */
    private List<AbstractGoods> leafs;

    /**
     * Get the <code>Root</code> value.
     *
     * @return an <code>AbstractGoods</code> value
     */
    public final AbstractGoods getRoot() {
        return root;
    }

    /**
     * Set the <code>Root</code> value.
     *
     * @param newRoot The new Root value.
     */
    public final void setRoot(final AbstractGoods newRoot) {
        this.root = newRoot;
    }

    /**
     * Get the <code>Leafs</code> value.
     *
     * @return a <code>List<AbstractGoods></code> value
     */
    public final List<AbstractGoods> getLeafs() {
        return leafs;
    }

    /**
     * Set the <code>Leafs</code> value.
     *
     * @param newLeafs The new Leafs value.
     */
    public final void setLeafs(final List<AbstractGoods> newLeafs) {
        this.leafs = newLeafs;
    }

    public void addLeaf(AbstractGoods goods) {
        if (root == null) {
            leafs.add(goods);
            root = new AbstractGoods(goods.getType().getStoredAs(), goods.getAmount());
        } else if (goods.getType().getStoredAs() != root.getType()) {
            throw new IllegalArgumentException(goods.getType().getId() + " is not stored as "
                                               + root.getType());
        } else {
            leafs.add(goods);
            root.setAmount(root.getAmount() + goods.getAmount());
        }
    }

    public int remove(AbstractGoods goods) {
        int consumed = goods.getAmount();
        if (goods.getType() == root.getType()) {
            if (consumed > root.getAmount()) {
                consumed = root.getAmount();
                root.setAmount(0);
            } else {
                root.setAmount(root.getAmount() - consumed);
            }
            for (AbstractGoods leaf : leafs) {
                leaf.setAmount(Math.min(leaf.getAmount(), root.getAmount()));
            }
        } else {
            for (AbstractGoods leaf : leafs) {
                if (leaf.getType() == goods.getType()) {
                    if (consumed > leaf.getAmount()) {
                        consumed = leaf.getAmount();
                        leaf.setAmount(0);
                    } else {
                        leaf.setAmount(leaf.getAmount() - consumed);
                    }
                    root.setAmount(root.getAmount() - consumed);
                    break;
                }
            }
        }
        return consumed;
    }
}