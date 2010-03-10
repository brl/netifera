package com.netifera.platform.ui.flatworld.quadtrees;

import com.netifera.platform.ui.flatworld.support.FloatPoint;


public interface IQuadTreeElementsVisitor<E> {
	void visit(QuadTree<E> tree, FloatPoint location, E element);
}
