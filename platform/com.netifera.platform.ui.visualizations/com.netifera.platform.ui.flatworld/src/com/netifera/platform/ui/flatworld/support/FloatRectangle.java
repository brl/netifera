package com.netifera.platform.ui.flatworld.support;


public class FloatRectangle {

	public final float x;
	public final float y;
	public final float width;
	public final float height;
	
	public FloatRectangle(float x, float y, float width, float height) {
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
	}

	public boolean equals (Object object) {
		if (object == this) return true;
		if (!(object instanceof FloatRectangle)) return false;
		FloatRectangle r = (FloatRectangle)object;
		return (r.x == this.x) && (r.y == this.y) && (r.width == this.width) && (r.height == this.height);
	}

	public int hashCode () {
		return Float.floatToIntBits(x) ^ Float.floatToIntBits(y) ^ Float.floatToIntBits(width) ^ Float.floatToIntBits(height);
	}

	public boolean contains (float x, float y) {
		return (x >= this.x) && (y >= this.y) && ((x - this.x) < width) && ((y - this.y) < height);
	}
	
	public boolean contains (FloatPoint point) {
		return contains(point.x, point.y);
	}
	
	public FloatRectangle intersection(FloatRectangle rect) {
		if (this == rect) return this;
		float left = x > rect.x ? x : rect.x;
		float top = y > rect.y ? y : rect.y;
		float lhs = x + width;
		float rhs = rect.x + rect.width;
		float right = lhs < rhs ? lhs : rhs;
		lhs = y + height;
		rhs = rect.y + rect.height;
		float bottom = lhs < rhs ? lhs : rhs;
		return new FloatRectangle (
			right < left ? 0 : left,
			bottom < top ? 0 : top,
			right < left ? 0 : right - left,
			bottom < top ? 0 : bottom - top);
	}
	
	public boolean intersects(FloatRectangle rect) {
		return !intersection(rect).isEmpty();
	}
	
	public boolean isEmpty () {
		return (width <= 0) || (height <= 0);
	}
	
	public FloatPoint topLeft() {
		return new FloatPoint(x,y);
	}

	public FloatPoint bottomRight() {
		return new FloatPoint(x+width,y+height);
	}
}
