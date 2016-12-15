package illinoisParser;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A class for storing a thread-safe mapping between Strings and an integer id.
 * This data structure can be used by the grammar to store the IDs for words or 
 * categories, allowing efficient chart parsing (over integers) while preserving 
 * type safety between words and categories.
 *  
 * @author ramusa2
 *
 */
public class IntegerMapping<T> implements Serializable {
	
	private final Class<T> objectType;
	private final String nameOfMapping;
	private final ConcurrentHashMap<T, Integer> objectToID;
	private final List<T> idToType;
	
	public IntegerMapping(String mapName, Class<T> type) {
		this.objectType = type;
		this.nameOfMapping = mapName;
		this.objectToID = new ConcurrentHashMap<T, Integer>();
		this.idToType = new ArrayList<T>(); 
	}
	
	/**
	 * Returns the integer ID for str, adding it to the mapping if it does not already exist
	 */
	public Integer getIDAndAddIfAbsent(T item) {
		Integer i = this.objectToID.get(item);
	    if (i == null) {
	      i = this.addToMapping(item);
	    }
	    return i;
	}
	
	/**
	 * Adds str to the mapping if it does not already exist.
	 * Returns the ID str is mapped to (if str was already in the mapping, this function
	 * returns the previously mapped ID).
	 */
	private synchronized Integer addToMapping(T item) {
		Integer i = objectToID.get(item);
		if(i == null) {
			i = this.idToType.size();
			this.idToType.add(item);
			this.objectToID.put(item, i);
		}
		return i;
	}
	
	/**
	 * Returns true if item is already in the mapping, false otherwise. Does not change the mapping.
	 */
	public boolean contains(T item) {
		return objectToID.containsKey(item);
	}
	
	/**
	 * Returns the String object mapped to id, or null if no mapping exists
	 * Note: this method has auto-unboxing, use sparingly 
	 */
	public T getItemByID(Integer id) {
		if(id >= 0 && id < idToType.size()) {
			return idToType.get(id);
		}
		return null;
	}

	/**
	 * Returns the name of this mapping
	 */
	public final String getNameOfMapping() {
		return this.nameOfMapping;
	}
	
	/**
	 * Return the type of object stored in this mapping
	 */
	public Class<T> getObjectType() {
		return objectType;
	}

	/**
	 * Returns the collection of items in this mapping
	 */
	public Collection<T> items() {
		return idToType;
	}

	/**
	 * Returns the number of items in this mapping
	 */
	public int size() {
		return idToType.size();
	}

	/**
	 * Returns the integer ID of item if it already belongs to this mapping, else -1.
	 */
	public int checkID(T item) {
		Integer i = this.objectToID.get(item);
	    if (i == null) {
	      return -1;
	    }
	    return i;
	}
	
	/**
	 * Returns a pointer to the equivalent item if one already exists in the mapping, else null.
	 */
	public T getItemIfItExists(T item) {
		int id = this.checkID(item);
		if(id != -1) {
			return this.getItemByID(id);
		}
		return null;
	}

	/**
	 * Removes any existing entries from this mapping.
	 */
	public void clear() {
		this.objectToID.clear();
		this.idToType.clear();
	}
}
