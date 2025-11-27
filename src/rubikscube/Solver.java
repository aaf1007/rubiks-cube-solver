package rubikscube;

import java.io.File;
import java.io.FileWriter;
import java.lang.invoke.MethodHandles;

public class Solver {

	// 12 Possible Moves - Uppercase = CW, Lowercase = CCW
	private static final char[] MOVES = {
		'F', 'f', 'B', 'b', 'L', 'l', 'R', 'r', 'U', 'u', 'D', 'd'
	};

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

	// Solve Function
	public static String solve(RubiksCube cube) {
		// Initialize Heuristic
        ManhattanDistance heuristic = new ManhattanDistance();

		// H - Initial Threshold
        int threshold = heuristic.calculate(cube);
        
        while (true) {
			// Start Search
            SearchResult result = search(cube, 0, threshold, heuristic, -1);
            
            if (result.solved) {
                return result.path.trim();
            }
            
            if (result.nextThreshold == Integer.MAX_VALUE) {
                return "Unsolvable"; 
            }
            
			// Increase Depth limit to lowest cost we saw that exceeded the current limit
            threshold = result.nextThreshold;
        }
    }

	// Recursive IDA* Search
    private static SearchResult search(RubiksCube cube, int g, int bound, 
                                     ManhattanDistance h, int prevMoveIndex) {
        
		// F = G + H
        int f = g + h.calculate(cube);
		
		// Prune if cost exceeds limit
        if (f > bound) 
			return new SearchResult(false, null, f);

		// Solved
        if (cube.isSolved()) 
			return new SearchResult(true, "", 0);

        int min = Integer.MAX_VALUE;

		// Try all 12 Moves
        for (int i = 0; i < 12; i++) {
            // Optimization: Don't undo the move we just made (e.g. F then f)
            if (prevMoveIndex != -1 && isInverse(i, prevMoveIndex)) continue; 
            
            char moveChar = MOVES[i];

            cube.applyMove(moveChar);

            SearchResult res = search(cube, g + 1, bound, h, i);

            if (res.solved) {
                return new SearchResult(true, formatMove(moveChar) + " " + res.path, 0);
            }

            if (res.nextThreshold < min) min = res.nextThreshold;

            // Backtrack
            cube.applyMove(getInverseChar(moveChar)); 
        }

        return new SearchResult(false, null, min);
    }

    private static boolean isInverse(int i1, int i2) {
        return (i1 / 2 == i2 / 2) && (i1 != i2);
    }
    
    private static char getInverseChar(char c) {
        if (Character.isLowerCase(c)) return Character.toUpperCase(c);
        return Character.toLowerCase(c);
    }
    
    private static String formatMove(char c) {
        if (Character.isLowerCase(c)) return Character.toUpperCase(c) + "'";
        return String.valueOf(c);
    }

	// Data Holder
    static class SearchResult {
        boolean solved;
        String path;
        int nextThreshold;

        public SearchResult(boolean s, String p, int n) {
            this.solved = s; 
            this.path = p; 
            this.nextThreshold = n;
        }
    }

}
