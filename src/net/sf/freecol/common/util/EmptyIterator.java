
package net.sf.freecol.common.util;


import java.util.Iterator;
import java.util.NoSuchElementException;


public final class EmptyIterator implements Iterator
{
    public static final  EmptyIterator  SHARED_INSTANCE = new EmptyIterator();


    // ----------------------------------------------------------- constructors

    private EmptyIterator() {
    }


    // ---------------------------------------------------- interface: Iterator

    public boolean hasNext()
    {
        return false;
    }


    public Object next()
    {
        return new NoSuchElementException();
    }


    public void remove()
    {
        throw new IllegalStateException();
    }

}
