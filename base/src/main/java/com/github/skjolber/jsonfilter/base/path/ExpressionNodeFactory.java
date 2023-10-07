package com.github.skjolber.jsonfilter.base.path;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter;
import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter.FilterType;

public class ExpressionNodeFactory {

	// /one/two/three/five
	// /one/two/*/three1
	// /one/two/*/three1

	public ExpressionNode toExpressionNode(String[] expressions, FilterType[] types) {
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

				current = next;
			}
			current.filterType = map.get(filteredExpressions.get(i));
		}
		
		// /one/two/three/five
		// /one/two/*/three1
		//
		// is logically the same as
		//
		// /one/two/three/five
		// /one/two/three/three1
		// /one/two/*/three1
		//
		// so add star children to non-star children
		// 
		
		List<ExpressionNode> nodes = new ArrayList<>();
		nodes.add(root);
		while(!nodes.isEmpty()) {
			List<ExpressionNode> nextNodes = new ArrayList<>();
			for(int i = 0; i < nodes.size(); i++) {
				
				ExpressionNode current = nodes.get(i);
				
				if(current.children != null) {
					nextNodes.addAll(current.children);
				}
				
				ExpressionNode starNode = null;
				for (int j = 0; j < current.children.size(); j++) {
					ExpressionNode childNode = current.children.get(j);
					if(childNode.path.equals("*")) {
						starNode = childNode;
						
						break;
					}
				}
				
				if(starNode != null) {
					
					for (int j = 0; j < current.children.size(); j++) {
						ExpressionNode targetNode = current.children.get(j);
						if(starNode != targetNode) {

							for (int l = 0; l < starNode.children.size(); l++) {
								ExpressionNode childStarNode = starNode.children.get(l);

								targetNode.append(childStarNode);
							}
						}
					}
				}
			}
			nodes = nextNodes;
		}
		return root;
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

	public ExpressionNode toExpressionNode(List<String> pathsList, List<FilterType> typesList) {
		return toExpressionNode(pathsList.toArray(new String[pathsList.size()]), typesList.toArray(new FilterType[typesList.size()]));
	}
}
