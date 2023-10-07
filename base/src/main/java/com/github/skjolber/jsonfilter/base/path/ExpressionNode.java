package com.github.skjolber.jsonfilter.base.path;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter.FilterType;

public class ExpressionNode {
	String path;
	int index;
	FilterType filterType;
	List<ExpressionNode> children = new ArrayList<>();
	
	public ExpressionNode find(String path) {
		for (ExpressionNode expressionNode : children) {
			if(expressionNode.path.equals(path)) {
				return expressionNode;
			}
		}
		return null;
	}

	public void append(ExpressionNode node) {
		if(filterType == null) {
			ExpressionNode preexisting = find(node.path);
			if(preexisting == null) {
				children.add(node);
			} else {
				// move up one level
				for (ExpressionNode preexistingChild : preexisting.children) {
					for (ExpressionNode expressionNode : node.children) {
						preexistingChild.append(expressionNode);
					}
				}
			}
		}
	}
	
	public String print() {
		StringBuilder builder = new StringBuilder();
		
		print(builder);
		
		return builder.toString().trim();
	}
	
	public void print(StringBuilder builder) {
		for(int i = 0; i < index; i++) {
			builder.append(" ");
		}
		if(path != null) {
			builder.append(path);
		}
		if(filterType != null) {
			builder.append(" (");
			builder.append(filterType.toString());
			builder.append(")");
		}
		builder.append("\n");
		
		if(children != null) {
			for (ExpressionNode expressionNode : children) {
				expressionNode.print(builder);
			}
		}
	}
}