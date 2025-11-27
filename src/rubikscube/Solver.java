package rubikscube;

import java.io.File;
import java.io.FileWriter;
import java.lang.invoke.MethodHandles;

public class Solver {

	// 12 Possible Moves - Uppercase = CW, Lowercase = CCW
	private static final char[] MOVES = {
		'F', 'f', 'B', 'b', 'L', 'l', 'R', 'r', 'U', 'u', 'D', 'd'
	};

    // Precomputed inverse index: MOVES[i]'s inverse is MOVES[INVERSE_INDEX[i]]
    private static final int[] INVERSE_INDEX = {1, 0, 3, 2, 5, 4, 7, 6, 9, 8, 11, 10};

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
            int result = search(cube, 0, threshold, heuristic, -1, -1, threshold);
            
            if (result == -1) {
                // Build Solution string
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < solutionDepth; i++) {
                    if (i > 0)
                        sb.append(" ");
                    sb.append(formatMove(solutionMoves[i]));
                }
                return sb.toString();
            }
            
            if (result == Integer.MAX_VALUE) {
                return "Unsolvable"; 
            }
            
			// Increase Depth limit to lowest cost we saw that exceeded the current limit
            threshold = result;
        }
    }

    // Use char array instead of String concatenation
    private static final char[] solutionMoves = new char[20];
    private static int solutionDepth;

	// Recursive IDA* Search
    // Returns: -1 = solved, Integer.MAX_VALUE = no solution, else = nextThreshold
    private static int search(RubiksCube cube, int g, int bound, 
                                     ManhattanDistance h, int prevMoveIndex, int prevPrevMoveIndex, int hValue) {
        
		// F = G + H
        int f = g + hValue;
		
		// Prune if cost exceeds limit
        if (f > bound) 
			return f;

		// Solved
        if (cube.isSolved()) {
            solutionDepth = g;
            return -1;
        }

        int min = Integer.MAX_VALUE;

		// Try all 12 Moves
        for (int i = 0; i < 12; i++) {
            // Optimization: Don't undo the move we just made (e.g. F then f)
            if (isRedundant(i, prevMoveIndex, prevPrevMoveIndex)) 
                continue; 
            
            char moveChar = MOVES[i];

            // Apply move
            cube.applyMove(moveChar);

            // Calculate new H value for recursion
            int newHValue = h.calculate(cube);
            
            // Recursive call
            int res = search(cube, g + 1, bound, h, i, prevMoveIndex, newHValue);

            if (res == -1) {
                solutionMoves[g] = moveChar;
                cube.applyMove(MOVES[INVERSE_INDEX[i]]);
                return -1;
            }

            if (res < min) 
                min = res;

            // Backtrack
            cube.applyMove(MOVES[INVERSE_INDEX[i]]); 
        }

        return min;
    }

    /**
     * Checks if the current move is redundant given the previous move.
     * Prunes:
     * 1. Immediate Inverse: F followed by f
     * 2. Commutative Duplicates: B followed by F (Enforces F before B)
     */
    private static boolean isRedundant(int curr, int prev, int prevPrev) {
        if (prev == -1) return false;

        int currFace = curr / 2;
        int prevFace = prev / 2;

        // 1. Immediate Inverse (F then f)
        if (currFace == prevFace && curr != prev) 
            return true;

        // 2. Commutative: opposite faces (0,1), (2,3), (4,5) -> enforce lower first
        // Pairs: F/B (0,1), L/R (2,3), U/D (4,5)
        int currPair = currFace / 2;
        int prevPair = prevFace / 2;
        if (currPair == prevPair && currFace < prevFace)
            return true;

        // 3. Three consecutive same-face moves
        if (prevPrev != -1) {
            int prevPrevFace = prevPrev / 2;
            if (currFace == prevFace && prevFace == prevPrevFace)
                return true;
        }

        return false;
    }
    
    private static String formatMove(char c) {
        if (Character.isLowerCase(c)) return Character.toUpperCase(c) + "'";
        return String.valueOf(c);
    }

}
