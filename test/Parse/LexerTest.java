package Parse;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java_cup.runtime.Symbol;


class LexerTest {
    private static Yylex lexer;

    @AfterAll
    static void resetLexer() {
        lexer = null;
    }

    @Test
    public void scan_tokenKeyword() throws Exception {
        scan("nil array if then else of while do for to break let in end type var function");

        assertEquals(next_token().sym, sym.NIL);
        assertEquals(next_token().sym, sym.ARRAY);
        assertEquals(next_token().sym, sym.IF);
        assertEquals(next_token().sym, sym.THEN);
        assertEquals(next_token().sym, sym.ELSE);
        assertEquals(next_token().sym, sym.OF);
        assertEquals(next_token().sym, sym.WHILE);
        assertEquals(next_token().sym, sym.DO);
        assertEquals(next_token().sym, sym.FOR);
        assertEquals(next_token().sym, sym.TO);
        assertEquals(next_token().sym, sym.BREAK);
        assertEquals(next_token().sym, sym.LET);
        assertEquals(next_token().sym, sym.IN);
        assertEquals(next_token().sym, sym.END);
        assertEquals(next_token().sym, sym.TYPE);
        assertEquals(next_token().sym, sym.VAR);
        assertEquals(next_token().sym, sym.FUNCTION);
        assertEquals(next_token().sym, sym.EOF);
    }

    @Test
    public void scan_tokenIdentifier() throws Exception {
        String[] answers = {"a", "bb", "c1", "_d", "_1"};
        int count = 0;

        scan("a bb c1 _d _1");

        Symbol tok = next_token();
        while (tok.sym != sym.EOF) {
            assertEquals(tok.sym, sym.ID);
            assertEquals(tok.value, answers[count++]);
            tok = next_token();
        }
        assertEquals(next_token().sym, sym.EOF);
    }

    @Test
    public void scan_tokenLiteral() throws Exception {
        String[] answers = {"1", "22", "300", "4"};
        int count = 0;

        scan("1 22 300 0004");

        Symbol tok = next_token();
        while (tok.sym != sym.EOF) {
            assertEquals(tok.sym, sym.INT);
            assertEquals(tok.value, answers[count++]);
            tok = next_token();
        }
        assertEquals(next_token().sym, sym.EOF);
    }

    @Test
    public void scan_tokenString() throws Exception {
        String[] answers = {"", " ", "a", "1", "a 1", "a_1", "bb\nbb", "cc\tcc", "abcdef"};
        int count = 0;

        scan("\"\" \" \" \"a\" \"1\" \"a 1\" \"a_1\" \"bb\\nbb\" \"cc\\tcc\" \"abc\\   \\def\"");

        Symbol tok = next_token();
        while (tok.sym != sym.EOF) {
            assertEquals(tok.sym, sym.STRING);
            assertEquals(tok.value, answers[count++]);
            tok = next_token();
        }
        assertEquals(next_token().sym, sym.EOF);
    }

    @Test
    public void scan_tokenOperator() throws Exception {
        scan(", : ; ( ) [ ] { } . + - / = <> < <= > >= & | :=");

        assertEquals(next_token().sym, sym.COMMA);
        assertEquals(next_token().sym, sym.COLON);
        assertEquals(next_token().sym, sym.SEMICOLON);
        assertEquals(next_token().sym, sym.LPAREN);
        assertEquals(next_token().sym, sym.RPAREN);
        assertEquals(next_token().sym, sym.LBRACK);
        assertEquals(next_token().sym, sym.RBRACK);
        assertEquals(next_token().sym, sym.LBRACE);
        assertEquals(next_token().sym, sym.RBRACE);
        assertEquals(next_token().sym, sym.DOT);
        assertEquals(next_token().sym, sym.PLUS);
        assertEquals(next_token().sym, sym.MINUS);
        assertEquals(next_token().sym, sym.DIVIDE);
        assertEquals(next_token().sym, sym.EQ);
        assertEquals(next_token().sym, sym.NEQ);
        assertEquals(next_token().sym, sym.LT);
        assertEquals(next_token().sym, sym.LE);
        assertEquals(next_token().sym, sym.GT);
        assertEquals(next_token().sym, sym.GE);
        assertEquals(next_token().sym, sym.AND);
        assertEquals(next_token().sym, sym.OR);
        assertEquals(next_token().sym, sym.ASSIGN);
        assertEquals(next_token().sym, sym.EOF);
    }

    @Test
    public void scan_tokenComment() throws Exception {
        scan("/**/ /* */");
        assertEquals(next_token().sym, sym.EOF);

        scan("/* /* nested-comment */ */");
        assertEquals(next_token().sym, sym.EOF);
    }

    @Test
    public void scan_tokenLiteralIdentifier() throws Exception {
        scan("1aa");
        Symbol tok;

        tok = next_token();
        assertEquals(tok.sym, sym.INT);
        assertEquals(tok.value, "1");
        tok = next_token();
        assertEquals(tok.sym, sym.ID);
        assertEquals(tok.value, "aa");

        assertEquals(next_token().sym, sym.EOF);
    }

    @Test
    public void scan_tokenMathExample() throws Exception {
        Symbol tok;

        scan("x:=1+-a(b)*c_1;");

        tok = next_token();
        assertEquals(tok.sym, sym.ID);
        assertEquals(tok.value, "x");

        assertEquals(next_token().sym, sym.ASSIGN);

        tok = next_token();
        assertEquals(tok.sym, sym.INT);
        assertEquals(tok.value, "1");

        assertEquals(next_token().sym, sym.PLUS);
        assertEquals(next_token().sym, sym.MINUS);

        tok = next_token();
        assertEquals(tok.sym, sym.ID);
        assertEquals(tok.value, "a");

        assertEquals(next_token().sym, sym.LPAREN);
        tok = next_token();
        assertEquals(tok.sym, sym.ID);
        assertEquals(tok.value, "b");
        assertEquals(next_token().sym, sym.RPAREN);

        assertEquals(next_token().sym, sym.TIMES);

        tok = next_token();
        assertEquals(tok.sym, sym.ID);
        assertEquals(tok.value, "c_1");

        assertEquals(next_token().sym, sym.SEMICOLON);

        assertEquals(next_token().sym, sym.EOF);
    }

    @Test
    public void scan_illegalChar() throws Exception {
        String[] input = {"_", "@", "#", "$", "%", "^", "~"};

        scan("_ @ # $ % ^ ~");

        for (int i = 0; i < input.length; i++) {
            try {
                next_token();
                fail("Character '" + input[i] + "' is not a token in the tiger specification");
            } catch (ErrorMsg.ParsingException expected) { }
        }

        assertEquals(next_token().sym, sym.EOF);
    }

    private void scan(String input) {
        ErrorMsg.ErrorMsg errorMsg = new ErrorMsg.ErrorMsg("test");
        java.io.Reader in = new java.io.StringReader(input);
        lexer = new Yylex(in, errorMsg);
    }

    private Symbol next_token() throws java.io.IOException, ErrorMsg.ParsingException {
        return lexer.next_token();
    }
}