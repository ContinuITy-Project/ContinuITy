package org.continuity.commons.utils;

public class DataHolder<T> {

	private T content;

	public T get() {
		return content;
	}

	public void set(T content) {
		this.content = content;
	}

}
