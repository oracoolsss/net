package com.oracoolsss;

import java.io.IOException;

public class Main {

    public static void main(String[] args) {
        try {
            Proxy proxy = new Proxy(Integer.parseInt(args[0]));
            proxy.run();
        } catch (NumberFormatException e) {
            System.out.println(e.getMessage());
        } catch (IOException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }
}
