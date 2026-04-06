package com.github.skjolber.jsonfilter.base.path;

import java.util.ArrayList;
import java.util.List;

import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter;

public class PathItemFactory {

	public PathItem create(ExpressionNode root) {
		// index 1 ... n so that easier to use in filters
		return create(null, root, 1);
	}
	
	protected PathItem create(PathItem parent, ExpressionNode node, int level) {
		if(node.hasFilterType()) {
			return new EndPathItem(level, parent, node.getFilterType());
		}
		List<ExpressionNode> nodeChildren = node.getChildren();

		if(nodeChildren.size() == 1) {
			ExpressionNode childNode = nodeChildren.get(0);
			
			if(childNode.getPath().equals(AbstractPathJsonFilter.STAR)) {
				StarPathItem anyPathItem = new StarPathItem(level, parent);
				
				PathItem childPathItem = create(anyPathItem, childNode, level + 1);
				anyPathItem.setNext(childPathItem);
				
				return anyPathItem;
			}
			SinglePathItem singlePathItem = new SinglePathItem(level, childNode.getPath(), parent);
			
			PathItem childPathItem = create(singlePathItem, childNode, level + 1);
			singlePathItem.setNext(childPathItem);
			
			return singlePathItem;
		}
		List<String> keys = new ArrayList<>();
		for(ExpressionNode child : node.getChildren()) {
			keys.add(child.getPath());
		}

		int anyIndex = keys.indexOf(AbstractPathJsonFilter.STAR);
		if(anyIndex != -1) {
						
			//
			// /a/b/c/d
			// /a/b/h/d
			//
			// /a/b/c/d
			// /a/*/c/e
			//
			// /a/*/e/f
			// /a/b/c/d
			//
			// /a/*/e/g
			// /a/*/*/f
			//
			
			keys.remove(anyIndex);
			
			ExpressionNode anyNode = nodeChildren.remove(anyIndex);

			StarMultiPathItem multiPathItem = new StarMultiPathItem(keys, level, parent);

			PathItem anyPathItem = create(multiPathItem, anyNode, level + 1);

			for(int k = 0; k < nodeChildren.size(); k++) {
				ExpressionNode childNode = nodeChildren.get(k);
				
				PathItem childPathItem = create(multiPathItem, childNode, level + 1);
				
				multiPathItem.setNext(childPathItem, k);
			}
			
			multiPathItem.setAny(anyPathItem);
			
			return multiPathItem;
			
		}
		
		MultiPathItem multiPathItem = new MultiPathItem(keys, level, parent);
		for(int k = 0; k < nodeChildren.size(); k++) {
			ExpressionNode childNode = nodeChildren.get(k);
			
			PathItem childPathItem = create(multiPathItem, childNode, level + 1);
			
			multiPathItem.setNext(childPathItem, k);
		}
		
		return multiPathItem;
	}
	
}
