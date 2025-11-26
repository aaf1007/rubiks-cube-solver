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

    private byte[][] cube; // 2D array of the cube
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
        this.cube = new byte[9][12];
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
            for (byte c : line.getBytes()) {
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
        cube = new byte[9][12];
        String[] lines = str.split("\n");
        for (int i = 0; i < 9; i++) {
            String line = lines[i];
            for (int j = 0; j < line.length(); j++) {
                cube[i][j] = (byte) line.charAt(j);
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
    private void rotateFace(int row, int col) {
        // byte[][] temp = new byte[3][3];
        
        // // Copy face to temp
        // for (int i = 0; i < 3; i++) {
        //     for (int j = 0; j < 3; j++) {
        //         temp[i][j] = cube[startRow + i][startCol + j];
        //     }
        // }
        
        // // Rotate clockwise: [i][j] -> [j][2-i]
        // for (int i = 0; i < 3; i++) {
        //     for (int j = 0; j < 3; j++) {
        //         cube[startRow + j][startCol + 2 - i] = temp[i][j];
        //     }
        // }

        // Updated Implementaion with optimized memory allocations

        // 1. Rotate Corners
        byte temp = cube[row][col];

        // TopLeft - BottomLeft
        cube[row][col] = cube[row+2][col];
        // BottomLeft - BottomRight 
        cube[row + 2][col] = cube[row + 2][col + 2];
        // BottomRight - TopRight
        cube[row + 2][col + 2] = cube[row][col + 2]; 
        // TopRight - TopLeft (temp)
        cube[row][col + 2] = temp; 

        // 2. Rotate Edges
        temp = cube [row][col+1];

        // TopMid - LeftMid
        cube[row][col+1] = cube[row+1][col];
        // LeftMid - BottomMid
        cube[row + 1][col] = cube[row + 2][col + 1]; 
        // BottomMid - RightMid
        cube[row + 2][col + 1] = cube[row + 1][col + 2]; 
        // RightMid - TopMid (temp)   
        cube[row + 1][col + 2] = temp;         
    }
    /**
     * @param moves - String of moves
     * Applies the sequence of moves on the Rubik's Cube
     */
    public void applyMoves(String moves) {
        // TODO implement me
        // For all loop to call applyMove(byte)

        char[] move = moves.toCharArray();

        for (int i = 0; i < move.length; i++) {
            if (Character.isLowerCase(move[i])) {
                // Counter-clockwise rotation so call it 3 times
                for (int j = 0; j < 3; j++)
                    applyMove(Character.toUpperCase(move[i]));
                
                continue;
            }
            
            applyMove(move[i]);
        }
    }

    /**
     * For applying one moves
     * @param byte of single move
     */
     public void applyMove(char move) {
        switch (move) {
            case 'F': {
                rotateFace(3, 3); // White face
                // Save edge pieces that will be moved

                // Save bottom row of Up face
                byte t1 = cube[2][3];
                byte t2 = cube[2][4];
                byte t3 = cube[2][5];
                
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
                cube[3][6] = t1;
                cube[4][6] = t2;
                cube[5][6] = t3;
                break;
            }

            case 'B': {
                rotateFace(3, 9); // Yellow face

                // Save top row of Up face
                byte t1 = cube[0][3];
                byte t2 = cube[0][4];
                byte t3 = cube[0][5];
                
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
                cube[3][0] = t3;
                cube[4][0] = t2;
                cube[5][0] = t1;
                break;
            }
            
            case 'R': {
                rotateFace(3, 6); // Blue face

                // Save Back left column <- temp
                byte t1 = cube[3][9];
                byte t2 = cube[4][9];
                byte t3 = cube[5][9];
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
                cube[6][5] = t3;
                cube[7][5] = t2;
                cube[8][5] = t1;
                break;
            }

            case 'L': {
                rotateFace(3, 0); // Green face

                // Save front left column
                byte t1 = cube[3][3];
                byte t2 = cube[4][3];
                byte t3 = cube[5][3];
                
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
                cube[6][3] = t1;
                cube[7][3] = t2;
                cube[8][3] = t3;
                break;
            }

            case 'U': {
                rotateFace(0, 3); // Orange face

                // Save Front top row
                byte t1 = cube[3][3];
                byte t2 = cube[3][4];
                byte t3 = cube[3][5];
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
                cube[3][0] = t1;
                cube[3][1] = t2;
                cube[3][2] = t3;
                break;
            }

            case 'D': {
                rotateFace(6, 3); // Red face

                // Save front bottom row
                byte t1 = cube[5][3];
                byte t2 = cube[5][4];
                byte t3 = cube[5][5];
                
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
                cube[5][6] = t1;
                cube[5][7] = t2;
                cube[5][8] = t3;
                break;
            }
        }
    }

    /**
     * 
     * @param row
     * @param col
     * @return byte color
     */
    public byte getColor(int row, int col) {
        return cube[row][col];
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
        // Make 2D byte array a string
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 12; j++) {
                if (cube[i][j] != '\0') {
                    result.append((char) cube[i][j]);
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
    public byte[][] getCube() {
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
