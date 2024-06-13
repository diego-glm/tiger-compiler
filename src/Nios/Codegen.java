package Nios;

import Temp.Temp; //
import Temp.TempList; //
import Temp.Label; //
import Temp.LabelList; //

import java.util.Hashtable; //


public class Codegen {
  NiosFrame frame;
  public Codegen(NiosFrame f) {frame = f;}

  private Assem.InstrList ilist = null, last = null;

  private void emit(Assem.Instr inst) {
    if (last != null)
      last = last.tail = new Assem.InstrList(inst, null);
    else {
      if (ilist != null)
        throw new Error("Codegen.emit");
      last = ilist = new Assem.InstrList(inst, null);
    }
  }

  Assem.InstrList codegen(Tree.Stm s) {
    munchStm(s);
    Assem.InstrList l = ilist;
    ilist = last = null;
    return l;
  }

  static Assem.Instr OPER(String a, TempList d, TempList s, LabelList j) {
    return new Assem.OPER("\t" + a, d, s, j);
  }
  static Assem.Instr OPER(String a, TempList d, TempList s) {
    return new Assem.OPER("\t" + a, d, s);
  }
  static Assem.Instr MOVE(String a, Temp d, Temp s) {
    return new Assem.MOVE("\t" + a, d, s);
  }

  static TempList L(Temp h) {
    return new TempList(h, null);
  }
  static TempList L(Temp h, TempList t) {
    return new TempList(h, t);
  }

  void munchStm(Tree.Stm s) {
    if (s instanceof Tree.MOVE) 
      munchStm((Tree.MOVE)s);
    else if (s instanceof Tree.UEXP)
      munchStm((Tree.UEXP)s);
    else if (s instanceof Tree.JUMP)
      munchStm((Tree.JUMP)s);
    else if (s instanceof Tree.CJUMP)
      munchStm((Tree.CJUMP)s);
    else if (s instanceof Tree.LABEL)
      munchStm((Tree.LABEL)s);
    else
      throw new Error("Codegen.munchStm");
  }
  /* Tiling
     MOVE(MEM, Exp) -- store
      MOVE(MEM(BINOP(+, e1,       CONST(i))), e2)
      MOVE(MEM(BINOP(+, TEMP,     CONST(i))), e2)
      MOVE(MEM(BINOP(+, TEMP(FP), CONST(i))), e2)
      MOVE(MEM(CONST, e2)
      MOVE(MEM(TEMP, e2))
      MOVE(MEM(TEMP(FP), e2)
      MOVE(MEM(e1), e2)
      ----------------
      MOVE(TEMP(i), MEM(BINOP(+, e1, CONST(j))))
      MOVE(TEMP(i), MEM(BINOP(+, TEMP, CONST(j))))
      MOVE(TEMP(i), MEM(BINOP(+, TEMP(FP), CONST(j))))
      MOVE(TEMP(i), MEM(CONST(j)))
      MOVE(TEMP(i), MEM(TEMP(j)))
      MOVE(TEMP(i), MEM(TEMP(FP)))
      MOVE(TEMP(i), MEM(e2))
      ---------------
      MOVE(TEMP(i), e2)
  */
  void munchStm(Tree.MOVE s) {
                               // VVV this is new to me (intellij/java21 told me to simplify)
    if (s.dst instanceof Tree.MEM mem) {
        Tree.Exp e2 = s.src;

      if (mem.exp instanceof Tree.BINOP binop) {
        String i = "" + ((Tree.CONST) binop.right).value;
        Temp e1;

        if (binop.left instanceof Tree.TEMP temp) {
          e1 = temp.temp;
          if (e1 == frame.FP) {
            e1 = frame.SP;
            i += frame.name + "_framesize";
          }
        } else { e1 = munchExp(binop.left); }

        emit(OPER("stw `s0, " + i + "(`s1)",
                null, L(munchExp(e2), L(e1)))); return;

      } else if (mem.exp instanceof Tree.CONST cons) {

        emit(OPER("stw `s0, " + cons.value + "(`s1)",
                  null, L(munchExp(e2), L(frame.ZERO))));

      } else if (mem.exp instanceof Tree.TEMP temp) {
        Temp e1 = temp.temp;

        String i = "";
        if (e1 == frame.FP) {
          i += frame.name + "_framesize";
        }

        emit(OPER("stw `s0,  " + i + "(`s1)",
                null, L(munchExp(e2), L(frame.SP)))); return;
      }
    } else if (s.dst instanceof Tree.TEMP temp) {
      Tree.MEM mem = (Tree.MEM) s.src;
      Temp e2 = temp.temp;

      if (mem.exp instanceof Tree.BINOP binop) {
        String i = "" + ((Tree.CONST) binop.right).value;
        Temp e1;

        if (binop.left instanceof Tree.TEMP temp2) {
          e1 = temp2.temp;
          if (e1 == frame.FP) {
            e1 = frame.SP;
            i += frame.name + "_framesize";
          }
        } else { e1 = munchExp(binop.left); }

        emit(OPER("ldw `d0, " + i + "(`s0)",
                L(e2), L(e1))); return;

      } else if (mem.exp instanceof Tree.CONST cons) {

        emit(OPER("ldw `d0, (`s0)",
                L(e2), L(munchExp(cons))));

      } else if (mem.exp instanceof Tree.TEMP temp2) {
        String i = "";
        if (temp2.temp == frame.FP) {
          i += frame.name + "_framesize";
        }
        emit(OPER("ldw `d0, " + i + "(`s0)",
                L(e2), L(frame.SP))); return;
      } else {
        emit(MOVE("mov `d0, `s0",
                e2, munchExp(mem))); return;
      }
    }
  }

  void munchStm(Tree.UEXP s) {
    munchExp(s.exp);
  }

  void munchStm(Tree.JUMP s) {
    if (s.exp instanceof Tree.NAME name) {
      emit(OPER("br " + name.label.toString(),
              null, null, s.targets)); return;
    }
      emit(OPER("jmp `s0",
              null, L(munchExp(s.exp)), s.targets)); return;
  }

  private static String[] CJUMP = new String[10];
  static {
    CJUMP[Tree.CJUMP.EQ ] = "beq";
    CJUMP[Tree.CJUMP.NE ] = "bne";
    CJUMP[Tree.CJUMP.LT ] = "blt";
    CJUMP[Tree.CJUMP.GT ] = "bgt";
    CJUMP[Tree.CJUMP.LE ] = "ble";
    CJUMP[Tree.CJUMP.GE ] = "bge";
    CJUMP[Tree.CJUMP.ULT] = "bltu";
    CJUMP[Tree.CJUMP.ULE] = "bleu";
    CJUMP[Tree.CJUMP.UGT] = "bgtu";
    CJUMP[Tree.CJUMP.UGE] = "bgeu";
  }

  void munchStm(Tree.CJUMP s) {
    LabelList branches = new LabelList(s.iftrue, new LabelList(s.iffalse, null));

    emit(OPER(CJUMP[s.relop] + " `s0, `s1, " + s.iftrue.toString(),
            null, L(munchExp(s.left), L(munchExp(s.right))), branches));
  }

  void munchStm(Tree.LABEL l) {
    emit(new Assem.LABEL(l.label.toString() + ":", l.label));
  }

  Temp munchExp(Tree.Exp s) {
    if (s instanceof Tree.CONST)
      return munchExp((Tree.CONST)s);
    else if (s instanceof Tree.NAME)
      return munchExp((Tree.NAME)s);
    else if (s instanceof Tree.TEMP)
      return munchExp((Tree.TEMP)s);
    else if (s instanceof Tree.BINOP)
      return munchExp((Tree.BINOP)s);
    else if (s instanceof Tree.MEM)
      return munchExp((Tree.MEM)s);
    else if (s instanceof Tree.CALL)
      return munchExp((Tree.CALL)s);
    else
      throw new Error("Codegen.munchExp");
  }

  Temp munchExp(Tree.CONST e) {
    Temp i = new Temp();
    emit(OPER("movi `d0, " + e.value,
            L(i), null));

    return i;
  }

  Temp munchExp(Tree.NAME e) {
    Temp i = new Temp();
    emit(OPER("movia `d0, " + e.label.toString(),
            L(i), null));

    return i;
  }

  Temp munchExp(Tree.TEMP e) {
    if (e.temp == frame.FP) {
      Temp t = new Temp();
      emit(OPER("addi `d0 `s0 " + frame.name + "_framesize",
            L(t), L(frame.SP)));
      return t;
    }
    return e.temp;
  }

  private static String[] BINOP = new String[10];
  static {
    BINOP[Tree.BINOP.PLUS   ] = "add";
    BINOP[Tree.BINOP.MINUS  ] = "sub";
    BINOP[Tree.BINOP.MUL    ] = "mul";
    BINOP[Tree.BINOP.DIV    ] = "div";
    BINOP[Tree.BINOP.AND    ] = "and";
    BINOP[Tree.BINOP.OR     ] = "or";
    BINOP[Tree.BINOP.LSHIFT ] = "sll";
    BINOP[Tree.BINOP.RSHIFT ] = "srl";
    BINOP[Tree.BINOP.ARSHIFT] = "sra";
    BINOP[Tree.BINOP.XOR    ] = "xor";
  }


  private static String[] IBINOP = new String[10];
  static {
    IBINOP[Tree.BINOP.PLUS   ] = "addi";
    IBINOP[Tree.BINOP.MINUS  ] = "subi";
    IBINOP[Tree.BINOP.MUL    ] = "muli";
    IBINOP[Tree.BINOP.DIV    ] = "divi";
    IBINOP[Tree.BINOP.AND    ] = "andi";
    IBINOP[Tree.BINOP.OR     ] = "ori";
    IBINOP[Tree.BINOP.LSHIFT ] = "slli";
    IBINOP[Tree.BINOP.RSHIFT ] = "srli";
    IBINOP[Tree.BINOP.ARSHIFT] = "srai";
    IBINOP[Tree.BINOP.XOR    ] = "xori";
  }

  private static int shift(int i) {
    int shift = 0;
    if ((i >= 2) && ((i & (i - 1)) == 0)) {
      while (i > 1) {
        shift += 1;
        i >>= 1;
      }
    }
    return shift;
  }

  /*
  munchExp(BINOP(i, e1,       CONST(i)))
  munchExp(BINOP(i, CONST(i), e2))
  munchExp(BINOP(i, e1,       e2))
   */
  Temp munchExp(Tree.BINOP e) {
    Tree.CONST e2 = (Tree.CONST) e.right;
    Temp t = new Temp();

    emit(OPER(IBINOP[e.binop] + " `d0, `s0, " + e2.value,
            L(t), L(munchExp(e.left))));


    return t;
  }

  /*
  munchExp(MEM(BINOP(PLUS,e1,CONST(i))))
  munchExp(MEM(BINOP(PLUS,CONST(i),e1)))
  munchExp(MEM(CONST (i)))
  munchExp(MEM(e1))
  */
  Temp munchExp(Tree.MEM e) {
    Temp t = new Temp();

    if (e.exp instanceof Tree.BINOP binop) {
      Temp e1;
      
      if (binop.left instanceof Tree.TEMP temp) { 
        e1 = temp.temp;
      } else {
        e1 = munchExp(binop.left);
      }
      
      String i = "" + ((Tree.CONST) binop.right).value;
      if (e1 == frame.FP) {
        e1 = frame.SP;
        i += frame.name + "_framesize";
      }

      emit(OPER("ldw `d0, " + i + "(`s0)", L(t), L(e1)));
      return t;
    } else if (e.exp instanceof Tree.CONST cons) {
      emit(OPER("ldw `d0, " + cons.value + "(`s0)",
              L(t), L(frame.ZERO)));
      return t;
    } else {
      if (e.exp instanceof Tree.TEMP temp) {
        if (temp.temp == frame.FP) {
          emit(OPER("ldw `d0, " + frame.name + "_framesize(`s0)",
                  L(t), L(frame.SP)));
          return t;
        }
      }

      emit(OPER("ldw `d0, (`s0)",
              L(t), L(munchExp(e.exp))));
      return t;
    }
  }

  Temp munchExp(Tree.CALL s) {
    if (s.func instanceof Tree.NAME name)
      emit(OPER("call " + name.label.toString(),
              frame.calldefs, munchArgs(0, s.args)));
    else
      emit(OPER("callr `d0",
              frame.calldefs, L(munchExp(s.func), munchArgs(0, s.args))));
    return frame.V0;
  }

  private TempList munchArgs(int i, Tree.ExpList args) {
    if (args == null)
      return null;
    Temp src = munchExp(args.head);
    if (i > frame.maxArgs)
      frame.maxArgs = i;
    switch (i) {
      case 0:
        emit(MOVE("mov `d0 `s0", frame.A0, src));
        break;
      case 1:
        emit(MOVE("mov `d0 `s0", frame.A1, src));
        break;
      case 2:
        emit(MOVE("mov `d0 `s0", frame.A2, src));
        break;
      case 3:
        emit(MOVE("mov `d0 `s0", frame.A3, src));
        break;
      default:
        emit(OPER("sdw `s0 " + (i-1)*frame.wordSize() + "(`s1)",
              null, L(src, L(frame.SP))));
        break;
    }
    return L(src, munchArgs(i+1, args.tail));
  }
}
