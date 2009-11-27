package com.netifera.platform.ui.flatworld.quadtrees;


public interface IQuadTreeVisitor<E> {
	boolean visit(QuadTree<E> tree);
}
