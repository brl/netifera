package com.netifera.platform.ui.flatworld.quadtrees;

import java.util.HashMap;
import java.util.Map;

import com.netifera.platform.ui.flatworld.support.FloatPoint;
import com.netifera.platform.ui.flatworld.support.FloatRectangle;

public class QuadTree<E> {

	static public int BRANCH_THRESHOLD = 8;
	
	final private FloatRectangle bounds;
	private QuadTree<E> quadrants[];
	private Map<FloatPoint,E> contents = new HashMap<FloatPoint,E>();

	public QuadTree(FloatRectangle bounds) {
		this.bounds = bounds;
	}

	public FloatRectangle getBounds() {
		return bounds;
	}
	
	public int size() {
		if (isLeaf())
			return contents.size();
		int count = 0;
		for (QuadTree<E> quadrant: quadrants)
			count += quadrant.size();
		return count;
	}
	
	private boolean isLeaf() {
		return contents != null;
	}
	
	public void put(FloatPoint location, E element) {
/*		if (!bounds.contains(location))
			return;
*/
		if (isLeaf()) {
			contents.put(location, element);
			branchIfBig();
		} else {
			for (QuadTree<E> quadrant: quadrants) {
				if (quadrant.bounds.contains(location)) {
					quadrant.put(location, element);
					return;
				}
			}
		}
	}

	public E get(FloatPoint location) {
/*		if (!bounds.contains(location))
			return null;
*/		if (isLeaf()) {
			return contents.get(location);
		} else {
			for (QuadTree<E> quadrant: quadrants) {
				if (quadrant.bounds.contains(location)) {
					return quadrant.get(location);
				}
			}
		}
		return null;
	}

	public void visit(FloatRectangle region, IQuadTreeVisitor<E> visitor) {
		if (!region.intersects(bounds))
			return;
		if (visitor.visit(this)) {
			if (!isLeaf()) {
				for (QuadTree<E> quadrant: quadrants)
					quadrant.visit(region, visitor);
			}
		}
	}

	public void visit(FloatRectangle region, IQuadTreeElementsVisitor<E> visitor) {
		region = bounds.intersection(region);
		if (region.isEmpty())
			return;
		if (isLeaf()) {
			if (bounds.equals(region))
				for (FloatPoint location: contents.keySet())
					visitor.visit(this, location, contents.get(location));
			else
				for (FloatPoint location: contents.keySet())
					if (region.contains(location))
						visitor.visit(this, location, contents.get(location));
		} else {
			for (QuadTree<E> quadrant: quadrants)
				quadrant.visit(region, visitor);
		}
	}
	
	@SuppressWarnings("unchecked")
	private void branchIfBig() {
		if (contents.size() < BRANCH_THRESHOLD)
			return;
		
		float w = 0;
		float h = 0;
		for (FloatPoint location: contents.keySet()) {
			w += location.x - bounds.x;
			h += location.y - bounds.y;
		}
		w /= contents.size();
		h /= contents.size();
		
		quadrants = new QuadTree[4];
		quadrants[0] = new QuadTree<E>(new FloatRectangle(bounds.x, bounds.y, w, h));
		quadrants[1] = new QuadTree<E>(new FloatRectangle(bounds.x + w, bounds.y, bounds.width - w, h));
		quadrants[2] = new QuadTree<E>(new FloatRectangle(bounds.x, bounds.y + h, w, bounds.height - h));
		quadrants[3] = new QuadTree<E>(new FloatRectangle(bounds.x + w, bounds.y + h, bounds.width - w, bounds.height - h));

		for (FloatPoint location: contents.keySet()) {
			for (QuadTree<E> quadrant: quadrants) {
				if (quadrant.bounds.contains(location)) {
					quadrant.put(location, contents.get(location));
					break;
				}
			}
		}
		contents = null;
	}
}
