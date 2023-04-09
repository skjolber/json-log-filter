package com.github.skjolber.jsonfilter.base.path;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter;
import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter.FilterType;

public class PathItemFactory {

	private static class ExpressionNode {
		private String path;
		private int index;
		private FilterType filterType;
		private List<ExpressionNode> children = new ArrayList<>();
		
		public ExpressionNode find(String path) {
			for (ExpressionNode expressionNode : children) {
				if(expressionNode.path.equals(path)) {
					return expressionNode;
				}
			}
			return null;
		}
		
		public List<String> getKeys() {
			Set<String> keys = new HashSet<>();
			for (ExpressionNode child : children) {
				keys.add(child.path);
			}
			return new ArrayList<>(keys);
		}

		@Override
		public String toString() {
			return "ExpressionNode [path=" + path + ", index=" + index + ", filterType=" + filterType + ", children=" + getKeys() +"]";
		}
	}
	
	// /one/two/three/five
	// /one/two/*/three1
	// /one/two/*/three1
	
	public PathItem create(String[] expressions, FilterType[] types) {
		Map<String, FilterType> map = new HashMap<>();
		for(int i = 0; i < expressions.length; i++) {
			map.put(expressions[i], types[i]);
		}
		List<String> filteredExpressions = filter(expressions);
		
		ExpressionNode root = new ExpressionNode();
		
		for(int i = 0; i < filteredExpressions.size(); i++) {
			String[] paths = parse(filteredExpressions.get(i));
			
			ExpressionNode current = root;
			for(int k = 0; k < paths.length; k++) {
				String path = paths[k];

				if(current.filterType != null) {
					break;
				}

				ExpressionNode next = current.find(path);
				if(next == null) {
					next = new ExpressionNode();
					next.index = k;
					next.path = path;

					current.children.add(next);
				}

				/*
				if(current.children.containsKey(AbstractPathJsonFilter.STAR) && current.children.size() != 1) {
					throw new IllegalStateException("Support for wildcard and specific field names for the same parents not implemented");
				}
				*/
				
				current = next;
			}
			current.filterType = map.get(filteredExpressions.get(i));
		}
		
		// index 1 ... n so that easier to use in filters
		return create(null, root, 1);
	}
	
	public PathItem create(PathItem parent, ExpressionNode node, int level) {
		if(node.filterType != null) {
			return new EndPathItem(level, parent, node.filterType);
		}
		if(node.children.size() == 1) {
			ExpressionNode childNode = node.children.get(0);
			
			if(childNode.path.equals(AbstractPathJsonFilter.STAR)) {
				AnyPathItem anyPathItem = new AnyPathItem(level, parent);
				
				PathItem childPathItem = create(anyPathItem, childNode, level + 1);
				anyPathItem.setNext(childPathItem);
				
				return anyPathItem;
			}
			SinglePathItem singlePathItem = new SinglePathItem(level, childNode.path, parent);
			
			PathItem childPathItem = create(singlePathItem, childNode, level + 1);
			singlePathItem.setNext(childPathItem);
			
			return singlePathItem;
		}
		List<String> keys = new ArrayList<>();
		for(ExpressionNode child : node.children) {
			keys.add(child.path);
		}

		int anyIndex = keys.indexOf(AbstractPathJsonFilter.STAR);
		if(anyIndex != -1) {
			keys.remove(anyIndex);
			
			ExpressionNode anyNode = node.children.remove(anyIndex);
			
			AnyMultiPathItem multiPathItem = new AnyMultiPathItem(keys, level, parent);

			for(int k = 0; k < node.children.size(); k++) {
				ExpressionNode childNode = node.children.get(k);
				
				PathItem childPathItem = create(multiPathItem, childNode, level + 1);
				
				multiPathItem.setNext(childPathItem, k);
			}
			
			multiPathItem.setAny(create(parent, anyNode, level + 1));
			
			return multiPathItem;
			
		}
		
		MultiPathItem multiPathItem = new MultiPathItem(keys, level, parent);
		for(int k = 0; k < node.children.size(); k++) {
			ExpressionNode childNode = node.children.get(k);
			
			PathItem childPathItem = create(multiPathItem, childNode, level + 1);
			
			multiPathItem.setNext(childPathItem, k);
		}
		
		return multiPathItem;
	}

	private List<String> filter(String[] expressions) {
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
		return filteredExpressions;
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

	public PathItem create(List<String> pathsList, List<FilterType> typesList) {
		return create(pathsList.toArray(new String[pathsList.size()]), typesList.toArray(new FilterType[typesList.size()]));
	}
	
}
