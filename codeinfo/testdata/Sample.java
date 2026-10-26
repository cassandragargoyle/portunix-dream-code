/*
 * Sample source for manual and integration tests of ptx-codeinfo
 * Expected counts are listed in testdata/README.md
 */
package com.example;

import java.util.List;

public class Sample {

    // Greeting printed by main
    private static final String GREETING = "Hello // not a comment";

    public static void main(String[] args) {
        List<String> names = List.of("Ada", "Linus"); // trailing comment
        for (String name : names) {
            System.out.println(GREETING + " " + name);
        }

        /* block comment on one line */
        String block = """
            text block /* not a comment */
            """;
        System.out.println(block);
    }
}
