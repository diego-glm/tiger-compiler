package Nios;

class InFrame extends Frame.Access {
  int offset;
  InFrame(int o) {
    offset = o;
  }

  public Tree.Exp exp(Tree.Exp fp) {
    return new Tree.MEM
      (new Tree.BINOP(Tree.BINOP.PLUS, fp, new Tree.CONST(offset)));
  }

  public String toString() {
    return Integer.toString(this.offset);
  }
}
