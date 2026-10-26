/*
 *  Fixture: ideographic spaces (U+3000) only inside literals and comments
 */
public class UnicodeSpaces {
    String text = "a　b";
    char space = '　';
    String block = """
        　indented
        """;
    // line comment with 　 space
    int value = 1; /* block 　 comment */
}
