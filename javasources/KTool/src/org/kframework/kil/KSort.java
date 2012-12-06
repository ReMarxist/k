package org.kframework.kil;

public enum KSort {
	K,
	Bag,
	Set,
	Map,
	List,
	BagItem,
	SetItem,
	MapItem,
	ListItem,
	KItem,
	ListOfK,
	CellLabel,
	KLabel,;

	public static KSort getKSort(String sort) {
		if (sort.equals("List{K}")) return ListOfK;
		try {
			return valueOf(sort);
		} catch (Exception e) {
			return  K;
		}
	}
	
	@Override
	public String toString() {
		if (this == ListOfK) return "List{K}";
		return super.toString();
	}
	
	public KSort mainSort() {
		switch(this) {
		case Bag:
		case BagItem:
			return Bag;
		case Map:
		case MapItem:
			return Map;
		case Set:
		case SetItem:
			return Set;
		case List:
		case ListItem:
			return List;
		case KItem:
			return K;
		}
		return this;
	}

	public boolean isDefaulable() {
		return (
				this == Map || 
				this == Bag ||
				this == List ||
				this == Set ||
				this == K
				);
	}
}
