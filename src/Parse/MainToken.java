package Parse;
import ErrorMsg.*;

/**
 * The Main class is the entry point of the program. It processes files and prints out the tokens
 * encountered by the lexer.
 */
public class MainToken {

  /**
   * The main method of the program. It takes command-line arguments specifying the filenames to process.
   *
   * @param argv An array of strings representing the filenames to process.
   * @throws java.io.IOException If an I/O error occurs while reading the input files.
   */
  public static void main(String[] argv) throws java.io.IOException {
      for (String filename : argv) {
          if (argv.length > 1)
              System.out.println("***Processing: " + filename);

          ErrorMsg errorMsg = new ErrorMsg(filename);
          java.io.Reader inp = new java.io.InputStreamReader(new java.io.FileInputStream(filename));
          Yylex lexer = new Yylex(inp, errorMsg);
          java_cup.runtime.Symbol tok;

          String extra = "";

          do {
              try {
                  tok = lexer.next_token();
              } catch (ParsingException expected) {
                  expected.printStackTrace();
                  tok = new java_cup.runtime.Symbol(sym.error);
              }
              extra = "";
              extra = switch (tok.sym) {
                  case sym.ID -> "\t$" + tok.value;
                  case sym.INT -> "\t#" + tok.value;
                  case sym.STRING -> " \"" + tok.value + "\"";
                  default -> extra;
              };
              //System.out.println(symnames[tok.sym] + " " + tok.left + extra);
              System.out.println(symnames[tok.sym] + " " + extra);
          } while (tok.sym != sym.EOF);

          inp.close();
      }
  }

  /** An array of strings representing the names of the symbols used by the lexer. */
  static String[] symnames = new String[100];

  /** Static initialization block that initializes the symnames array with the names of the symbols. */
  static {
    symnames[sym.EOF] = "EOF";
    symnames[sym.INT] = "INT";
    symnames[sym.GT] = "GT";
    symnames[sym.DIVIDE] = "DIVIDE";
    symnames[sym.COLON] = "COLON";
    symnames[sym.ELSE] = "ELSE";
    symnames[sym.OR] = "OR";
    symnames[sym.NIL] = "NIL";
    symnames[sym.DO] = "DO";
    symnames[sym.GE] = "GE";
    symnames[sym.error] = "error";
    symnames[sym.LT] = "LT";
    symnames[sym.OF] = "OF";
    symnames[sym.MINUS] = "MINUS";
    symnames[sym.ARRAY] = "ARRAY";
    symnames[sym.TYPE] = "TYPE";
    symnames[sym.FOR] = "FOR";
    symnames[sym.TO] = "TO";
    symnames[sym.TIMES] = "TIMES";
    symnames[sym.COMMA] = "COMMA";
    symnames[sym.LE] = "LE";
    symnames[sym.IN] = "IN";
    symnames[sym.END] = "END";
    symnames[sym.ASSIGN] = "ASSIGN";
    symnames[sym.STRING] = "STRING";
    symnames[sym.DOT] = "DOT";
    symnames[sym.LPAREN] = "LPAREN";
    symnames[sym.RPAREN] = "RPAREN";
    symnames[sym.IF] = "IF";
    symnames[sym.SEMICOLON] = "SEMICOLON";
    symnames[sym.ID] = "ID";
    symnames[sym.WHILE] = "WHILE";
    symnames[sym.LBRACK] = "LBRACK";
    symnames[sym.RBRACK] = "RBRACK";
    symnames[sym.NEQ] = "NEQ";
    symnames[sym.VAR] = "VAR";
    symnames[sym.BREAK] = "BREAK";
    symnames[sym.AND] = "AND";
    symnames[sym.PLUS] = "PLUS";
    symnames[sym.LBRACE] = "LBRACE";
    symnames[sym.RBRACE] = "RBRACE";
    symnames[sym.LET] = "LET";
    symnames[sym.THEN] = "THEN";
    symnames[sym.EQ] = "EQ";
  }

}
