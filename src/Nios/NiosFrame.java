package Nios;
import java.util.Hashtable;
import Symbol.Symbol;
import Temp.Temp;
import Temp.Label;
import Frame.Frame;
import Frame.Access;
import Frame.AccessList;

public class NiosFrame extends Frame {
  private int offset = 0;

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
    offset = 0;
    Utils.BoolList lstBool = f;
    AccessList lstAcc = null;
    AccessList lstAccNext;

    if (lstBool != null) { // One element
      lstAcc = new AccessList(allocLocal(lstBool.head), null);
      lstBool = lstBool.tail;
      offset += wordSize();
      lstBool = lstBool.tail;
    }

    formals = lstAcc;
    while (lstBool != null) { // Multiple elements
      lstAccNext = new AccessList(allocLocal(lstBool.head), null);
      lstAcc.tail = lstAccNext;

      lstAcc = lstAccNext;
      offset += wordSize();
      lstBool = lstBool.tail;
    }
  }

  private static final int wordSize = 4;
  public int wordSize() { return wordSize; }

  public Access allocLocal(boolean escape) {
    if (escape) {
      offset -= wordSize;
      return new InFrame(offset);
    } else {
      return new InReg(new Temp());
    }
  }
}
