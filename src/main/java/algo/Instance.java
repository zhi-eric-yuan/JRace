/*
 * Created by Zhi Yuan
 */
package algo;

/**
 * @author yuan
 * Created on Apr 16, 2013
 *
 */
public class Instance {

	protected long seed;
	/**
	 * @return the seed
	 */
	public long getSeed() {
		return seed;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the dir
	 */
	public String getDir() {
		return dir;
	}

	/**
	 * @return the insInit
	 */
	public String getInsInit() {
		return insInit;
	}

	/**
	 * @return the seedInit
	 */
	public String getSeedInit() {
		return seedInit;
	}

	protected String name;
	protected String dir;
	protected String insInit;
	protected String seedInit;
	
	/**
	 * 
	 */
	public Instance(long seed, String name) {
		this.seed = seed;
		this.name = name;
	}

	/**
	 * 
	 */
	public Instance(long seed, String name, String dir, String insInit, String seedInit) {
		this(seed, name);
		this.dir = dir;
		this.insInit = insInit;
		this.seedInit = seedInit;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + (int) (seed ^ (seed >>> 32));
		return result;
	}

	public boolean equals_bk(Object obj) {
		return obj instanceof Instance && ((Instance)obj).name.equals(name) 
			&& ((Instance)obj).seed == seed;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Instance other = (Instance) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (seed != other.seed)
			return false;
		return true;
	}
	
	public String toString() {
		StringBuffer sb = new StringBuffer(name);
		sb.append(" ");
		sb.append(seed);
		return sb.toString();
	}
}
