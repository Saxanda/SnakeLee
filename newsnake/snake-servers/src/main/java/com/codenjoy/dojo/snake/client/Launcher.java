package com.codenjoy.dojo.snake.client;


import com.codenjoy.dojo.client.WebSocketRunner;

/**
 * User: Ruslan
 */
public class Launcher {



    public static void main(String[] args) {
        String url = "http://164.90.168.158/codenjoy-contest/board/player/1v856zqzewbl4r5dnbqc?code=6387579613757116868";
        MySolver solver = new MySolver();
        Board boardA = new Board();
        Board boardB = new Board();
        WebSocketRunner.runClient(

                url,
                solver,
                boardA);

    }

}
