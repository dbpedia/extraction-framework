package org.dbpedia.extraction.live.util.collections;

import java.util.Set;

public class SetDiff<T>
{
	private Set<T>	added;
	private Set<T>	removed;
	private Set<T>	retained;

	public SetDiff(Set<T> added, Set<T> removed, Set<T> retained)
	{
		this.added = added;
		this.removed = removed;
		this.retained = retained;
	}

	public Set<T> getAdded()
	{
		return added;
	}

	public void setAdded(Set<T> added)
	{
		this.added = added;
	}

	public Set<T> getRemoved()
	{
		return removed;
	}

	public void setRemoved(Set<T> removed)
	{
		this.removed = removed;
	}

	public Set<T> getRetained()
	{
		return retained;
	}

	public void setRetained(Set<T> retained)
	{
		this.retained = retained;
	}
}
