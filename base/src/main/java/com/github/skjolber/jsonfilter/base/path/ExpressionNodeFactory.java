package com.github.skjolber.jsonfilter.base.path;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter;
import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter.FilterType;

public class ExpressionNodeFactory {

	// /one/two/three/five
	// /one/two/*/three1
	// /one/two/*/three1

	public ExpressionNode toExpressionNode(String[] expressions, FilterType[] types) {
		// parse first, so that the same path in slash and dot notation is treated as one.
		// if the same path is both pruned and anonymized, prune takes precedence
		Map<List<String>, FilterType> map = new LinkedHashMap<>();
		for(int i = 0; i < expressions.length; i++) {
			List<String> path = Arrays.asList(parse(expressions[i]));
			map.merge(path, types[i], FilterType::strongest);
		}
		List<List<String>> filteredPaths = filter(map.keySet());
		
		ExpressionNode root = new ExpressionNode();
		
		add:
		for(List<String> paths : filteredPaths) {
			ExpressionNode current = root;
			for(int k = 0; k < paths.size(); k++) {
				if(current.hasFilterType()) {
					// an ancestor is filtered as a whole, which covers this path
					continue add;
				}
				String path = paths.get(k);

				ExpressionNode next = current.find(path);
				if(next == null) {
					next = new ExpressionNode();
					next.setIndex(k);
					next.setPath(path);

					current.add(next);
				}

				current = next;
			}
			current.setFilterType(map.get(paths));
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
				
				List<ExpressionNode> currentChildren = current.getChildren();
				nextNodes.addAll(currentChildren);
				
				ExpressionNode starNode = null;
				for (int j = 0; j < currentChildren.size(); j++) {
					ExpressionNode childNode =currentChildren.get(j);
					if(childNode.getPath().equals("*")) {
						starNode = childNode;
						
						break;
					}
				}
				
				if(starNode != null) {
					for (int j = 0; j < currentChildren.size(); j++) {
						ExpressionNode targetNode = currentChildren.get(j);
						if(starNode != targetNode) {

							List<ExpressionNode> starNodeChildren = starNode.getChildren();
							for (int l = 0; l < starNodeChildren.size(); l++) {
								ExpressionNode childStarNode = starNodeChildren.get(l);

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
	

	private List<List<String>> filter(Collection<List<String>> paths) {
		List<List<String>> sorted = new ArrayList<>(paths);
		sorted.sort((a, b) -> Integer.compare(a.size(), b.size()));
		
		List<List<String>> filteredPaths = new ArrayList<>(sorted.size());
		
		// filter, comparing parsed paths so that slash and dot notation can be mixed
		// /one/two/three
		// $.one.two.three.four <--- removed
		// /one/two/threes/fours <--- kept
		
		add:
		for(List<String> path : sorted) {
			for (List<String> filteredPath : filteredPaths) {
				if(filteredPath.size() < path.size() && path.subList(0, filteredPath.size()).equals(filteredPath)) {
					continue add;
				}
			}
			filteredPaths.add(path);
		}
		return filteredPaths;
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
