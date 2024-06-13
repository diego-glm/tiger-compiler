package FindEscape;

import Absyn.*;
import Symbol.Table;

public class FindEscape {
  Table escEnv = new Table();
  private FunctionDec currentFun = null;

  public FindEscape(Exp var1) {
    this.traverseExp(0, (Exp)var1);
  }

  void traverseVar(int var1, Var var2) {
    if (var2 instanceof SimpleVar) {
      this.traverseVar(var1, (SimpleVar)var2);
    } else if (var2 instanceof FieldVar) {
      this.traverseVar(var1, (FieldVar)var2);
    } else {
      if (!(var2 instanceof SubscriptVar)) {
        throw new Error("FindEscape.traverseVar");
      }

      this.traverseVar(var1, (SubscriptVar)var2);
    }

  }

  void traverseVar(int var1, SimpleVar var2) {
    Escape var3 = (Escape)this.escEnv.get(var2.name);
    if (var3 != null && var3.depth < var1) {
      var3.setEscape();
    }

  }

  void traverseVar(int var1, FieldVar var2) {
    this.traverseVar(var1, var2.var);
  }

  void traverseVar(int var1, SubscriptVar var2) {
    this.traverseVar(var1, var2.var);
    this.traverseExp(var1, var2.index);
  }

  void traverseExp(int var1, Exp var2) {
    if (var2 instanceof VarExp) {
      this.traverseExp(var1, (VarExp)var2);
    } else if (var2 instanceof CallExp) {
      this.traverseExp(var1, (CallExp)var2);
    } else if (var2 instanceof OpExp) {
      this.traverseExp(var1, (OpExp)var2);
    } else if (var2 instanceof RecordExp) {
      this.traverseExp(var1, (RecordExp)var2);
    } else if (var2 instanceof SeqExp) {
      this.traverseExp(var1, (SeqExp)var2);
    } else if (var2 instanceof AssignExp) {
      this.traverseExp(var1, (AssignExp)var2);
    } else if (var2 instanceof IfExp) {
      this.traverseExp(var1, (IfExp)var2);
    } else if (var2 instanceof WhileExp) {
      this.traverseExp(var1, (WhileExp)var2);
    } else if (var2 instanceof ForExp) {
      this.traverseExp(var1, (ForExp)var2);
    } else if (var2 instanceof LetExp) {
      this.traverseExp(var1, (LetExp)var2);
    } else if (var2 instanceof ArrayExp) {
      this.traverseExp(var1, (ArrayExp)var2);
    }

  }

  void traverseExp(int var1, VarExp var2) {
    this.traverseVar(var1, var2.var);
  }

  void traverseExp(int var1, CallExp var2) {
    if (this.currentFun != null) {
      this.currentFun.leaf = false;
    }

    for(ExpList var3 = var2.args; var3 != null; var3 = var3.tail) {
      this.traverseExp(var1, var3.head);
    }

  }

  void traverseExp(int var1, OpExp var2) {
    this.traverseExp(var1, var2.left);
    this.traverseExp(var1, var2.right);
  }

  void traverseExp(int var1, RecordExp var2) {
    for(FieldExpList var3 = var2.fields; var3 != null; var3 = var3.tail) {
      this.traverseExp(var1, var3.init);
    }

  }

  void traverseExp(int var1, SeqExp var2) {
    for(ExpList var3 = var2.list; var3 != null; var3 = var3.tail) {
      this.traverseExp(var1, var3.head);
    }

  }

  void traverseExp(int var1, AssignExp var2) {
    this.traverseVar(var1, var2.var);
    this.traverseExp(var1, var2.exp);
  }

  void traverseExp(int var1, IfExp var2) {
    this.traverseExp(var1, var2.test);
    this.traverseExp(var1, var2.thenclause);
    this.traverseExp(var1, var2.elseclause);
  }

  void traverseExp(int var1, WhileExp var2) {
    this.traverseExp(var1, var2.test);
    this.traverseExp(var1, var2.body);
  }

  void traverseExp(int var1, ForExp var2) {
    this.traverseExp(var1, var2.var.init);
    this.traverseExp(var1, var2.hi);
    this.escEnv.beginScope();
    this.escEnv.put(var2.var.name, new VarEscape(var1, var2.var));
    this.traverseExp(var1, var2.body);
    this.escEnv.endScope();
  }

  void traverseExp(int var1, LetExp var2) {
    this.escEnv.beginScope();

    for(DecList var3 = var2.decs; var3 != null; var3 = var3.tail) {
      this.traverseDec(var1, var3.head);
    }

    this.traverseExp(var1, var2.body);
    this.escEnv.endScope();
  }

  void traverseExp(int var1, ArrayExp var2) {
    this.traverseExp(var1, var2.size);
    this.traverseExp(var1, var2.init);
  }

  void traverseDec(int var1, Dec var2) {
    if (var2 instanceof VarDec) {
      this.traverseDec(var1, (VarDec)var2);
    } else if (var2 instanceof FunctionDec) {
      this.traverseDec(var1, (FunctionDec)var2);
    }

  }

  void traverseDec(int var1, VarDec var2) {
    this.traverseExp(var1, var2.init);
    this.escEnv.put(var2.name, new VarEscape(var1, var2));
  }

  void traverseDec(int var1, FunctionDec var2) {
    FunctionDec var3 = this.currentFun;

    for(FunctionDec var4 = var2; var4 != null; var4 = var4.next) {
      this.escEnv.beginScope();
      this.currentFun = var4;

      for(FieldList var5 = var4.params; var5 != null; var5 = var5.tail) {
        this.escEnv.put(var5.name, new FormalEscape(var1 + 1, var5));
      }

      this.traverseExp(var1 + 1, var4.body);
      this.escEnv.endScope();
    }

    this.currentFun = var3;
  }
}

