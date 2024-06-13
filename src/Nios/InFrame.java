package Nios;

class InFrame extends Frame.Access {
  int offset;
  InFrame(int o) {
    offset = o;
  }

  public String toString() {
    return Integer.toString(this.offset);
  }
}
