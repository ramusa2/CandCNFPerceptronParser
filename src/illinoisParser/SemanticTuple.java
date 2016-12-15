package illinoisParser;

class SemanticTuple {

  private DepRel[][] dep;
  private CCGcat cat;
  private int parses = 0;

  static void copyDepRel(DepRel[][] target, DepRel[][] source) {
    if (source != null) {
      for (int i = 0; i < source.length; i++) {
        for (int j = 0; j < source.length; j++) {
          if (source[i][j] != null) {
            target[i][j] = source[i][j].copy();
          }
        }
      }
    }
  }

  static void addDependency(DepRel[][] target, DepList filled) {
    if (filled != null) {
      fillDepRel(target, filled.argIndex, filled.headIndex, filled.headCat,
              filled.argPos, filled.extracted, filled.bounded, filled.modifier);
    } else {
      System.out.println("ERROR: addDependency: filled is null");
      // printAsTree(System.out);
    }
  }

  private static void fillDepRel(DepRel[][] target, int arg, int head,
                                 String cat, int slot, boolean extracted,
                                 boolean bounded, boolean modifier) {
    target[arg][head] = new DepRel(cat, slot, extracted, bounded, modifier);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    SemanticTuple other = (SemanticTuple) obj;
    return other.dep.equals(dep) && other.cat.equals(cat);
  }

}
