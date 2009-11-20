package com.netifera.platform.ui.flatworld;


public interface IQuadTreeVisitor<E> {
	void visit(QuadTree<E> tree, FloatPoint location, E element);
}
