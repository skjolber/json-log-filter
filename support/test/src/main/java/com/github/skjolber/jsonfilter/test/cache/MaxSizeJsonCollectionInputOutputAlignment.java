package com.github.skjolber.jsonfilter.test.cache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.github.skjolber.jsonfilter.test.truth.JsonFilters;

/**
 * 
 * Align max size inputs and outputs, so even if multiple inputs go into the same outputs.
 * 
 */

public class MaxSizeJsonCollectionInputOutputAlignment {

	public static MaxSizeJsonCollectionInputOutputAlignment create(List<MaxSizeJsonCollection> inputs, List<MaxSizeJsonCollection> outputs, JsonFilters filter) {
		
		Map<String, MaxSizeJsonCollection> outputMap = new HashMap<>();
		
		for (MaxSizeJsonCollection maxSizeJsonCollection : outputs) {
			outputMap.put(maxSizeJsonCollection.getContentAsString(), maxSizeJsonCollection);
		}

		List<MaxSizeJsonCollection> outputResults = new ArrayList<>(inputs.size());

		for (MaxSizeJsonCollection input : inputs) {
			String inputString = input.getContentAsString();
			String outputString = filter.getCharacters().process(inputString);
			
			MaxSizeJsonCollection maxSizeJsonCollection = outputMap.get(outputString);
			if(maxSizeJsonCollection == null) {
				
				System.out.println("Input\n" + inputString);
				System.out.println("Wanted from " + filter.getCharacters() + "\n" + outputString);
				System.out.println("Have");
				for (Entry<String, MaxSizeJsonCollection> entry : outputMap.entrySet()) {
					System.out.println(entry.getKey());
				}
				throw new RuntimeException();
			}
			outputResults.add(maxSizeJsonCollection);
		}

		return new MaxSizeJsonCollectionInputOutputAlignment(inputs, outputResults);
	}
	
	private final List<MaxSizeJsonCollection> inputs;
	private final List<MaxSizeJsonCollection> outputs;

	public MaxSizeJsonCollectionInputOutputAlignment(List<MaxSizeJsonCollection> inputs, List<MaxSizeJsonCollection> outputs) {
		this.inputs = inputs;
		this.outputs = outputs;
	}
	
	public List<MaxSizeJsonCollection> getInputs() {
		return inputs;
	}
	
	public List<MaxSizeJsonCollection> getOutputs() {
		return outputs;
	}
	
	public int size() {
		return inputs.size();
	}
	
	public MaxSizeJsonCollection getInput(int index) {
		return inputs.get(index);
	}

	public MaxSizeJsonCollection getOutput(int index) {
		return outputs.get(index);
	}

}
