package Nios;
import java.util.Hashtable;
import Symbol.Symbol;
import Temp.Temp;
import Temp.Label;
import Frame.Frame;
import Frame.Access;
import Frame.AccessList;
import Frame.Proc;
import Temp.TempList;

public class NiosFrame extends Frame {

  private int count = 0;
  public Frame newFrame(Symbol name, Utils.BoolList formals) {
    Label label;
    if (name == null)
      label = new Label();
    else if (this.name != null)
      label = new Label(this.name + "." + name + "." + count++);
    else
      label = new Label(name);
    return new NiosFrame(label, formals);
  }

  public NiosFrame() {}
  private NiosFrame(Label n, Utils.BoolList f) {
    name = n;
    formals = allocFormals(0, f);
  }

  private static final int wordSize = 4;
  public int wordSize() { return wordSize; }

  private int offset = 0;
  public Access allocLocal(boolean escape) {
    if (escape) {
      offset -= wordSize;
      return new InFrame(offset);
    } else
      return new InReg(new Temp());
  }

  private AccessList allocFormals(int offset, Utils.BoolList formals) {
    if (formals == null)
      return null;
    Access a;
    if (formals.head)
      a = new InFrame(offset);
    else
      a = new InReg(new Temp());
    return new AccessList(a, allocFormals(offset + wordSize, formals.tail));
  }

  static final Temp ZERO = new Temp(); // zero reg
  static final Temp AT = new Temp(); // reserved for assembler
  static final Temp V0 = new Temp(); // function result
  static final Temp V1 = new Temp(); // second function result
  static final Temp A0 = new Temp(); // argument1
  static final Temp A1 = new Temp(); // argument2
  static final Temp A2 = new Temp(); // argument3
  static final Temp A3 = new Temp(); // argument4
  static final Temp T0 = new Temp(); // caller-saved
  static final Temp T1 = new Temp();
  static final Temp T2 = new Temp();
  static final Temp T3 = new Temp();
  static final Temp T4 = new Temp();
  static final Temp T5 = new Temp();
  static final Temp T6 = new Temp();
  static final Temp T7 = new Temp();
  static final Temp S0 = new Temp(); // callee-saved
  static final Temp S1 = new Temp();
  static final Temp S2 = new Temp();
  static final Temp S3 = new Temp();
  static final Temp S4 = new Temp();
  static final Temp S5 = new Temp();
  static final Temp S6 = new Temp();
  static final Temp S7 = new Temp();
  static final Temp T8 = new Temp(); // caller-saved
  static final Temp T9 = new Temp();
  static final Temp K0 = new Temp(); // reserved for OS kernel
  static final Temp K1 = new Temp(); // reserved for OS kernel
  static final Temp GP = new Temp(); // pointer to global area
  static final Temp SP = new Temp(); // stack pointer
  static final Temp FP = new Temp(); // virtual frame pointer (eliminated)
  static final Temp S8 = new Temp(); // actual frame pointer
  static final Temp RA = new Temp(); // return address

  public Temp FP() { return FP; }
  public Temp RV() { return V0; }

  private static Hashtable<String, Label> labels = new Hashtable<String, Label>();
  public Tree.Exp externalCall(String func, Tree.ExpList args) {
    String u = func.intern();
    Label l = labels.get(u);
    if (l == null) {
      l = new Label("_" + u);
      labels.put(u, l);
    }
    return new Tree.CALL(new Tree.NAME(l), args);
  }

  public Tree.Stm procEntryExit1(Tree.Stm body) {
    body = saveTemps(argRegs, formals, body);
    TempList callees = L(RA, calleeSaves);
    // change true to false if your register allocator handles spills
    AccessList a = allocCallees(callees, true);
    body = saveTemps(callees, a, body);
    body = restoreTemps(callees, a, body);
    return body;
  }

  public String string(Label lab, String string) {
    int length = string.length();
    String lit = "";
    for (int i = 0; i < length; i++) {
      char c = string.charAt(i);
      switch(c) {
        case '\b': lit += "\\b"; break;
        case '\t': lit += "\\t"; break;
        case '\n': lit += "\\n"; break;
        case '\f': lit += "\\f"; break;
        case '\r': lit += "\\r"; break;
        case '\"': lit += "\\\""; break;
        case '\\': lit += "\\\\"; break;
        default:
                   if (c < ' ' || c > '~') {
                     int v = (int)c;
                     lit += "\\" + ((v>>6)&7) + ((v>>3)&7) + (v&7);
                   } else
                     lit += c;
                   break;
      }
    }
    return "\t.data\n\t.word " + length + "\n" + lab.toString()
      + ":\t.asciz\t\"" + lit + "\"";
  }

  private static final Label badPtr = new Label("_BADPTR");
  public Label badPtr() {
    return badPtr;
  }

  private static final Label badSub = new Label("_BADSUB");
  public Label badSub() {
    return badSub;
  }

  private static final Hashtable<Temp, String> tempMap = new Hashtable<Temp, String>(32);
  static {
    tempMap.put(ZERO, "zero");
    tempMap.put(AT,   "at");
    tempMap.put(V0,   "r2");
    tempMap.put(V1,   "r3");
    tempMap.put(A0,   "r4");
    tempMap.put(A1,   "r5");
    tempMap.put(A2,   "r6");
    tempMap.put(A3,   "r7");
    tempMap.put(T0,   "r8");
    tempMap.put(T1,   "r9");
    tempMap.put(T2,   "r10");
    tempMap.put(T3,   "r11");
    tempMap.put(T4,   "r12");
    tempMap.put(T5,   "r13");
    tempMap.put(T6,   "r14");
    tempMap.put(T7,   "r15");
    tempMap.put(T8,   "r24"); // exception temporary
    tempMap.put(T9,   "r25"); // break temporary
    tempMap.put(S0,   "r16");
    tempMap.put(S1,   "r17");
    tempMap.put(S2,   "r18");
    tempMap.put(S3,   "r19");
    tempMap.put(S4,   "r20");
    tempMap.put(S5,   "r21");
    tempMap.put(S6,   "r22");
    tempMap.put(S7,   "r23");
    tempMap.put(GP,   "gp");
    tempMap.put(SP,   "sp");
    tempMap.put(FP,   "fp");
    tempMap.put(RA,   "ra");
    tempMap.put(K0,   "ea");
    tempMap.put(K1,   "ba");
    tempMap.put(S8,   "fp");	// $s8 is alias for $fp (when virtual)
  }
  public String tempMap(Temp temp) {
    return tempMap.get(temp);
  }

  static TempList L(Temp h, TempList t) {
    return new TempList(h, t);
  }
  static TempList L(Temp h) {
    return new TempList(h, null);
  }
  static TempList L(TempList a, TempList b) {
    return new TempList(a, b);
  }

  // Register lists: must not overlap and must include every register that
  // might show up in Assem instructions
  static TempList specialRegs, argRegs, callerSaves, calleeSaves;
  {
    // registers dedicated to special purposes
    specialRegs = L(ZERO,L(AT,L(K0,L(K1,L(GP,L(FP,L(SP,L(RA))))))));
    // registers in which to pass outgoing arguments (including static link)
    argRegs     = L(A0,L(A1,L(A2,L(A3))));
    // registers that the called procedure (callee) must preserve for caller
    calleeSaves = L(S0,L(S1,L(S2,L(S3,L(S4,L(S5,L(S6,L(S7,L(S8)))))))));
    // registers that the callee may trash
    callerSaves = L(T0,L(T1,L(T2,L(T3,L(T4,L(T5,L(T6,L(T7,L(T8,L(T9))))))))));
    callerSaves = L(V0,L(V1, callerSaves));
  }

  static Assem.InstrList append(Assem.InstrList a, Assem.InstrList b) {
    if (a == null)
      return b;
    Assem.InstrList p;
    for (p = a; p.tail != null; p = p.tail);
    p.tail = b;
    return a;
  }

  static TempList calldefs, returnSink;
  {
    // registers defined by a call
    calldefs = L(RA, L(argRegs, callerSaves));
    // registers live on return
    returnSink = L(V0, L(specialRegs, calleeSaves));
  }

  public Assem.InstrList procEntryExit2(Assem.InstrList body) {
    return append(body,
        new Assem.InstrList(
          new Assem.OPER("", null, returnSink), null));
  }

  int maxArgs = 0;
  public Proc procEntryExit3(Assem.InstrList body) {
    int frameSize = maxArgs * wordSize - offset;
    String pre = "\t.text\n" + name + ":\n" + name + "_framesize=" + frameSize;
    String post = "\tret";
    if (frameSize != 0) {
      pre += "\n\tsubi sp, sp, " + name + "_framesize";
      post = "\taddi sp, sp,  " + name + "_framesize\n" + post;
    }
    return new Proc(pre, body, post);
  }

  public Assem.InstrList codegen(Tree.Stm stm) {
    return (new Codegen(this)).codegen(stm);
  }
  static TempList registers;
  {
    registers = L(argRegs, specialRegs);
    registers = L(calleeSaves, registers);
    registers = L(callerSaves, registers);
  }
  public TempList registers() {
    return registers;
  }

  private AccessList allocCallees(TempList t, boolean spill) {
    if (t == null)
      return null;
    return new AccessList(allocLocal(spill), allocCallees(t.tail, spill));
  }

  private Tree.Stm saveTemps(TempList t, AccessList a, Tree.Stm body) {
    if (t == null || a == null)
      return body;
    body = saveTemps(t.tail, a.tail, body);
    Tree.Stm move = new Tree.MOVE(a.head.exp(new Tree.TEMP(FP)),
        new Tree.TEMP(t.head));
    if (body == null)
      return move;
    return new Tree.SEQ(move, body);
  }

  private Tree.Stm restoreTemps(TempList t, AccessList a, Tree.Stm body) {
    if (t == null || a == null)
      return body;
    body = restoreTemps(t.tail, a.tail, body);
    Tree.Stm move = new Tree.MOVE(new Tree.TEMP(t.head),
        a.head.exp(new Tree.TEMP(FP)));
    if (body == null)
      return move;
    return new Tree.SEQ(body, move);
  }
}
