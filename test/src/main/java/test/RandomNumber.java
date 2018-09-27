package test;
// -----------------------------------------------------------------------
// Author: Brian Bushree
//
// Usage: Any time a random number needs to be generated.
// -----------------------------------------------------------------------
import java.util.Random;

public class RandomNumber {
	
	// -----------------------------------------------------------------------
	// Parameters: int min - minimum integer value to allow (inclusive)
	//			   int max - maximum integer value to allow (inclusive)
	//
	// Returns: an integer between min and max inclusively
	// -----------------------------------------------------------------------
	public static int randomInt(int min, int max) {
		Random rand = new Random();
		return rand.nextInt((max - min) + 1) + min;
	}
}