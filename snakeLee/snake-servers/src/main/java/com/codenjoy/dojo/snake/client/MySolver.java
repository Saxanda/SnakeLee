package com.codenjoy.dojo.snake.client;

import com.codenjoy.dojo.client.Solver;
import com.codenjoy.dojo.services.Direction;
import com.codenjoy.dojo.services.Point;
import com.codenjoy.dojo.snake.model.Elements;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MySolver implements Solver<Board> {
    static int  gameStatistic = 0;
    
    public static Point getEndOfTail(Board board) {
        return board.get(Elements.TAIL_END_DOWN,
                Elements.TAIL_END_LEFT,
                Elements.TAIL_END_UP,
                Elements.TAIL_END_RIGHT).get(0);
    }
    public static List<Point> findObstaclesOnBoard(Board board, List<Point> ...obstacles) {

        Point endOfTail = getEndOfTail(board);
        return Stream.of(obstacles)
                .flatMap(Collection::stream)
                //.filter(p -> !p.equals(endOfTail))
                .collect(Collectors.toList());
    }

    public static String getXYDirection(Point currentPoint, Point nextPoint) {
        int dx = nextPoint.getX() - currentPoint.getX();
        int dy = nextPoint.getY() - currentPoint.getY();

        if (dx > 0) return Direction.RIGHT.toString();
        else if (dx < 0) return Direction.LEFT.toString();
        else if (dy > 0) return Direction.UP.toString();
        else return Direction.DOWN.toString();

    }
    public static String getXYDirectionTrace(List<Point> trace) {
        return getXYDirection(trace.get(0), trace.get(1));
    }
    public static String longRouteToTail(Board board, List<Point> obstacles){
        Lee lee = new Lee(board.size(), board.size());
        Point head = board.getHead();
        Point tail = getEndOfTail(board);
        lee.initializeBoard(obstacles);
        Optional<Point> nextPoint = lee.neighbours(head)
                .filter(lee::isOnBoard)
                .filter(p -> !obstacles.contains(p))
                .max(Comparator.comparing(p -> lee.trace(p, tail, obstacles).orElse(lee.get()).size()));
        System.out.println(nextPoint);
        return getXYDirection(head,nextPoint.orElse(head));
    }
    @Override
    public String get(Board board) {
        return snakeSolver(board).toString();}
     public String snakeSolver(Board board){
        int size = board.size();

         Lee leeApple = new Lee(size, size);
         Lee leeTail = new Lee(size, size);
         Lee leeStone = new Lee(size, size);
         //System.out.println("Show me the board: "+board);
         List<Point> apples = board.getApples();
         List<Point> walls = board.getWalls();
         List<Point> snake = board.getSnake();
         int snakeSize = snake.size();
         //List<Point> snakeNoTail =snake.subList(0,snakeSize-1);
         List<Point> snakeNoTail = snake.stream().filter(t -> !t.equals(snake.get(snake.size()-1))).collect(Collectors.toList());
         List<Point> stones = board.getStones();

         Point appleAt = apples.get(0);
         Point stoneAt = stones.get(0);
         Point head = board.getHead();
         Point tail = snake.get(snake.size()-1);
         //Point tail = getEndOfTail(board);

         Optional<List<Point>> moveToApple = null;
         Optional<List<Point>> moveToStone = null;
         Optional<List<Point>> moveToTail = null;
         Optional<List<Point>> tailToApple = null;
         Optional<List<Point>> headToTail = null;


         List<Point> obstacles = MySolver.findObstaclesOnBoard(board,walls, snake);
         List<Point> obstaclesNoStones = MySolver.findObstaclesOnBoard(board,walls,snake);
         List<Point> obstaclesNoTail = MySolver.findObstaclesOnBoard(board,walls,snakeNoTail);

         moveToApple = leeApple.trace(head,appleAt,obstacles);
         moveToStone = leeStone.trace(head,stoneAt,obstaclesNoStones);
         moveToTail = leeTail.trace(head,tail,obstaclesNoTail);
         System.out.println("Statistic : " + gameStatistic);
         System.out.println("Snake size : " + snakeSize);
         System.out.println("Tail at : " + tail);
         System.out.println("Snake to apple : " + moveToApple);
         System.out.println("Snake to tail : " + moveToTail );
        if(board.isGameOver()){ gameStatistic++; System.out.printf("Game number - [%d]", gameStatistic); return Direction.random().toString();}
  //      else if (moveToStone.isPresent()&&snakeSize>73) { return MySolver.getXYDirectionTrace(moveToStone.get());}

        else if (moveToStone.isPresent()&&snakeSize>10) { return MySolver.longRouteToTail(board,obstaclesNoTail);}
        else if (moveToApple.isPresent()){return MySolver.getXYDirectionTrace(moveToApple.get());}
//        else if (moveToStone.isPresent()&&!moveToApple.isPresent()) { return MySolver.getXYDirectionTrace(moveToStone.get());}
//        else if (!moveToStone.isPresent()&&!moveToApple.isPresent()) { return MySolver.longRouteToTail(board,obstaclesNoTail);}
//        else if (moveToApple.isPresent()){return MySolver.getXYDirectionTrace(moveToApple.get());}
//        else if(snakeSize>73){ return MySolver.getXYDirectionTrace(moveToStone.get());}




         return Direction.random().toString();

        }



}
