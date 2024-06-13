package Semant;
import Translate.Exp;
import Types.Type;
import Symbol.Symbol;
import java.util.Hashtable;

public class Semant {
    Env env;
    public Semant(ErrorMsg.ErrorMsg err) {
        this(new Env(err));
    }
    Semant(Env e) {
        env = e;
    }

    public void transProg(Absyn.Exp exp) {
        transExp(exp);
    }

    private void error(int pos, String msg) {
        try {
            env.errorMsg.error(pos, msg);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Singleton
    static final Types.VOID   VOID   = new Types.VOID();
    static final Types.INT    INT    = new Types.INT();
    static final Types.STRING STRING = new Types.STRING();
    static final Types.NIL    NIL    = new Types.NIL();
    static final Types.INT    LOOPVAR = new Types.INT(); // Temporary

    /** Check if two types are the same, using the in_this coerceTo to_that logic */
    private void checkSame(Type is_this, Type to_that, int pos, String msg) {
        if (!((NIL.coerceTo(is_this) || (is_this.actual() instanceof Types.RECORD))  &&
                ((NIL.coerceTo(to_that) || (to_that.actual() instanceof Types.RECORD))) &&
                ((is_this.actual() instanceof Types.RECORD) ^ (to_that.actual() instanceof Types.RECORD))))
            if (!is_this.coerceTo(to_that)) error(pos, msg);
    }

    private void checkString(ExpTy et, int pos) {
        checkSame(STRING, et.ty, pos, "string required");
    }

    private void checkInt(ExpTy et, int pos) {
        checkSame(INT, et.ty, pos, "integer required");
    }

    /***/
    private void checkMustBeVOID(Type ty, int pos) {
        boolean is_it = ty.coerceTo(VOID);
        if (!is_it) error(pos, "result type mismatch");
    }

    /**Check if obj is null*/
    private boolean isNull(Object obj, int pos, String msg) {
        boolean is_it = (obj == null);
        if (is_it) error(pos, "undeclared type: " + msg);
        return is_it;
    }

    /** Check if entry is Var */
    private boolean isVarEntry(Entry et,  int pos, String name) {
        boolean is_it = (et instanceof VarEntry);
        if (!is_it) error(pos, "undeclared variable: " + name);
        return is_it;
    }

    /** Check if entry is Fun */
    private boolean isFunEntry(Entry et, int pos, String name) {
        boolean is_it = (et instanceof FunEntry);
        if (!is_it) error(pos, "undeclared function: " + name);
        return is_it;
    }

    /** Check if given type is RECORD (not by reference) */
    private boolean isRecord(Type ty, int pos, String msg) {
        boolean is_it = (ty instanceof Types.RECORD);
        if (!is_it) error(pos, msg);
        return is_it;
    }

    /** Check if given type is ARRAY (not by reference) */
    private boolean isArray(Type ty, int pos, String msg) {
        boolean is_it = (ty instanceof Types.ARRAY);
        if (!is_it) error(pos, msg);
        return is_it;
    }

    /**Check if the entry is the loop variable and inside the loop*/
    private boolean isForVarLoop(VarEntry entry, boolean loop, int pos) {
        boolean is_it = (entry.ty == LOOPVAR) && loop;
        if (is_it)
            error(pos, "assignment to loop index");
        return is_it;
    }

    ExpTy transExp(Absyn.Exp e) {
        ExpTy result;

        if (e == null)
            return new ExpTy(null, VOID);
        else if (e instanceof Absyn.StringExp) // Str
            result = transExp((Absyn.StringExp) e);
        else if (e instanceof Absyn.IntExp) // Int
            result = transExp((Absyn.IntExp) e);
        else if (e instanceof Absyn.NilExp) // nil
            result = transExp((Absyn.NilExp) e);
        else if (e instanceof Absyn.VarExp) // Var
            result = transExp((Absyn.VarExp) e);
        else if (e instanceof Absyn.OpExp) // Op
            result = transExp((Absyn.OpExp) e);
        else if (e instanceof Absyn.AssignExp) // :=
            result = transExp((Absyn.AssignExp) e);
        else if (e instanceof Absyn.CallExp) // id()
            result = transExp((Absyn.CallExp) e);
        else if (e instanceof Absyn.SeqExp) // (;;;)
            result = transExp((Absyn.SeqExp) e);
        else if (e instanceof Absyn.RecordExp) // id{}
            result = transExp((Absyn.RecordExp) e);
        else if (e instanceof Absyn.ArrayExp) // id[] of
            result = transExp((Absyn.ArrayExp) e);
        else if (e instanceof Absyn.IfExp) // if then else
            result = transExp((Absyn.IfExp) e);
        else if (e instanceof Absyn.WhileExp) // while do
            result = transExp((Absyn.WhileExp) e);
        else if (e instanceof Absyn.ForExp) // for to do
            result = transExp((Absyn.ForExp) e);
        else if (e instanceof Absyn.BreakExp) // break
            result = transExp((Absyn.BreakExp) e);
        else if (e instanceof Absyn.LetExp) // Let in
            result = transExp((Absyn.LetExp) e);
        else throw new Error("Semant.transExp"); // Error
        e.type = result.ty;

        return result;
    }

    ExpTy transExp(Absyn.StringExp e) { return new ExpTy(null, STRING); }

    ExpTy transExp(Absyn.IntExp e) { return new ExpTy(null, INT); }

    ExpTy transExp(Absyn.NilExp e) { return new ExpTy(null, NIL); }

    /** Dispatch for Var */
    ExpTy transVar(Absyn.Var v, boolean loop) {
        ExpTy result;

        if (v instanceof Absyn.SimpleVar) // id
            result = transVar((Absyn.SimpleVar) v, loop);
        else if (v instanceof Absyn.SubscriptVar) // var[exp]
            result = transVar((Absyn.SubscriptVar) v);
        else if (v instanceof Absyn.FieldVar) // var.id
            result = transVar((Absyn.FieldVar) v);
        else throw new Error("Semant.transVar");

        return result;
    }

    ExpTy transVar(Absyn.SimpleVar v, boolean loop) {
        Entry entry = (Entry) env.venv.get(v.name); // id

        if (isVarEntry(entry, v.pos, v.name.toString())) {
            VarEntry ventry = (VarEntry) entry;
            isForVarLoop(ventry, loop, v.pos);
            return new ExpTy(null, ventry.ty);
        }

        return new ExpTy(null, VOID);
    }

    ExpTy transVar(Absyn.FieldVar v) { // lvalue.id
        Type actual = transVar(v.var, false).ty.actual(); // lvalue

        if (isRecord(actual, v.var.pos, "record required")) { // need to be a record id
            for (Types.RECORD f = (Types.RECORD) actual; f != null; f = f.tail)
                if (f.fieldName == v.field) // if one field from Var dec equals to id
                    return new ExpTy(null, f.fieldType);
            error(v.pos, "undeclared field: " + v.field);// Field not found
        }
        return new ExpTy(null, VOID);
    }

    ExpTy transVar(Absyn.SubscriptVar v) { // lvalue[expr]
        Type actual = transVar(v.var, false).ty.actual(); // lvalue
        ExpTy indexTy = transExp(v.index); // expr

        checkInt(indexTy, v.index.pos);
        if (isArray(actual, v.pos, "array required")) { // need to be a array id
            Types.ARRAY arr = (Types.ARRAY) actual;
            return  new ExpTy(null, arr.element);
        }
        return new ExpTy(null, VOID);
    }

    ExpTy transExp(Absyn.OpExp e) {
        ExpTy left = transExp(e.left);
        ExpTy right = transExp(e.right);

        switch (e.oper) {
            case Absyn.OpExp.PLUS:
            case Absyn.OpExp.MINUS:
            case Absyn.OpExp.MUL:
            case Absyn.OpExp.DIV:
                checkInt(left, e.left.pos);
                checkInt(right, e.right.pos);
                break;
            case Absyn.OpExp.EQ:
            case Absyn.OpExp.NE:
                String mss = "incompatible operands to " + (e.oper == Absyn.OpExp.EQ? "equality" : "inequality") + " operator";
                if (NIL.coerceTo(right.ty)) {
                    isRecord(left.ty.actual(), e.pos, "nil can only be comparable to record type");
                } else checkSame(left.ty, right.ty, e.pos, mss);
                break;
            case Absyn.OpExp.LT:
            case Absyn.OpExp.LE:
            case Absyn.OpExp.GT:
            case Absyn.OpExp.GE:
                if (INT.coerceTo(left.ty) || STRING.coerceTo(left.ty))
                    checkSame(left.ty, right.ty, e.pos, "not valid types for comparison operator");
                break;
            default:
                throw new Error("unknown operator");
        }

        return new ExpTy(null, INT);
    }

    ExpTy transExp(Absyn.AssignExp e) {
        ExpTy vt = transVar(e.var, true); //var
        ExpTy et = transExp(e.exp); // exp

        checkSame(et.ty, vt.ty, e.pos, "assignment type mismatch");
        return new ExpTy(null, VOID);
    }
    
    ExpTy transExp(Absyn.CallExp e) {
        Entry entry = (Entry) env.venv.get(e.func);

        if (isFunEntry(entry, e.pos, e.func.toString())) {
            FunEntry fun_entry = (FunEntry) entry;
            transArgs(e.pos, fun_entry.formals, e.args);
            return new ExpTy(null, fun_entry.result);
        }

        return new ExpTy(null, VOID);
    }

    ExpTy transExp(Absyn.SeqExp e) {
        Absyn.ExpList explst = e.list;
        ExpTy last_exp = new ExpTy(null, VOID);

        while (explst != null) {
            last_exp = transExp(explst.head);
            explst = explst.tail;
        }

        return new ExpTy(null, last_exp.ty);
    }

    ExpTy transExp(Absyn.RecordExp e) {
        Types.NAME name = (Types.NAME) env.tenv.get(e.typ);

        if (!isNull(name, e.pos, e.typ.toString())) {
            Type actual = name.actual();
            if (isRecord(actual, e.pos, "record type expected")) {
                Types.RECORD r = (Types.RECORD) actual;
                transFields(e.pos, r, e.fields);
                return new ExpTy(null, name);
            }
        }

        return new ExpTy(null, VOID);
    }

    ExpTy transExp(Absyn.ArrayExp e) {
        Types.NAME name = (Types.NAME) env.tenv.get(e.typ);
        ExpTy sizet = transExp(e.size);
        ExpTy initt = transExp(e.init);

        checkInt(sizet, e.size.pos);
        if (!isNull(name, e.pos, e.typ.toString())) {
            Type actual = name.actual();
            if (isArray(actual, e.pos, "array type expected")) {
                Types.ARRAY a = (Types.ARRAY) actual;
                checkSame(initt.ty, a.element, e.init.pos, "element type mismatch");
                return new ExpTy(null, name);
            }
        }

        return new ExpTy(null, VOID);
    }

    ExpTy transExp(Absyn.IfExp e) {
        ExpTy testt = transExp(e.test);
        ExpTy clause1t = transExp(e.thenclause);
        ExpTy clause2t = transExp(e.elseclause);

        checkInt(testt, e.pos);
        checkSame(clause1t.ty, clause2t.ty, e.pos, "result type mismatch");

        return new ExpTy(null, clause2t.ty);
    }

    ExpTy transExp(Absyn.WhileExp e) {
        ExpTy testt = transExp(e.test);
        ExpTy bodyt = transExp(e.body);

        checkInt(testt, e.test.pos);
        checkMustBeVOID(bodyt.ty, e.body.pos);

        return new ExpTy(null, VOID);
    }

    ExpTy transExp(Absyn.ForExp e) {
        ExpTy start = transExp(e.var.init);
        ExpTy end = transExp(e.hi);

        checkInt(start, e.var.pos);
        checkInt(end, e.hi.pos);

        env.venv.beginScope();

        e.var.entry = new VarEntry(LOOPVAR); // Per manual, the loop var cannot be alter inside loop
        env.venv.put(e.var.name, e.var.entry);

        ExpTy body = transExp(e.body);

        env.venv.endScope();

        checkMustBeVOID(body.ty, e.body.pos);

        return new ExpTy(null, VOID);
    }

    ExpTy transExp(Absyn.BreakExp e) {
        error(e.pos, "break outside loop");
        return new ExpTy(null, VOID);
    }

    ExpTy transExp(Absyn.LetExp e) {
        env.venv.beginScope();
        env.tenv.beginScope();
        for (Absyn.DecList d = e.decs; d != null; d = d.tail) {
            transDec(d.head);
        }
        ExpTy body = transExp(e.body);
        env.venv.endScope();
        env.tenv.endScope();
        return new ExpTy(null, body.ty);
    }

    /** Dispatch for Dec */
    Exp transDec(Absyn.Dec d) {
        Exp result;

        if (d instanceof Absyn.TypeDec)
            result = transDec((Absyn.TypeDec)d);
        else if (d instanceof Absyn.VarDec)
            result = transDec((Absyn.VarDec)d);
        else if (d instanceof Absyn.FunctionDec)
            result = transDec((Absyn.FunctionDec)d);
        else throw new Error("Semant.transDec");

        return result;
    }

    /**Var Declaration*/
    Exp transDec(Absyn.VarDec d) {
        ExpTy init = transExp(d.init);
        Type type;
        if (d.typ == null) { // type-id not given
            if (init.ty.coerceTo(NIL)) error(d.pos, "record type required"); // Must not be NIL
            type = init.ty;
        } else { // type-id given
            type = transTy(d.typ);
            checkSame(init.ty, type, d.pos, "assignment type mismatch");
        }
        d.entry = new VarEntry(type);
        env.venv.put(d.name, d.entry);
        return null;
    }

    /**Function Declaration*/
    Exp transDec(Absyn.FunctionDec d) {
        Hashtable<Symbol, Symbol> table = new Hashtable<>();
        for (Absyn.FunctionDec f = d; f != null; f = f.next) {
            if (table.put(f.name, f.name) != null)
                error(f.pos, "function redeclared");
            Types.RECORD fields = transTypeFields(new Hashtable<>(), f.params);
            Type type = transTy(f.result);

            f.entry = new FunEntry(fields, type);
            this.env.venv.put(f.name, f.entry);
        }

        for (Absyn.FunctionDec f = d; f != null; f = f.next) {
            this.env.venv.beginScope();
            putEnvLinkedList(f.entry.formals);
            Semant fun = new Semant(this.env);
            ExpTy body = fun.transExp(f.body);
            if (!body.ty.coerceTo(f.entry.result))
                error(f.body.pos, "result type mismatch");
            this.env.venv.endScope();
        }
        return null;
    }

    /**Type Declaration*/
    Exp transDec(Absyn.TypeDec d) {
        Hashtable<Symbol, Symbol> table = new Hashtable<>();

        for (Absyn.TypeDec type = d; type != null; type = type.next) {
            if (table.put(type.name, type.name) != null)
                error(type.pos, "type redeclared");
            type.entry = new Types.NAME(type.name);
            this.env.tenv.put(type.name, type.entry);
        }

        for (Absyn.TypeDec type = d; type != null; type = type.next) {
            Types.NAME name = type.entry;
            name.bind(transTy(type.ty));
        }

        for (Absyn.TypeDec type = d; type != null; type = type.next) {
            Types.NAME name = type.entry;
            if (name.isLoop())
                error(type.pos, "illegal type cycle");
        }

        return null;
    }

    /**Dispatch for Ty*/
    Type transTy(Absyn.Ty ty) {
        Type result;

        if (ty instanceof Absyn.NameTy)
            result = transTy((Absyn.NameTy) ty);
        else if (ty instanceof Absyn.RecordTy)
            result = transTy((Absyn.RecordTy) ty);
        else if (ty instanceof Absyn.ArrayTy)
            result = transTy((Absyn.ArrayTy) ty);
        else throw new Error("Semant.transTy");

        return result;
    }

    /**NameTy*/
    Type transTy(Absyn.NameTy namet) {
        if (namet != null) {
            Types.NAME name = (Types.NAME) this.env.tenv.get(namet.name);
            if (name != null)
                return name;
            error(namet.pos, "undefined type: " + namet.name);
        }
        return VOID;
    }

    /**RecordTy*/
    Type transTy(Absyn.RecordTy recordt) {
        Types.RECORD type = transTypeFields(new Hashtable<>(), recordt.fields);
        if (type != null)
            return type;
        return VOID;
    }

    /**ArrayTy*/
    Type transTy(Absyn.ArrayTy arrayt) {
        Types.NAME name = (Types.NAME)this.env.tenv.get(arrayt.typ);
        if (name != null)
            return new Types.ARRAY(name);
        error(arrayt.pos, "undefined type: " + arrayt.typ);
        return VOID;
    }

    /** Helper */
    private void transArgs(int pos, Types.RECORD rec, Absyn.ExpList lst) {
        if (rec == null) {
            if (lst != null) {
                error(lst.head.pos, "too many arguments");
            }

        } else if (lst == null) {
            error(pos, "missing argument for " + String.valueOf(rec.fieldName));
        } else {
            if (!transExp(lst.head).ty.coerceTo(rec.fieldType)) {
                error(lst.head.pos, "argument type mismatch");
            }

            transArgs(pos, rec.tail, lst.tail);
        }
    }

    /** Helper */
    private void transFields(int pos, Types.RECORD rec, Absyn.FieldExpList f_lst) {
        if (rec == null) {
            if (f_lst == null) return;
            error(f_lst.pos, "too many expressions");
            return;
        }
        if (f_lst == null) {
            error(pos, "missing expression for " + rec.fieldName);
            return;
        }
        // Continue
        ExpTy e = transExp(f_lst.init);
        if (f_lst.name != rec.fieldName)
            error(f_lst.pos, "field name mismatch");
        if (!e.ty.coerceTo(rec.fieldType))
            error(f_lst.pos, "field type mismatch");
        transFields(pos, rec.tail, f_lst.tail);
    }

    /** Helper */
    private void putEnvLinkedList(Types.RECORD f) {
        if (f != null) {
            this.env.venv.put(f.fieldName, new VarEntry(f.fieldType));
            putEnvLinkedList(f.tail);
        }
    }

    /** Helper */
    private Types.RECORD transTypeFields(Hashtable<Symbol, Symbol> table, Absyn.FieldList f) {
        if (f != null) {
            Types.NAME name = (Types.NAME) this.env.tenv.get(f.typ);
            isNull(name, f.pos, f.typ.toString());
            if (table.put(f.name, f.name) != null)
                error(f.pos, "function parameter/record field redeclared: " + f.name);
            return new Types.RECORD(f.name, name, transTypeFields(table, f.tail));
        }
        return null;
    }
}