package illinoisParser;

class DepRel {
  final String cat;
  final int slot;
  boolean extracted;
  boolean bounded;

  // if PROPBANK:
  private String label = null;
  private int start = 0;
  private int end = 0;
  // public boolean compl = false;
  public boolean modifier = false;

  // end PROPBANK
  DepRel(String myCat, int mySlot, boolean myExtracted,
         boolean myBounded, boolean myMod) {
    cat = myCat;
    slot = mySlot;
    extracted = myExtracted;
    bounded = myBounded;
    modifier = myMod;
  }

  // PROPBANK:
  private DepRel(String myCat, int mySlot, boolean myExtracted,
                 boolean myBounded, String myLabel,
                 int myStart, int myEnd, boolean myCompl) {
    cat = myCat;
    slot = mySlot;
    extracted = myExtracted;
    bounded = myBounded;
    label = myLabel;
    start = myStart;
    end = myEnd;
    modifier = myCompl;// was compl
  }

  DepRel copy() {
    // PROPBANK
    DepRel copy = null;
    if (label != null) {
      copy = new DepRel(cat, slot, extracted, bounded,
                        label, start, end, modifier);
    } else
      // END PROPBANK
      copy = new DepRel(cat, slot, extracted, bounded, modifier);
    return copy;
  }

  // METHODS
  // =======
  // for PROPBANK
  String maxProj() {
    if (label != null) {
      return (new StringBuffer(" ")).append(label).append(' ')
              .append(start).append(' ').append(end).append(' ').toString();
    } else
      return " ";
  }
}

// --- END OF FILE: /home/julia/CCG/StatCCGChecked/StatCCG/DepRel.java

