package test;

public class ArrayOperations {

	// ArrayOperations() {
	// 	super("test");
	// }

	public static void test() {
		return;
	}
	
	public static void main(String[] args) {

        System.out.println("Main Thread");
        Thread t = new Thread()
        {
        	@Override
            public void run()
            {
                System.out.println("Child Thread");

		        Thread t = new Thread()
		        {
		            public void run()
		            {
		                System.out.println("Child-Child Thread");
		            }
		        };
		        t.start();
            }
        };
        t.start();

        MyThread t2 = new MyThread()
        {
        	@Override
            public void run()
            {
                System.out.println("Child MyThread");
            }
        };
        t2.start();


        /************************/

		// initiate constants and user choice variable
		final int arraySize = 10;
		final int min = 0;
		final int max = 100;
		int function;

		// fake input
		// int input = 1;

		// make random array
		int[] array = makeRandomArray(arraySize,min,max);		

		// welcome
		System.out.println("\n\nWelcome to the Array Operations Program!\n\nAn array of size " + arraySize + " was initiated with random numbers ranging from " + min + " to " + max + ".");
		
		// start interface loop
		do {
			System.out.print("\n\nPlease choose from the following list of options:\n\n1. Print the maximum number in the array.\n2. Print the minimum number in the array.\n3. Print the sum of the array\n4. Print the average of the array\n5. Reverse the array.\n6. Print the occurences for each number in the array.\n7. Exit the Program.\n\nEnter a number: ");
			function = GetInput.getInt(7);
			// function = input++;

			switch(function) {
				case 1:
					System.out.println("\nThe maximum is: " + findMax(array, min));
					break;
				case 2:
					System.out.println("\nThe minimum is: " + findMin(array, min));
					break;
				case 3:
					System.out.println("\nThe sum is: " + findSum(array));
					break;
				case 4:
					System.out.println("\nThe average is: " + findAvg(array));
					break;
				case 5:
					System.out.println("\n\n\nARRAY BEFORE REVERSE:");
					printArray(array);
					array = reverseArray(array);
					System.out.println("\n\n\nARRAY AFTER REVERSE:");
					printArray(array);
					break;
				case 6:
					int[] occur = makeOccurArray(array, min, max);
					printOccurArray(occur, min, max);
					break;
					
				case 7:
					System.out.println("\nGoodbye!\n");
					break;
			}

		} while(function != 7); // && input < 8

	}

////////////////////////////////////////////////////////////////////////////////
	// operation methods
////////////////////////////////////////////////////////////////////////////////


	private static int findMax(int[] array, int min) {
		int max = min - 1;

		for (int i = 0; i <= array.length-1; i++) {
			if (array[i] > max) {
				max = array[i];
			}
		}

		return max;
	}

	private static int findMin(int[] array, int max) {
		int min = max + 1;

		for (int i = 0; i <= array.length-1; i++) {
			if (array[i] < min) {
				min = array[i];
			}
		}

		return min;
	}

	private static int findSum(int[] array) {
		int sum = 0;

		for (int i = 0; i <= array.length-1; i++) {
			sum += array[i];
		}

		return sum;
	}

	private static float findAvg(int[] array) {
		int sum = findSum(array);
		float avg = sum/(float)array.length;
		return avg;
	}

	private static int[] reverseArray(int[] array) {

		int[] reversed = new int[array.length];

		for (int i = 0; i <= array.length-1; i++) {
			reversed[i] = array[array.length - i - 1];
		}

		return reversed;
	}

	final static int[] makeOccurArray(int[] array, int min, int max) {
		int[] occurArray = new int[(max-min)+1];

		for (int c = min; c <= max; c++) {
			int counter = 0;
			for (int i = 0; i <= array.length-1; i++) {
				if (array[i] == c) {
					counter++;
				}
			}
			occurArray[c-min] = counter;
		}

		return occurArray;
	}

////////////////////////////////////////////////////////////////////////////////
	// helper methods
////////////////////////////////////////////////////////////////////////////////

	protected static int[] makeRandomArray(int size, int min, int max) {
		int[] array = new int[size];

		for (int i = 0; i <= size-1; i++) {
			array[i] = RandomNumber.randomInt(min,max);
		}

		return array;
	}

	private static void printArray(int[] array) {
		System.out.println();
		for (int i = 0; i <= array.length-1; i++) {
			System.out.print(array[i] + "\t");
		}
	}

	private static void printOccurArray(int[] array, int min, int max) {
		System.out.println("\nNUM | OCCUR");
		for (int i = 0; i <= array.length-1; i++) {
			System.out.print(min+i + ": " + array[i] + "\n");
		}
	}
}