package rubikscube;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

public class ManhattanDistance {
    
    // Defines which arr indices below to which Corner piece
    // There are 8 corners so each corner has 3 facelets
    // Format will be: { {r1, c1}, {r2, c2}, {r3, c3} }
    private static final int[][][] CORNER_INDICES = {

        // Corner 0: Up-Front-Left 
        { {2,3}, {3,3}, {3,2} },
        // Corner 1: Up-Front-Right
        { {2,5}, {3,5}, {3,6} } ,
        // Corner 2: Down-Front-Left
        { {6,3}, {5,3}, {5,2} } ,
        // Corner 3: Down-Front-Right
        { {6,5}, {5,5}, {5,6} } ,

        // Corner 4: Up-Back-Left
        { {0,3}, {3,11}, {3,0} } ,
        // Corner 5: Up-Back-Right
        { {0,5}, {3,9}, {3,8} } ,
        // Corner 6: Down-Back-Left
        { {8,3}, {5,11}, {5,0} } ,
        // Corner 7: Down-Back-Right
        { {8,5}, {5,9}, {5,8} }
    };

    // Defines which array indices belong to which Edge piece
    // 12 edges so each edge has 2 facelets
    private static final int[][][] EDGE_INDICES = {

        // --- TOP LAYER EDGES (Touching Up Face) ---
        // Edge 0: Up-Front (Up row 2, Front row 3)
        {{2,4}, {3,4}}, 
        // Edge 1: Up-Right (Up col 5, Right col 6)
        // Note: Up col 5 touches Right row 3 (Top of Right)
        {{1,5}, {3,7}}, 
        // Edge 2: Up-Back (Up row 0, Back row 3)
        {{0,4}, {3,10}},
        // Edge 3: Up-Left (Up col 3, Left col 2)
        {{1,3}, {3,1}}, 


        // --- MIDDLE LAYER EDGES (Verticals) ---
        // Edge 4: Front-Left (Front col 3, Left col 2)
        {{4,3}, {4,2}}, 
        // Edge 5: Front-Right (Front col 5, Right col 6)
        {{4,5}, {4,6}}, 
        // Edge 6: Back-Right (Back col 9, Right col 8)
        {{4,9}, {4,8}}, 
        // Edge 7: Back-Left (Back col 11, Left col 0)
        {{4,11}, {4,0}}, 

        // --- BOTTOM LAYER EDGES (Touching Down Face) ---
        // Edge 8: Down-Front (Down row 6, Front row 5)
        {{6,4}, {5,4}}, 
        // Edge 9: Down-Right (Down col 5, Right col 6)
        {{7,5}, {5,7}}, 
        // Edge 10: Down-Back (Down row 8, Back row 5)
        {{8,4}, {5,10}}, 
        // Edge 11: Down-Left (Down col 3, Left col 2)
        {{7,3}, {5,1}}
    };

    // --- Calculations ---

    public int calculate(RubiksCube cube) {
        int totalDist = 0;

        // Sum Corner Distances
        for (int i = 0; i < 8; i++) {
            // Get 3 colors currently at Corner Position i
            byte c1 = cube.getColor(CORNER_INDICES[i][0][0], CORNER_INDICES[i][0][1]);
            byte c2 = cube.getColor(CORNER_INDICES[i][1][0], CORNER_INDICES[i][1][1]);
            byte c3 = cube.getColor(CORNER_INDICES[i][2][0], CORNER_INDICES[i][2][1]);

            // Now Identify which piece this is
            int pieceID = identifyCorner(c1,c2,c3);

            // Look how far pieceID is from position i
            if (pieceID != -1) {
                totalDist += cornerDist[pieceID][i];
            }
        }

        // Sum Edge Distances
        for (int i = 0; i < 12; i++) {
            byte c1 = cube.getColor(EDGE_INDICES[i][0][0], EDGE_INDICES[i][0][1]);
            byte c2 = cube.getColor(EDGE_INDICES[i][1][0], EDGE_INDICES[i][1][1]);
            
            int pieceID = identifyEdge(c1, c2);
            if (pieceID != -1) {
                totalDist += edgeDist[pieceID][i];
            }
        }

        // Return total / 4
        return totalDist / 4;
    }

    private int getDistance(int[][] table, int pieceID, int currentPos) {
        return table[pieceID][currentPos];
    }

    private int identifyCorner(byte c1, byte c2, byte c3) {
        byte[] key = {c1, c2, c3};
        Arrays.sort(key);

        // Corner 0: UFL (Green, Orange, White)
        if (matches(key, RubiksCube.GREEN, RubiksCube.ORANGE, RubiksCube.WHITE)) 
            return 0;

        // Corner 1: UFR (Blue, Orange, White)
        else if (matches(key, RubiksCube.BLUE, RubiksCube.ORANGE, RubiksCube.WHITE)) 
            return 1;

        // Corner 2: DFL (Green, Red, White)
        else if (matches(key, RubiksCube.GREEN, RubiksCube.RED, RubiksCube.WHITE)) 
            return 2;

        // Corner 3: DFR (Blue, Red, White)
        else if (matches(key, RubiksCube.BLUE, RubiksCube.RED, RubiksCube.WHITE)) 
            return 3;

        // Corner 4: UBL (Green, Orange, Yellow)
        else if (matches(key, RubiksCube.GREEN, RubiksCube.ORANGE, RubiksCube.YELLOW))       
            return 4;

        // Corner 5: UBR (Blue, Orange, Yellow)
        else if (matches(key, RubiksCube.BLUE, RubiksCube.ORANGE, RubiksCube.YELLOW)) 
            return 5;

        // Corner 6: DBL (Green, Red, Yellow)
        else if (matches(key, RubiksCube.GREEN, RubiksCube.RED, RubiksCube.YELLOW)) 
            return 6;

        // Corner 7: DBR (Blue, Red, Yellow)
        else if (matches(key, RubiksCube.BLUE, RubiksCube.RED, RubiksCube.YELLOW)) 
            return 7;

        else
            return -1;
    } 



    private int identifyEdge(byte c1, byte c2) {
        byte[] key = {c1, c2};
        Arrays.sort(key);

        // Edge 0: UF (White, Orange)
        if (matches(key, RubiksCube.WHITE, RubiksCube.ORANGE)) 
            return 0;
        // Edge 1: UR (Blue, Orange)
        if (matches(key, RubiksCube.BLUE, RubiksCube.ORANGE)) 
            return 1;
        // Edge 2: UB (Yellow, Orange)
        if (matches(key, RubiksCube.YELLOW, RubiksCube.ORANGE)) 
            return 2;
        // Edge 3: UL (Green, Orange)
        if (matches(key, RubiksCube.GREEN, RubiksCube.ORANGE)) 
            return 3;

        // Edge 4: FL (White, Green)
        if (matches(key, RubiksCube.WHITE, RubiksCube.GREEN)) 
            return 4;
        // Edge 5: FR (White, Blue)
        if (matches(key, RubiksCube.WHITE, RubiksCube.BLUE)) 
            return 5;
        // Edge 6: BR (Yellow, Blue)
        if (matches(key, RubiksCube.YELLOW, RubiksCube.BLUE)) 
            return 6;
        // Edge 7: BL (Yellow, Green)
        if (matches(key, RubiksCube.YELLOW, RubiksCube.GREEN)) 
            return 7;

        // Edge 8: DF (White, Red)
        if (matches(key, RubiksCube.WHITE, RubiksCube.RED)) 
            return 8;
        // Edge 9: DR (Blue, Red)
        if (matches(key, RubiksCube.BLUE, RubiksCube.RED)) 
            return 9;
        // Edge 10: DB (Yellow, Red)
        if (matches(key, RubiksCube.YELLOW, RubiksCube.RED)) 
            return 10;
        // Edge 11: DL (Green, Red)
        if (matches(key, RubiksCube.GREEN, RubiksCube.RED)) 
            return 11;

        return -1;
    }

    private boolean matches(byte[] key, byte... expected) {
        Arrays.sort(expected);
        return Arrays.equals(key, expected);
    }

    // --- PDB Implementations ---

    // Lookup Tables: [PieceID][PositionID] -> Moves
    private static int [][] cornerDist = new int[8][8];
    private static int [][] edgeDist = new int[12][12];

    // Generate PDB
    static {
        generateCornerTables();
        generateEdgeTables();
    }

    // Calculates the shortest path from every corner to every other corner
    private static void generateCornerTables () {
        // Adjacency Graph: Which corners are connected by 1 move?
        // Indices match the CORNER_INDICES array above.

        int [][] neighbours = {
            {1, 2, 4}, // 0 (UFL) connects to UFR , DFL , UBL
            {0, 3, 5}, // 1 (UFR) connects to UFL , DFR , UBR
            {0, 3, 6}, // 2 (DFL) connects to UFL , DFR , DBL
            {1, 2, 7}, // 3 (DFR) connects to UFR , DFL , DBR
            {0, 5, 6}, // 4 (UBL) connects to UFL , UBR , DBL
            {1, 4, 7}, // 5 (UBR) connects to UFR , UBL , DBR
            {2, 4, 7}, // 6 (DBL) connects to DFL , UBL , DBR
            {3, 5, 6} // 7 (DBR) connects to DFR , UBR , DBL
        };

        runBFS (cornerDist , neighbours , 8);
    }
    
     private static void generateEdgeTables () {
     // Adjacency Graph for Edges (Each edge has 4 neighbours )

        int [][] neighbours = {
            {1, 3, 4, 5}, // 0 (UF) -> UR , UL , FL , FR
            {0, 2, 5, 6}, // 1 (UR) -> UF , UB , FR , BR
            {1, 3, 6, 7}, // 2 (UB) -> UR , UL , BR , BL
            {0, 2, 4, 7}, // 3 (UL) -> UF , UB , FL , BL
            {0, 8, 3, 11}, // 4 (FL) -> UF , DF , UL , DL
            {0, 8, 1, 9}, // 5 (FR) -> UF , DF , UR , DR
            {2, 10, 1, 9}, // 6 (BR) -> UB , DB , UR , DR
            {2, 10, 3, 11}, // 7 (BL) -> UB , DB , UL , DL
            {4, 5, 9, 11}, // 8 (DF) -> FL , FR , DR , DL
            {5, 6, 8, 10}, // 9 (DR) -> FR , BR , DF , DB
            {6, 7, 9, 11}, // 10( DB) -> BR , BL , DR , DL
            {4, 7, 8, 10} // 11( DL) -> FL , BL , DF , DB
        };

        runBFS (edgeDist , neighbours , 12);
    }

    // BFS Helper to fill tables
    private static void runBFS(int[][] table, int[][] graph, int count) {
        for (int startNode = 0; startNode < count; startNode++) {
            Arrays.fill(table[startNode], -1);
            table[startNode][startNode] = 0;

            Queue<Integer> queue = new LinkedList<>();
            queue.add(startNode);

            while (!queue.isEmpty()) {
                int current = queue.poll();
                int currentDist = table[startNode][current];

                for (int neighbour: graph[current]) {
                    if (table[startNode][neighbour] == -1) {
                        table[startNode][neighbour] = currentDist + 1;
                        queue.add(neighbour);
                    }
                }
            }
        }
    }

}
