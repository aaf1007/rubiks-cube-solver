import java.io.IOException;
import rubikscube.IncorrectFormatException;
import rubikscube.RubiksCube;

public class TestRubiksCube {

    public static String stateInit =
                    "   OOO\n" +
                    "   OOO\n" +
                    "   OOO\n" +
                    "GGGWWWBBBYYY\n" +
                    "GGGWWWBBBYYY\n" +
                    "GGGWWWBBBYYY\n" +
                    "   RRR\n" +
                    "   RRR\n" +
                    "   RRR\n";

    public static String stateF =
                    "   OOO\n" +
                    "   OOO\n" +
                    "   GGG\n" +
                    "GGRWWWOBBYYY\n" +
                    "GGRWWWOBBYYY\n" +
                    "GGRWWWOBBYYY\n" +
                    "   BBB\n" +
                    "   RRR\n" +
                    "   RRR\n";

    public static String stateRighty =
                    "   OOG\n" +
                    "   OOW\n" +
                    "   OOW\n" +
                    "YGGWWRBBOYBB\n" +
                    "GGGWWOYBBYYY\n" +
                    "GGGWWWOBBYYY\n" +
                    "   RRB\n" +
                    "   RRR\n" +
                    "   RRR\n";

    public static void testDefaultConstructor() {
        RubiksCube cube = new RubiksCube();
        String state = cube.toString();
        if (state.equals(stateInit))
            System.out.println("testDefaultConstructor OK");
        else
            System.out.println("testDefaultConstructor ERROR");
    }


    public static void testOtherConstructor() {
        try {
            RubiksCube cube = new RubiksCube("cube_f.txt");
            String state = cube.toString();
            if (state.equals(stateF))
                System.out.println("testOtherConstructor OK");
            else
                System.out.println("testOtherConstructor ERROR");
        } catch (IOException e) {
            System.out.println("testOtherConstructor ERROR - could not open a file");
            return;
        } catch (IncorrectFormatException e) {
            System.out.println("testOtherConstructor ERROR - throw incorrect format exception");
            return;
        }
    }


    public static void testMoveF() {
        RubiksCube cube = new RubiksCube();
        cube.applyMoves("F");
        String state = cube.toString();
        if (state.equals(stateF))
            System.out.println("testMoveF OK");
        else
            System.out.println("testMoveF ERROR");
    }

    // starts with the default state and makes 4 lefts
    // this returns to the initial state
    public static void testMoveLLLL() {
        RubiksCube cube = new RubiksCube();
        cube.applyMoves("LLLL");
        String state = cube.toString();
        if (state.equals(stateInit))
            System.out.println("testMoveLLLL OK");
        else
            System.out.println("testMoveLLLL ERROR");
    }

    public static void testMoveRighty() {
        RubiksCube cube = new RubiksCube();
        cube.applyMoves("RURRRUUU");
        String state = cube.toString();
        if (state.equals(stateRighty))
            System.out.println("testMoveRighty OK");
        else
            System.out.println("testMoveRighty ERROR");
    }


    public static void testMoveSixRighty() {
        RubiksCube cube = new RubiksCube();
        cube.applyMoves("RURRRUUU".repeat(6));
        String state = cube.toString();
        if (state.equals(stateInit))
            System.out.println("testMoveSixRighty OK");
        else
            System.out.println("testMoveSixRighty ERROR");
    }


    public static void testOrder1() {
        if (RubiksCube.order("D") == 4)
            System.out.println("testOrder1 OK");
        else
            System.out.println("testOrder1 ERROR");
    }


    public static void testOrder2() {
        if (RubiksCube.order("RURRRUUU") == 6)
            System.out.println("testOrder2 OK");
        else
            System.out.println("testOrder2 ERROR");
    }




    public static void main(String[] args) {

        testDefaultConstructor();
        testOtherConstructor();

        testMoveF();
        testMoveLLLL();
        testMoveRighty();
        testMoveSixRighty();

        testOrder1();
        testOrder2();
    }
}
