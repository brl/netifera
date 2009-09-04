package com.netifera.platform.net.wordlists.examples;

import java.util.ArrayList;
import java.util.List;

import com.netifera.platform.api.iterables.FiniteIterable;
import com.netifera.platform.api.iterables.ListIndexedIterable;
import com.netifera.platform.net.wordlists.IWordList;

public class Top100LastNames implements IWordList {

	public String getName() {
		return "Top 100 Last Names in USA";
	}

	public FiniteIterable<String> getWords() {
		List<String> words = new ArrayList<String>();
		for (String word: "Johnson Williams Jones Brown Davis Miller Wilson Moore Taylor Anderson Thomas Jackson White Harris Martin Thompson Garcia Martinez Robinson Clark Rodriguez Lewis Lee Walker Hall Allen Young Hernandez King Wright Lopez Hill Scott Green Adams Baker Gonzalez Nelson Carter Mitchell Perez Roberts Turner Phillips Campbell Parker Evans Edwards Collins Stewart Sanchez Morris Rogers Reed Cook Morgan Bell Murphy Bailey Rivera Cooper Richardson Cox Howard Ward Torres Peterson Gray Ramirez James Watson Brooks Kelly Sanders Price Bennett Wood Barnes Ross Henderson Coleman Jenkins Perry Powell Long Patterson Hughes Flores Washington Butler Simmons Foster Gonzales Bryant Alexander Russell Griffin Diaz Hayes".toLowerCase().split(" "))
			words.add(word);
		return new ListIndexedIterable<String>(words);
	}

}
