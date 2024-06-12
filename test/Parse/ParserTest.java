package Parse;

import Absyn.*;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ParserTest {
    private static Grammar parser;

    @Test
    @Order(1)
    public void parseTree_basicExp() throws Exception {
        Exp exp;

        exp = scan("\"string\"");
        assertInstanceOf(StringExp.class, exp);

        exp = scan("1");
        assertInstanceOf(IntExp.class, exp);

        exp = scan("nil");
        assertInstanceOf(NilExp.class, exp);
    }

    @Test
    @Order(2)
    public void parseTree_SeqExp() throws Exception {
        Exp exp;
        ExpList lst;

        exp = scan("()");

        assertInstanceOf(SeqExp.class, exp);
        assertNull(((SeqExp) exp).list);

        exp = scan("(1)");

        assertInstanceOf(SeqExp.class, exp);
        assertNotNull(((SeqExp) exp).list); lst = ((SeqExp) exp).list;
        assertInstanceOf(IntExp.class, lst.head);
        assertNull(lst.tail);

        exp = scan("(1; 1; 1; 1)");

        assertInstanceOf(SeqExp.class, exp);
        assertNotNull(((SeqExp) exp).list);
        for (lst = ((SeqExp) exp).list; lst != null ; lst = lst.tail) {
            assertInstanceOf(IntExp.class, lst.head);
        }
    }

    @Test
    @Order(3)
    public void parseTree_Variable() throws Exception {
        Exp exp;
        VarExp varExp;
        SubscriptVar subVar;
        FieldVar fldVar;

        String[] input = {"id", "id[1]", "id1.id2"};
        for (int i = 0; i < input.length; i++) {
            exp = scan(input[i]);

            assertInstanceOf(VarExp.class, exp); varExp = (VarExp) exp;
            switch (i) {
                case 0 : assertInstanceOf(SimpleVar.class, varExp.var); break;
                case 1 : assertInstanceOf(SubscriptVar.class, varExp.var); break;
                case 2 : assertInstanceOf(FieldVar.class, varExp.var); break;
            }
        }

        // Nested variables
        input = new String[]{"id[1][2]", "id2.id1.id0", "id[1].id", "id.id[1]"};
        for (int i = 0; i < input.length; i++) {
            exp = scan(input[i]);
            assertInstanceOf(VarExp.class, exp);
            varExp = (VarExp) exp;
            switch (i) {
                case 0 :
                    assertInstanceOf(SubscriptVar.class, varExp.var); subVar = (SubscriptVar) varExp.var;
                    assertInstanceOf(IntExp.class, subVar.index);
                    assertEquals(2, ((IntExp) subVar.index).value);

                    assertInstanceOf(SubscriptVar.class, subVar.var); subVar = (SubscriptVar) subVar.var;
                    assertInstanceOf(IntExp.class, subVar.index);
                    assertEquals(1, ((IntExp) subVar.index).value);
                    break;
                case 1 :
                    assertInstanceOf(FieldVar.class, varExp.var); fldVar = (FieldVar) varExp.var;
                    assertEquals("id0", fldVar.field.toString());

                    assertInstanceOf(FieldVar.class, fldVar.var); fldVar = (FieldVar) fldVar.var;
                    assertEquals("id1", fldVar.field.toString());

                    assertInstanceOf(SimpleVar.class, fldVar.var);
                    assertEquals("id2", ((SimpleVar) fldVar.var).name.toString());
                    break;
                case 2 :
                    assertInstanceOf(FieldVar.class, varExp.var); fldVar = (FieldVar) varExp.var;
                    assertInstanceOf(SubscriptVar.class, fldVar.var);
                    break;
                case 3 :
                    assertInstanceOf(SubscriptVar.class, varExp.var); subVar = (SubscriptVar) varExp.var;
                    assertInstanceOf(FieldVar.class, subVar.var);
                    break;
            }
        }
    }

    @Test
    @Order(4)
    public void parseTree_Operation() throws Exception {
        Exp exp;
        OpExp opExp;
        int[] oper = {OpExp.PLUS, OpExp.MINUS, OpExp.MUL, OpExp.DIV, OpExp.EQ,
                OpExp.NE, OpExp.LT, OpExp.LE, OpExp.GT, OpExp.GE, OpExp.MINUS};
        int i = 0;

        exp = scan("(1+1; 1-1; 1*1; 1/1; 1=1; 1<>1; 1<1; 1<=1; 1>1; 1>=1; -1)");

        assertNotNull(((SeqExp) exp).list);
        assertInstanceOf(SeqExp.class, exp);
        for (ExpList expLst = ((SeqExp) exp).list;
             expLst != null; expLst = expLst.tail) {

            assertInstanceOf(OpExp.class, expLst.head); opExp = (OpExp) expLst.head;
            assertEquals(oper[i++], opExp.oper);
        }
    }

    @Test
    @Order(5)
    public void parseTree_Assign() throws Exception {
        Exp exp;
        AssignExp ass;
        ExpList lst;
        int count = 0;

        exp = scan("(id := 1; id[1] := \"this\"; id1.id2 := 2)");

        assertInstanceOf(SeqExp.class, exp);
        assertNotNull(((SeqExp) exp).list);
        for (lst = ((SeqExp) exp).list; lst != null ; lst = lst.tail) {
            assertInstanceOf(AssignExp.class, lst.head); ass = (AssignExp) lst.head;
            switch (count++) {
                case 0 :
                    assertInstanceOf(SimpleVar.class, ass.var);
                    assertInstanceOf(IntExp.class, ass.exp);
                    assertEquals(1, ((IntExp) ass.exp).value);
                    break;
                case 1 :
                    assertInstanceOf(SubscriptVar.class, ass.var);
                    assertInstanceOf(StringExp.class, ass.exp);
                    assertEquals("this", ((StringExp) ass.exp).value);
                    break;
                case 2 :
                    assertInstanceOf(FieldVar.class, ass.var);
                    assertInstanceOf(IntExp.class, ass.exp);
                    assertEquals(2, ((IntExp) ass.exp).value);
                    break;
            }
        }
    }

    @Test
    @Order(6)
    public void parseTree_Call() throws Exception {
        Exp exp;
        CallExp call;
        ExpList lst, arg;
        int count = 0;

        exp = scan("(id1(); id2(1); id3(2, \"string\"))");

        assertInstanceOf(SeqExp.class, exp);
        assertNotNull(((SeqExp) exp).list);
        for (lst = ((SeqExp) exp).list; lst != null ; lst = lst.tail) {
            assertInstanceOf(CallExp.class, lst.head);
            call = (CallExp) lst.head;
            switch (count++) {
                case 0 :
                    assertEquals("id1", call.func.toString());
                    assertNull(call.args);
                    break;
                case 1 :
                    assertEquals("id2", call.func.toString());
                    assertInstanceOf(ExpList.class, call.args); arg = call.args;
                    assertInstanceOf(IntExp.class, arg.head);
                    assertEquals(1, ((IntExp) arg.head).value);
                    break;
                case 2 :
                    assertEquals("id3", call.func.toString());
                    assertInstanceOf(ExpList.class, call.args); arg = call.args;
                    assertInstanceOf(IntExp.class, arg.head);
                    assertEquals(2, ((IntExp) arg.head).value); arg = arg.tail;
                    assertInstanceOf(StringExp.class, arg.head);
                    assertEquals("string", ((StringExp) arg.head).value);
                    break;
            }
        }
    }

    @Test
    @Order(7)
    public void parseTree_Record() throws Exception {
        Exp exp;
        RecordExp rcd;
        FieldExpList lst;

        exp = scan("idr{}");

        assertInstanceOf(RecordExp.class, exp); rcd = (RecordExp) exp;
        assertEquals("idr", rcd.typ.toString());
        assertNull(rcd.fields);
        
        exp = scan("idr{id1 = 0}");

        assertInstanceOf(RecordExp.class, exp); rcd = (RecordExp) exp;
        assertEquals("idr", rcd.typ.toString());
        assertInstanceOf(FieldExpList.class, rcd.fields); lst = rcd.fields;
        assertEquals("id1", lst.name.toString());
        assertInstanceOf(IntExp.class, lst.init);
        assertNull(lst.tail);

        exp = scan("idr{id1 = 0, id2 = \"str\"}");

        assertInstanceOf(RecordExp.class, exp); rcd = (RecordExp) exp;
        assertEquals("idr", rcd.typ.toString());
        assertInstanceOf(FieldExpList.class, rcd.fields); lst = rcd.fields;
        assertEquals("id1", lst.name.toString());
        assertInstanceOf(IntExp.class, lst.init);

        assertInstanceOf(FieldExpList.class, lst.tail); lst = lst.tail;
        assertEquals("id2", lst.name.toString());
        assertInstanceOf(StringExp.class, lst.init);
    }

    @Test
    @Order(8)
    public void parseTree_Array() throws Exception {
        Exp exp;
        ArrayExp arr;

        exp = scan("id[1] of 0");

        assertInstanceOf(ArrayExp.class, exp); arr = (ArrayExp) exp;
        assertEquals("id", arr.typ.toString());
        assertInstanceOf(IntExp.class, arr.size);
        assertEquals(1, ((IntExp) arr.size).value);
        assertInstanceOf(IntExp.class, arr.init);
        assertEquals(0, ((IntExp) arr.init).value);
    }

    @Test
    @Order(9)
    public void parseTree_AND_OR() throws Exception {
        Exp exp;
        SimpleVar var;
        IfExp ifexp;

        exp = scan("test1 & test2");

        assertInstanceOf(IfExp.class, exp); ifexp = (IfExp) exp;

        assertInstanceOf(VarExp.class, ifexp.test);
        assertInstanceOf(SimpleVar.class, ((VarExp) ifexp.test).var); var = (SimpleVar) ((VarExp) ifexp.test).var;
        assertEquals("test1", var.name.toString());

        assertInstanceOf(VarExp.class, ifexp.thenclause);
        assertInstanceOf(SimpleVar.class, ((VarExp) ifexp.thenclause).var); var = (SimpleVar) ((VarExp) ifexp.thenclause).var;
        assertEquals("test2", var.name.toString());

        assertInstanceOf(IntExp.class, ifexp.elseclause);
        assertEquals(0, ((IntExp) ifexp.elseclause).value);

        exp = scan("test1 | test2");

        assertInstanceOf(IfExp.class, exp); ifexp = (IfExp) exp;

        assertInstanceOf(VarExp.class, ifexp.test);
        assertInstanceOf(SimpleVar.class, ((VarExp) ifexp.test).var); var = (SimpleVar) ((VarExp) ifexp.test).var;
        assertEquals("test1", var.name.toString());

        assertInstanceOf(IntExp.class, ifexp.thenclause);
        assertEquals(1, ((IntExp) ifexp.thenclause).value);

        assertInstanceOf(VarExp.class, ifexp.elseclause);
        assertInstanceOf(SimpleVar.class, ((VarExp) ifexp.elseclause).var); var = (SimpleVar) ((VarExp) ifexp.elseclause).var;
        assertEquals("test2", var.name.toString());
    }

    @Test
    @Order(9)
    public void parseTree_IfThenElse() throws Exception {
        Exp exp;
        IfExp ifexp, ifexp2;
        int count = 0;

        String[] input = {"if 1 then 2",
                          "if 1 then 2 else 3",
                          "if 1 then if 11 then 22 else 33",
                          "if 1 then if 11 then 22 else 33 else 3",};

        for (String s : input) {
            exp = scan(s);

            assertInstanceOf(IfExp.class, exp);
            ifexp = (IfExp) exp;
            switch (count++) {
                case 0: // if 1 then 2
                    assertInstanceOf(IntExp.class, ifexp.test);
                    assertEquals(1, ((IntExp) ifexp.test).value);
                    assertInstanceOf(IntExp.class, ifexp.thenclause);
                    assertEquals(2, ((IntExp) ifexp.thenclause).value);
                    assertNull(ifexp.elseclause);
                    break;
                case 1: // if 1 then 2 else 3"
                    assertInstanceOf(IntExp.class, ifexp.test);
                    assertEquals(1, ((IntExp) ifexp.test).value);
                    assertInstanceOf(IntExp.class, ifexp.thenclause);
                    assertEquals(2, ((IntExp) ifexp.thenclause).value);
                    assertInstanceOf(IntExp.class, ifexp.elseclause);
                    assertEquals(3, ((IntExp) ifexp.elseclause).value);
                    break;
                case 2: // if 1 then if 11 then 22 else 33
                    assertInstanceOf(IntExp.class, ifexp.test);
                    assertEquals(1, ((IntExp) ifexp.test).value);
                    assertInstanceOf(IfExp.class, ifexp.thenclause);
                    ifexp2 = (IfExp) ifexp.thenclause;
                    assertInstanceOf(IntExp.class, ifexp2.test);
                    assertEquals(11, ((IntExp) ifexp2.test).value);
                    assertInstanceOf(IntExp.class, ifexp2.thenclause);
                    assertEquals(22, ((IntExp) ifexp2.thenclause).value);
                    assertInstanceOf(IntExp.class, ifexp2.elseclause);
                    assertEquals(33, ((IntExp) ifexp2.elseclause).value);
                    assertNull(ifexp.elseclause);
                    break;
                case 3: // if 1 then if 11 then 22 else 33 else 3
                    assertInstanceOf(IntExp.class, ifexp.test);
                    assertEquals(1, ((IntExp) ifexp.test).value);
                    assertInstanceOf(IfExp.class, ifexp.thenclause);
                    ifexp2 = (IfExp) ifexp.thenclause;
                    assertInstanceOf(IntExp.class, ifexp2.test);
                    assertEquals(11, ((IntExp) ifexp2.test).value);
                    assertInstanceOf(IntExp.class, ifexp2.thenclause);
                    assertEquals(22, ((IntExp) ifexp2.thenclause).value);
                    assertInstanceOf(IntExp.class, ifexp2.elseclause);
                    assertEquals(33, ((IntExp) ifexp2.elseclause).value);
                    assertInstanceOf(IntExp.class, ifexp.elseclause);
                    assertEquals(3, ((IntExp) ifexp.elseclause).value);
                    break;
            }
        }
    }

    @Test
    @Order(10)
    public void parseTree_While() throws Exception {
        Exp exp;
        WhileExp wh;

        exp = scan("while 1 do 2");

        assertInstanceOf(WhileExp.class, exp); wh = (WhileExp) exp;
        assertInstanceOf(IntExp.class, wh.test);
        assertEquals(1, ((IntExp) wh.test).value);
        assertInstanceOf(IntExp.class, wh.body);
        assertEquals(2, ((IntExp) wh.body).value);
    }

    @Test
    @Order(11)
    public void parseTree_For() throws Exception {
        Exp exp;
        ForExp fo;

        exp = scan("for i := 0 to 5 do 2");

        assertInstanceOf(ForExp.class, exp); fo = (ForExp) exp;

        assertEquals("i", fo.var.name.toString());
        assertInstanceOf(IntExp.class, fo.var.init);
        assertEquals(0, ((IntExp) fo.var.init).value);

        assertInstanceOf(IntExp.class, fo.hi);
        assertEquals(5, ((IntExp) fo.hi).value);

        assertInstanceOf(IntExp.class, fo.body);
        assertEquals(2, ((IntExp) fo.body).value);
    }

    @Test
    @Order(12)
    public void parseTree_Break() throws Exception {
        Exp exp;

        exp = scan("break");

        assertInstanceOf(BreakExp.class, exp);
    }

    @Test
    @Order(13)
    public void parseTree_Let() throws Exception {
        Exp exp;
        LetExp l;
        DecList decs;
        Dec dec;
        TypeDec type;
        VarDec var;
        FunctionDec fun;
        FieldList field;

        exp = scan("let in end");

        assertInstanceOf(LetExp.class, exp); l = (LetExp) exp;
        assertNull(l.decs);
        assertInstanceOf(SeqExp.class, l.body);
        assertNull(((SeqExp) l.body).list);

        exp = scan("let in 1 end");

        assertInstanceOf(LetExp.class, exp); l = (LetExp) exp;
        assertNull(l.decs);
        assertNotNull(l.body);
        assertInstanceOf(SeqExp.class, l.body);
        assertInstanceOf(IntExp.class, ((SeqExp) l.body).list.head);

        exp = scan("" +
                "let " +
                    "type myint = int " +
                    "var num := 1 " +
                    "function g1() = 2 " +
                    "var num:int := 3 " +
                    "function g2():int = 4" +
                "in end");

        assertInstanceOf(LetExp.class, exp); l = (LetExp) exp;
        assertNotNull(l.decs);
        assertInstanceOf(SeqExp.class, l.body);
        assertNull(((SeqExp) l.body).list);
        decs = l.decs;

        // type myint = int
        dec = decs.head;
        assertInstanceOf(TypeDec.class, dec); type = ((TypeDec) dec);
        assertEquals("myint", type.name.toString());
        assertInstanceOf(NameTy.class, type.ty);
        assertEquals("int", ((NameTy) type.ty).name.toString());
        assertNull(type.next);
        // var num := 1
        decs = decs.tail;
        dec = decs.head;
        assertInstanceOf(VarDec.class, dec); var = (VarDec) dec;
        assertEquals("num", var.name.toString());
        assertNull(var.typ);
        assertInstanceOf(IntExp.class, var.init);
        // function g1() = 2
        decs = decs.tail;
        dec = decs.head;
        assertInstanceOf(FunctionDec.class, dec); fun = (FunctionDec) dec;
        assertEquals("g1", fun.name.toString());
        assertNull(fun.result);
        assertInstanceOf(IntExp.class, fun.body);
        assertNull(fun.next);
        // var num:int := 1
        decs = decs.tail;
        dec = decs.head;
        assertInstanceOf(VarDec.class, dec); var = (VarDec) dec;
        assertEquals("num", var.name.toString());
        assertEquals("int", var.typ.name.toString());
        assertInstanceOf(IntExp.class, var.init);
        // function g1():int = 2
        decs = decs.tail;
        dec = decs.head;
        assertInstanceOf(FunctionDec.class, dec); fun = (FunctionDec) dec;
        assertEquals("g2", fun.name.toString());
        assertEquals("int", fun.result.name.toString());
        assertInstanceOf(IntExp.class, fun.body);
        assertNull(fun.next);
        assertNull(decs.tail);

        exp = scan("" +
                "let " +
                    "type myint = int " +
                    "type arrtype = array of myint " +
                    "type rectype = {name:string, age:int} " +
                "in end");

        assertInstanceOf(LetExp.class, exp); l = (LetExp) exp;
        assertNotNull(l.decs);
        assertInstanceOf(SeqExp.class, l.body);
        assertNull(((SeqExp) l.body).list);
        decs = l.decs;

        // type myint = int
        dec = decs.head;
        assertInstanceOf(TypeDec.class, dec); type = ((TypeDec) dec);
        assertEquals("myint", type.name.toString());
        assertInstanceOf(NameTy.class, type.ty);
        assertEquals("int", ((NameTy) type.ty).name.toString());
        assertNotNull(type.next);
        // type arrtype = array of myint
        type = type.next;
        assertEquals("arrtype", type.name.toString());
        assertInstanceOf(ArrayTy.class, type.ty);
        assertEquals("myint", ((ArrayTy) type.ty).typ.toString());
        assertNotNull(type.next);
        // type rectype = {name:string, age:int}
        type = type.next;
        assertEquals("rectype", type.name.toString());
        assertInstanceOf(RecordTy.class, type.ty); field = ((RecordTy) type.ty).fields;
        assertEquals("name", field.name.toString());
        assertEquals("string", field.typ.toString());
        field = field.tail;
        assertEquals("age", field.name.toString());
        assertEquals("int", field.typ.toString());
        assertNull(field.tail);
        assertNull(type.next);

        assertInstanceOf(SeqExp.class, l.body);
        assertNull(((SeqExp) l.body).list);

        exp = scan("" +
                "let " +
                    "function g(a: int, b: string) = h(a+1)" +
                    "function h(d: int) = g(d, \"str\")" +
                    "function i() = 1" +
                "in end");

        assertInstanceOf(LetExp.class, exp); l = (LetExp) exp;
        assertNotNull(l.decs);
        assertInstanceOf(SeqExp.class, l.body);
        assertNull(((SeqExp) l.body).list);
        decs = l.decs;
        dec = decs.head;

        // function g(a: int, b: string) = h(a+1)
        assertInstanceOf(FunctionDec.class, dec); fun = (FunctionDec) dec;
        assertEquals("g", fun.name.toString());
        assertNotNull(fun.params); field = fun.params;
        assertEquals("a", field.name.toString());
        assertEquals("int", field.typ.toString());
        field = field.tail;
        assertEquals("b", field.name.toString());
        assertEquals("string", field.typ.toString());
        assertInstanceOf(CallExp.class, fun.body);
        // function h(d: int) = g(d, "str")
        fun = fun.next;
        assertEquals("h", fun.name.toString());
        assertNotNull(fun.params); field = fun.params;
        assertEquals("d", field.name.toString());
        assertEquals("int", field.typ.toString());
        assertInstanceOf(CallExp.class, fun.body);
        // function i() = 1
        fun = fun.next;
        assertEquals("i", fun.name.toString());
        assertNull(fun.params);
        assertInstanceOf(IntExp.class, fun.body);

        assertNull(decs.tail);
    }

    static void resetParser() { parser = null; }

    private Exp scan(String input) throws Exception {
        resetParser();
        ErrorMsg.ErrorMsg errorMsg = new ErrorMsg.ErrorMsg("test");
        java.io.Reader in = new java.io.StringReader(input);
        parser = new Grammar(in, errorMsg);

        return (Exp) (parser.parse().value);
    }
}
