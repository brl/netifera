package com.netifera.platform.ui.flatworld.support;

public class FloatPoint {
	public final float x;
	public final float y;

	public FloatPoint(float x, float y) {
		this.x = x;
		this.y = y;
	}
	
	public int hashCode() {
		return Float.floatToIntBits(x) ^ Float.floatToIntBits(y);
	}
	
	public boolean equals(Object o) {
		if (o instanceof FloatPoint)
			return ((FloatPoint)o).x == x && ((FloatPoint)o).y == y;
		return false;
	}
}
