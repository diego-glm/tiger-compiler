package Parse;
import ErrorMsg.*;

import java_cup.runtime.Symbol;

%%

%public
%cup
%char
%line
%throws ParsingException

%{

    private ErrorMsg errorMsg;
    private StringBuffer string = new StringBuffer();
    private int count;

    private void newline() {
      errorMsg.newline(yychar);
    }

    private void err(long pos, String s) throws Exception {
      errorMsg.error(pos, "Unknown character \"" + s + "\"");
    }

    private void err(String s) throws Exception {
      err(yychar, s);
    }

    private java_cup.runtime.Symbol tok(int kind) {
        return tok(kind, null);
    }

    private java_cup.runtime.Symbol tok(int kind, Object value) {
        return new java_cup.runtime.Symbol(kind,(int) yychar, ((int) yychar) + yylength(), value);
    }

    Yylex(java.io.Reader s, ErrorMsg e) { //InputStream
      this(s);
      errorMsg=e;
    }

%}

%eofval{
	 return tok(sym.EOF);
%eofval}

WhiteSpace        = [ \t\f]
Identifier        = ( "_" [:jletterdigit:]+) | ([:letter:] [:jletterdigit:]*)
DecIntegerLiteral = [0-9]+

%state STRING STR_LINE_BEAK COMMENT

%%
<YYINITIAL> {

  /* keywords */
  nil                  { return tok(sym.NIL); }
  array                { return tok(sym.ARRAY); }
  if                   { return tok(sym.IF); }
  then                 { return tok(sym.THEN); }
  else                 { return tok(sym.ELSE); }
  of                   { return tok(sym.OF); }
  while	               { return tok(sym.WHILE); }
  do                   { return tok(sym.DO); }
  for	               { return tok(sym.FOR); }
  to	               { return tok(sym.TO); }
  break	               { return tok(sym.BREAK); }
  let	               { return tok(sym.LET); }
  in                   { return tok(sym.IN); }
  end                  { return tok(sym.END); }
  type                 { return tok(sym.TYPE); }
  var                  { return tok(sym.VAR); }
  function             { return tok(sym.FUNCTION); }

  /* identifiers */
  {Identifier}         { return tok(sym.ID, yytext()); }

  /* literals */
  {DecIntegerLiteral}  { int num = Integer.parseInt(yytext());
                         return tok(sym.INT, num);}

  /* operators */
  ","	               { return tok(sym.COMMA); }
  ":"	               { return tok(sym.COLON); }
  ";"	               { return tok(sym.SEMICOLON); }
  "("	               { return tok(sym.LPAREN); }
  ")"	               { return tok(sym.RPAREN); }
  "["	               { return tok(sym.LBRACK); }
  "]"	               { return tok(sym.RBRACK); }
  "{"	               { return tok(sym.LBRACE); }
  "}"	               { return tok(sym.RBRACE); }
  "."	               { return tok(sym.DOT); }
  "+"	               { return tok(sym.PLUS); }
  "-"	               { return tok(sym.MINUS); }
  "*"	               { return tok(sym.TIMES); }
  "/"	               { return tok(sym.DIVIDE); }
  "="	               { return tok(sym.EQ); }
  "<>"	               { return tok(sym.NEQ); }
  "<"	               { return tok(sym.LT); }
  "<="	               { return tok(sym.LE); }
  ">"	               { return tok(sym.GT); }
  ">="                 { return tok(sym.GE); }
  "&"	               { return tok(sym.AND); }
  "|"	               { return tok(sym.OR); }
  ":="	               { return tok(sym.ASSIGN); }

  /* whitespace */
  {WhiteSpace}         { /* ignore */ }
  \n	               { newline(); }
}

/* string */
<YYINITIAL> \"         { string.setLength(0); yybegin(STRING); }
<STRING> {

  /* end of string */
  \"                   { yybegin(YYINITIAL);
                         return tok(sym.STRING, string.toString()); }

  /* string content */
  [^\n\r\"\\]+         { string.append( yytext() ); }

  /* escape-sequence */
  \\n                  { string.append('\n'); }
  \\t                  { string.append('\t'); }
  \\r                  { string.append('\r'); }
  \\\"                 { string.append('\"'); }
  \\\\                 { string.append('\\'); }
  \\                   { yybegin(STR_LINE_BEAK); }
}

/* esc-seq line break */
<STR_LINE_BEAK>  [^\\] { /* ignore */ }
<STR_LINE_BEAK>  \\    { yybegin(STRING); }

/* comments */
<YYINITIAL> "/*"       { count = 1; yybegin(COMMENT); }
<COMMENT> {
  /* comment content*/
  [^"/*"]+             { /* ignore */ }
  [^"*/"]+             { /* ignore */ }

  /* nested comments */
  "/*"                 { count++; }
  "*/"                 { count--; if (count == 0) yybegin(YYINITIAL); }
}

/* error */
.                      { try{ err(yytext()); } catch (Exception e) { e.printStackTrace(); }
                         return tok(sym.error, yytext()); }