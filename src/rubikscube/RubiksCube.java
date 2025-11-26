package rubikscube;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Scanner;

public class RubiksCube {
    /**
     * default constructor
     * Creates a Rubik's Cube in an initial state:
     *    OOO
     *    OOO
     *    OOO
     * GGGWWWBBBYYY
     * GGGWWWBBBYYY
     * GGGWWWBBBYYY
     *    RRR
     *    RRR
     *    RRR
     */

    private char[][] cube; // 2D array of the cube
    private static final String SOLVED_STATE =
            "   OOO\n" +
            "   OOO\n" +
            "   OOO\n" +
            "GGGWWWBBBYYY\n" +
            "GGGWWWBBBYYY\n" +
            "GGGWWWBBBYYY\n" +
            "   RRR\n" +
            "   RRR\n" +
            "   RRR\n";

    public RubiksCube() {
        // TODO implement me
        initializeFromString(SOLVED_STATE);
    }

    /**
     * @param fileName
     * @throws IOException
     * @throws IncorrectFormatException
     * Creates a Rubik's Cube from the description in fileName
     */
    public RubiksCube(String fileName) throws IOException, IncorrectFormatException {
        // TODO implement me
        File file = new File(fileName);
        Scanner reader = new Scanner(file);
        StringBuilder builder = new StringBuilder();
        
        int lineCount = 0;
        while (reader.hasNextLine()) {
            String line = reader.nextLine();
            builder.append(line).append("\n");
            lineCount++;
        }
        reader.close();
        
        // Validate format
        if (lineCount != 9) {
            throw new IncorrectFormatException("File must contain exactly 9 lines");
        }
        
        String content = builder.toString();
        validateFormat(content);
        initializeFromString(content);
    }

    /**
     *  Copy Constructor
     * @param other
     */
    public RubiksCube(RubiksCube other) {
        this.cube = new char[9][12];
        for (int i = 0; i < 9; i++) {
            System.arraycopy(other.cube[i], 0, this.cube[i], 0, 12);
        }
    }

    /**
     * 
     * @param str - RubiksCube represented as a String
     * @return RubiksCube object
     */
    public RubiksCube stringToRubiksCube(String str) {
        RubiksCube ret = new RubiksCube();
        this.initializeFromString(str);
        return ret;
    }

    /**
     *  Validates format of file
     */
    private void validateFormat(String content) throws IncorrectFormatException {
        String[] lines = content.split("\n");
        if (lines.length != 9) {
            throw new IncorrectFormatException("Must have exactly 9 lines");
        }
        
        // Check first 3 and last 3 lines (should start with 3 spaces)
        for (int i = 0; i < 3; i++) {
            if (lines[i].length() != 6 || !lines[i].startsWith("   ")) {
                throw new IncorrectFormatException("Lines 0-2 must start with 3 spaces and have 3 colors");
            }
            if (lines[i + 6].length() != 6 || !lines[i + 6].startsWith("   ")) {
                throw new IncorrectFormatException("Lines 6-8 must start with 3 spaces and have 3 colors");
            }
        }
        
        // Check middle 3 lines (should have 12 characters)
        for (int i = 3; i < 6; i++) {
            if (lines[i].length() != 12) {
                throw new IncorrectFormatException("Lines 3-5 must have exactly 12 characters");
            }
        }
        
        // Check valid colors
        String validColors = "WYRGBO";
        for (String line : lines) {
            for (char c : line.toCharArray()) {
                if (c != ' ' && validColors.indexOf(c) == -1) {
                    throw new IncorrectFormatException("Invalid color: " + c);
                }
            }
        }
    }

    /**
     *  Initializes cube from string
     */
    private void initializeFromString(String str) {
        cube = new char[9][12];
        String[] lines = str.split("\n");
        for (int i = 0; i < 9; i++) {
            String line = lines[i];
            for (int j = 0; j < line.length(); j++) {
                cube[i][j] = line.charAt(j);
            }
            // Fill remaining with spaces if line is shorter
            for (int j = line.length(); j < 12; j++) {
                cube[i][j] = ' ';
            }
        }
    }

    /**
     * Rotates a 3x3 face clockwise
     */
    private void rotateFace(int startRow, int startCol) {
        char[][] temp = new char[3][3];
        
        // Copy face to temp
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                temp[i][j] = cube[startRow + i][startCol + j];
            }
        }
        
        // Rotate clockwise: [i][j] -> [j][2-i]
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                cube[startRow + j][startCol + 2 - i] = temp[i][j];
            }
        }
    }

    /**
     * @param moves - String of moves
     * Applies the sequence of moves on the Rubik's Cube
     */
    public void applyMoves(String moves) {
        // TODO implement me
        // For all loop to call applyMove(char)

        String validMoves = "FBRLUD";
        char[] move = moves.toCharArray();

        for (int i = 0; i < move.length; i++) {
            if (i+1 < move.length && move[i+1] == '\'') {
                // Counter-clockwise rotation so call it 3 times
                for (int j = 0; j < 3; j++)
                    applyMove(move[i]);

                i++; // Skip \'
                continue;
            }

            if (move[i] == '\'')
                continue;
            
            applyMove(move[i]);
        }
    }

    /**
     * For applying one moves
     * @param char of single move
     */
     private void applyMove(char move) {
        switch (move) {
            case 'F':
                rotateFace(3, 3); // White face
                // Save edge pieces that will be moved
                char[] tempF = new char[3];
                // Save bottom row of Up face
                tempF[0] = cube[2][3];
                tempF[1] = cube[2][4];
                tempF[2] = cube[2][5];
                
                // Up bottom row <- Left right column
                cube[2][3] = cube[5][2];
                cube[2][4] = cube[4][2];
                cube[2][5] = cube[3][2];
                
                // Left right column <- Down top row
                cube[3][2] = cube[6][3];
                cube[4][2] = cube[6][4];
                cube[5][2] = cube[6][5];
                
                // Down top row <- Right left column
                cube[6][3] = cube[5][6];
                cube[6][4] = cube[4][6];
                cube[6][5] = cube[3][6];
                
                // Right left column <- saved Up bottom row
                cube[3][6] = tempF[0];
                cube[4][6] = tempF[1];
                cube[5][6] = tempF[2];
                break;
                
            case 'B':
                rotateFace(3, 9); // Yellow face
                char[] tempB = new char[3];
                // Save top row of Up face
                tempB[0] = cube[0][3];
                tempB[1] = cube[0][4];
                tempB[2] = cube[0][5];
                
                // Up top row <- Right right column
                cube[0][3] = cube[3][8];
                cube[0][4] = cube[4][8];
                cube[0][5] = cube[5][8];
                
                // Right right column <- Down bottom row
                cube[3][8] = cube[8][5];
                cube[4][8] = cube[8][4];
                cube[5][8] = cube[8][3];
                
                // Down bottom row <- Left left column
                cube[8][3] = cube[5][0];
                cube[8][4] = cube[4][0];
                cube[8][5] = cube[3][0];
                
                // Left left column <- saved Up top row
                cube[3][0] = tempB[2];
                cube[4][0] = tempB[1];
                cube[5][0] = tempB[0];
                break;
                
            case 'R': 
                rotateFace(3, 6); // Blue face
                char[] tempR = new char[3];

                // Save Back left column <- temp
                tempR[0] = cube[3][9];
                tempR[1] = cube[4][9];
                tempR[2] = cube[5][9];
                // Back left column <- Top right column
                cube[5][9] = cube [0][5];
                cube[4][9] = cube [1][5];
                cube[3][9] = cube [2][5];
                // Top right column <- Front right column
                cube[0][5] = cube[3][5];
                cube[1][5] = cube[4][5];
                cube[2][5] = cube[5][5];
                // Front right col <- Bottom right col
                cube[3][5] = cube[6][5];
                cube[4][5] = cube[7][5];
                cube[5][5] = cube[8][5];
                // Bottom right col <- saved temp
                cube[6][5] = tempR[2];
                cube[7][5] = tempR[1];
                cube[8][5] = tempR[0];
                break;
                
            case 'L':
                rotateFace(3, 0); // Green face
                char[] tempL = new char[3];
                // Save front left column
                tempL[0] = cube[3][3];
                tempL[1] = cube[4][3];
                tempL[2] = cube[5][3];
                
                // Front left column <- Up left column
                cube[3][3] = cube[0][3];
                cube[4][3] = cube[1][3];
                cube[5][3] = cube[2][3];
                
                // Up left column <- Back right column (note: back face is mirrored)
                cube[0][3] = cube[5][11];
                cube[1][3] = cube[4][11];
                cube[2][3] = cube[3][11];
                
                // Back right column <- Down left column
                cube[3][11] = cube[8][3];
                cube[4][11] = cube[7][3];
                cube[5][11] = cube[6][3];
                
                // Down left column <- saved Front left column
                cube[6][3] = tempL[0];
                cube[7][3] = tempL[1];
                cube[8][3] = tempL[2];
                break;
                
            case 'U': 
                rotateFace(0, 3); // Orange face
                char[] tempU = new char[3];
                // Save Front top row
                tempU[0] = cube[3][3];
                tempU[1] = cube[3][4];
                tempU[2] = cube[3][5];
                // Front top row <- Right top row
                cube[3][3] = cube[3][6];
                cube[3][4] = cube[3][7];
                cube[3][5] = cube[3][8];
                // Right top row <- Back top row
                cube[3][6] = cube[3][9];
                cube[3][7] = cube[3][10];
                cube[3][8] = cube[3][11];
                // Back top row <- Left top row
                cube[3][9] = cube[3][0];
                cube[3][10] = cube[3][1];
                cube[3][11] = cube[3][2];
                // Left top row <- saved temp
                cube[3][0] = tempU[0];
                cube[3][1] = tempU[1];
                cube[3][2] = tempU[2];
                break;
                  
            case 'D':
                rotateFace(6, 3); // Red face
                char[] tempD = new char[3];
                // Save front bottom row
                tempD[0] = cube[5][3];
                tempD[1] = cube[5][4];
                tempD[2] = cube[5][5];
                
                // Front bottom row <- Left bottom row
                cube[5][3] = cube[5][0];
                cube[5][4] = cube[5][1];
                cube[5][5] = cube[5][2];
                
                // Left bottom row <- Back bottom row
                cube[5][0] = cube[5][9];
                cube[5][1] = cube[5][10];
                cube[5][2] = cube[5][11];
                
                // Back bottom row <- Right bottom row
                cube[5][9] = cube[5][6];
                cube[5][10] = cube[5][7];
                cube[5][11] = cube[5][8];
                
                // Right bottom row <- saved Front bottom row
                cube[5][6] = tempD[0];
                cube[5][7] = tempD[1];
                cube[5][8] = tempD[2];
                break;
        }
    }

    /**
     * returns true if the current state of the Cube is solved,
     * i.e., it is in this state:
     *    OOO
     *    OOO
     *    OOO
     * GGGWWWBBBYYY
     * GGGWWWBBBYYY
     * GGGWWWBBBYYY
     *    RRR
     *    RRR
     *    RRR
     */
    public boolean isSolved() {
        // TODO implement me
        return toString().equals(SOLVED_STATE);
    }

    @Override
    public String toString() {
        // TODO implement me
        // Make 2D char array a string
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 12; j++) {
                if (cube[i][j] != '\0') {
                    result.append(cube[i][j]);
                }
            }
            // Trim trailing spaces for rows 0-2 and 6-8
            if ((i < 3 || i >= 6)) {
                while (result.length() > 0 && result.charAt(result.length() - 1) == ' ') {
                    result.deleteCharAt(result.length() - 1);
                }
            }
            result.append('\n');
        }
        return result.toString();
    }

    /**
     *
     * @param moves
     * @return the order of the sequence of moves
     */
    public static int order(String moves) {
        // TODO implement me
        if (moves == null || moves.isEmpty()) {
            return 1;
        }
        
        RubiksCube cube = new RubiksCube();
        String initialState = cube.toString();
        
        int count = 0;
        do {
            cube.applyMoves(moves);
            count++;
            // Prevent infinite loop - Rubik's cube group order is finite
            if (count > 1260) { // LCM of possible face orders
                return -1;
            }
        } while (!cube.toString().equals(initialState));
        
        return count;
    }

    /**
     * Get internal cube array (for heuristic calculations)
     */
    public char[][] getCube() {
        return cube;
    }

    /**
     * @return RubiksCube obj that is copied from param
     */
    public RubiksCube copy() {
        return new RubiksCube(this);
    }
    
    @Override
    public int hashCode() {
        return Arrays.deepHashCode(cube);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;

        RubiksCube other = (RubiksCube) obj;

        return Arrays.deepEquals(this.cube, other.cube);
    }
}
