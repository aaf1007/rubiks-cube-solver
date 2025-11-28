package rubikscube;

/**
 * TwoPhaseSolver - Implementation of Kociemba's Two-Phase Algorithm
 *
 * This solver works by breaking the Rubik's cube solve into two phases:
 *
 * PHASE 1: Transform any cube state into the G1 subgroup
 *   - G1 is defined by: all edges oriented, all corners oriented,
 *     and the 4 middle-slice edges are in the middle slice
 *   - Uses all 18 moves (F, F2, F', B, B2, B', R, R2, R', L, L2, L', U, U2, U', D, D2, D')
 *   - Typically takes 7-12 moves
 *
 * PHASE 2: Solve from G1 to the solved state
 *   - Uses only 10 moves that preserve G1: {U, U2, U', D, D2, D', F2, B2, R2, L2}
 *   - Quarter turns of F,B,R,L would disturb orientations, so only half-turns allowed
 *   - Typically takes 8-18 moves
 *
 * The algorithm uses coordinate compression and precomputed pruning tables
 * to make the IDA* search very efficient.
 *
 * @author Anton Florendo
 * @course CMPT 225 - Spring 2025
 */
public class TwoPhaseSolver {

    // ============================================================================
    // SECTION 1: CONSTANTS - Color and Face Definitions
    // ============================================================================

    /**
     * Color constants matching RubiksCube.java
     * These represent the center colors of each face in the solved state
     */
    private static final byte ORANGE = 'O';  // Up face
    private static final byte WHITE  = 'W';  // Front face
    private static final byte YELLOW = 'Y';  // Back face
    private static final byte RED    = 'R';  // Down face
    private static final byte GREEN  = 'G';  // Left face
    private static final byte BLUE   = 'B';  // Right face

    // ============================================================================
    // SECTION 2: CORNER AND EDGE DEFINITIONS
    // ============================================================================

    /**
     * Corner numbering convention (0-7):
     *
     * Corner 0: URF (Up-Right-Front)    - Colors: Orange, Blue, White
     * Corner 1: UFL (Up-Front-Left)     - Colors: Orange, White, Green
     * Corner 2: ULB (Up-Left-Back)      - Colors: Orange, Green, Yellow
     * Corner 3: UBR (Up-Back-Right)     - Colors: Orange, Yellow, Blue
     * Corner 4: DFR (Down-Front-Right)  - Colors: Red, White, Blue
     * Corner 5: DLF (Down-Left-Front)   - Colors: Red, Green, White
     * Corner 6: DBL (Down-Back-Left)    - Colors: Red, Yellow, Green
     * Corner 7: DRB (Down-Right-Back)   - Colors: Red, Blue, Yellow
     *
     * Each corner has 3 facelets. The order is: U/D facelet, then clockwise
     * as viewed from U/D face.
     */

    /**
     * CORNER_FACELETS[corner][facelet] = {row, col} in byte[9][12] array
     *
     * For each corner position (0-7), stores the (row, col) coordinates
     * of its 3 facelets in the cube's byte[9][12] representation.
     *
     * Facelet 0: The U/D face sticker (determines orientation reference)
     * Facelet 1: Clockwise from facelet 0 (when viewed from U/D)
     * Facelet 2: Counter-clockwise from facelet 0
     */
    private static final int[][][] CORNER_FACELETS = {
        // Corner 0: URF - facelets on U, R, F faces
        {{2, 5}, {3, 6}, {3, 5}},

        // Corner 1: UFL - facelets on U, F, L faces
        {{2, 3}, {3, 3}, {3, 2}},

        // Corner 2: ULB - facelets on U, L, B faces
        {{0, 3}, {3, 0}, {3, 11}},

        // Corner 3: UBR - facelets on U, B, R faces
        {{0, 5}, {3, 9}, {3, 8}},

        // Corner 4: DFR - facelets on D, F, R faces
        {{6, 5}, {5, 5}, {5, 6}},

        // Corner 5: DLF - facelets on D, L, F faces
        {{6, 3}, {5, 2}, {5, 3}},

        // Corner 6: DBL - facelets on D, B, L faces
        {{8, 3}, {5, 11}, {5, 0}},

        // Corner 7: DRB - facelets on D, R, B faces
        {{8, 5}, {5, 8}, {5, 9}}
    };

    /**
     * Edge numbering convention (0-11):
     *
     * U/D layer edges (0-7) - These stay in U/D layers in G1:
     * Edge 0: UR (Up-Right)      - Colors: Orange, Blue
     * Edge 1: UF (Up-Front)      - Colors: Orange, White
     * Edge 2: UL (Up-Left)       - Colors: Orange, Green
     * Edge 3: UB (Up-Back)       - Colors: Orange, Yellow
     * Edge 4: DR (Down-Right)    - Colors: Red, Blue
     * Edge 5: DF (Down-Front)    - Colors: Red, White
     * Edge 6: DL (Down-Left)     - Colors: Red, Green
     * Edge 7: DB (Down-Back)     - Colors: Red, Yellow
     *
     * Slice edges (8-11) - These must be in middle slice in G1:
     * Edge 8:  FR (Front-Right)  - Colors: White, Blue
     * Edge 9:  FL (Front-Left)   - Colors: White, Green
     * Edge 10: BL (Back-Left)    - Colors: Yellow, Green
     * Edge 11: BR (Back-Right)   - Colors: Yellow, Blue
     */

    /**
     * EDGE_FACELETS[edge][facelet] = {row, col} in byte[9][12] array
     *
     * For each edge position (0-11), stores the (row, col) coordinates
     * of its 2 facelets in the cube's byte[9][12] representation.
     *
     * Facelet 0: Primary facelet (determines orientation reference)
     *   - For U/D edges: the U or D face sticker
     *   - For slice edges: the F or B face sticker
     * Facelet 1: Secondary facelet
     */
    private static final int[][][] EDGE_FACELETS = {
        // Edge 0: UR - facelets on U, R faces
        {{1, 5}, {3, 7}},

        // Edge 1: UF - facelets on U, F faces
        {{2, 4}, {3, 4}},

        // Edge 2: UL - facelets on U, L faces
        {{1, 3}, {3, 1}},

        // Edge 3: UB - facelets on U, B faces
        {{0, 4}, {3, 10}},

        // Edge 4: DR - facelets on D, R faces
        {{7, 5}, {5, 7}},

        // Edge 5: DF - facelets on D, F faces
        {{6, 4}, {5, 4}},

        // Edge 6: DL - facelets on D, L faces
        {{7, 3}, {5, 1}},

        // Edge 7: DB - facelets on D, B faces
        {{8, 4}, {5, 10}},

        // Edge 8: FR (slice) - facelets on F, R faces
        {{4, 5}, {4, 6}},

        // Edge 9: FL (slice) - facelets on F, L faces
        {{4, 3}, {4, 2}},

        // Edge 10: BL (slice) - facelets on B, L faces
        {{4, 11}, {4, 0}},

        // Edge 11: BR (slice) - facelets on B, R faces
        {{4, 9}, {4, 8}}
    };

    // ============================================================================
    // SECTION 3: COORDINATE SYSTEM SIZES
    // ============================================================================

    /**
     * Coordinate sizes for the Kociemba algorithm:
     *
     * Phase 1 coordinates:
     * - twist: 3^7 = 2187 (corner orientations, 8th is determined by parity)
     * - flip:  2^11 = 2048 (edge orientations, 12th is determined by parity)
     * - slice: C(12,4) = 495 (which 4 of 12 positions have slice edges)
     *
     * Phase 2 coordinates:
     * - cornerPerm: 8! = 40320 (corner permutation)
     * - udEdgePerm: 8! = 40320 (U/D layer edge permutation)
     * - slicePerm:  4! = 24 (slice edge permutation within middle layer)
     */
    private static final int N_TWIST = 2187;       // 3^7
    private static final int N_FLIP = 2048;        // 2^11
    private static final int N_SLICE = 495;        // C(12,4)
    private static final int N_CORNER_PERM = 40320; // 8!
    private static final int N_UD_EDGE_PERM = 40320; // 8!
    private static final int N_SLICE_PERM = 24;    // 4!

    private static final int N_MOVES = 18;         // All moves for Phase 1
    private static final int N_MOVES_P2 = 10;      // G1-preserving moves for Phase 2

    // ============================================================================
    // SECTION 4: BINOMIAL COEFFICIENTS
    // ============================================================================

    /**
     * Precomputed binomial coefficients C(n,k) for slice coordinate encoding.
     *
     * The slice coordinate uses the combinatorial number system to encode
     * which 4 of 12 positions contain slice edges. We need C(n,k) for
     * n up to 12 and k up to 4.
     */
    private static final int[][] BINOMIAL = new int[13][5];

    static {
        // Initialize binomial coefficients using Pascal's triangle
        for (int n = 0; n <= 12; n++) {
            for (int k = 0; k <= 4; k++) {
                if (k == 0 || k == n) {
                    BINOMIAL[n][k] = 1;
                } else if (k > n) {
                    BINOMIAL[n][k] = 0;
                } else {
                    BINOMIAL[n][k] = BINOMIAL[n-1][k-1] + BINOMIAL[n-1][k];
                }
            }
        }
    }

    /**
     * Factorial values for permutation encoding (Lehmer code)
     */
    private static final int[] FACTORIAL = {1, 1, 2, 6, 24, 120, 720, 5040, 40320};

    // ============================================================================
    // SECTION 5: MOVE DEFINITIONS
    // ============================================================================

    /**
     * Move indexing for Phase 1 (18 moves):
     *
     * Index  Move   Description
     * -----  ----   -----------
     *   0     U     Up clockwise
     *   1     U2    Up 180 degrees
     *   2     U'    Up counter-clockwise
     *   3     R     Right clockwise
     *   4     R2    Right 180 degrees
     *   5     R'    Right counter-clockwise
     *   6     F     Front clockwise
     *   7     F2    Front 180 degrees
     *   8     F'    Front counter-clockwise
     *   9     D     Down clockwise
     *  10     D2    Down 180 degrees
     *  11     D'    Down counter-clockwise
     *  12     L     Left clockwise
     *  13     L2    Left 180 degrees
     *  14     L'    Left counter-clockwise
     *  15     B     Back clockwise
     *  16     B2    Back 180 degrees
     *  17     B'    Back counter-clockwise
     */
    private static final String[] MOVE_NAMES = {
        "U", "U2", "U'", "R", "R2", "R'", "F", "F2", "F'",
        "D", "D2", "D'", "L", "L2", "L'", "B", "B2", "B'"
    };

    /**
     * Phase 2 moves (10 moves that preserve G1):
     *
     * In G1, we can only use:
     * - U, U2, U' (any U move preserves orientations)
     * - D, D2, D' (any D move preserves orientations)
     * - R2, L2, F2, B2 (only half-turns, as quarter-turns would flip edges)
     */
    private static final int[] PHASE2_MOVES = {0, 1, 2, 9, 10, 11, 4, 13, 7, 16};
    // Corresponds to: U, U2, U', D, D2, D', R2, L2, F2, B2

    // ============================================================================
    // SECTION 6: MOVE EFFECT TABLES
    // ============================================================================

    /**
     * These tables define exactly how each move affects the cube.
     *
     * CORNER_MOVE_PERM[move][position] = new position of corner that was at 'position'
     * CORNER_MOVE_ORIENT[move][position] = orientation change (0, 1, or 2) added to corner
     * EDGE_MOVE_PERM[move][position] = new position of edge that was at 'position'
     * EDGE_MOVE_ORIENT[move][position] = orientation change (0 or 1) added to edge
     *
     * These are derived from the physical mechanics of how faces rotate.
     */

    /**
     * Corner permutation: CORNER_MOVE_PERM[m][i] = position FROM which the corner
     * at position i came after move m.
     *
     * Corners: 0=URF, 1=UFL, 2=ULB, 3=UBR, 4=DFR, 5=DLF, 6=DBL, 7=DRB
     */
    private static final int[][] CORNER_MOVE_PERM = {
        // U (clockwise): cycle 1->0->3->2->1
        // pos 0 <- 1, pos 1 <- 2, pos 2 <- 3, pos 3 <- 0
        {1, 2, 3, 0, 4, 5, 6, 7},  // U
        {2, 3, 0, 1, 4, 5, 6, 7},  // U2
        {3, 0, 1, 2, 4, 5, 6, 7},  // U'

        // R (clockwise): cycle 0->3->7->4->0
        // pos 0 <- 4, pos 3 <- 0, pos 7 <- 3, pos 4 <- 7
        {4, 1, 2, 0, 7, 5, 6, 3},  // R
        {7, 1, 2, 4, 3, 5, 6, 0},  // R2
        {3, 1, 2, 7, 0, 5, 6, 4},  // R'

        // F (clockwise): cycle 0->1->5->4->0
        // pos 0 <- 1, pos 1 <- 5, pos 5 <- 4, pos 4 <- 0
        {1, 5, 2, 3, 0, 4, 6, 7},  // F
        {5, 4, 2, 3, 1, 0, 6, 7},  // F2
        {4, 0, 2, 3, 5, 1, 6, 7},  // F'

        // D (clockwise): cycle 4->5->6->7->4
        // pos 4 <- 5, pos 5 <- 6, pos 6 <- 7, pos 7 <- 4
        {0, 1, 2, 3, 5, 6, 7, 4},  // D
        {0, 1, 2, 3, 6, 7, 4, 5},  // D2
        {0, 1, 2, 3, 7, 4, 5, 6},  // D'

        // L (clockwise): cycle 1->2->6->5->1
        // pos 1 <- 2, pos 2 <- 6, pos 6 <- 5, pos 5 <- 1
        {0, 2, 6, 3, 4, 1, 5, 7},  // L
        {0, 6, 5, 3, 4, 2, 1, 7},  // L2
        {0, 5, 1, 3, 4, 6, 2, 7},  // L'

        // B (clockwise): cycle 2->3->7->6->2
        // pos 2 <- 3, pos 3 <- 7, pos 7 <- 6, pos 6 <- 2
        {0, 1, 3, 7, 4, 5, 2, 6},  // B
        {0, 1, 7, 6, 4, 5, 3, 2},  // B2
        {0, 1, 6, 2, 4, 5, 7, 3}   // B'
    };

    /**
     * Corner orientation changes.
     *
     * When a corner moves, its orientation may change:
     * - U and D moves: no orientation change (corners stay on same layer)
     * - R, L, F, B moves: corners moving to a different layer twist
     *
     * Orientation values: 0 = correct, 1 = clockwise twist, 2 = counter-clockwise twist
     * The change is added modulo 3.
     */
    private static final int[][] CORNER_MOVE_ORIENT = {
        // U moves: no twist
        {0, 0, 0, 0, 0, 0, 0, 0},  // U
        {0, 0, 0, 0, 0, 0, 0, 0},  // U2
        {0, 0, 0, 0, 0, 0, 0, 0},  // U'

        // R moves: corners 0,3,4,7 twist
        {2, 0, 0, 1, 1, 0, 0, 2},  // R
        {0, 0, 0, 0, 0, 0, 0, 0},  // R2
        {2, 0, 0, 1, 1, 0, 0, 2},  // R'

        // F moves: corners 0,1,4,5 twist
        {1, 2, 0, 0, 2, 1, 0, 0},  // F
        {0, 0, 0, 0, 0, 0, 0, 0},  // F2
        {1, 2, 0, 0, 2, 1, 0, 0},  // F'

        // D moves: no twist
        {0, 0, 0, 0, 0, 0, 0, 0},  // D
        {0, 0, 0, 0, 0, 0, 0, 0},  // D2
        {0, 0, 0, 0, 0, 0, 0, 0},  // D'

        // L moves: corners 1,2,5,6 twist
        {0, 1, 2, 0, 0, 2, 1, 0},  // L
        {0, 0, 0, 0, 0, 0, 0, 0},  // L2
        {0, 1, 2, 0, 0, 2, 1, 0},  // L'

        // B moves: corners 2,3,6,7 twist
        {0, 0, 1, 2, 0, 0, 2, 1},  // B
        {0, 0, 0, 0, 0, 0, 0, 0},  // B2
        {0, 0, 1, 2, 0, 0, 2, 1}   // B'
    };

    /**
     * Edge permutation: EDGE_MOVE_PERM[m][i] = position FROM which the edge
     * at position i came after move m.
     *
     * Edges: 0=UR, 1=UF, 2=UL, 3=UB, 4=DR, 5=DF, 6=DL, 7=DB, 8=FR, 9=FL, 10=BL, 11=BR
     */
    private static final int[][] EDGE_MOVE_PERM = {
        // U (clockwise): cycle 0->3->2->1->0
        // pos 0 <- 1, pos 1 <- 2, pos 2 <- 3, pos 3 <- 0
        {1, 2, 3, 0, 4, 5, 6, 7, 8, 9, 10, 11},  // U
        {2, 3, 0, 1, 4, 5, 6, 7, 8, 9, 10, 11},  // U2
        {3, 0, 1, 2, 4, 5, 6, 7, 8, 9, 10, 11},  // U'

        // R (clockwise): cycle 0->8->4->11->0
        // pos 0 <- 11, pos 8 <- 0, pos 4 <- 8, pos 11 <- 4
        {11, 1, 2, 3, 8, 5, 6, 7, 0, 9, 10, 4},  // R
        {4, 1, 2, 3, 11, 5, 6, 7, 0, 9, 10, 8},  // R2 (was correct)
        {8, 1, 2, 3, 0, 5, 6, 7, 4, 9, 10, 11},  // R'

        // F (clockwise): cycle 1->8->5->9->1
        // pos 1 <- 9, pos 8 <- 1, pos 5 <- 8, pos 9 <- 5
        {0, 9, 2, 3, 4, 8, 6, 7, 1, 5, 10, 11},  // F
        {0, 5, 2, 3, 4, 9, 6, 7, 1, 8, 10, 11},  // F2
        {0, 8, 2, 3, 4, 9, 6, 7, 5, 1, 10, 11},  // F'

        // D (clockwise): cycle 4->5->6->7->4
        // pos 4 <- 7, pos 5 <- 4, pos 6 <- 5, pos 7 <- 6
        {0, 1, 2, 3, 7, 4, 5, 6, 8, 9, 10, 11},  // D
        {0, 1, 2, 3, 6, 7, 4, 5, 8, 9, 10, 11},  // D2
        {0, 1, 2, 3, 5, 6, 7, 4, 8, 9, 10, 11},  // D'

        // L (clockwise): cycle 2->9->6->10->2
        // pos 2 <- 10, pos 9 <- 2, pos 6 <- 9, pos 10 <- 6
        {0, 1, 10, 3, 4, 5, 9, 7, 8, 2, 6, 11},  // L
        {0, 1, 6, 3, 4, 5, 10, 7, 8, 2, 9, 11},  // L2
        {0, 1, 9, 3, 4, 5, 10, 7, 8, 6, 2, 11},  // L'

        // B (clockwise): cycle 3->10->7->11->3
        // pos 3 <- 11, pos 10 <- 3, pos 7 <- 10, pos 11 <- 7
        {0, 1, 2, 11, 4, 5, 6, 10, 8, 9, 3, 7},  // B
        {0, 1, 2, 7, 4, 5, 6, 11, 8, 9, 3, 10},  // B2
        {0, 1, 2, 10, 4, 5, 6, 3, 8, 9, 7, 11}   // B'
    };

    /**
     * Edge orientation changes.
     *
     * An edge is "flipped" (orientation = 1) when its primary color
     * (U/D for U/D edges, F/B for slice edges) is not on a U/D face.
     *
     * - U and D moves: never flip edges
     * - R and L moves: never flip edges (edges stay on same face type)
     * - F and B moves: flip the 4 edges that cycle
     */
    private static final int[][] EDGE_MOVE_ORIENT = {
        // U moves: no flip
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},  // U
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},  // U2
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},  // U'

        // R moves: no flip
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},  // R
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},  // R2
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},  // R'

        // F moves: flip edges 1,5,8,9
        {0, 1, 0, 0, 0, 1, 0, 0, 1, 1, 0, 0},  // F
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},  // F2
        {0, 1, 0, 0, 0, 1, 0, 0, 1, 1, 0, 0},  // F'

        // D moves: no flip
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},  // D
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},  // D2
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},  // D'

        // L moves: no flip
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},  // L
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},  // L2
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},  // L'

        // B moves: flip edges 3,7,10,11
        {0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 1, 1},  // B
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},  // B2
        {0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 1, 1}   // B'
    };

    /**
     * Position-based edge orientation changes.
     *
     * With position-based orientation, an edge's orientation depends on:
     * 1. Physical flip (F/B quarter-turns swap facelets) - captured in EDGE_MOVE_ORIENT
     * 2. Position type change (moving between U/D positions 0-7 and E-slice positions 8-11)
     *
     * Formula: orientChange[move][i] = physicalFlip XOR positionTypeChange
     * where positionTypeChange = (i < 8) XOR (fromPos < 8)
     *
     * This is computed statically from EDGE_MOVE_PERM and EDGE_MOVE_ORIENT.
     */
    private static final int[][] EDGE_MOVE_ORIENT_POS;
    static {
        EDGE_MOVE_ORIENT_POS = new int[N_MOVES][12];
        for (int move = 0; move < N_MOVES; move++) {
            for (int i = 0; i < 12; i++) {
                int fromPos = EDGE_MOVE_PERM[move][i];
                int physFlip = EDGE_MOVE_ORIENT[move][i];
                // Position type: 0 for U/D positions (0-7), 1 for E-slice (8-11)
                int posType_i = (i < 8) ? 0 : 1;
                int posType_from = (fromPos < 8) ? 0 : 1;
                int typeChange = posType_i ^ posType_from;
                EDGE_MOVE_ORIENT_POS[move][i] = typeChange ^ physFlip;
            }
        }
    }

    // ============================================================================
    // SECTION 7: MOVE TABLES (Generated at runtime)
    // ============================================================================

    /**
     * Move tables: twistMove[twist][move] = new twist after applying move
     * These are generated once at class loading time.
     *
     * Note: cornerPermMove and udEdgePermMove use int because values can
     * exceed Short.MAX_VALUE (32767), as 40320 > 32767.
     */
    private static short[][] twistMove;      // [2187][18]
    private static short[][] flipMove;       // [2048][18]
    private static short[][] sliceMove;      // [495][18]
    private static int[][] cornerPermMove;   // [40320][18] - must be int!
    private static int[][] udEdgePermMove;   // [40320][18] - must be int!
    private static short[][] slicePermMove;  // [24][10]

    // ============================================================================
    // SECTION 8: PRUNING TABLES (Generated via BFS)
    // ============================================================================

    /**
     * Pruning tables: store the minimum number of moves needed to reach
     * the goal state from any given coordinate combination.
     *
     * These provide admissible heuristics for IDA* search.
     */
    private static byte[] twistSlicePrune;    // [2187 * 495]
    private static byte[] flipSlicePrune;     // [2048 * 495]
    private static byte[] twistFlipPrune;     // [2187 * 2048] - ~4.5 MB for better Phase 1 heuristic
    private static byte[] cornerSlicePrune;   // [40320 * 24]
    private static byte[] udEdgeSlicePrune;   // [40320 * 24]

    // Flag to track initialization
    private static boolean initialized = false;

    // ============================================================================
    // SECTION 9: STATIC INITIALIZER
    // ============================================================================

    /**
     * Initialize all tables when the class is first loaded.
     * This takes 2-3 seconds but only happens once.
     */
    public static synchronized void initTables() {
        if (initialized) return;

        // Allocate move tables
        twistMove = new short[N_TWIST][N_MOVES];
        flipMove = new short[N_FLIP][N_MOVES];
        sliceMove = new short[N_SLICE][N_MOVES];
        cornerPermMove = new int[N_CORNER_PERM][N_MOVES];   // int for values > 32767
        udEdgePermMove = new int[N_UD_EDGE_PERM][N_MOVES];  // int for values > 32767
        slicePermMove = new short[N_SLICE_PERM][N_MOVES_P2];

        // Allocate pruning tables
        twistSlicePrune = new byte[N_TWIST * N_SLICE];
        flipSlicePrune = new byte[N_FLIP * N_SLICE];
        twistFlipPrune = new byte[N_TWIST * N_FLIP];  // ~4.5 MB
        cornerSlicePrune = new byte[N_CORNER_PERM * N_SLICE_PERM];
        udEdgeSlicePrune = new byte[N_UD_EDGE_PERM * N_SLICE_PERM];

        // Build tables
        buildMoveTables();
        buildPruneTables();

        initialized = true;
    }

    // ============================================================================
    // SECTION 10: COORDINATE EXTRACTION FROM CUBE
    // ============================================================================

    /**
     * Identify which corner piece (0-7) is at a given position.
     *
     * Each corner is uniquely identified by its three colors.
     * We read the colors at the position and match to the known corner.
     *
     * @param cube The RubiksCube to examine
     * @param pos The corner position (0-7) to check
     * @return The corner piece ID (0-7) currently at that position
     */
    private static int getCornerAtPosition(RubiksCube cube, int pos) {
        // Get the three facelet colors at this corner position
        byte c0 = cube.getColor(CORNER_FACELETS[pos][0][0], CORNER_FACELETS[pos][0][1]);
        byte c1 = cube.getColor(CORNER_FACELETS[pos][1][0], CORNER_FACELETS[pos][1][1]);
        byte c2 = cube.getColor(CORNER_FACELETS[pos][2][0], CORNER_FACELETS[pos][2][1]);

        // Sort colors to create unique identifier (order-independent)
        byte[] colors = {c0, c1, c2};
        java.util.Arrays.sort(colors);

        // Match to corner piece based on color combination
        // Corner 0 (URF): Blue, Orange, White -> B, O, W
        if (colors[0] == 'B' && colors[1] == 'O' && colors[2] == 'W') return 0;
        // Corner 1 (UFL): Green, Orange, White -> G, O, W
        if (colors[0] == 'G' && colors[1] == 'O' && colors[2] == 'W') return 1;
        // Corner 2 (ULB): Green, Orange, Yellow -> G, O, Y
        if (colors[0] == 'G' && colors[1] == 'O' && colors[2] == 'Y') return 2;
        // Corner 3 (UBR): Blue, Orange, Yellow -> B, O, Y
        if (colors[0] == 'B' && colors[1] == 'O' && colors[2] == 'Y') return 3;
        // Corner 4 (DFR): Blue, Red, White -> B, R, W
        if (colors[0] == 'B' && colors[1] == 'R' && colors[2] == 'W') return 4;
        // Corner 5 (DLF): Green, Red, White -> G, R, W
        if (colors[0] == 'G' && colors[1] == 'R' && colors[2] == 'W') return 5;
        // Corner 6 (DBL): Green, Red, Yellow -> G, R, Y
        if (colors[0] == 'G' && colors[1] == 'R' && colors[2] == 'Y') return 6;
        // Corner 7 (DRB): Blue, Red, Yellow -> B, R, Y
        if (colors[0] == 'B' && colors[1] == 'R' && colors[2] == 'Y') return 7;

        return -1; // Error - should never happen with valid cube
    }

    /**
     * Get the orientation of the corner at a given position.
     *
     * Orientation is defined relative to the U/D axis:
     * - 0: The U/D colored facelet is on a U or D face (correct)
     * - 1: The U/D colored facelet is clockwise from correct (twisted CW)
     * - 2: The U/D colored facelet is counter-clockwise from correct (twisted CCW)
     *
     * @param cube The RubiksCube to examine
     * @param pos The corner position (0-7) to check
     * @return The orientation (0, 1, or 2)
     */
    private static int getCornerOrientation(RubiksCube cube, int pos) {
        // Get colors at each facelet of this corner position
        byte c0 = cube.getColor(CORNER_FACELETS[pos][0][0], CORNER_FACELETS[pos][0][1]); // U/D facelet
        byte c1 = cube.getColor(CORNER_FACELETS[pos][1][0], CORNER_FACELETS[pos][1][1]); // CW facelet
        byte c2 = cube.getColor(CORNER_FACELETS[pos][2][0], CORNER_FACELETS[pos][2][1]); // CCW facelet

        // Check which facelet has a U or D color (Orange or Red)
        if (c0 == ORANGE || c0 == RED) {
            return 0;  // U/D color is on U/D face - correctly oriented
        } else if (c1 == ORANGE || c1 == RED) {
            return 1;  // U/D color is on CW facelet - twisted clockwise
        } else {
            return 2;  // U/D color is on CCW facelet - twisted counter-clockwise
        }
    }

    /**
     * Identify which edge piece (0-11) is at a given position.
     *
     * Each edge is uniquely identified by its two colors.
     *
     * @param cube The RubiksCube to examine
     * @param pos The edge position (0-11) to check
     * @return The edge piece ID (0-11) currently at that position
     */
    private static int getEdgeAtPosition(RubiksCube cube, int pos) {
        byte c0 = cube.getColor(EDGE_FACELETS[pos][0][0], EDGE_FACELETS[pos][0][1]);
        byte c1 = cube.getColor(EDGE_FACELETS[pos][1][0], EDGE_FACELETS[pos][1][1]);

        // Sort colors to create unique identifier
        byte[] colors = {c0, c1};
        if (colors[0] > colors[1]) {
            byte temp = colors[0];
            colors[0] = colors[1];
            colors[1] = temp;
        }

        // Match to edge piece based on color combination
        // Edges are sorted: B<G<O<R<W<Y
        if (colors[0] == 'B' && colors[1] == 'O') return 0;   // UR: Blue-Orange
        if (colors[0] == 'O' && colors[1] == 'W') return 1;   // UF: Orange-White
        if (colors[0] == 'G' && colors[1] == 'O') return 2;   // UL: Green-Orange
        if (colors[0] == 'O' && colors[1] == 'Y') return 3;   // UB: Orange-Yellow
        if (colors[0] == 'B' && colors[1] == 'R') return 4;   // DR: Blue-Red
        if (colors[0] == 'R' && colors[1] == 'W') return 5;   // DF: Red-White
        if (colors[0] == 'G' && colors[1] == 'R') return 6;   // DL: Green-Red
        if (colors[0] == 'R' && colors[1] == 'Y') return 7;   // DB: Red-Yellow
        if (colors[0] == 'B' && colors[1] == 'W') return 8;   // FR: Blue-White
        if (colors[0] == 'G' && colors[1] == 'W') return 9;   // FL: Green-White
        if (colors[0] == 'G' && colors[1] == 'Y') return 10;  // BL: Green-Yellow
        if (colors[0] == 'B' && colors[1] == 'Y') return 11;  // BR: Blue-Yellow

        return -1; // Error
    }

    /**
     * Get the orientation of the edge at a given position.
     *
     * POSITION-BASED orientation (not piece-based):
     * - 0: Edge is correctly oriented for this position
     * - 1: Edge is flipped for this position
     *
     * For positions 0-7 (U/D layer): correct if facelet 0 has U/D color (Orange/Red)
     * For positions 8-11 (E-slice): correct if facelet 0 has F/B color (White/Yellow)
     *
     * This definition makes the flip coordinate independent of which edges are where,
     * allowing move tables to work correctly.
     *
     * @param cube The RubiksCube to examine
     * @param pos The edge position (0-11) to check
     * @return The orientation (0 or 1)
     */
    private static int getEdgeOrientation(RubiksCube cube, int pos) {
        // PIECE-BASED orientation (standard Kociemba convention)
        //
        // For each edge, we identify if it's a U/D edge (has U/D colored sticker)
        // or an E-slice edge (has F/B colored stickers, no U/D color).
        //
        // - U/D edges (8 of them): orient=0 if U/D sticker is on U/D face
        // - E-slice edges (4 of them): orient=0 if F/B sticker is on F/B face
        //
        // This is piece-based because it depends on which physical edge piece
        // is at the position, not just the position.

        byte c0 = cube.getColor(EDGE_FACELETS[pos][0][0], EDGE_FACELETS[pos][0][1]);
        byte c1 = cube.getColor(EDGE_FACELETS[pos][1][0], EDGE_FACELETS[pos][1][1]);

        // Check if this is a U/D edge (has Orange or Red sticker)
        boolean c0isUD = (c0 == ORANGE || c0 == RED);
        boolean c1isUD = (c1 == ORANGE || c1 == RED);

        if (c0isUD || c1isUD) {
            // This is a U/D edge. Check if U/D sticker is on a U/D face.
            // U/D faces are rows 0-2 (U face) and rows 6-8 (D face).
            // The reference facelet (facelet 0) is on a U/D face for positions 0-7.
            if (c0isUD) {
                // c0 has U/D color. Is facelet 0 on a U/D face?
                int row = EDGE_FACELETS[pos][0][0];
                return (row <= 2 || row >= 6) ? 0 : 1;
            } else {
                // c1 has U/D color. Is facelet 1 on a U/D face?
                int row = EDGE_FACELETS[pos][1][0];
                return (row <= 2 || row >= 6) ? 0 : 1;
            }
        } else {
            // This is an E-slice edge (has F/B colors: White/Yellow and Green/Blue)
            // Check if F/B sticker (White/Yellow) is on a F/B face.
            // F face is cols 3-5 rows 3-5, B face is cols 9-11 rows 3-5.
            boolean c0isFB = (c0 == WHITE || c0 == YELLOW);
            if (c0isFB) {
                // c0 has F/B color. Is facelet 0 on a F/B face?
                int col = EDGE_FACELETS[pos][0][1];
                return (col >= 3 && col <= 5) || (col >= 9 && col <= 11) ? 0 : 1;
            } else {
                // c1 has F/B color. Is facelet 1 on a F/B face?
                int col = EDGE_FACELETS[pos][1][1];
                return (col >= 3 && col <= 5) || (col >= 9 && col <= 11) ? 0 : 1;
            }
        }
    }

    /**
     * Compute the twist coordinate (corner orientations).
     *
     * The twist is a number from 0 to 2186 (3^7 - 1) that encodes
     * the orientation of the first 7 corners. The 8th corner's
     * orientation is determined by parity (sum must be divisible by 3).
     *
     * Formula: twist = sum(orientation[i] * 3^(6-i)) for i = 0 to 6
     *
     * @param cube The RubiksCube to examine
     * @return The twist coordinate (0 to 2186)
     */
    public static int getTwist(RubiksCube cube) {
        int twist = 0;
        for (int i = 0; i < 7; i++) {
            twist = twist * 3 + getCornerOrientation(cube, i);
        }
        return twist;
    }

    /**
     * Compute the flip coordinate (edge orientations).
     *
     * The flip is a number from 0 to 2047 (2^11 - 1) that encodes
     * the orientation of the first 11 edges. The 12th edge's
     * orientation is determined by parity (sum must be even).
     *
     * Formula: flip = sum(orientation[i] * 2^(10-i)) for i = 0 to 10
     *
     * @param cube The RubiksCube to examine
     * @return The flip coordinate (0 to 2047)
     */
    public static int getFlip(RubiksCube cube) {
        int flip = 0;
        for (int i = 0; i < 11; i++) {
            flip = flip * 2 + getEdgeOrientation(cube, i);
        }
        return flip;
    }

    /**
     * Compute the slice coordinate.
     *
     * The slice coordinate (0 to 494) encodes WHICH 4 of the 12 edge
     * positions currently contain slice edges (edges 8-11). It does NOT
     * track which specific slice edge is where, just which positions
     * have slice edges.
     *
     * Uses the combinatorial number system for encoding.
     *
     * @param cube The RubiksCube to examine
     * @return The slice coordinate (0 to 494)
     */
    public static int getSlice(RubiksCube cube) {
        // Find positions that contain slice edges
        int[] slicePositions = new int[4];
        int count = 0;

        for (int pos = 0; pos < 12 && count < 4; pos++) {
            int edge = getEdgeAtPosition(cube, pos);
            if (edge >= 8) {  // It's a slice edge (8, 9, 10, or 11)
                slicePositions[count++] = pos;
            }
        }

        // Encode using combinatorial number system
        // slice = C(p0,1) + C(p1,2) + C(p2,3) + C(p3,4)
        return BINOMIAL[slicePositions[0]][1] +
               BINOMIAL[slicePositions[1]][2] +
               BINOMIAL[slicePositions[2]][3] +
               BINOMIAL[slicePositions[3]][4];
    }

    /**
     * Compute the corner permutation coordinate.
     *
     * Uses Lehmer code (factorial number system) to encode which
     * corner piece is at each position. Range: 0 to 40319 (8! - 1).
     *
     * @param cube The RubiksCube to examine
     * @return The corner permutation coordinate (0 to 40319)
     */
    public static int getCornerPerm(RubiksCube cube) {
        int[] corners = new int[8];
        for (int i = 0; i < 8; i++) {
            corners[i] = getCornerAtPosition(cube, i);
        }

        int perm = 0;
        for (int i = 0; i < 7; i++) {
            // Count how many corners after position i have smaller ID
            int count = 0;
            for (int j = i + 1; j < 8; j++) {
                if (corners[j] < corners[i]) count++;
            }
            perm = perm * (8 - i) + count;
        }
        return perm;
    }

    /**
     * Compute the U/D edge permutation coordinate.
     *
     * Uses Lehmer code to encode the permutation of the 8 U/D layer
     * edges (edges 0-7). This is only valid in G1 where slice edges
     * are already in the middle slice.
     *
     * @param cube The RubiksCube to examine
     * @return The UD edge permutation coordinate (0 to 40319)
     */
    public static int getUDEdgePerm(RubiksCube cube) {
        int[] edges = new int[8];
        for (int pos = 0; pos < 8; pos++) {
            edges[pos] = getEdgeAtPosition(cube, pos);
        }

        int perm = 0;
        for (int i = 0; i < 7; i++) {
            int count = 0;
            for (int j = i + 1; j < 8; j++) {
                if (edges[j] < edges[i]) count++;
            }
            perm = perm * (8 - i) + count;
        }
        return perm;
    }

    /**
     * Compute the slice edge permutation coordinate.
     *
     * Uses Lehmer code to encode the permutation of the 4 slice edges
     * within the middle slice. Range: 0 to 23 (4! - 1).
     *
     * This is only valid in G1 where slice edges are in positions 8-11.
     *
     * @param cube The RubiksCube to examine
     * @return The slice permutation coordinate (0 to 23)
     */
    public static int getSlicePerm(RubiksCube cube) {
        int[] sliceEdges = new int[4];
        for (int i = 0; i < 4; i++) {
            int edge = getEdgeAtPosition(cube, i + 8); // Positions 8-11
            sliceEdges[i] = edge - 8;  // Normalize to 0-3
        }

        int perm = 0;
        for (int i = 0; i < 3; i++) {
            int count = 0;
            for (int j = i + 1; j < 4; j++) {
                if (sliceEdges[j] < sliceEdges[i]) count++;
            }
            perm = perm * (4 - i) + count;
        }
        return perm;
    }

    // ============================================================================
    // SECTION 11: MOVE TABLE GENERATION (Dynamic - using actual cube simulation)
    // ============================================================================

    /**
     * The 18 moves as characters for RubiksCube.applyMove()
     * Index: 0=U, 1=U2, 2=U', 3=R, 4=R2, 5=R', 6=F, 7=F2, 8=F',
     *        9=D, 10=D2, 11=D', 12=L, 13=L2, 14=L', 15=B, 16=B2, 17=B'
     */
    private static final char[][] MOVE_CHARS = {
        {'U'},           // 0: U
        {'U', 'U'},      // 1: U2
        {'u'},           // 2: U' (lowercase = CCW in RubiksCube)
        {'R'},           // 3: R
        {'R', 'R'},      // 4: R2
        {'r'},           // 5: R'
        {'F'},           // 6: F
        {'F', 'F'},      // 7: F2
        {'f'},           // 8: F'
        {'D'},           // 9: D
        {'D', 'D'},      // 10: D2
        {'d'},           // 11: D'
        {'L'},           // 12: L
        {'L', 'L'},      // 13: L2
        {'l'},           // 14: L'
        {'B'},           // 15: B
        {'B', 'B'},      // 16: B2
        {'b'}            // 17: B'
    };

    /**
     * Build all move tables by simulating actual moves on the cube.
     * This is more reliable than manually computing permutation tables.
     */
    private static void buildMoveTables() {
        buildTwistMoveTable();
        buildFlipMoveTable();
        buildSliceMoveTable();
        buildCornerPermMoveTable();
        buildUDEdgePermMoveTable();
        buildSlicePermMoveTable();
    }

    // Debug methods
    public static int twistMoveTable(int twist, int move) { return twistMove[twist][move]; }
    public static int flipMoveTable(int flip, int move) { return flipMove[flip][move]; }
    public static int getEdgeOrientationPublic(RubiksCube cube, int pos) { return getEdgeOrientation(cube, pos); }
    public static int getEdgeAtPositionPublic(RubiksCube cube, int pos) { return getEdgeAtPosition(cube, pos); }

    /**
     * Apply a move (by index) to a RubiksCube and return it.
     */
    public static void applyMoveByIndex(RubiksCube cube, int moveIndex) {
        for (char c : MOVE_CHARS[moveIndex]) {
            cube.applyMove(c);
        }
    }

    /**
     * Get the inverse of a move.
     * - CW (index % 3 == 0): inverse is CCW (index + 2)
     * - 180 (index % 3 == 1): inverse is itself
     * - CCW (index % 3 == 2): inverse is CW (index - 2)
     */
    private static int getInverseMove(int move) {
        int type = move % 3;
        if (type == 0) return move + 2;  // CW -> CCW
        if (type == 2) return move - 2;  // CCW -> CW
        return move;                      // 180 -> 180
    }

    /**
     * Build the twist move table by simulating moves.
     *
     * For each move, we need to know:
     * 1. Where each corner goes (permutation)
     * 2. How much each corner's orientation changes
     *
     * We derive this by applying the move to a solved cube and observing results.
     */
    private static void buildTwistMoveTable() {
        // OPTIMIZED: Use static CORNER_MOVE_PERM and CORNER_MOVE_ORIENT tables directly
        // No RubiksCube object creation - pure coordinate math
        for (int twist = 0; twist < N_TWIST; twist++) {
            int[] orient = decodeTwist(twist);

            for (int move = 0; move < N_MOVES; move++) {
                int[] newOrient = new int[8];
                for (int i = 0; i < 8; i++) {
                    int fromPos = CORNER_MOVE_PERM[move][i];
                    newOrient[i] = (orient[fromPos] + CORNER_MOVE_ORIENT[move][i]) % 3;
                }
                twistMove[twist][move] = (short) encodeTwist(newOrient);
            }
        }
    }

    private static int[] decodeTwist(int twist) {
        int[] orient = new int[8];
        int sum = 0;
        for (int i = 6; i >= 0; i--) {
            orient[i] = twist % 3;
            twist /= 3;
            sum += orient[i];
        }
        orient[7] = (3 - (sum % 3)) % 3;
        return orient;
    }

    private static int encodeTwist(int[] orient) {
        int twist = 0;
        for (int i = 0; i < 7; i++) {
            twist = twist * 3 + orient[i];
        }
        return twist;
    }

    /**
     * Build the flip move table by simulating moves on cubes.
     *
     * With position-based orientation, the orientation changes depend on complex
     * interactions between edge colors and position types. We compute the changes
     * dynamically by applying moves to a solved cube and checking orientations.
     */
    private static void buildFlipMoveTable() {
        // With piece-based orientation, the formula is:
        // newOrient[i] = oldOrient[fromPos] XOR physicalFlip[move][i]
        //
        // physicalFlip[m][i] = 1 if move m physically flips the edge at position i.
        // We compute this by applying each move to a solved cube and checking
        // which positions have orientation 1 afterward.

        // First, compute the physical flip for each move
        int[][] physicalFlip = new int[N_MOVES][12];
        for (int move = 0; move < N_MOVES; move++) {
            RubiksCube cube = new RubiksCube();
            applyMoveByIndex(cube, move);
            for (int i = 0; i < 12; i++) {
                physicalFlip[move][i] = getEdgeOrientation(cube, i);
            }
        }

        // Build the flip move table using the formula
        for (int flip = 0; flip < N_FLIP; flip++) {
            int[] orient = decodeFlip(flip);

            for (int move = 0; move < N_MOVES; move++) {
                int[] newOrient = new int[12];
                for (int i = 0; i < 12; i++) {
                    int fromPos = EDGE_MOVE_PERM[move][i];
                    // XOR old orientation with physical flip
                    newOrient[i] = orient[fromPos] ^ physicalFlip[move][i];
                }
                flipMove[flip][move] = (short) encodeFlip(newOrient);
            }
        }
    }

    private static int[] decodeFlip(int flip) {
        int[] orient = new int[12];
        int sum = 0;
        for (int i = 10; i >= 0; i--) {
            orient[i] = flip % 2;
            flip /= 2;
            sum += orient[i];
        }
        orient[11] = sum % 2;
        return orient;
    }

    private static int encodeFlip(int[] orient) {
        int flip = 0;
        for (int i = 0; i < 11; i++) {
            flip = flip * 2 + orient[i];
        }
        return flip;
    }

    /**
     * Build the slice move table using precomputed permutation table.
     * OPTIMIZED: Uses EDGE_MOVE_PERM directly.
     * No RubiksCube object creation - pure coordinate math.
     */
    private static void buildSliceMoveTable() {
        for (int slice = 0; slice < N_SLICE; slice++) {
            int[] positions = decodeSlice(slice);
            boolean[] hasSlice = new boolean[12];
            for (int p : positions) {
                hasSlice[p] = true;
            }

            for (int move = 0; move < N_MOVES; move++) {
                // After move, edge at position i came from EDGE_MOVE_PERM[move][i]
                boolean[] newHasSlice = new boolean[12];
                for (int i = 0; i < 12; i++) {
                    int fromPos = EDGE_MOVE_PERM[move][i];
                    newHasSlice[i] = hasSlice[fromPos];
                }

                int[] newPositions = new int[4];
                int count = 0;
                for (int i = 0; i < 12; i++) {
                    if (newHasSlice[i]) {
                        newPositions[count++] = i;
                    }
                }

                sliceMove[slice][move] = (short) encodeSlice(newPositions);
            }
        }
    }

    private static int[] decodeSlice(int slice) {
        int[] positions = new int[4];
        int k = 4;
        for (int n = 11; n >= 0 && k > 0; n--) {
            if (slice >= BINOMIAL[n][k]) {
                slice -= BINOMIAL[n][k];
                positions[4 - k] = n;
                k--;
            }
        }
        return positions;
    }

    private static int encodeSlice(int[] positions) {
        java.util.Arrays.sort(positions);
        return BINOMIAL[positions[0]][1] +
               BINOMIAL[positions[1]][2] +
               BINOMIAL[positions[2]][3] +
               BINOMIAL[positions[3]][4];
    }

    /**
     * Build the corner permutation move table by simulating moves.
     */
    private static void buildCornerPermMoveTable() {
        // OPTIMIZED: Uses CORNER_MOVE_PERM directly.
        // No RubiksCube object creation - pure coordinate math.
        for (int perm = 0; perm < N_CORNER_PERM; perm++) {
            int[] corners = decodePerm8(perm);

            for (int move = 0; move < N_MOVES; move++) {
                int[] newCorners = new int[8];
                for (int i = 0; i < 8; i++) {
                    int fromPos = CORNER_MOVE_PERM[move][i];
                    newCorners[i] = corners[fromPos];
                }

                cornerPermMove[perm][move] = encodePerm8(newCorners);
            }
        }
    }

    /**
     * Build the UD edge permutation move table.
     * OPTIMIZED: Uses EDGE_MOVE_PERM directly.
     */
    private static void buildUDEdgePermMoveTable() {
        for (int perm = 0; perm < N_UD_EDGE_PERM; perm++) {
            int[] edges = decodePerm8(perm);

            for (int move = 0; move < N_MOVES; move++) {
                int[] newEdges = new int[8];
                for (int i = 0; i < 8; i++) {
                    int fromPos = EDGE_MOVE_PERM[move][i];
                    if (fromPos < 8) {
                        newEdges[i] = edges[fromPos];
                    } else {
                        // Edge swapped with slice - shouldn't happen in G1 moves
                        newEdges[i] = edges[i];
                    }
                }

                udEdgePermMove[perm][move] = encodePerm8(newEdges);
            }
        }
    }

    /**
     * Build the slice permutation move table.
     * OPTIMIZED: Uses EDGE_MOVE_PERM directly.
     */
    private static void buildSlicePermMoveTable() {
        for (int perm = 0; perm < N_SLICE_PERM; perm++) {
            int[] sliceEdges = decodePerm4(perm);

            for (int m = 0; m < N_MOVES_P2; m++) {
                int move = PHASE2_MOVES[m];

                int[] newSliceEdges = new int[4];
                for (int i = 0; i < 4; i++) {
                    int fromPos = EDGE_MOVE_PERM[move][i + 8];
                    if (fromPos >= 8) {
                        newSliceEdges[i] = sliceEdges[fromPos - 8];
                    } else {
                        newSliceEdges[i] = sliceEdges[i];
                    }
                }

                slicePermMove[perm][m] = (short) encodePerm4(newSliceEdges);
            }
        }
    }

    private static int[] decodePerm8(int perm) {
        int[] result = new int[8];
        boolean[] used = new boolean[8];

        for (int i = 0; i < 8; i++) {
            int fact = FACTORIAL[7 - i];
            int idx = perm / fact;
            perm %= fact;

            int count = 0;
            for (int j = 0; j < 8; j++) {
                if (!used[j]) {
                    if (count == idx) {
                        result[i] = j;
                        used[j] = true;
                        break;
                    }
                    count++;
                }
            }
        }
        return result;
    }

    private static int encodePerm8(int[] perm) {
        int result = 0;
        for (int i = 0; i < 7; i++) {
            int count = 0;
            for (int j = i + 1; j < 8; j++) {
                if (perm[j] < perm[i]) count++;
            }
            result = result * (8 - i) + count;
        }
        return result;
    }

    private static int[] decodePerm4(int perm) {
        int[] result = new int[4];
        boolean[] used = new boolean[4];

        for (int i = 0; i < 4; i++) {
            int fact = FACTORIAL[3 - i];
            int idx = perm / fact;
            perm %= fact;

            int count = 0;
            for (int j = 0; j < 4; j++) {
                if (!used[j]) {
                    if (count == idx) {
                        result[i] = j;
                        used[j] = true;
                        break;
                    }
                    count++;
                }
            }
        }
        return result;
    }

    private static int encodePerm4(int[] perm) {
        int result = 0;
        for (int i = 0; i < 3; i++) {
            int count = 0;
            for (int j = i + 1; j < 4; j++) {
                if (perm[j] < perm[i]) count++;
            }
            result = result * (4 - i) + count;
        }
        return result;
    }

    // ============================================================================
    // SECTION 12: PRUNING TABLE GENERATION (BFS)
    // ============================================================================

    /**
     * Build all pruning tables using breadth-first search.
     */
    private static void buildPruneTables() {
        buildTwistSlicePruneTable();
        buildFlipSlicePruneTable();
        buildTwistFlipPruneTable();
        buildCornerSlicePruneTable();
        buildUDEdgeSlicePruneTable();
    }

    /**
     * Build twist × slice pruning table for Phase 1.
     *
     * Uses BFS starting from the goal state (twist=0, slice=0) to compute
     * the minimum number of moves to reach that state from any other state.
     */
    private static void buildTwistSlicePruneTable() {
        java.util.Arrays.fill(twistSlicePrune, (byte) -1);

        int[] queue = new int[N_TWIST * N_SLICE];
        int head = 0, tail = 0;

        // Start from goal: twist=0, slice=0 (slice edges in middle, corners oriented)
        int goalSlice = encodeSlice(new int[]{8, 9, 10, 11});
        queue[tail++] = 0 * N_SLICE + goalSlice;
        twistSlicePrune[0 * N_SLICE + goalSlice] = 0;

        while (head < tail) {
            int index = queue[head++];
            int twist = index / N_SLICE;
            int slice = index % N_SLICE;
            int depth = twistSlicePrune[index];

            // Try all 18 moves
            for (int move = 0; move < N_MOVES; move++) {
                int newTwist = twistMove[twist][move];
                int newSlice = sliceMove[slice][move];
                int newIndex = newTwist * N_SLICE + newSlice;

                if (twistSlicePrune[newIndex] == -1) {
                    twistSlicePrune[newIndex] = (byte)(depth + 1);
                    queue[tail++] = newIndex;
                }
            }
        }
    }

    /**
     * Build flip × slice pruning table for Phase 1.
     */
    private static void buildFlipSlicePruneTable() {
        java.util.Arrays.fill(flipSlicePrune, (byte) -1);

        int[] queue = new int[N_FLIP * N_SLICE];
        int head = 0, tail = 0;

        int goalSlice = encodeSlice(new int[]{8, 9, 10, 11});
        queue[tail++] = 0 * N_SLICE + goalSlice;
        flipSlicePrune[0 * N_SLICE + goalSlice] = 0;

        while (head < tail) {
            int index = queue[head++];
            int flip = index / N_SLICE;
            int slice = index % N_SLICE;
            int depth = flipSlicePrune[index];

            for (int move = 0; move < N_MOVES; move++) {
                int newFlip = flipMove[flip][move];
                int newSlice = sliceMove[slice][move];
                int newIndex = newFlip * N_SLICE + newSlice;

                if (flipSlicePrune[newIndex] == -1) {
                    flipSlicePrune[newIndex] = (byte)(depth + 1);
                    queue[tail++] = newIndex;
                }
            }
        }
    }

    /**
     * Build twist × flip pruning table for Phase 1.
     *
     * This table provides a better heuristic by combining twist and flip directly,
     * without considering slice. Goal: twist=0, flip=0.
     */
    private static void buildTwistFlipPruneTable() {
        java.util.Arrays.fill(twistFlipPrune, (byte) -1);

        int[] queue = new int[N_TWIST * N_FLIP];
        int head = 0, tail = 0;

        // Goal: twist=0, flip=0
        queue[tail++] = 0;  // index = twist * N_FLIP + flip = 0 * 2048 + 0 = 0
        twistFlipPrune[0] = 0;

        while (head < tail) {
            int index = queue[head++];
            int twist = index / N_FLIP;
            int flip = index % N_FLIP;
            int depth = twistFlipPrune[index];

            for (int move = 0; move < N_MOVES; move++) {
                int newTwist = twistMove[twist][move];
                int newFlip = flipMove[flip][move];
                int newIndex = newTwist * N_FLIP + newFlip;

                if (twistFlipPrune[newIndex] == -1) {
                    twistFlipPrune[newIndex] = (byte)(depth + 1);
                    queue[tail++] = newIndex;
                }
            }
        }
    }

    /**
     * Build corner perm × slice perm pruning table for Phase 2.
     */
    private static void buildCornerSlicePruneTable() {
        java.util.Arrays.fill(cornerSlicePrune, (byte) -1);

        int[] queue = new int[N_CORNER_PERM * N_SLICE_PERM];
        int head = 0, tail = 0;

        // Goal: cornerPerm=0, slicePerm=0
        queue[tail++] = 0;
        cornerSlicePrune[0] = 0;

        while (head < tail) {
            int index = queue[head++];
            int cornerPerm = index / N_SLICE_PERM;
            int slicePerm = index % N_SLICE_PERM;
            int depth = cornerSlicePrune[index];

            // Only Phase 2 moves
            for (int m = 0; m < N_MOVES_P2; m++) {
                int move = PHASE2_MOVES[m];
                int newCornerPerm = cornerPermMove[cornerPerm][move];
                int newSlicePerm = slicePermMove[slicePerm][m];
                int newIndex = newCornerPerm * N_SLICE_PERM + newSlicePerm;

                if (cornerSlicePrune[newIndex] == -1) {
                    cornerSlicePrune[newIndex] = (byte)(depth + 1);
                    queue[tail++] = newIndex;
                }
            }
        }
    }

    /**
     * Build UD edge perm × slice perm pruning table for Phase 2.
     */
    private static void buildUDEdgeSlicePruneTable() {
        java.util.Arrays.fill(udEdgeSlicePrune, (byte) -1);

        int[] queue = new int[N_UD_EDGE_PERM * N_SLICE_PERM];
        int head = 0, tail = 0;

        queue[tail++] = 0;
        udEdgeSlicePrune[0] = 0;

        while (head < tail) {
            int index = queue[head++];
            int udEdgePerm = index / N_SLICE_PERM;
            int slicePerm = index % N_SLICE_PERM;
            int depth = udEdgeSlicePrune[index];

            for (int m = 0; m < N_MOVES_P2; m++) {
                int move = PHASE2_MOVES[m];
                int newUdEdgePerm = udEdgePermMove[udEdgePerm][move];
                int newSlicePerm = slicePermMove[slicePerm][m];
                int newIndex = newUdEdgePerm * N_SLICE_PERM + newSlicePerm;

                if (udEdgeSlicePrune[newIndex] == -1) {
                    udEdgeSlicePrune[newIndex] = (byte)(depth + 1);
                    queue[tail++] = newIndex;
                }
            }
        }
    }

    // ============================================================================
    // SECTION 13: PHASE 1 SEARCH (IDA*)
    // ============================================================================

    /**
     * Phase 1: Find moves to reach G1 subgroup.
     *
     * G1 is defined by:
     * - twist = 0 (all corners correctly oriented)
     * - flip = 0 (all edges correctly oriented)
     * - slice = goalSlice (slice edges in middle slice positions 8-11)
     *
     * @param twist Initial twist coordinate
     * @param flip Initial flip coordinate
     * @param slice Initial slice coordinate
     * @param maxDepth Maximum search depth
     * @return Array of move indices, or null if not found
     */
    private static int[] searchPhase1(RubiksCube startCube, int maxDepth) {
        int goalSlice = encodeSlice(new int[]{8, 9, 10, 11});

        int twist = getTwist(startCube);
        int flip = getFlip(startCube);
        int slice = getSlice(startCube);

        // Get initial heuristic (max of all three pruning tables)
        int h1 = twistSlicePrune[twist * N_SLICE + slice];
        int h2 = flipSlicePrune[flip * N_SLICE + slice];
        int h3 = twistFlipPrune[twist * N_FLIP + flip];

        // Handle unreachable states
        if (h1 < 0) h1 = 0;
        if (h2 < 0) h2 = 0;
        if (h3 < 0) h3 = 0;

        int h = Math.max(Math.max(h1, h2), h3);

        System.out.println("Phase 1: twist=" + twist + ", flip=" + flip + ", slice=" + slice + ", h=" + h);

        // Iterative deepening
        for (int depth = h; depth <= maxDepth; depth++) {
            System.out.println("  Trying depth " + depth + "...");
            int[] solution = new int[depth];
            RubiksCube cube = startCube.copy();
            if (searchPhase1DFS(cube, twist, flip, slice, goalSlice, 0, depth, -1, solution)) {
                return java.util.Arrays.copyOf(solution, depth);
            }
        }
        return null;
    }

    /**
     * Phase 1 depth-first search with pruning.
     * Uses actual cube state to compute flip (since flip move table has issues with edge type mismatches).
     */
    private static boolean searchPhase1DFS(RubiksCube cube, int twist, int flip, int slice, int goalSlice,
                                            int g, int bound, int lastMove, int[] solution) {
        // Compute heuristic (max of all three pruning tables)
        int h1 = twistSlicePrune[twist * N_SLICE + slice];
        int h2 = flipSlicePrune[flip * N_SLICE + slice];
        int h3 = twistFlipPrune[twist * N_FLIP + flip];

        // Handle unreachable states
        if (h1 < 0) h1 = 0;
        if (h2 < 0) h2 = 0;
        if (h3 < 0) h3 = 0;

        int h = Math.max(Math.max(h1, h2), h3);

        // Prune if we can't reach the goal within bound
        if (g + h > bound) return false;

        // Check if goal reached
        if (twist == 0 && flip == 0 && slice == goalSlice) {
            return true;
        }

        // If we've reached the depth limit, can't continue
        if (g >= bound) return false;

        // Try all moves
        for (int move = 0; move < N_MOVES; move++) {
            if (!isValidPhase1Move(move, lastMove)) continue;

            // Apply move to cube
            applyMoveByIndex(cube, move);

            // Use move tables for twist and slice
            int newTwist = twistMove[twist][move];
            int newSlice = sliceMove[slice][move];
            // Compute flip from cube state (move table has issues with position-based orientation)
            int newFlip = getFlip(cube);

            solution[g] = move;

            if (searchPhase1DFS(cube, newTwist, newFlip, newSlice, goalSlice, g + 1, bound, move, solution)) {
                // Undo move before returning
                int inverse = getInverseMove(move);
                applyMoveByIndex(cube, inverse);
                return true;
            }

            // Undo move (apply inverse)
            int inverse = getInverseMove(move);
            applyMoveByIndex(cube, inverse);
        }

        return false;
    }

    /**
     * Check if a move is valid given the last move.
     *
     * We prune:
     * - Same face twice in a row (e.g., U then U2)
     * - Opposite faces in wrong order (enforce U before D, R before L, F before B)
     */
    private static boolean isValidPhase1Move(int move, int lastMove) {
        if (lastMove == -1) return true;

        int face = move / 3;      // 0=U, 1=R, 2=F, 3=D, 4=L, 5=B
        int lastFace = lastMove / 3;

        // Same face is not allowed
        if (face == lastFace) return false;

        // For opposite faces, enforce ordering (U<D, R<L, F<B)
        // face 0 (U) vs face 3 (D): allow U after D is disallowed -> face 0 after lastFace 3
        // But we want to DISALLOW the second of the pair
        if (face == 0 && lastFace == 3) return false;  // U after D
        if (face == 1 && lastFace == 4) return false;  // R after L
        if (face == 2 && lastFace == 5) return false;  // F after B

        return true;
    }

    // ============================================================================
    // SECTION 14: PHASE 2 SEARCH (IDA*)
    // ============================================================================

    /**
     * Phase 2: Solve from G1 to solved state.
     *
     * Uses only G1-preserving moves: U, U2, U', D, D2, D', R2, L2, F2, B2
     *
     * @param cornerPerm Corner permutation coordinate
     * @param udEdgePerm UD edge permutation coordinate
     * @param slicePerm Slice permutation coordinate
     * @param maxDepth Maximum search depth
     * @return Array of Phase 2 move indices, or null if not found
     */
    private static int[] searchPhase2(int cornerPerm, int udEdgePerm, int slicePerm, int maxDepth) {
        int h1 = cornerSlicePrune[cornerPerm * N_SLICE_PERM + slicePerm];
        int h2 = udEdgeSlicePrune[udEdgePerm * N_SLICE_PERM + slicePerm];

        // Handle unreachable states (table entry = -1)
        if (h1 < 0) h1 = 0;
        if (h2 < 0) h2 = 0;

        int h = Math.max(h1, h2);

        // Check if already solved
        if (cornerPerm == 0 && udEdgePerm == 0 && slicePerm == 0) {
            return new int[0];
        }

        for (int depth = Math.max(h, 1); depth <= maxDepth; depth++) {
            int[] solution = new int[depth];
            if (searchPhase2DFS(cornerPerm, udEdgePerm, slicePerm, 0, depth, -1, solution)) {
                return java.util.Arrays.copyOf(solution, depth);
            }
        }
        return null;
    }

    /**
     * Phase 2 depth-first search.
     */
    private static boolean searchPhase2DFS(int cornerPerm, int udEdgePerm, int slicePerm,
                                            int g, int bound, int lastMove, int[] solution) {
        // Check if goal reached
        if (cornerPerm == 0 && udEdgePerm == 0 && slicePerm == 0) {
            return true;
        }

        int h1 = cornerSlicePrune[cornerPerm * N_SLICE_PERM + slicePerm];
        int h2 = udEdgeSlicePrune[udEdgePerm * N_SLICE_PERM + slicePerm];

        // Handle unreachable states
        if (h1 < 0) h1 = 0;
        if (h2 < 0) h2 = 0;

        int h = Math.max(h1, h2);

        // Prune if we can't reach the goal within bound
        if (g + h > bound) return false;

        // If we've reached the depth limit, can't continue
        if (g >= bound) return false;

        for (int m = 0; m < N_MOVES_P2; m++) {
            if (!isValidPhase2Move(m, lastMove)) continue;

            int move = PHASE2_MOVES[m];
            int newCornerPerm = cornerPermMove[cornerPerm][move];
            int newUdEdgePerm = udEdgePermMove[udEdgePerm][move];
            int newSlicePerm = slicePermMove[slicePerm][m];

            solution[g] = m;

            if (searchPhase2DFS(newCornerPerm, newUdEdgePerm, newSlicePerm, g + 1, bound, m, solution)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Check if a Phase 2 move is valid.
     *
     * Phase 2 moves: 0=U, 1=U2, 2=U', 3=D, 4=D2, 5=D', 6=R2, 7=L2, 8=F2, 9=B2
     */
    private static boolean isValidPhase2Move(int move, int lastMove) {
        if (lastMove == -1) return true;

        // Get face from Phase 2 move index
        int face, lastFace;
        if (move < 3) face = 0;      // U
        else if (move < 6) face = 3; // D
        else if (move == 6) face = 1; // R
        else if (move == 7) face = 4; // L
        else if (move == 8) face = 2; // F
        else face = 5;                // B

        if (lastMove < 3) lastFace = 0;
        else if (lastMove < 6) lastFace = 3;
        else if (lastMove == 6) lastFace = 1;
        else if (lastMove == 7) lastFace = 4;
        else if (lastMove == 8) lastFace = 2;
        else lastFace = 5;

        if (face == lastFace) return false;

        // Opposite face ordering
        if (face == 0 && lastFace == 3) return false;
        if (face == 1 && lastFace == 4) return false;
        if (face == 2 && lastFace == 5) return false;

        return true;
    }

    // ============================================================================
    // SECTION 15: MAIN SOLVE METHOD
    // ============================================================================

    /**
     * Solve the Rubik's cube using the two-phase algorithm.
     *
     * @param cube The scrambled RubiksCube to solve
     * @return Solution string in the format "FFRRUUBBD..." (only uppercase)
     */
    public static String solve(RubiksCube cube) {
        // Initialize tables if not already done
        initTables();

        // Check if already solved
        if (cube.isSolved()) {
            return "";
        }

        // Make a working copy
        RubiksCube workCube = new RubiksCube(cube);

        // Get initial Phase 1 coordinates
        int twist = getTwist(workCube);
        int flip = getFlip(workCube);
        int slice = getSlice(workCube);
        int goalSlice = encodeSlice(new int[]{8, 9, 10, 11});

        int[] phase1Solution = new int[0];

        // Phase 1: Get to G1 subgroup
        if (twist != 0 || flip != 0 || slice != goalSlice) {
            phase1Solution = searchPhase1(workCube, 12);
            if (phase1Solution == null) {
                return "ERROR: Phase 1 failed";
            }

            // Apply Phase 1 moves to get the cube to G1 state
            for (int move : phase1Solution) {
                applyMoveToRubiksCube(workCube, move);
            }
        }

        // Get Phase 2 coordinates
        int cornerPerm = getCornerPerm(workCube);
        int udEdgePerm = getUDEdgePerm(workCube);
        int slicePerm = getSlicePerm(workCube);

        int[] phase2Solution = new int[0];

        // Phase 2: Solve completely
        if (cornerPerm != 0 || udEdgePerm != 0 || slicePerm != 0) {
            phase2Solution = searchPhase2(cornerPerm, udEdgePerm, slicePerm, 18);
            if (phase2Solution == null) {
                return "ERROR: Phase 2 failed";
            }
        }

        // Format solution
        return formatSolution(phase1Solution, phase2Solution);
    }

    /**
     * Apply a move (by index) to a RubiksCube.
     */
    private static void applyMoveToRubiksCube(RubiksCube cube, int moveIndex) {
        // Convert move index to char
        // 0=U, 1=U2, 2=U', 3=R, 4=R2, 5=R', etc.
        char[] faces = {'U', 'R', 'F', 'D', 'L', 'B'};
        int face = moveIndex / 3;
        int type = moveIndex % 3; // 0=CW, 1=180, 2=CCW

        char faceChar = faces[face];

        if (type == 0) {
            cube.applyMove(faceChar);
        } else if (type == 1) {
            cube.applyMove(faceChar);
            cube.applyMove(faceChar);
        } else {
            // Counter-clockwise = 3 clockwise
            cube.applyMove(faceChar);
            cube.applyMove(faceChar);
            cube.applyMove(faceChar);
        }
    }

    /**
     * Format the solution as a string of uppercase letters only.
     *
     * Each move becomes:
     * - Clockwise (index % 3 == 0): single letter, e.g., "F"
     * - 180 degrees (index % 3 == 1): double letter, e.g., "FF"
     * - Counter-clockwise (index % 3 == 2): triple letter, e.g., "FFF"
     */
    private static String formatSolution(int[] phase1, int[] phase2) {
        StringBuilder sb = new StringBuilder();

        char[] faces = {'U', 'R', 'F', 'D', 'L', 'B'};

        // Phase 1 moves
        for (int move : phase1) {
            int face = move / 3;
            int type = move % 3;
            char faceChar = faces[face];

            if (type == 0) {
                sb.append(faceChar);
            } else if (type == 1) {
                sb.append(faceChar).append(faceChar);
            } else {
                sb.append(faceChar).append(faceChar).append(faceChar);
            }
        }

        // Phase 2 moves (convert from Phase 2 index to face)
        for (int m : phase2) {
            int move = PHASE2_MOVES[m];
            int face = move / 3;
            int type = move % 3;
            char faceChar = faces[face];

            if (type == 0) {
                sb.append(faceChar);
            } else if (type == 1) {
                sb.append(faceChar).append(faceChar);
            } else {
                sb.append(faceChar).append(faceChar).append(faceChar);
            }
        }

        return sb.toString();
    }
}
