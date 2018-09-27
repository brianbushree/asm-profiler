package test;
// -----------------------------------------------------------------------
// Author: Brian Bushree
//
// Usage: Any time input is needed from the user via Scanner, use these methods.
// -----------------------------------------------------------------------
import java.util.Scanner;
import java.io.*;

public class GetInput {

	// -----------------------------------------------------------------------
	// Descrption: Asks user for integer.
	//
	// Parameters: none
	//
	// Returns: an integer
	// -----------------------------------------------------------------------
	public static int getInt() {
		String error = "You failed to enter an integer...\nPlease try again: ";

		Scanner scan = new Scanner(System.in);
		String num = scan.nextLine();
		int numF = 0;

		while (true) {
			try {
				numF = Integer.parseInt(num);
				return numF;
			} catch (NumberFormatException e) {
				System.out.print(error);
				num = scan.nextLine();
			}
		}
	}

	// -----------------------------------------------------------------------
	// Descrption: Asks user for integer between 1 and max
	//
	// Parameters: int max - maximum integer value to allow (inclusive)
	//
	// Returns: an integer between 1 and max
	// -----------------------------------------------------------------------
	public static int getInt(int max) {
		String error = "You failed to enter an integer between 1 and " + max + "...\nPlease try again: ";

		Scanner scan = new Scanner(System.in);
		String num = scan.nextLine();
		int numI = 0;

		while (true) {
			try {
				numI = Integer.parseInt(num);

			} catch (NumberFormatException e) {
				System.out.print(error);
				num = scan.nextLine();
			}

			if (numI > 0 && numI <= max) {
				return numI;
			} else {
				System.out.print(error);
				num = scan.nextLine();
			}
		}
	}

	// -----------------------------------------------------------------------
	// Descrption: Asks user for integer between min and max inclusive
	//
	// Parameters: int max - maximum integer value to allow (inclusive)
	//			   int min - minimum integer value to allow (inclusive)
	//
	// Returns: an integer between min and max (inclusive)
	// -----------------------------------------------------------------------
	public static int getInt(int min, int max) {
		String error = "You failed to enter an integer between " + min + " and " + max + "...\nPlease try again: ";

		Scanner scan = new Scanner(System.in);
		String num = scan.nextLine();
		int numI = 0;

		while (true) {
			try {
				numI = Integer.parseInt(num);

			} catch (NumberFormatException e) {
				System.out.print(error);
				num = scan.nextLine();
			}

			if (numI >= min && numI <= max) {
				return numI;
			} else {
				System.out.print(error);
				num = scan.nextLine();
			}
		}
	}

	// -----------------------------------------------------------------------
	// Descrption: Asks user for float.
	//
	// Parameters: none
	//
	// Returns: a float
	// -----------------------------------------------------------------------
	public static Float getFloat() {
		String error = "You failed to enter a number...\nPlease try again: ";

		Scanner scan = new Scanner(System.in);
		String num = scan.nextLine();
		Float numF = 0f;

		while (true) {
			try {
				numF = Float.parseFloat(num);
				return numF;
			} catch (NumberFormatException e) {
				System.err.print(error);
				num = scan.nextLine();
			}
		}

	}

	// -----------------------------------------------------------------------
	// Descrption: Asks user for letter in charString.
	//
	// Parameters: String charString - contains acceptable letter values
	//
	// Returns: a String containing a letter in charString
	// -----------------------------------------------------------------------
	public static String getLetter(String charString) {
		String error = "You failed to enter one of the following options: '" + charString + "'\nPlease try again: ";
		Scanner scan = new Scanner(System.in);
		String input = scan.nextLine();
		String test = "";

		while (true) {
			// check for charString
			for (int i = 0; i <= charString.length() - 1; i++) {
				test = "" + charString.charAt(i);
				if (test.equalsIgnoreCase(input)) {
					return input.toLowerCase();
				}
			}
			System.err.print(error);
			input = scan.nextLine();
		}

	}

	// -----------------------------------------------------------------------
	// Descrption: Displays acceptable csvFormat and asks user to choose from all .txt and .csv files in current directory
	//
	// Parameters: String csvFormat - contains a csv pattern (ex. 'fName, lName, bday')
	//
	// Returns: the selected File
	// -----------------------------------------------------------------------
	public static File getCSVFile(String csvFormat) {
		System.out.println("\nPlease choose from one of the following text files:\n!!NOTE!! MUST BE IN THE FOLLOWING CSV FORMAT: " + csvFormat + "\n");

    	File curDir = new File("../");

    	// filter all non txt and csvs
    	FilenameFilter filter = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				String lowercaseName = name.toLowerCase();
				if (lowercaseName.endsWith(".txt") || lowercaseName.endsWith(".csv")) {
					return true;
				} else {
					return false;
				}
			}
		};

		FilenameFilter filter2 = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				String lowercaseName = name.toLowerCase();
				if (lowercaseName.endsWith(".txt") || lowercaseName.endsWith(".csv")) {
					return true;
				} else {
					return false;
				}
			}
		};

		// list files
		File[] files = curDir.listFiles(filter);
		for (int i = 1; i <= files.length; i++) {
			File file = files[i-1];
			if (!file.isDirectory()) {
				System.out.println(i + "\t" + file.getPath());
			} 
		}

		System.out.print("\nEnter choice: ");
		int choice = getInt(files.length);

		return files[choice-1];
    }

    // -----------------------------------------------------------------------
	// Descrption: Asks user for a String.
	//
	// Parameters: none
	//
	// Returns: a String
	// -----------------------------------------------------------------------
	public static String getStr() {
		String error = "\nYou failed to enter a String...\nPlease try again: ";

		Scanner scan = new Scanner(System.in);
		String input = scan.nextLine();
		while (input.length() == 0) {
			System.out.print(error);
			input = scan.nextLine();
		}
		return input;
	}

}