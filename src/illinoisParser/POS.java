package illinoisParser;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Object representation of POS
 * 
 * @author bisk1
 */
public class POS implements Externalizable {
  /**
   * integer ID
   */
  int id;

  /**
   * Empty
   */
  public POS() {}

  /**
   * POS constructor using string.
   * 
   * @param pos
   */
  public POS(String pos) {
    id = TAGSET.add(pos);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    return id == ((POS)o).id;
  }

  @Override
  public int hashCode() {
    return id;
  }

  @Override
  public String toString() {
    return TAGSET.STRINGS.get(id);
  }

  @Override
  public void readExternal(ObjectInput in) throws IOException,
  ClassNotFoundException {
    id = TAGSET.add((String) in.readObject());
  }

  @Override
  public void writeExternal(ObjectOutput out) throws IOException {
	  out.writeObject(TAGSET.STRINGS.get(id));
  }
}
