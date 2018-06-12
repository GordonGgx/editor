package com.ggx.editor.utils;

public class Range
{
	public final int start;
	public final int end;

	public Range(int start, int end) {
		this.start = start;
		this.end = end;
	}

	@Override
	public String toString() {
		return start + "-" + end;
	}
}
