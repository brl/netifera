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
		//WARNING this can be wrong, but will work for fixed float values taken from a geolocation coordinates database
		if (o instanceof FloatPoint)
			return ((FloatPoint)o).x == x && ((FloatPoint)o).y == y;
		return false;
	}
	
	public String toString() {
		return "("+x+", "+y+")";
	}
}
