package Parse;

public class Parse {

  public ErrorMsg.ErrorMsg errorMsg;
  public Absyn.Exp absyn;

  public Parse(String filename) throws java.io.IOException {
    errorMsg = new ErrorMsg.ErrorMsg(filename);
    java.io.Reader inp = new java.io.InputStreamReader(new java.io.FileInputStream(filename));

    Grammar parser = new Grammar(inp, errorMsg);
    /* open input files, etc. here */

    try {
      absyn = (Absyn.Exp)(parser.parse().value);
    } catch (Throwable e) {
      e.printStackTrace();
      throw new Error(e.toString());
    }
    finally {
      try {inp.close();} catch (java.io.IOException e) {}
    }
  }
}
