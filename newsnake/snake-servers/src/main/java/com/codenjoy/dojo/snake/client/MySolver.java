package com.codenjoy.dojo.snake.client;

import com.codenjoy.dojo.client.Solver;
import com.codenjoy.dojo.services.Direction;
import com.codenjoy.dojo.services.Point;
import com.codenjoy.dojo.services.PointImpl;
import com.codenjoy.dojo.snake.model.Elements;
import com.codenjoy.dojo.utils.JsonUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class MySolver implements Solver<Board> {
    public String makeDecision(Board board) {
        Lee snakeLeeApple = new Lee(board.size(), board.size());
        Lee snakeLeeStone = new Lee(board.size(), board.size());
        Lee snakeLeeTail = new Lee(board.size(), board.size());
        Point head = board.getHead();
        List<Point> obstacles = findObstaclesOnBoard(board, board.getWalls(), board.getSnake(), board.getStones());
        List<Point> obstaclesNoStone = findObstaclesOnBoard(board, board.getWalls(), board.getSnake());
        List<Point> obstaclesNoTail = findObstaclesOnBoard(board, board.getWalls(), board.getSnake().subList(0, board.getSnake().size() - 1), board.getStones());
        List<Point> emptyCellsWithAppleAndStone = getEmptyCellsWithAppleAndStone(board);
        List<Point> emptyCellsWithApple = getEmptyCellsWithApple(board);
        Optional<List<Point>> traceToTail = snakeLeeTail.trace(head, tail(board), obstaclesNoTail);
        Optional<List<Point>> traceToStone = snakeLeeStone.trace(head, board.getStones().get(0), obstaclesNoStone);
        Optional<List<Point>> traceToApple = snakeLeeApple.trace(head, board.getApples().get(0), obstacles);

        // Build sets of reachable cells for each goal
        Set<Point> reachableCellsApple = traceToApple.orElse(Collections.emptyList()).stream().collect(Collectors.toSet());
        Set<Point> reachableCellsStone = traceToStone.orElse(Collections.emptyList()).stream().collect(Collectors.toSet());
        Set<Point> reachableCellsTail = traceToTail.orElse(Collections.emptyList()).stream().collect(Collectors.toSet());

        if (emptyCellsWithAppleAndStone.isEmpty()) {
            System.out.println("To make decision: CHASE TAIL");
            // No empty cells available, chase a tail
            return snakeDirection(traceToTail);
        }

        for (Point cell : emptyCellsWithApple) {
            boolean isReachableApple = reachableCellsApple.contains(cell);
            boolean isReachableStone = reachableCellsStone.contains(cell);
            boolean isReachableTail = reachableCellsTail.contains(cell);
            boolean hasApple = board.getApples().contains(cell);
            boolean hasStone = board.getStones().contains(cell);

            // Print debugging information
//            System.out.println("Cell: " + cell + ", isReachableTail: " + isReachableTail +
//                    ", isReachableApple: " + isReachableApple + ", isReachableStone : " + isReachableStone +
//                    ", hasApple: " + hasApple + ", hasStone: " + hasStone);

            // Decision tree logic
            if (board.getSnake().size() >= 50 && hasStone) {
                System.out.println("Snake size >= 70, eating stone at: " + cell);
                return moveSnake(head, cell);
            }
            else if (isReachableApple && isReachableStone && isReachableTail && hasApple) {
                System.out.println("Current CELL, and isReachableApple && isReachableStone && isReachableTail && hasApple: " + cell);
                return moveSnake(head, cell);
            }
            else if (isReachableApple && !isReachableStone && isReachableTail && traceToTail.get().size()>21 && hasApple) {
                System.out.println("Current CELL, and isReachableApple && !isReachableStone && isReachableTail && traceToTail.get().size()>21 && hasApple: " + cell);
                return snakeDirection(traceToTail);
            }
            else if (!isReachableApple && isReachableStone && isReachableTail && traceToTail.get().size()>21 && hasStone) {
                System.out.println("Current CELL, and !isReachableApple && isReachableStone && isReachableTail && traceToTail.get().size()>21 && hasStone: " + cell);
                return snakeDirection(traceToTail);
            }
            else if (!isReachableApple && isReachableStone && isReachableTail && hasStone) {
                System.out.println("Current CELL, and !isReachableApple && isReachableStone && isReachableTail && hasStone: " + cell);
                return moveSnake(head, cell);
            }
            else if (!isReachableApple && isReachableStone && !isReachableTail && hasStone) {
                System.out.println("Current CELL, and !isReachableApple && isReachableStone && !isReachableTail && hasStone: " + cell);
                return moveSnake(head, cell);
            }
            else if (!isReachableApple && !isReachableStone && !isReachableTail && hasStone) {
                System.out.println("Current CELL, and !isReachableApple && !isReachableStone && !isReachableTail && hasStone: " + cell);
                return moveTowardsMoreEmptyCells(board);
            }
            else if (!isReachableApple && !isReachableStone && !isReachableTail && hasApple) {
                System.out.println("Current CELL, and !isReachableApple && !isReachableStone && !isReachableTail && hasApple: " + cell);
                return moveTowardsMoreEmptyCells(board);
            }
            else if (isOnlyOptionToChaseTail(isReachableTail, isReachableApple, isReachableStone, emptyCellsWithApple)) {
                System.out.println("Current CELL, and isOnlyOptionToChaseTail(isReachableTail, isReachableApple, isReachableStone, emptyCellsWithApple: " + cell);
                return snakeDirection(traceToTail);
            }

            // Add more conditions 
        }

        System.out.println("To make decision: Move to an area with more empty cells");
        return snakeDirection(traceToApple);
    }
    private boolean isOnlyOptionToChaseTail(boolean isReachableTail, boolean isReachableApple, boolean isReachableStone, List<Point> emptyCellsWithApple) {
        return isReachableTail && !isReachableApple && !isReachableStone && emptyCellsWithApple.size() == 1;
    }
    private String moveTowardsMoreEmptyCells(Board board) {
        Point head = board.getHead();
        List<Point> emptyCells = getEmptyCells(board);
        Lee explorationLee = new Lee(board.size(), board.size());
        Point found = findNearestOpenSpace(head, emptyCells);
        // Remove the found open space from the list
        emptyCells.remove(found);
        Optional<List<Point>> traceToOpenSpace = explorationLee.trace(head, found, emptyCells);
//        System.out.println("traceToOpenSpace "+traceToOpenSpace);
//        System.out.println("emptyCells "+findNearestOpenSpace(head, emptyCells));
        if (traceToOpenSpace.isPresent()) {
            System.out.println("Moving towards open space");
            return snakeDirection(traceToOpenSpace);
        } else {
            System.out.println("No open space found, making a default move");
            //TODO Default Move
            return "UP";
        }
    }

    private List<Point> getEmptyCells(Board board) {
        List<Point> emptyCells = new ArrayList<>();

        int boardSize = board.size();

        for (int x = 0; x < boardSize; x++) {
            for (int y = 0; y < boardSize; y++) {
                Point currentCell = new PointImpl(x, y);

                // Check if the current cell is empty
                if (isEmptyCell(currentCell, board)) {
                    emptyCells.add(currentCell);
                }
            }
        }

        return emptyCells;
    }

    private boolean isEmptyCell(Point cell, Board board) {
        // Check if the cell is not occupied by snake, walls, stones, or apples
        return !board.getSnake().contains(cell)
                && !board.getWalls().contains(cell)
                && !board.getStones().contains(cell)
                && !board.getApples().contains(cell);
    }

    private Point findNearestOpenSpace(Point start, List<Point> emptyCells) {
        // Find the nearest open space using a simple heuristic
        return emptyCells.stream()
                .min(Comparator.comparingInt(cell -> calculateDistance(start, cell)))
                .orElse(start);
    }

    private int calculateDistance(Point point1, Point point2) {
        // Calculate the Manhattan distance between two points
        return Math.abs(point1.getX() - point2.getX()) + Math.abs(point1.getY() - point2.getY());
    }

    private List<Point> getEmptyCellsWithApple(Board board) {
        return getEmptyCellsWithAppleAndStone(board).stream()
                .filter(cell -> !board.getStones().contains(cell))
                .collect(Collectors.toList());
    }

    public static List<Point> findObstaclesOnBoard(Board board, List<Point>... obstacles) {
        return Stream.of(obstacles)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    public boolean hasEmptyCellAccessible(Board board) {
        Lee snakeLee = new Lee(board.size(), board.size());
        Point head = board.getHead();
        List<Point> obstacles = findObstaclesOnBoard(board, board.getWalls(), board.getSnake(), board.getStones());

        // Check if there is any empty cell where the snake can go
        return iterateThroughBoard(board)
                .anyMatch(cell -> board.getAt(cell).equals(Elements.NONE) && // Empty cell
                        !obstacles.contains(cell) && // Not an obstacle
                        snakeLee.trace(head, cell, obstacles).isPresent()); // Snake can reach this cell
    }

    public boolean hasAppleAndStoneCellInArea(Board board) {
        Lee snakeLee = new Lee(board.size(), board.size());
        Point head = board.getHead();
        List<Point> obstacles = findObstaclesOnBoard(board, board.getWalls(), board.getSnake(), board.getStones());

        // Check if there is any empty cell where the snake can go and there is an apple and a stone in the area
        return iterateThroughBoard(board)
                .anyMatch(cell -> board.getAt(cell).equals(Elements.NONE) && // Empty cell
                        //!obstacles.contains(cell) && // Not an obstacle
                        snakeLee.trace(head, cell, obstacles).isPresent() && // Snake can reach this cell
                        board.getApples().contains(cell) && // Apple is in the area
                        board.getStones().contains(cell)); // Stone is in the area
    }

    public List<Point> getEmptyCellsWithAppleAndStone(Board board) {
        Lee snakeLee = new Lee(board.size(), board.size());
        Point head = board.getHead();
        List<Point> obstacles = findObstaclesOnBoard(board, board.getWalls(), board.getSnake(), board.getStones());

        // Filter out empty cells where the snake can go and there is an apple and a stone in the area
        return iterateThroughBoard(board)
                .filter(cell -> {
                    boolean isEmptyCell = board.getAt(cell).equals(Elements.NONE);
                    boolean isReachable = snakeLee.isCellReachable(head, cell, obstacles);
                    boolean hasApple = board.getApples().contains(cell);
                    boolean hasStone = board.getStones().contains(cell);

                    // Print debugging information
//                    System.out.println("Cell: " + cell + ", isEmptyCell: " + isEmptyCell +
//                            ", isReachable: " + isReachable + ", hasApple: " + hasApple + ", hasStone: " + hasStone);

                    // return isEmptyCell && isReachable && hasApple && hasStone;
                    return isReachable;
                })
                .collect(Collectors.toList());
    }

    public List<Point> analyzeEmptyCells(Board board) {
        Lee snakeLee = new Lee(board.size(), board.size());
        Point head = board.getHead();
        List<Point> obstacles = findObstaclesOnBoard(board, board.getWalls(), board.getSnake(), board.getStones());

        // Filter out empty cells where the snake can go
        return iterateThroughBoard(board)
                .filter(cell -> board.getAt(cell).equals(Elements.NONE)) // Empty cell
                .filter(cell -> !obstacles.contains(cell)) // Not an obstacle
                .filter(cell -> {
                    Optional<List<Point>> trace = snakeLee.trace(head, cell, obstacles);
                    return trace.isPresent(); // Snake can reach this cell
                })
                .collect(Collectors.toList());
    }

    private Stream<Point> iterateThroughBoard(Board board) {
        int width = board.size();
        int height = board.size();
        return IntStream.range(0, height)
                .mapToObj(y -> IntStream.range(0, width)
                        .mapToObj(x -> PointImpl.pt(x, y)))
                .flatMap(Function.identity());
    }

    public static String moveSnake(Point currentPoint, Point nextPoint) {
        int dx = nextPoint.getX() - currentPoint.getX();
        int dy = nextPoint.getY() - currentPoint.getY();

        if (dx > 0) return Direction.RIGHT.toString();
        else if (dx < 0) return Direction.LEFT.toString();
        else if (dy > 0) return Direction.UP.toString();
        else return Direction.DOWN.toString();
    }

    public static String snakeDirection(Optional<List<Point>> trace) {

        return moveSnake(trace.get().get(0), trace.get().get(1));
    }

    public static Point tail(Board board) {
        return board.get(
                Elements.TAIL_END_DOWN,
                Elements.TAIL_END_LEFT,
                Elements.TAIL_END_UP,
                Elements.TAIL_END_RIGHT).get(0);
    }

    @Override
    public String get(Board board) {
        return snakeSolver(board).toString();
    }

    private String snakeSolver(Board board) {
//        Test and Debug my Solver

//        int size = board.size();
////        Lee snakeLeeApple = new Lee(size, size);
//        Lee snakeLeeTail = new Lee(size, size);
////        Point apple = board.getApples().get(0);
//        Point stone = board.getStones().get(0);//  only one apple and stone
//        Point head = board.getHead();
//       Point tail = tail(board);
//
//        List<Point> walls = board.getWalls();
//
//        List<Point> snake = board.getSnake();
//
//
//        List<Point> obstacles = findObstaclesOnBoard(board, walls, snake, List.of(stone));
//        List<Point> obstaclesNoStones = findObstaclesOnBoard(board, walls, snake);
//        List<Point> obstaclesNoTail = findObstaclesOnBoard(board, walls, board.getSnake().subList(0, board.getSnake().size() - 1), List.of(stone));
//        Optional<List<Point>> traceToApple = snakeLeeApple.trace(head, apple, obstacles);
////        Optional<List<Point>> traceToStone = snakeLeeApple.trace(head, stone, walls);
//        Point traceToAppleForward = traceToApple.get().get(1);
//
//        boolean c = hasEmptyCellAccessible(board);
//        boolean b = hasAppleAndStoneCellInArea(board);
//        boolean s = board.getStones().contains(stone);

       // List<Point> e = getEmptyCellsWithApple(board);
//        System.out.println(" Has Tail :->" + tail(board).equals(tail));
//        System.out.println("No tail check it out: " + obstaclesNoTail);
//        Optional<List<Point>> traceToTail = snakeLeeTail.trace(head, tail, obstaclesNoTail);
//        System.out.println("trace to Stone" + traceToTail);
//        System.out.println("Snake no tail: " + board.getSnake().subList(0, board.getSnake().size() - 1));


            // Choose an alternative action when traceToTail is not present or empty
            // For example, you can return a default direction or handle this case accordingly
            return makeDecision(board);

    }
}


