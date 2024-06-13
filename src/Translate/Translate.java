package Translate;
import Symbol.Symbol;
import Tree.BINOP;
import Tree.CJUMP;
import Temp.Temp;
import Temp.Label;

/* Notes:
  CONST(int) : make a constant
  NAME(l) : symbolic constant for label
  TEMP(t) : temporary register
  LABEL(l) : label location inside the nested tree calls
  BINOP(op, l, r) : l op r
  CALL(f, a) : Procedure call location f with argument l
  ESEQ(s, e) : s is side effect, e is the actual return value
  SEQ(s, s) : two side effects (no return)
  MOVE(d, s) : evaluate s and move to location d
  MEM(e) : Fetch memory location address e (MOVE(MEM(e),e) means store at address e)
  EXP(e) : Uexp: evaluate e and discard result
  JUMP(t) : branching to label t
  CJUMP(cond, l, r, t, f): if (l cond r) JUMP(t) else JUMP(f)
*/

public class Translate {
  public Frame.Frame frame;
  public Translate(Frame.Frame f) {
    frame = f;
  }
  private Frag frags;
  public void procEntryExit(Level level, Exp body) {
    Frame.Frame myframe = level.frame;
    Tree.Exp bodyExp = body.unEx();
    Tree.Stm bodyStm;
    if (bodyExp != null)
      bodyStm = MOVE(TEMP(myframe.RV()), bodyExp);
    else
      bodyStm = body.unNx();
    ProcFrag frag = new ProcFrag(myframe.procEntryExit1(bodyStm), myframe);
    frag.next = frags;
    frags = frag;
  }
  public Frag getResult() {
    return frags;
  }

  private static Tree.Exp CONST(int value) {
    return new Tree.CONST(value);
  }
  private static Tree.Exp NAME(Label label) {
    return new Tree.NAME(label);
  }
  private static Tree.Exp TEMP(Temp temp) {
    return new Tree.TEMP(temp);
  }
  private static Tree.Exp BINOP(int binop, Tree.Exp left, Tree.Exp right) {
    return new Tree.BINOP(binop, left, right);
  }
  private static Tree.Exp MEM(Tree.Exp exp) {
    return new Tree.MEM(exp);
  }
  private static Tree.Exp CALL(Tree.Exp func, Tree.ExpList args) {
    return new Tree.CALL(func, args);
  }
  private static Tree.Exp ESEQ(Tree.Stm stm, Tree.Exp exp) {
    if (stm == null)
      return exp;
    return new Tree.ESEQ(stm, exp);
  }

  private static Tree.Stm MOVE(Tree.Exp dst, Tree.Exp src) {
    return new Tree.MOVE(dst, src);
  }
  private static Tree.Stm EXP(Tree.Exp exp) {
    return new Tree.UEXP(exp);
  }
  private static Tree.Stm JUMP(Label target) {
    return new Tree.JUMP(target);
  }
  private static
    Tree.Stm CJUMP(int relop, Tree.Exp l, Tree.Exp r, Label t, Label f) {
      return new Tree.CJUMP(relop, l, r, t, f);
    }
  private static Tree.Stm SEQ(Tree.Stm left, Tree.Stm right) {
    if (left == null)
      return right;
    if (right == null)
      return left;
    return new Tree.SEQ(left, right);
  }
  private static Tree.Stm LABEL(Label label) {
    return new Tree.LABEL(label);
  }

  private static Tree.ExpList ExpList(Tree.Exp head, Tree.ExpList tail) {
    return new Tree.ExpList(head, tail);
  }
  private static Tree.ExpList ExpList(Tree.Exp head) {
    return ExpList(head, null);
  }
  private static Tree.ExpList ExpList(ExpList exp) {
    if (exp == null)
      return null;
    return ExpList(exp.head.unEx(), ExpList(exp.tail));
  }

  public Exp Error() {
    return new Ex(CONST(0));
  }

  public Exp SimpleVar(Access access, Level level) {
    Tree.Exp fp = TEMP(level.frame.FP());

    fp = levelHelper(level, access.home, fp);

    return new Ex(access.acc.exp(fp));
  }

  public Exp FieldVar(Exp record, int index) {
    Label store = new Label();
    Temp loc    = new Temp();
    return new Ex(
                ESEQ(
                  SEQ(
                    MOVE(TEMP(loc), record.unEx()),
                    SEQ(
                      CJUMP(CJUMP.EQ, TEMP(loc), CONST(0), frame.badPtr(), store),
                      LABEL(store))),
                  MEM(BINOP(BINOP.PLUS, TEMP(loc), BINOP(BINOP.MUL, CONST(index), CONST(frame.wordSize()))))));
  }

  public Exp SubscriptVar(Exp array, Exp index) {
    Label cont1 = new Label();
    Label cont2 = new Label();
    Temp addr = new Temp();
    Temp offset = new Temp();
    return new Ex(
                ESEQ(
                  SEQ(
                    MOVE(TEMP(addr), array.unEx()),
                    SEQ(
                      MOVE(TEMP(offset), index.unEx()),
                      SEQ(
                        CJUMP(CJUMP.LT, TEMP(offset), CONST(0), frame.badPtr(), cont1),
                        SEQ(
                          LABEL(cont1),
                          SEQ(
                            CJUMP(CJUMP.GT, TEMP(offset), MEM(BINOP(BINOP.MINUS, TEMP(addr), CONST(frame.wordSize()))), frame.badPtr(), cont2),
                            LABEL(cont2)))))),
                  MEM(BINOP(BINOP.PLUS, TEMP(addr), BINOP(BINOP.MUL, TEMP(offset), CONST(frame.wordSize()))))));
  }

  public Exp NilExp() {
    return new Ex(CONST(0));
  }

  public Exp IntExp(int value) {
    return new Ex(CONST(value));
  }

  private java.util.Hashtable<String,Label> strings = new java.util.Hashtable<String,Label>();
  public Exp StringExp(String lit) {
    String u = lit.intern();
    Label lab = (Label)strings.get(u);
    if (lab == null) {
      lab = new Label();
      strings.put(u, lab);
      DataFrag frag = new DataFrag(frame.string(lab, u));
      frag.next = frags;
      frags = frag;
    }
    return new Ex(NAME(lab));
  }

  private Tree.Exp CallExp(Symbol f, ExpList args, Level from) {
    return frame.externalCall(f.toString(), ExpList(args));
  }
  private Tree.Exp CallExp(Level f, ExpList args, Level from) {
    Tree.Exp fp = TEMP(from.frame.FP());

    if (f.parent != from) {
      fp = levelHelper(from, f.parent, fp);
    }

    return CALL(NAME(f.frame.name), ExpList(fp, ExpList(args)));
  }

  public Exp FunExp(Symbol f, ExpList args, Level from) {
    return new Ex(CallExp(f, args, from));
  }
  public Exp FunExp(Level f, ExpList args, Level from) {
    return new Ex(CallExp(f, args, from));
  }
  public Exp ProcExp(Symbol f, ExpList args, Level from) {
    return new Nx(EXP(CallExp(f, args, from)));
  }
  public Exp ProcExp(Level f, ExpList args, Level from) {
    return new Nx(EXP(CallExp(f, args, from)));
  }

  public Exp OpExp(int op, Exp left, Exp right) {
    switch (op) {
      case Absyn.OpExp.PLUS:
        return new Ex(BINOP(BINOP.PLUS, left.unEx(), right.unEx()));
      case Absyn.OpExp.MINUS:
        return new Ex(BINOP(BINOP.MINUS, left.unEx(), right.unEx()));
      case Absyn.OpExp.MUL:
        return new Ex(BINOP(BINOP.MUL, left.unEx(), right.unEx()));
      case Absyn.OpExp.DIV:
        return new Ex(BINOP(BINOP.DIV, left.unEx(), right.unEx()));
      case Absyn.OpExp.EQ:
        return new RelCx(CJUMP.EQ, left.unEx(), right.unEx());
      case Absyn.OpExp.NE:
        return new RelCx(CJUMP.NE, left.unEx(), right.unEx());
      case Absyn.OpExp.LT:
        return new RelCx(CJUMP.LT, left.unEx(), right.unEx());
      case Absyn.OpExp.LE:
        return new RelCx(CJUMP.LE, left.unEx(), right.unEx());
      case Absyn.OpExp.GT:
        return new RelCx(CJUMP.GT, left.unEx(), right.unEx());
      case Absyn.OpExp.GE:
        return new RelCx(CJUMP.GE, left.unEx(), right.unEx());
    }
    return Error();
  }

  public Exp StrOpExp(int op, Exp left, Exp right) {
    Tree.Exp strcmp_tree = frame.externalCall("strcmp", ExpList(left.unEx(), ExpList(right.unEx())));
    switch (op) {
      case Absyn.OpExp.EQ:
        return new RelCx(CJUMP.EQ, strcmp_tree, CONST(0));
      case Absyn.OpExp.NE:
        return new RelCx(CJUMP.NE, strcmp_tree, CONST(0));
      case Absyn.OpExp.LT:
        return new RelCx(CJUMP.LT, strcmp_tree, CONST(0));
      case Absyn.OpExp.LE:
        return new RelCx(CJUMP.LE, strcmp_tree, CONST(0));
      case Absyn.OpExp.GT:
        return new RelCx(CJUMP.GT, strcmp_tree, CONST(0));
      case Absyn.OpExp.GE:
        return new RelCx(CJUMP.GE, strcmp_tree, CONST(0));
    }
    return Error();
  }

  public Exp RecordExp(ExpList init) {
    Temp pt = new Temp();

    Tree.Exp alloc_Record = frame.externalCall("allocRecord", ExpList(CONST(RecordSizeHelper(init))));

    Tree.SEQ list = null;
    if (init != null) {
      int offset = 0;
      list = (Tree.SEQ) SEQ(MOVE(MEM(BINOP(BINOP.PLUS, TEMP(pt), CONST(offset))), init.head.unEx()), null);
      Tree.SEQ elements = list;
      while (init.tail != null) {
        offset += frame.wordSize();
        init = init.tail;

        elements.right = (Tree.SEQ) SEQ(MOVE(MEM(BINOP(BINOP.PLUS, TEMP(pt), CONST(offset))), init.head.unEx()), null);
        elements = (Tree.SEQ) elements.right;
      }
    }
    return new Ex(
            ESEQ(
              SEQ(
                MOVE(TEMP(pt), alloc_Record),
                list),
              TEMP(pt)));
  }

  public int RecordSizeHelper(ExpList list) {
    int count = 0;

    while (list != null) {
      count += 1;
      list = list.tail;
    }

    return count;
  }

  public Exp SeqExp(ExpList e) {
    if (e == null)
      return new Nx(null);// Empty seq
    else
      return SeqExpHelper(e, null);
    }

  public Exp SeqExpHelper(ExpList e, Tree.Stm prev) {
    if (e.tail == null) {
      if (e.head.unEx() == null)
        return new Nx(SEQ(prev, e.head.unNx()));
      else
        return new Ex(ESEQ(prev, e.head.unEx()));
    } else {
      Tree.Stm seq = SEQ(prev, e.head.unNx());
      return SeqExpHelper(e.tail, seq);
    }
  }

  public Exp AssignExp(Exp l, Exp r) {
    return new Nx(MOVE(l.unEx(), r.unEx()));
  }

  public Exp IfExp(Exp cc, Exp aa, Exp bb) {
    return new IfThenElseExp(cc, aa, bb);
  }

  public Exp WhileExp(Exp test, Exp body, Label done) {
    Label start = new Label();
    Label end = new Label();
    return new Nx(
                SEQ(
                  SEQ(
                    SEQ(
                      LABEL(start),
                      test.unCx(end, done)),
                    SEQ(
                      SEQ(
                        LABEL(end),
                        body.unNx()),
                      JUMP(start))),
                  LABEL(done)));
  }

  public Exp ForExp(Access i, Exp lo, Exp hi, Exp body, Label done) {
    Label start = new Label();
    Label end = new Label();
    Temp iterator = new Temp();
    Tree.Exp var = i.acc.exp(TEMP(i.home.frame.FP()));
    return new Nx(
                SEQ(
                  SEQ(
                    SEQ(
                      SEQ(
                        MOVE(var, lo.unEx()),
                        MOVE(TEMP(iterator), hi.unEx())),
                      CJUMP(CJUMP.LE, var, TEMP(iterator), start, done)),
                    SEQ(
                      SEQ(
                        SEQ(
                          LABEL(start),
                          body.unNx()),
                        CJUMP(CJUMP.LT, var, TEMP(iterator), end, done)),
                      SEQ(
                        SEQ(
                          LABEL(end),
                          MOVE(var, BINOP(BINOP.PLUS, var, CONST(1)))),
                        JUMP(start)))),
                  LABEL(done)));
  }

  public Exp BreakExp(Label done) { return new Nx(JUMP(done)); }

  public Exp LetExp(ExpList lets, Exp body) {

    Tree.Stm decl = LetsHelper(lets, null);

    if (body.unEx() == null) {
      return new Nx(SEQ(decl, body.unNx()));
    } else {
      return new Ex(ESEQ(decl, body.unEx()));
    }
  }

  public Tree.Stm LetsHelper(ExpList l, Tree.Stm prev) {
    if (l == null) {
      return prev;
    } else {
      Tree.Stm seq = SEQ(prev, l.head.unNx());
      return LetsHelper(l.tail, seq);
    }
  }

  public Exp ArrayExp(Exp size, Exp init) {
    return new Ex(frame.externalCall("initArray", ExpList(size.unEx(), ExpList(init.unEx()))));
  }

  public Exp VarDec(Access a, Exp init) {
    return new Nx(MOVE(a.acc.exp(TEMP(a.home.frame.FP())), init.unEx()));
  }

  public Exp TypeDec() {
    return new Nx(null);
  }

  public Exp FunctionDec() {
    return new Nx(null);
  }

  private Tree.Exp levelHelper(Level start, Level end, Tree.Exp pt) {
    while (start != end) {
      pt = start.frame.formals.head.exp(pt);
      start = start.parent;
    }
    return pt;
  }
}
