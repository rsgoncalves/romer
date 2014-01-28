package uk.ac.manchester.cs.romer.utils;

import java.util.Random;

public class Randomizer {
	
	public static void main(String[] args) {
		double minValue = 12034;
		double maxValue = 12478;
	
		for(int i = 0; i < 384; i++) {
			double r = minValue + ((new Random()).nextDouble() * (maxValue - minValue));
			System.out.println(r);
		}
	}
}
