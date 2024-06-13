package Tree;

@SuppressWarnings("unused")
public class UEXP extends Stm {
  public Exp exp; 
  public UEXP(Exp e) {exp=e;}
  public ExpList kids() {return new ExpList(exp,null);}
  public Stm build(ExpList kids) {
    return new UEXP(kids.head);
  }
}
