package com.github.skjolber.jsonfilter.base.match;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter;
import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter.FilterType;
import com.github.skjolber.jsonfilter.base.match.PathItemFactory.ExpressionNode;

public class PathItemFactory {

	private static class ExpressionNode {
		private int index;
		private ExpressionNode parent;
		
		private FilterType filterType;
		
		private Map<String, List<ExpressionNode>> children = new HashMap<>();
		
		private String key;
		
		private PathItem target;
	}
	
	// /one/two/three/five
	// /one/two/*/three1
	// /one/two/*/three1
	
	public PathItem create(String[] expressions, FilterType[] types) {
		Arrays.sort(expressions, (a, b) -> Integer.compare(a.length(), b.length()));
		
		List<String> filteredExpressions = new ArrayList<>(expressions.length);
		
		
		// filter
		// /one/two/three
		// /one/two/three <--- removed
		// /one/two/three/four <--- removed
		// /one/two/threes/fours <--- kept
		
		add:
		for(String expression : expressions) {
			for (String filteredExpression : filteredExpressions) {
				if(filteredExpression.equals(expression)) {
					continue add;
				}
				
				if(filteredExpression.startsWith(expression)) {
					if(expression.length() > filteredExpression.length()) {
						char c = expression.charAt(filteredExpression.length());
						if(c == '/' || c == '.') {
							continue add; 
						}
					}
				}
			}
			filteredExpressions.add(expression);
		}
		
		ExpressionNode root = new ExpressionNode();
		
		for(int i = 0; i < filteredExpressions.size(); i++) {
			String[] paths = parse(filteredExpressions.get(i));
			
			ExpressionNode current = root;
			for(int k = 0; k < paths.length; k++) {
				String path = paths[k];

				if(current.filterType != null) {
					break;
				}

				ExpressionNode next = new ExpressionNode();
				next.index = k;
				next.parent = current;
				next.key = path;

				List<ExpressionNode> expressionNode = current.children.get(path);
				if(expressionNode == null) {
					expressionNode = new ArrayList<>();
					current.children.put(path, expressionNode);
				}
				
				expressionNode.add(next);
				
				if(current.children.containsKey(AbstractPathJsonFilter.STAR) && current.children.size() != 1) {
					throw new IllegalStateException("Support for wildcard and specific field names for the same parents not implemented");
				}
				
				current = next;
			}
			current.filterType = types[i];
		}

		PathItem rootPathItem = toPathItem(null, root.children.keySet(), 0);
		root.target = rootPathItem;
		
		List<ExpressionNode> pendingChildren = new ArrayList<>();
		pendingChildren.add(root);
		
		// hver key går inn i en ny node
		
		while(!pendingChildren.isEmpty()) {
			List<ExpressionNode> nextPendingChildren = new ArrayList<>();

			for(int i = 0; i < pendingChildren.size(); i++) {
				ExpressionNode expressionNode = pendingChildren.get(i);

				if(expressionNode.target instanceof AnyPathItem) {
					AnyPathItem item = (AnyPathItem)expressionNode.target;
					
					PathItem pathItem = toPathItem(expressionNode.target, expressionNode.children.keySet(), expressionNode.target.getIndex() + 1);
					
					expressionNode.target = pathItem;
					
					item.setNext(pathItem);
				} else if(expressionNode.target instanceof MultiPathItem) {
					MultiPathItem item = (MultiPathItem)expressionNode.target;
					
					String[] fieldNames = item.getFieldNames();
					for(int k = 0; k < fieldNames.length; k++) {

						
						
					}
				} else if(expressionNode.target instanceof SinglePathItem) {
					SinglePathItem item = (SinglePathItem)expressionNode.target;
					PathItem pathItem = toPathItem(expressionNode.target, expressionNode.children.keySet(), expressionNode.target.getIndex() + 1);
					
					expressionNode.target = pathItem;
					
					item.setNext(pathItem);
				} else {
					throw new RuntimeException();
				}
				
				
				
			}
			
			
			
			pendingChildren = nextPendingChildren;
		}
	}
	
	private PathItem toPathItem(PathItem parent, ExpressionNode node, int index) {
		if(node.children.size() == 1) {
			String key = node.children.keySet().iterator().next();
			
			boolean star = key.equals(AbstractPathJsonFilter.STAR);
			if(star) {
				AnyPathItem anyPathItem = new AnyPathItem(index, parent);
				
				return anyPathItem;
			} else {
				SinglePathItem singlePathItem = new SinglePathItem(index, key, parent);
				
				return singlePathItem;
			}
		}
		
		MultiPathItem multiPathItem = new MultiPathItem(node.children.keySet(), index, parent);
		
		
		
		return multiPathItem;
	}
	
	protected static String[] parse(String expression) {
		if(expression.startsWith("$")) {
			expression = expression.substring(1);
		}
		String[] split = expression.split("/|\\.");
		String[] elementPath = new String[split.length - 1];
		for(int k = 0; k < elementPath.length; k++) {
			elementPath[k] = AbstractPathJsonFilter.intern(split[k + 1]);
		}
		return elementPath;
	}

	
}
