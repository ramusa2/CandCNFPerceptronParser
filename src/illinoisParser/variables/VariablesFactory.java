package illinoisParser.variables;

import illinoisParser.variables.structs.*;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.concurrent.ConcurrentHashMap;

public abstract class VariablesFactory {

	/** Unary variable cache **/
	protected static final ConcurrentHashMap<ConditioningVariables, ConditioningVariables> unaryCache =
			new ConcurrentHashMap<ConditioningVariables, ConditioningVariables>();

	/** Binary variable cache **/
	protected static final ConcurrentHashMap<ConditioningVariables, ConditioningVariables> binaryCache =
			new ConcurrentHashMap<ConditioningVariables, ConditioningVariables>();

	/** Ternary variable cache **/
	protected static final ConcurrentHashMap<ConditioningVariables, ConditioningVariables> ternaryCache =
			new ConcurrentHashMap<ConditioningVariables, ConditioningVariables>();

	/** Quarternary variable cache **/
	protected static final ConcurrentHashMap<ConditioningVariables, ConditioningVariables> quarternaryCache =
			new ConcurrentHashMap<ConditioningVariables, ConditioningVariables>();

	/** Quinternary variable cache **/
	protected static final ConcurrentHashMap<ConditioningVariables, ConditioningVariables> quinternaryCache =
			new ConcurrentHashMap<ConditioningVariables, ConditioningVariables>();

	/** Complex variable cache **/
	protected static final ConcurrentHashMap<ComplexConditioningVariables, ComplexConditioningVariables> complexCache = 
			new ConcurrentHashMap<ComplexConditioningVariables, ComplexConditioningVariables>();	

	public static final ConditioningVariables getEmpty() {
		return EmptyConditioningVariables.getEmpty();
	}

	public static final ConditioningVariables cache(int a) {
		return getFromOrAddToCache(new UnaryConditioningVariables(a), unaryCache, true);
	}

	public static final ConditioningVariables get(int a) {		
		return getFromOrAddToCache(new UnaryConditioningVariables(a), unaryCache, false);
	}

	public static final ConditioningVariables cache(int a, int b) {
		return getFromOrAddToCache(new BinaryConditioningVariables(a, b), binaryCache, true);
	}

	public static final ConditioningVariables get(int a, int b) {		
		return getFromOrAddToCache(new BinaryConditioningVariables(a, b), binaryCache, false);
	}

	public static final ConditioningVariables cache(int a, int b, int c) {
		return getFromOrAddToCache(new TernaryConditioningVariables(a, b, c), ternaryCache, true);
	}

	public static final ConditioningVariables get(int a, int b, int c) {		
		return getFromOrAddToCache(new TernaryConditioningVariables(a, b, c), ternaryCache, false);
	}

	public static final ConditioningVariables cache(int a, int b, int c, int d) {
		return getFromOrAddToCache(new QuarternaryConditioningVariables(a, b, c, d), quarternaryCache, true);
	}

	public static final ConditioningVariables get(int a, int b, int c, int d) {		
		return getFromOrAddToCache(new QuarternaryConditioningVariables(a, b, c, d), quarternaryCache, false);
	}

	public static final ConditioningVariables cache(int a, int b, int c, int d, int e) {
		return getFromOrAddToCache(new QuinternaryConditioningVariables(a, b, c, d, e), quinternaryCache, true);
	}

	public static final ConditioningVariables get(int a, int b, int c, int d, int e) {		
		return getFromOrAddToCache(new QuinternaryConditioningVariables(a, b, c, d, e), quinternaryCache, false);
	}

	public static final ComplexConditioningVariables cache(ConditioningVariables backoffVars,
			ConditioningVariables remainderVars) {
		return getFromOrAddToCacheComplex(backoffVars, remainderVars, true);
	}
	public static final ComplexConditioningVariables get(ConditioningVariables backoffVars,
			ConditioningVariables remainderVars) {
		return getFromOrAddToCacheComplex(backoffVars, remainderVars, false);
	}


	// Cache methods

	private static final ConditioningVariables getFromOrAddToCache(ConditioningVariables temp, 
			ConcurrentHashMap<ConditioningVariables, ConditioningVariables> cache, 
			boolean addToCache) {
		ConditioningVariables cached = cache.get(temp);
		if(cached != null) {
			return cached;
		}
		if(!addToCache) {
			return temp;
		}
		// Atomic check
		ConditioningVariables verification = cache.putIfAbsent(temp, temp);
		if(verification == null) {
			return temp; // if verification is null, then we atomically added temp to the map, so we can return it
		}
		return verification; // if verification isn't null, then another thread cached a value first and we should return that one
	}

	private static final ComplexConditioningVariables getFromOrAddToCacheComplex(
			ConditioningVariables backoffVars,
			ConditioningVariables remainderVars, 
			boolean addToCache) {
		ComplexConditioningVariables temp = new ComplexConditioningVariables(backoffVars, remainderVars);
		ComplexConditioningVariables cached = complexCache.get(temp);
		if(cached != null) {
			return cached;
		}
		if(!addToCache) {
			return temp;
		}
		// Atomic check
		ComplexConditioningVariables verification = complexCache.putIfAbsent(temp, temp);
		if(verification == null) {
			return temp; // if verification is null, then we atomically added temp to the map, so we can return it
		}
		return verification; // if verification isn't null, then another thread cached a value first and we should return that one
	}

	// Methods for reading/writing variables to disk
	public static final void writeCache(ObjectOutput out) throws IOException {
		out.writeObject(EmptyConditioningVariables .getEmpty());
		out.writeObject(unaryCache);
		out.writeObject(binaryCache);
		out.writeObject(ternaryCache);
		out.writeObject(quarternaryCache);
		out.writeObject(quinternaryCache);
		out.writeObject(complexCache);
	}

	@SuppressWarnings("unchecked")
	public static final void readCache(ObjectInput in) throws IOException,
	ClassNotFoundException {
		EmptyConditioningVariables .setEmpty((EmptyConditioningVariables) in.readObject());

		ConcurrentHashMap<ConditioningVariables, ConditioningVariables> uTemp =
				(ConcurrentHashMap<ConditioningVariables, ConditioningVariables>) in.readObject();
		unaryCache.putAll(uTemp);

		ConcurrentHashMap<ConditioningVariables, ConditioningVariables> bTemp =
				(ConcurrentHashMap<ConditioningVariables, ConditioningVariables>) in.readObject();
		binaryCache.putAll(bTemp);

		ConcurrentHashMap<ConditioningVariables, ConditioningVariables> tTemp =
				(ConcurrentHashMap<ConditioningVariables, ConditioningVariables>) in.readObject();
		ternaryCache.putAll(tTemp);

		ConcurrentHashMap<ConditioningVariables, ConditioningVariables> q4Temp =
				(ConcurrentHashMap<ConditioningVariables,ConditioningVariables>) in.readObject();
		quarternaryCache.putAll(q4Temp);
		
		ConcurrentHashMap<ConditioningVariables, ConditioningVariables> q5Temp =
				(ConcurrentHashMap<ConditioningVariables,ConditioningVariables>) in.readObject();
		quinternaryCache.putAll(q5Temp);

		ConcurrentHashMap<ComplexConditioningVariables, ComplexConditioningVariables> cTemp =
				(ConcurrentHashMap<ComplexConditioningVariables,ComplexConditioningVariables>) in.readObject();
		complexCache.putAll(cTemp);
	}

	public static ConditioningVariables cache(int... vars) {
		int l = vars.length;
		if(l==0) {
			return getEmpty();
		}
		if(l==1) {
			return cache(vars[0]);
		}
		if(l==2) {
			return cache(vars[0], vars[1]);
		}
		if(l==3) {
			return cache(vars[0], vars[1], vars[2]);
		}
		if(l==4) {
			return cache(vars[0], vars[1], vars[2], vars[3]);
		}
		if(l==5) {
			return cache(vars[0], vars[1], vars[2], vars[3], vars[4]);
		}
		return null;
	}

	public static ConditioningVariables get(int... vars) {
		int l = vars.length;
		if(l==0) {
			return getEmpty();
		}
		if(l==1) {
			return get(vars[0]);
		}
		if(l==2) {
			return get(vars[0], vars[1]);
		}
		if(l==3) {
			return get(vars[0], vars[1], vars[2]);
		}
		if(l==4) {
			return get(vars[0], vars[1], vars[2], vars[3]);
		}
		if(l==5) {
			return get(vars[0], vars[1], vars[2], vars[3], vars[4]);
		}
		return null;
	}

}
