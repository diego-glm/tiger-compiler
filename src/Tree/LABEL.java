package Tree;
import Temp.Label;
@SuppressWarnings("unused")
public class LABEL extends Stm {
  public Label label;
  public LABEL(Label l) {label=l;}
  public ExpList kids() {return null;}
  public Stm build(ExpList kids) {
    return this;
  }
}
