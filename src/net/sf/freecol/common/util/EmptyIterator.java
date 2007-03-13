package net.sf.freecol.common.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

public final class EmptyIterator<T> implements Iterator<T> {
	public static final String COPYRIGHT = "Copyright (C) 2003-2006 The FreeCol Team";
	public static final String LICENSE = "http://www.gnu.org/licenses/gpl.html";
	public static final String REVISION = "$Revision$";

	public static <T> EmptyIterator<T> getInstance() {
		return new EmptyIterator<T>();
	}

	// ----------------------------------------------------------- constructors

	private EmptyIterator() {
	}

	// ---------------------------------------------------- interface: Iterator

	public boolean hasNext() {
		return false;
	}

	public T next() {
		throw new NoSuchElementException(
				"Programming error: next() should never be called on the EmptyIterator");
	}

	public void remove() {
		throw new IllegalStateException();
	}

}
