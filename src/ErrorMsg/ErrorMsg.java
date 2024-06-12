package ErrorMsg;

public class ErrorMsg {
    private LineList linePos = new LineList(-1,null);
    private long lineNum = 1;
    private String filename;
    public boolean anyErrors;

    public ErrorMsg(String f) {
        filename = f;
    }

    public void newline(long pos) {
        lineNum++;
        linePos = new LineList(pos,linePos);
    }


    public void error(long pos, String msg) throws Exception {
        long n = lineNum;
        LineList p = linePos;
        String sayPos = "0.0";

        anyErrors = true;

        while (p != null) {
            if (p.head < pos) {
                sayPos = ":" + n + "." + (pos - p.head);
                break;
            }
            p = p.tail;
            n--;
        }

        throw new ParsingException(filename + ":" + sayPos + ": " + msg);
    }
}

class LineList {
    long head;
    LineList tail;
    LineList(long h, LineList t) {head=h; tail=t;}
}

