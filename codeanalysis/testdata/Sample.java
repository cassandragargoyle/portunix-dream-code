package com.example;

public class Sample {

    public static void main(String[] args) {
        // TODO: Implement proper logging
        System.out.println("Hello World");

        String password = "secret123";  // This should trigger a warning

        try {
            doSomething();
        } catch (Exception e) {}  // Empty catch block
    }

    private static void doSomething() {
        // FIXME: This needs refactoring
    }
}
