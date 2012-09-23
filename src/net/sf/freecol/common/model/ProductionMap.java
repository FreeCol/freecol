/**
 *  Copyright (C) 2002-2012   The FreeCol Team
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProductionMap {

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


        public ProductionTree(AbstractGoods root, AbstractGoods... leafs) {
            if (leafs.length > 0) {
                this.leafs = new ArrayList<AbstractGoods>();
                int amount = root.getAmount();
                for (AbstractGoods leaf : leafs) {
                    this.leafs.add(new AbstractGoods(leaf));
                    amount += leaf.getAmount();
                }
                this.root = new AbstractGoods(root.getType(), amount);
            } else {
                this.root = new AbstractGoods(root);
            }
        }


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

        public void add(AbstractGoods goods) {
            if (goods.getType().getStoredAs() != root.getType()) {
                throw new IllegalArgumentException(goods.getType().getId() + " is not stored as "
                                                   + root.getType());
            } else {
                for (AbstractGoods leaf : leafs) {
                    if (leaf.getType() == goods.getType()) {
                        leaf.setAmount(leaf.getAmount() + goods.getAmount());
                        root.setAmount(root.getAmount() + goods.getAmount());
                        return;
                    }
                }
                leafs.add(new AbstractGoods(goods));
                root.setAmount(root.getAmount() + goods.getAmount());
            }
        }

        public int remove(AbstractGoods goods) {
            int consumed = goods.getAmount();
            if (goods.getType() == root.getType()) {
                root.setAmount(root.getAmount() - consumed);
                for (AbstractGoods leaf : leafs) {
                    leaf.setAmount(Math.min(leaf.getAmount(), root.getAmount()));
                }
            } else {
                for (AbstractGoods leaf : leafs) {
                    if (leaf.getType() == goods.getType()) {
                        leaf.setAmount(leaf.getAmount() - consumed);
                        root.setAmount(root.getAmount() - consumed);
                        break;
                    }
                }
            }
            return consumed;
        }

        public AbstractGoods get(GoodsType type) {
            if (root.getType() == type) {
                return root;
            } else {
                for (AbstractGoods leaf : leafs) {
                    if (leaf.getType() == type) {
                        return new AbstractGoods(type, leaf.getAmount());
                    }
                }
            }
            return null;
        }


    }


    private Map<GoodsType, Object> cache = new HashMap<GoodsType, Object>();


    public AbstractGoods get(GoodsType type) {
        Object value = cache.get(type);
        if (value == null) {
            return new AbstractGoods(type, 0);
        } else if (value instanceof Integer) {
            return new AbstractGoods(type, (Integer) value);
        } else {
            return ((ProductionTree) value).get(type);
        }
    }

    public void add(AbstractGoods goods) {
        GoodsType goodsType = goods.getType();
        Object value = cache.get(goodsType);
        if (value == null) {
            // no entry yet
            GoodsType rootType = goodsType.getStoredAs();
            if (rootType == goodsType) {
                cache.put(goodsType, goods.getAmount());
            } else {
                // is leaf of production tree
                value = cache.get(rootType);
                if (value instanceof ProductionTree) {
                    // entry is already present
                    ((ProductionTree) value).add(goods);
                } else {
                    // add new root entry
                    Integer amount = (value == null) ? 0 : (Integer) value;
                    value = new ProductionTree(new AbstractGoods(rootType, amount), goods);
                    cache.put(rootType, value);
                }
                // add the same entry for the goods type itself
                cache.put(goodsType, value);
            }
        } else if (value instanceof Integer) {
            cache.put(goodsType, (Integer) value + goods.getAmount());
        } else {
            ((ProductionTree) value).add(goods);
        }
    }

    public void remove(AbstractGoods goods) {
        Object value = cache.get(goods.getType());
        if (value instanceof ProductionTree) {
            ((ProductionTree) value).remove(goods);
        } else {
            add(new AbstractGoods(goods.getType(), -goods.getAmount()));
        }
    }


    public void add(List<AbstractGoods> goods) {
        for (AbstractGoods g : goods) {
            add(g);
        }
    }

    public void remove(List<AbstractGoods> goods) {
        for (AbstractGoods g : goods) {
            remove(g);
        }
    }

}