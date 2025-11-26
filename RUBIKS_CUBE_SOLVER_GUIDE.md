# Rubik's Cube Solver - Complete Implementation Guide
## A Student's Journey to Building an IDA* Solver

**Author's Note**: This guide is designed to help you understand and implement a Rubik's Cube solver from the ground up. Read each section carefully, take notes, and make sure you understand the concepts before moving to implementation.

---

## Table of Contents
1. [Understanding the Problem](#1-understanding-the-problem)
2. [Why IDA* is Perfect for This](#2-why-ida-is-perfect-for-this)
3. [IDA* Algorithm Deep Dive](#3-ida-algorithm-deep-dive)
4. [Heuristic Design - The Key to Success](#4-heuristic-design---the-key-to-success)
5. [Architecture & Design](#5-architecture--design)
6. [Implementation Roadmap](#6-implementation-roadmap)
7. [Critical Optimizations](#7-critical-optimizations)
8. [Testing Strategy](#8-testing-strategy)
9. [Common Pitfalls](#9-common-pitfalls)
10. [Debugging Tips](#10-debugging-tips)

---

## 1. Understanding the Problem

### 1.1 What Are We Really Doing?

Think of the Rubik's Cube as a **graph**:
- **Nodes (States)**: Each possible configuration of the cube
- **Edges (Moves)**: Each legal move (F, B, R, L, U, D, and their inverses)
- **Start Node**: Your scrambled cube
- **Goal Node**: The solved state

**Your mission**: Find a path from start to goal.

### 1.2 How Big is This Problem?

The Rubik's Cube has approximately **43 quintillion** (43,252,003,274,489,856,000) possible states!

**Key Insight**: You CANNOT:
- Store all states in memory
- Use traditional BFS (would run out of memory immediately)
- Try random moves (would take forever)

**You CAN**:
- Use smart search with a good heuristic
- Prune unnecessary paths
- Use depth-first approaches that don't store everything

### 1.3 The Branching Factor

From any state, you can make:
- 6 face moves clockwise: F, B, R, L, U, D
- 6 face moves counter-clockwise: F', B', R', L', U', D'
- Total: **12 possible moves** per state

**Important Note**: Your professor allows counter-clockwise moves (U', F', etc.). These are essential for efficiency!

**Counter-Clockwise Implementation**:
- U' (counter-clockwise) = doing U three times (U U U)
- But for searching, treat U and U' as separate moves (better efficiency)

---

## 2. Why IDA* is Perfect for This

### 2.1 The Problem with Other Algorithms

**BFS (Breadth-First Search)**:
- ✅ Finds shortest path
- ❌ Memory explosion: At depth 7, you'd store ~35 million states!
- ❌ Not feasible for Rubik's Cube

**DFS (Depth-First Search)**:
- ✅ Low memory
- ❌ Might explore very deep paths unnecessarily
- ❌ No guarantee of finding shortest path
- ❌ Could take forever

**A\* Search**:
- ✅ Finds optimal path with good heuristic
- ❌ Stores all explored states in memory
- ❌ Memory problems like BFS

### 2.2 Enter IDA* (Iterative Deepening A*)

**IDA* combines the best of both worlds**:
- Memory efficient like DFS (only stores current path)
- Finds optimal solution like A* (with admissible heuristic)
- Uses iterative deepening to avoid infinite paths

**How it works** (high level):
1. Try to solve with a depth limit (based on heuristic)
2. If you don't find solution, increase the limit
3. Repeat until solved

**Memory usage**: Only O(d) where d = solution depth (maybe 20-30 moves)
**Time**: More than A*, but still reasonable with good heuristic

---

## 3. IDA* Algorithm Deep Dive

### 3.1 Core Concepts You Need to Understand

**f(n) = g(n) + h(n)**

- **g(n)**: Cost to reach current state (number of moves so far)
- **h(n)**: Estimated cost to reach goal (your heuristic)
- **f(n)**: Total estimated cost

**Threshold**: The maximum f(n) value we're willing to explore in current iteration

### 3.2 The Algorithm (Detailed Pseudocode)

**Note**: This version uses move pruning instead of state tracking for better performance within the 10-second time limit.

```
function IDA_Star_Solve(initialCube):
    bound = heuristic(initialCube)  // Initial threshold
    movePath = []                   // Track moves made (for solution)

    loop forever:
        result = search(initialCube, movePath, 0, bound, null)

        if result == FOUND:
            return movePath  // Solution found!

        if result == INFINITY:
            return FAILURE  // No solution exists

        bound = result  // Increase threshold to next f-value
    end loop
end function


function search(cube, movePath, g, bound, lastMove):
    f = g + heuristic(cube)

    // Prune: if f exceeds bound, don't explore further
    if f > bound:
        return f  // Return the f-value for next threshold

    // Goal test
    if cube.isSolved():
        return FOUND

    min = INFINITY  // Track minimum f-value beyond bound

    // Try all possible moves
    for each move in [F, F', B, B', R, R', L, L', U, U', D, D']:
        // Smart pruning: avoid redundant move sequences
        if NOT isRedundantMove(lastMove, move):

            newCube = cube.copy()
            newCube.applyMove(move)
            movePath.add(move)

            result = search(newCube, movePath, g + 1, bound, move)

            if result == FOUND:
                return FOUND

            if result < min:
                min = result  // Track best f-value found

            movePath.removeLast()  // Backtrack
        end if
    end for

    return min
end function


function isRedundantMove(lastMove, currentMove):
    if lastMove is null:
        return false  // First move, can't be redundant

    // Get face letters (without prime notation)
    lastFace = first character of lastMove
    currentFace = first character of currentMove

    // Rule 1: Don't apply same face twice in a row (F then F)
    // This includes F then F' (inverse moves)
    if lastFace == currentFace:
        return true

    // Rule 2: Maintain ordering for opposite faces
    // This prevents exploring both F,B and B,F (they commute)
    if (lastFace == 'F' and currentFace == 'B'):
        return true
    if (lastFace == 'L' and currentFace == 'R'):
        return true
    if (lastFace == 'U' and currentFace == 'D'):
        return true

    return false
end function
```

### 3.3 Understanding Each Part

**Initialization**:
```
bound = heuristic(initialCube)
```
- We start with the heuristic estimate as our first threshold
- If h(initial) = 15, we first try to solve in ≤15 moves

**Iterative Deepening**:
```
bound = result
```
- Each iteration, we increase the bound
- The new bound is the smallest f-value we saw that exceeded the old bound
- This ensures we explore in order of increasing f-values

**The f > bound check**:
```
if f > bound:
    return f
```
- This is the "cutoff" - we stop exploring this path
- But we return f so we know what threshold to try next
- This prevents exploring paths that are too long/costly

**Goal Test**:
```
if currentCube.isSolved():
    return FOUND
```
- Check if we've reached the solved state
- If yes, the `path` variable contains the solution!

**Move Ordering**:
```
for each move in [F, F', B, B', R, R', L, L', U, U', D, D']:
```
- Try all 12 possible moves
- You can optimize by ordering moves (try better ones first)

**Avoiding Redundancy**:
```
if move is not inverse of last move in path:
```
- Don't do U then U' (they cancel out)
- Huge optimization!

**Cycle Detection**:
```
if newCube not in path:
```
- Don't revisit states in current path
- Prevents infinite loops
- Only checks current path (not all visited states - too much memory!)

### 3.4 Key Insight: Why This Works

IDA* explores states in order of increasing f-value, just like A*. But instead of storing all states, it:
1. Explores depth-first up to a threshold
2. Remembers the minimum f-value that exceeded threshold
3. Increases threshold and tries again

**Memory**: Only stores current path (~30 states max)
**Completeness**: Will find solution if it exists
**Optimality**: Finds shortest solution IF heuristic is admissible

---

## 4. Heuristic Design - The Key to Success

### 4.1 What is a Heuristic?

A heuristic h(n) estimates the cost to reach the goal from state n.

**Critical Properties**:

1. **Admissible**: h(n) ≤ actual cost (never overestimate)
   - If admissible, IDA* finds optimal solution
   - If not admissible, finds a solution (maybe not shortest)

2. **Consistent**: h(n) ≤ cost(n, n') + h(n')
   - Not strictly required for IDA*, but helpful

3. **Informative**: Higher h(n) = fewer states explored
   - h(n) = 0 is admissible but useless (becomes DFS)
   - Better heuristic = faster search

**The Trade-off**:
- Complex heuristic = expensive to compute, but fewer states explored
- Simple heuristic = cheap to compute, but more states explored

### 4.2 Heuristic Options (Ranked by Difficulty)

#### Option 1: Misplaced Pieces (Simplest)

**Idea**: Count how many pieces are not in their solved position.

**Example**:
```
Solved:     Current:
  OOO         ORO
  OOO         OOO
  OOO         OOO
```
Top face has 1 piece out of place.

**Implementation**:
```
Count all pieces not matching solved state
Since one move can fix at most ~8 pieces,
h(n) = misplaced_count / 8
```

**Pros**:
- Very easy to implement
- Fast to compute
- Admissible

**Cons**:
- Not very informative (low h-values)
- Will explore many states
- Might be too slow for hard puzzles

#### Option 2: Sum of Manhattan Distances (Medium)

**Idea**: For each piece, calculate how many "moves" it needs to reach home position.

**Challenge with Rubik's Cube**:
- Pieces don't move independently
- Manhattan distance doesn't directly apply (this isn't a sliding puzzle)
- But we can approximate!

**Corner Piece Distance**:
- A corner has 8 possible positions
- Count how many moves to get from current position to home
- This is still an estimate (admissible if done carefully)

**Implementation Concept**:
```
For each corner piece:
    Find its current position
    Find its home position
    Estimate minimum moves needed
    Sum these up

Divide by number of pieces affected per move
```

**Pros**:
- More informative than misplaced pieces
- Still admissible if done right
- Reasonable compute time

**Cons**:
- More complex to implement
- Need to track piece positions and orientations

#### Option 3: Separate Corner and Edge Heuristics (Recommended)

**Idea**: Corners and edges move independently in some ways. Calculate separate heuristics and take the maximum.

**Why this works**:
- A move that fixes corners might mess up edges
- Need at least h_corners moves to fix corners
- Need at least h_edges moves to fix edges
- h(n) = max(h_corners, h_edges) is admissible!

**Corner Heuristic**:
```
Count corners not in correct position (ignoring orientation)
h_corners = misplaced_corners / 4
(4 corners affected per move on average)
```

**Edge Heuristic**:
```
Count edges not in correct position
h_edges = misplaced_edges / 4
(4 edges affected per move on average)
```

**Combined**:
```
h(n) = max(h_corners, h_edges)
```

**Pros**:
- More informative than simple misplaced count
- Still fast to compute
- Admissible
- Good balance of accuracy and speed

**Cons**:
- Need to identify and track corners vs edges
- More implementation work

### 4.3 Recommended Starting Point

**Start Simple, Then Improve**:

1. **Phase 1**: Implement simple misplaced pieces
   - Get IDA* working
   - Test on easy scrambles
   - Establish baseline

2. **Phase 2**: Enhance to corner/edge separation
   - More informative
   - Should handle harder scrambles
   - Stay under 10-second limit

3. **Phase 3** (if needed): Add orientation awareness
   - Track if pieces are flipped/twisted
   - Even more informative
   - Diminishing returns on complexity

### 4.4 How to Make Heuristic Admissible

**Golden Rule**: Never overestimate!

**Techniques**:
1. **Divide by max pieces affected**: If a move affects at most 8 pieces, divide by 8
2. **Use minimum possible moves**: Always round down
3. **Ignore obstacles**: Assume pieces can move to goal without interference
4. **Test**: Manually verify on small examples

**Example Calculation**:
```
Misplaced pieces: 24
Each move affects at most 8 pieces
h(n) = 24 / 8 = 3 moves minimum

Actual might need 5 moves, but h(n) = 3 is safe (admissible)
```

### 4.5 Implementing the Heuristic Function

**What you need**:

1. **Define solved state for pieces**:
   - Which corners belong where
   - Which edges belong where

2. **Extract current piece positions**:
   - Parse your 2D array
   - Identify each piece by its colors

3. **Compare and count**:
   - How many pieces are displaced
   - How many are incorrectly oriented (optional)

4. **Calculate estimate**:
   - Apply your formula
   - Return integer value

**Important**: Your heuristic function will be called MANY times (millions). It must be fast!

---

## 5. Architecture & Design

### 5.1 Class Structure

Here's what you need:

```
src/rubikscube/
├── RubiksCube.java          (you already have this)
├── Solver.java              (main class - required)
├── CubeState.java           (optional: wrapper for state)
└── IncorrectFormatException.java (you already have this)
```

**Option 1: Minimal** (simpler)
- Just modify RubiksCube.java to add counter-clockwise moves
- Implement everything in Solver.java

**Option 2: Clean Separation** (recommended)
- Keep RubiksCube.java as-is for cube mechanics
- Create CubeState.java for state representation
- Solver.java contains IDA* algorithm

### 5.2 What Each Class Does

#### RubiksCube.java (Extend Your Existing)

**Current**:
```java
public void applyMoves(String moves)  // "FRBUD"
```

**Need to add**:
```java
// Apply single move including counter-clockwise
public void applyMove(char move)  // 'F' or use "F'" as 2-char

// Or better:
public void applyMove(String move)  // "F", "F'", "U", "U'", etc.

// Copy constructor for creating new states
public RubiksCube(RubiksCube other)

// Deep copy method
public RubiksCube copy()

// Fast equality check
@Override
public boolean equals(Object obj)

@Override
public int hashCode()
```

**How to implement counter-clockwise**:
```
F' = apply F three times (F F F)
R' = apply R three times (R R R)
etc.
```

#### Solver.java (Main IDA* Logic)

```java
package rubikscube;

public class Solver {
    private RubiksCube initialCube;
    private String solution;

    // Constructor
    public Solver(String inputFile) throws IOException, IncorrectFormatException

    // Main IDA* algorithm
    private String solve()

    // Recursive search function
    private int search(List<String> path, int g, int bound)

    // Heuristic function
    private int heuristic(RubiksCube cube)

    // Helper: is move redundant?
    private boolean isRedundantMove(String lastMove, String newMove)

    // Main method
    public static void main(String[] args)
}
```

### 5.3 State Representation

**Option 1**: Use RubiksCube directly
- Each state is a RubiksCube object
- Use copy() to create new states
- Simple but might be memory-heavy

**Option 2**: Use String representation
- Each state is the toString() output
- Very fast to compare
- Less memory
- But recreating cube from string each time is expensive

**Recommendation**: Use RubiksCube objects, but implement efficient equals() and copy().

### 5.4 Move Representation

**Define your move set**:
```java
private static final String[] MOVES = {
    "F", "F'",
    "B", "B'",
    "R", "R'",
    "L", "L'",
    "U", "U'",
    "D", "D'"
};
```

**Track path as**:
```java
List<String> path = new ArrayList<>();
path.add("F");
path.add("R'");
path.add("U");
// etc.
```

### 5.5 Data Structures You'll Need

1. **Current Path** (for IDA*):
   ```java
   List<String> movePath = new ArrayList<>();
   ```

2. **Cube States** (for cycle detection):
   ```java
   Set<RubiksCube> pathStates = new HashSet<>();
   // Only stores states in current path, not all visited
   ```

3. **Move Moves** (for solution):
   ```java
   StringBuilder solutionMoves = new StringBuilder();
   ```

---

## 6. Implementation Roadmap

### Phase 1: Extend RubiksCube for Counter-Clockwise Moves

**Goal**: Add support for F', B', R', L', U', D' moves

**Steps**:
1. Modify `applyMove()` or create new method that handles 2-character strings
2. Implement: If move is "X'", apply "X" three times
3. Add `copy()` method for deep copying
4. Add `equals()` and `hashCode()` for state comparison
5. Test thoroughly!

**Testing**:
```java
RubiksCube cube = new RubiksCube();
cube.applyMove("U");
cube.applyMove("U'");
// Should be back to solved state
assert cube.isSolved();
```

### Phase 2: Implement Simple Heuristic

**Goal**: Create a working heuristic function

**Start with**: Misplaced pieces heuristic

**Steps**:
1. Create method `countMisplacedPieces(RubiksCube cube)`
2. Compare each position to SOLVED_STATE
3. Count differences
4. Divide by 8 (or 7 to be safe)
5. Return integer

**Testing**:
```java
RubiksCube solved = new RubiksCube();
assert heuristic(solved) == 0;

RubiksCube oneMoveScrambled = new RubiksCube();
oneMoveScrambled.applyMove("U");
assert heuristic(oneMoveScrambled) >= 1;  // Should be at least 1
```

### Phase 3: Implement Basic IDA* (No Optimizations)

**Goal**: Get a working IDA* that can solve easy puzzles

**Steps**:
1. Create `solve()` method with iterative deepening loop
2. Create `search()` recursive method
3. Implement basic pruning (f > bound)
4. Implement goal test
5. Try all 12 moves
6. Track path
7. Backtrack

**Testing**:
```java
// Test on solved cube
RubiksCube solved = new RubiksCube();
assert solve(solved).equals("");

// Test on 1-move scramble
RubiksCube oneMove = new RubiksCube();
oneMove.applyMove("F");
String solution = solve(oneMove);
// Solution should be "F'" or "FFF"
```

### Phase 4: Add Redundancy Pruning

**Goal**: Avoid exploring useless moves

**Optimizations**:
1. Don't do U followed by U' (inverses cancel)
2. Don't do U followed by U U (can be replaced by U')
3. Don't do F followed by F followed by F followed by F (back to start)

**Implementation**:
```java
private boolean isRedundantMove(String lastMove, String newMove) {
    // If opposite faces, not redundant
    // If same face, check for redundancy
    // Opposite moves: F and F', U and U'
    // Same move 4 times: wasteful
}
```

### Phase 5: Improve Heuristic (If Needed)

**Goal**: Make search faster with better heuristic

**Enhancement**:
- Implement corner/edge separation
- Add orientation checking
- Test performance improvement

**Measure**:
- Count nodes explored
- Time to solve
- Compare old vs new heuristic

### Phase 6: Build Solver Main Class

**Goal**: Create the command-line interface

**Requirements**:
```java
public static void main(String[] args) {
    if (args.length != 2) {
        System.err.println("Usage: java rubikscube.Solver input.txt output.txt");
        System.exit(1);
    }

    String inputFile = args[0];
    String outputFile = args[1];

    // Read input
    // Solve
    // Write output
}
```

**Steps**:
1. Read scrambled cube from input file
2. Call IDA* solve()
3. Write solution moves to output file
4. Handle exceptions gracefully

### Phase 7: Testing on Provided Inputs

**Goal**: Verify correctness and performance

**Test on**:
1. 10 provided test files
2. 3 example solutions (verify your output matches or is also correct)
3. Measure timing
4. Ensure all complete in <10 seconds

### Phase 8: Final Optimizations (If Needed)

**If some puzzles timeout**:
1. Profile your code (find slow parts)
2. Optimize heuristic calculation
3. Optimize state comparison
4. Consider move ordering
5. Reduce object creation

---

## 7. Critical Optimizations

### 7.1 Pruning Redundant Moves

**Problem**: With 12 moves, branching factor is huge.

**Solution**: Don't try moves that undo previous moves.

**Example**:
```
Path: [F, R, U, ...]
Don't try: F' (undoes previous F)
```

**Implementation**:
```java
private boolean isInverse(String move1, String move2) {
    // F and F' are inverses
    // U and U' are inverses
    if (move1.charAt(0) == move2.charAt(0)) {
        // Same face
        if (move1.length() != move2.length()) {
            // One is prime, one isn't
            return true;
        }
    }
    return false;
}
```

**Advanced**: Don't do same face moves in certain patterns
```
F F F F = identity (back to start) - wasteful
F F = could be done as F' F' in reverse
```

### 7.2 Move Ordering

**Idea**: Try promising moves first.

**Heuristic-based ordering**:
```java
// For each move, apply it and calculate h(resulting state)
// Sort moves by h-value (lower = better)
// Try in that order
```

**Benefit**: Find solution faster in depth-first search.

**Cost**: Computing h() for all 12 moves is expensive.

**Verdict**: Probably not worth it for IDA* (time vs benefit).

### 7.3 Efficient State Comparison

**Problem**: Checking `if newCube not in path` is expensive if path is a List.

**Solution 1**: Use HashSet
```java
Set<RubiksCube> pathStates = new HashSet<>();
```
Requires good `equals()` and `hashCode()` implementation!

**Solution 2**: Use String representation
```java
Set<String> pathStates = new HashSet<>();
pathStates.contains(cube.toString());
```
Very fast comparison, but toString() must be consistent.

### 7.4 Reduce Object Creation

**Problem**: Creating new RubiksCube objects is expensive.

**Solution**: Reuse objects
```java
// Instead of:
RubiksCube newCube = currentCube.copy();
newCube.applyMove(move);

// Consider:
currentCube.applyMove(move);
int result = search(...);
currentCube.undoMove(move);  // Undo the move
```

Requires implementing `undoMove()` method.

**Undo implementation**:
```
Undo F by doing F' (or F F F)
Undo F' by doing F
```

### 7.5 Early Goal Testing

**Problem**: Checking isSolved() after every move.

**Optimization**: Check before recursive call
```java
if newCube.isSolved():
    // Found solution!
```

This avoids one extra recursion level.

### 7.6 Bound Management

**Careful with threshold updates**:
```java
// Track minimum f-value seen beyond bound
int min = Integer.MAX_VALUE;

for each move:
    int result = search(...);
    if (result < min):
        min = result

return min;  // Next threshold
```

This ensures optimal next bound selection.

---

## 8. Testing Strategy

### 8.1 Unit Testing

**Test RubiksCube extensions**:
```java
// Test counter-clockwise moves
RubiksCube cube = new RubiksCube();
String before = cube.toString();
cube.applyMove("F");
cube.applyMove("F'");
String after = cube.toString();
assert before.equals(after) : "F followed by F' should return to original";
```

**Test copy**:
```java
RubiksCube original = new RubiksCube();
original.applyMove("R");
RubiksCube copied = original.copy();
copied.applyMove("U");
// original and copied should be different
assert !original.equals(copied);
```

**Test heuristic**:
```java
RubiksCube solved = new RubiksCube();
assert heuristic(solved) == 0 : "Solved cube should have h=0";

RubiksCube scrambled = new RubiksCube();
scrambled.applyMoves("FRUBLD");
assert heuristic(scrambled) > 0 : "Scrambled cube should have h>0";
```

### 8.2 Integration Testing

**Test IDA* on known examples**:

1. **0-move solution** (already solved):
   ```java
   RubiksCube solved = new RubiksCube();
   String solution = solve(solved);
   assert solution.equals("") : "Solved cube needs 0 moves";
   ```

2. **1-move solution**:
   ```java
   RubiksCube cube = new RubiksCube();
   cube.applyMove("U");
   String solution = solve(cube);
   // Apply solution and check if solved
   RubiksCube test = new RubiksCube();
   test.applyMove("U");
   test.applyMoves(solution);
   assert test.isSolved() : "Solution should solve the cube";
   ```

3. **2-move solution**:
   ```java
   RubiksCube cube = new RubiksCube();
   cube.applyMoves("UR");
   String solution = solve(cube);
   // Verify solution works
   ```

### 8.3 Validation Against Examples

**Use provided solutions**:
1. Read one of the 3 example input files
2. Read corresponding solution file
3. Apply their solution to the scrambled cube
4. Verify it solves

```java
RubiksCube scrambled = new RubiksCube("input1.txt");
String exampleSolution = readSolutionFile("solution1.txt");
scrambled.applyMoves(exampleSolution);
assert scrambled.isSolved() : "Example solution should work";
```

Then run your solver:
```java
RubiksCube test = new RubiksCube("input1.txt");
String yourSolution = solve(test);
test.applyMoves(yourSolution);
assert test.isSolved() : "Your solution should work";
```

### 8.4 Performance Testing

**Measure time**:
```java
long startTime = System.currentTimeMillis();
String solution = solve(cube);
long endTime = System.currentTimeMillis();
long duration = endTime - startTime;
System.out.println("Solved in " + duration + "ms");
```

**Set timeout**:
```java
// Simple timeout check (not perfect, but helps)
private long startTime;
private static final long TIMEOUT = 9000; // 9 seconds

private int search(...) {
    if (System.currentTimeMillis() - startTime > TIMEOUT) {
        throw new TimeoutException();
    }
    // ... rest of search
}
```

### 8.5 Incremental Difficulty Testing

**Start easy, build up**:

1. Solved cube (0 moves)
2. One move scramble (1 move solution)
3. Two move scramble (2 move solution)
4. Three move scramble
5. Provided test files (unknown difficulty)

**Track**:
- Solution length
- Time taken
- Nodes explored (add counter in search)

---

## 9. Common Pitfalls

### 9.1 Infinite Loops

**Symptom**: Search never terminates.

**Causes**:
1. Not checking f > bound (search goes infinitely deep)
2. Not detecting cycles (revisiting same state)
3. Incorrect bound update

**Fix**:
```java
// Always check bound
if (f > bound) {
    return f;  // MUST return here!
}

// Always check cycles
if (pathStates.contains(newCube)) {
    continue;  // Skip this move
}

// Always update bound correctly
bound = searchResult;  // Next iteration
```

### 9.2 Incorrect Deep Copy

**Symptom**: Modifying state affects other states.

**Cause**: Shallow copy of 2D array.

**Wrong**:
```java
public RubiksCube copy() {
    RubiksCube newCube = new RubiksCube();
    newCube.cube = this.cube;  // Shallow copy - BAD!
    return newCube;
}
```

**Right**:
```java
public RubiksCube copy() {
    RubiksCube newCube = new RubiksCube();
    newCube.cube = new char[9][12];
    for (int i = 0; i < 9; i++) {
        for (int j = 0; j < 12; j++) {
            newCube.cube[i][j] = this.cube[i][j];
        }
    }
    return newCube;
}
```

Or use:
```java
for (int i = 0; i < 9; i++) {
    System.arraycopy(this.cube[i], 0, newCube.cube[i], 0, 12);
}
```

### 9.3 Incorrect Heuristic

**Symptom**: Not finding optimal solution, or not finding solution at all.

**Cause**: Heuristic overestimates (not admissible).

**Test**:
```java
// For solved cube, h must be 0
RubiksCube solved = new RubiksCube();
assert heuristic(solved) == 0;

// For one-move-away cube, h must be ≤ 1
RubiksCube oneMove = new RubiksCube();
oneMove.applyMove("F");
assert heuristic(oneMove) <= 1;  // Must not overestimate!
```

**Fix**: Always round down, divide by reasonable number.

### 9.4 Off-by-One in Depth/Bound

**Symptom**: Solution too long or missing.

**Cause**: Counting moves incorrectly.

**Check**:
- Is g incremented for each move?
- Is initial bound set correctly?
- Is goal test done before or after incrementing g?

### 9.5 String Comparison Issues

**Symptom**: States don't compare correctly.

**Cause**: Inconsistent toString() output.

**Fix**: Make sure toString() always produces identical output for identical states.

**Test**:
```java
RubiksCube cube1 = new RubiksCube();
RubiksCube cube2 = new RubiksCube();
assert cube1.toString().equals(cube2.toString());
```

### 9.6 Memory Issues

**Symptom**: OutOfMemoryError

**Cause**: Storing too many states.

**Fix**:
- IDA* should only store current path, not all visited states
- Check you're using Set for path states only, not global visited states
- Verify you're removing states when backtracking

### 9.7 Not Handling Counter-Clockwise

**Symptom**: Solutions are very long (many moves).

**Cause**: Only using clockwise moves.

**Fix**: Implement F', B', R', L', U', D' as proper moves.

---

## 10. Debugging Tips

### 10.1 Trace Your Search

**Add print statements**:
```java
private int search(List<String> path, int g, int bound) {
    System.out.println("Depth: " + g + ", Bound: " + bound + ", Path: " + path);
    // ... rest of code
}
```

For small examples, this shows the search tree.

### 10.2 Visualize the Cube

**Print current state**:
```java
System.out.println("Current state:");
System.out.println(currentCube);
System.out.println("Heuristic: " + heuristic(currentCube));
```

Compare to expected state.

### 10.3 Count Nodes Explored

**Add counter**:
```java
private int nodesExplored = 0;

private int search(...) {
    nodesExplored++;
    // ... rest of code
}

// After solving:
System.out.println("Nodes explored: " + nodesExplored);
```

**Use this to**:
- Compare heuristics (lower nodes = better heuristic)
- Understand why some puzzles are slow

### 10.4 Verify Each Move

**Test moves individually**:
```java
RubiksCube cube = new RubiksCube();
System.out.println("Initial:\n" + cube);

cube.applyMove("F");
System.out.println("After F:\n" + cube);

cube.applyMove("F'");
System.out.println("After F':\n" + cube);

assert cube.isSolved() : "Should be solved again";
```

### 10.5 Check Admissibility

**For several scrambles**:
```java
RubiksCube cube = new RubiksCube();
cube.applyMoves("FRU");  // 3 moves to scramble

int h = heuristic(cube);
// h should be ≤ 3 (since we know a 3-move solution exists: U' R' F')
assert h <= 3 : "Heuristic overestimating!";
```

### 10.6 Use Assertions Liberally

```java
assert bound > 0 : "Bound should be positive";
assert !path.isEmpty() : "Path should not be empty";
assert heuristic(new RubiksCube()) == 0 : "Solved cube has h=0";
```

Run with: `java -ea rubikscube.Solver input.txt output.txt`
(The -ea flag enables assertions)

### 10.7 Start Small

**Don't test on hard puzzles first!**

1. Test on solved cube
2. Test on 1-move scramble
3. Test on 2-move scramble
4. Gradually increase difficulty

This helps isolate where things break.

---

## 11. Step-by-Step Implementation Checklist

Use this as your guide:

### Week 1: Foundation
- [ ] Understand IDA* algorithm conceptually
- [ ] Study heuristic options
- [ ] Extend RubiksCube with counter-clockwise moves
- [ ] Implement copy() method
- [ ] Implement equals() and hashCode()
- [ ] Test all RubiksCube modifications thoroughly

### Week 2: Core Algorithm
- [ ] Implement basic heuristic (misplaced pieces)
- [ ] Create Solver.java skeleton
- [ ] Implement IDA* solve() method (outer loop)
- [ ] Implement search() method (recursive DFS)
- [ ] Test on solved cube (should return empty solution)
- [ ] Test on 1-move scramble
- [ ] Test on 2-move scramble

### Week 3: Optimization & Testing
- [ ] Add redundant move pruning
- [ ] Improve heuristic (corner/edge separation)
- [ ] Test on provided input files
- [ ] Measure performance
- [ ] Optimize slow parts
- [ ] Verify all solutions are correct
- [ ] Ensure all solve in <10 seconds

### Week 4: Documentation & Polish
- [ ] Write documentation PDF
- [ ] Explain your approach
- [ ] Document heuristic choices
- [ ] Compare different approaches tried
- [ ] Clean up code
- [ ] Add comments
- [ ] Create rubikscube.zip
- [ ] Final testing

---

## 12. Key Takeaways

### Core Concepts to Understand

1. **State Space Search**: The cube is a graph, we're finding a path.

2. **IDA***: Combines depth-first search with iterative deepening and heuristic guidance.

3. **Heuristic**: Estimates cost to goal. Must be admissible (never overestimate) for optimal solution.

4. **Pruning**: Eliminate redundant moves to reduce search space.

5. **Optimization**: Balance heuristic complexity vs computation time.

### Success Criteria

✅ Solves all test cases correctly
✅ All solutions complete within 10 seconds
✅ Code is clean and understandable
✅ Documentation explains your choices
✅ You understand what you implemented!

---

## 13. Questions to Ask Yourself

As you implement, constantly check:

1. **Does my heuristic ever overestimate?**
   - Test on known examples
   - If h(cube) > actual distance, it's not admissible

2. **Am I storing too much in memory?**
   - IDA* should use O(d) memory, not O(b^d)
   - Only store current path, not all visited states

3. **Are my moves correct?**
   - Test each move and its inverse
   - Verify F followed by F' returns to original state

4. **Is my search exploring efficiently?**
   - Count nodes explored
   - Higher heuristic = fewer nodes

5. **Can I solve simple cases?**
   - If 1-move scramble fails, fix before trying harder ones

6. **Am I avoiding redundant work?**
   - Don't do U then U'
   - Don't revisit states in current path

---

## 14. Final Encouragement

This project is challenging but incredibly rewarding! You're implementing a real AI search algorithm to solve a classic puzzle.

**Remember**:
- Start simple, build incrementally
- Test continuously
- Understand each piece before moving on
- Don't hesitate to revisit earlier phases if needed
- The professor said you can get 100% without solving everything - don't stress!

**When you get stuck**:
1. Go back to the algorithm pseudocode
2. Trace through a small example by hand
3. Add print statements to see what's happening
4. Test smaller, simpler cases
5. Ask for help (from me, your professor, or TAs)

**Most importantly**: Learn and enjoy the process! This is what computer science is all about - solving hard problems with clever algorithms.

Good luck! 🎲

---

## Appendix: Quick Reference

### IDA* at a Glance
```
bound = h(initial)
loop:
    result = search(path, 0, bound)
    if result == FOUND: return solution
    bound = result
```

### Search at a Glance
```
if f > bound: return f
if solved: return FOUND
min = ∞
for each move:
    if not redundant:
        apply move
        result = search(...)
        if result == FOUND: return FOUND
        min = minimum(min, result)
        undo move
return min
```

### Heuristic at a Glance
```
count = misplaced pieces
return count / 8
```

or

```
h_corners = misplaced corners / 4
h_edges = misplaced edges / 4
return max(h_corners, h_edges)
```

### Move Set
```
F, F', B, B', R, R', L, L', U, U', D, D'
```

### Redundancy Check
```
Don't do X followed by X'
Don't do X four times in a row
```

---

*End of Guide*
