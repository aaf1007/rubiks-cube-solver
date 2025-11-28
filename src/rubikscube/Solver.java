package rubikscube;

import java.io.File;
import java.io.FileWriter;
import java.lang.invoke.MethodHandles;

public class Solver {

	public static void main(String[] args) {
		//		System.out.println("number of arguments: " + args.length);
		//		for (int i = 0; i < args.length; i++) {
		//			System.out.println(args[i]);
		//		}
		
		if (args.length < 2) {
			System.out.println("File names are not specified");
			System.out.println("usage: java " + MethodHandles.lookup().lookupClass().getName() + " input_file output_file");
			return;
		}
		
		
		// TODO
		//File input = new File(args[0]);
		// solve...
		//File output = new File(args[1]);

		try {
			// Start timer
            long startTime = System.currentTimeMillis();

            String inputFileName = args[0];
            String outputFileName = args[1];

            // 1. Load the Scramble from the input file
            RubiksCube cube = new RubiksCube(inputFileName);
            
            // 2. Run the Solver
            System.out.println("Solving puzzle from: " + inputFileName);
            String solution = solve(cube);
            
			try ( // 3. Write solution to the output file
					FileWriter writer = new FileWriter(new File(outputFileName))) {
				writer.write(solution);
			}
            // Stop Timer
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            System.out.println("Solution found: " + solution);
            System.out.println("Written to: " + outputFileName);
			System.out.println("Total Execution Time: " + duration + " ms (" + (duration/1000.0) + " seconds)");

        } catch (Exception e) {
            e.printStackTrace();
        }

	}

	/**
	 * Solve using Kociemba's two-phase algorithm.
	 * Returns solution in format "U R F' D2 ..." with space-separated moves.
	 */
	public static String solve(RubiksCube cube) {
		String solution = TwoPhaseSolver.solve(cube);

		// Convert from Kociemba format (UURRDDF) to spaced format (U U R R D D F)
		if (solution == null || solution.isEmpty() || solution.startsWith("ERROR")) {
			return solution;
		}

		// Parse and format the solution
		StringBuilder formatted = new StringBuilder();
		for (int i = 0; i < solution.length(); i++) {
			char c = solution.charAt(i);
			if (formatted.length() > 0) {
				formatted.append(" ");
			}
			formatted.append(c);
		}

		return formatted.toString();
	}

}
