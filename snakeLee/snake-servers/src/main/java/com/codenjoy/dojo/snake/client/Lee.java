package com.codenjoy.dojo.snake.client;


import com.codenjoy.dojo.services.Point;
import com.codenjoy.dojo.services.PointImpl;
import com.codenjoy.dojo.snake.client.colored.Colored;
import com.codenjoy.dojo.snake.client.colored.Ansi;
import com.codenjoy.dojo.snake.client.colored.Attribute;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Lee extends Board{

    private final int EMPTY = 0;
    private final int START = 1;
    private final int OBSTACLE = -9;
    private final int WIDTH;
    private final int HEIGHT;
    private final int[][] board;

    public Lee(int width, int height) {
        WIDTH = width;
        HEIGHT = height;
        board = new int[height][width];
    }

    private int get(int x, int y) {
        return board[y][x];
    }

    private void set(int x, int y, int value) {
        board[y][x] = value;
    }

    private int get(Point point) {
        return get(point.getX(), point.getY());
    }

    private void set(Point point, int value) {
        set(point.getX(), point.getY(), value);
    }

    protected boolean isOnBoard(Point point) {
        return point.getX() >= 0 && point.getX() < WIDTH &&
                point.getY() >= 0 && point.getY() < HEIGHT;
    }

    private boolean isUnvisited(Point point) {
        return get(point) == EMPTY;
    }

    private Supplier<Stream<Point>> deltas() {
        return () -> Stream.of(
                new PointImpl(-1, 0), // offset, not a point
                new PointImpl(1, 0),  // offset, not a point
                new PointImpl(0, 1),  // offset, not a point
                new PointImpl(0, -1)  // offset, not a point
        );
    }
    private static Point movedXY(Point point, Point delta) {
        return new PointImpl(point.getX() + delta.getX(), point.getY() + delta.getY());
    }
    public Stream<Point> neighbours(Point point) {
        return deltas().get()
                .map(d -> movedXY(point,d))
                .filter(this::isOnBoard);
    }

    public Stream<Point> neighboursUnvisited(Point point) {
        return neighbours(point)
                .filter(this::isUnvisited);
    }

    public List<Point> neighboursByValue(Point point, int value) {
        return neighbours(point)
                .filter(p -> get(p) == value)
                .collect(Collectors.toList());
    }

    public void initializeBoard(List<Point> obstacles) {
        obstacles.forEach(p -> set(p, OBSTACLE));
    }

    public Optional<List<Point>> trace(Point start, Point finish, List<Point> obstacles) {
        // 1. initialization
        initializeBoard(obstacles);
        int[] counter = {START}; // HEAP due to lambda
        set(start, counter[0]);
        counter[0]++;
        boolean found = false;
        // 2. fill the board
        for (Set<Point> curr = new HashSet<>(Collections.singleton(start)); !(found || curr.isEmpty()); counter[0]++) {
            Set<Point> next = curr.stream()
                    .flatMap(this::neighboursUnvisited)
                    .collect(Collectors.toSet());

            next.forEach(p -> set(p, counter[0]));
            found = next.contains(finish);
            curr = next;
        }
        // 3. backtrace (reconstruct the path)
        if (!found) return Optional.empty();
        LinkedList<Point> path = new LinkedList<>();
        path.add(finish);
        counter[0]--;

        Point curr = finish;
        while (counter[0] > START) {
            counter[0]--;
            Point prev = neighboursByValue(curr, counter[0]).get(0);
            if (prev == start) break;
            path.addFirst(prev);
            curr = prev;
        }
        return Optional.of(path);
    }

    public String cellFormatted(Point p, List<Point> path) {
        int value = get(p);
       // String valueF = String.format("%3d", value);
        String valueF = String.format(" --", value);

        if (value == OBSTACLE) {
            Attribute a = new Attribute(Ansi.ColorFont.BLUE);
            return Colored.build(" XX", a);
        }

        if (path.isEmpty()) return valueF;

        if (path.contains(p)) {
            Attribute a = new Attribute(Ansi.ColorFont.RED);
            return Colored.build(valueF, a);
        }
        return valueF;//" --";
    }

    public String boardFormatted(List<Point> path) {
        return IntStream.range(0, HEIGHT).mapToObj(y ->
                        IntStream.range(0, WIDTH)
                                .mapToObj(x -> new PointImpl(x, y))
                                .map(p -> cellFormatted(p, path))
                                .collect(Collectors.joining())
                )
                .collect(Collectors.joining("\n"));
    }

    @Override
    public String toString() {
        return boardFormatted(Collections.emptyList());
    }
}
