package nachos.threads;

import nachos.machine.Machine;


class KVpairHashTableSem {
	private int k;
	private int v;
	private KVpairHashTableSem next; // if two keys clash with the same hash, then they will be chained together in a list.

	KVpairHashTableSem(int key, int value) {
		k = key;
		v = value;
		next = null;
	}
	int getValue() {
		return v;
	}
	int getKey() {
		return k;
	}
	KVpairHashTableSem getNext() {
		return next;
	}
	void setKey(int key) {
		k = key;
	}
	void setValue(int value) {
		v = value;
	}
	void setNext(KVpairHashTableSem next) {
		this.next = next;
	}
}
enum OperationTypeS {
	INSERT,
	REMOVE, 
	QUERY

}

class ThreadOperationS {
	public int getKey() {
		return key;
	}
	public void setKey(int key) {
		this.key = key;
	}
	public OperationTypeS getOpS() {
		return ops;
	}
	public void setOpS(OperationTypeS ops) {
		this.ops = ops;
	}
	public int getResult() {
		return result;
	}
	public void setResult(int result) {
		this.result = result;
	}
	int key;
	OperationTypeS ops;
	int result;
	
}

public class HashTableSem {
	private Semaphore[] lock;
	protected KVpairHashTableSem[] hTable;
	protected int[] SizeofBuckets;


public HashTableSem(int buckets){
	SizeofBuckets = new int[buckets];
	hTable = new KVpairHashTableSem[buckets];
	lock = new Semaphore[buckets];
	for (int i = 0; i < buckets; i++) {
		lock[i] = new Semaphore(0);
        hTable[i] = new KVpairHashTableSem(-1, 0);
        SizeofBuckets[i] = 0;
        }
}

private KVpairHashTableSem getTail(KVpairHashTableSem KVHTS, int key) throws Exception {
	KVpairHashTableSem temp = KVHTS;
	while (temp.getNext() != null) {
		temp = temp.getNext();
		if (temp.getKey() == key) {
			throw new HashTableException("Duplicated Key");
		}
	}
	return temp;
}


public int get(int key) throws Exception {
	int hash = key % lock.length;
	lock[hash].V();
	if (hTable[hash].getKey() == -1) {
		lock[hash].P();
		throw new HashTableException("Does not exist");
	} else if (hTable[hash].getKey() == key) {
		lock[hash].P();
		return hTable[hash].getValue();
	} else {
		KVpairHashTableSem temp = hTable[hash];
		while (temp.getNext() != null) {
			temp = temp.getNext();
			if (temp.getKey() == key) {
				lock[hash].P();
				return temp.getValue();
			}
		}

		throw new HashTableException("Not Found");
	}
}

public void put(int key, int v) throws Exception {	
	int hash = key % lock.length;
	lock[hash].V();
	boolean intStatus = Machine.interrupt().disable();
	if (hTable[hash].getKey()!= -1) {
		if (hTable[hash].getKey() == key) {
			lock[hash].P();
			Machine.interrupt().restore(intStatus);
			throw new HashTableException("Duplicate element");	
		}
		KVpairHashTableSem tail = getTail(hTable[hash], key);
		tail.setNext(new KVpairHashTableSem(key, v));
		SizeofBuckets[hash]++;

		Machine.interrupt().restore(intStatus);
	} else {
		hTable[hash] = new KVpairHashTableSem(key, v);
		SizeofBuckets[hash]++;
		Machine.interrupt().restore(intStatus);
	}
}

public void out(int key) throws Exception {
	int hash = key % lock.length;
	
	if (hTable[hash].getKey() == -1) {
		lock[hash].P();
		throw new HashTableException("Does not exist");
	} else if (hTable[hash].getKey() == key) {
		if (hTable[hash].getNext() != null) {
			hTable[hash] = hTable[hash].getNext();
		} else {
			SizeofBuckets[hash]--;
			hTable[hash].setKey(-1);
			
		}
	} else {
		KVpairHashTableSem temp = hTable[hash];
		while(temp.getNext() != null) {
			KVpairHashTableSem pred = temp;
			temp = temp.getNext();
			if (temp.getKey() == key) {
				if (temp.getNext() != null) {
					pred.setNext(temp.getNext());
					temp = temp.getNext();
					lock[hash].P();
					SizeofBuckets[hash]--;
					return;
				} else {
					pred.setNext(null);
					lock[hash].P();
					SizeofBuckets[hash]--;
				}
			}
		}
		lock[hash].P();
		throw new HashTableException("Not Found");
	}
}

public int getSizeofBuckets(int key) {
	return key % lock.length;
}



public void batch(int num_op, ThreadOperationS[] ops) throws Exception {
	for(int i =0; i < num_op; i++) {
		switch(ops[i].ops) {
		case INSERT: this.put(ops[i].key, ops[i].result); 
		case REMOVE: this.out(ops[i].key);
		case QUERY: ops[i].setResult(this.get(ops[i].key)); 
		default:
		}
	}
}

public static void selfTest(){
	System.out.println("HashTable seftTest begins...");
	try{
		HashTableSem hTable= new HashTableSem(10);
		hTable.put(1, 10);
		hTable.put(2, 11);
		hTable.put(5, 12);
		System.out.println(hTable.get(1));
		System.out.println(hTable.get(2));
		System.out.println(hTable.get(5));
		System.out.println("Inserted");
		System.out.println("Size");
		System.out.println(hTable.getSizeofBuckets(1));
		System.out.println(hTable.getSizeofBuckets(2));
		System.out.println(hTable.getSizeofBuckets(5));
		hTable.out(5);
		System.out.println("Removed");
		System.out.println(hTable.getSizeofBuckets(5));
		System.out.println("Thread Operations test begins...");
		ThreadOperationS[] test= new ThreadOperationS[6];
		for (int i=0;i<3;i++){
			test[i]= new ThreadOperationS();
		}
		test[0].setKey(1);
		test[0].setOpS(OperationTypeS.INSERT);
		test[0].setResult(100);
		test[1].setKey(2);
		test[1].setOpS(OperationTypeS.INSERT);
		test[1].setResult(1000);
		test[2].setKey(3);
		System.out.println(test[0].getResult());
		System.out.println(test[1].getResult());
		System.out.println("Thread Operations test ends...");
		hTable.batch(2,test);System.out.println("HashTable seftTest ends...");
	}catch (Exception e){
		System.out.println("Error in HashTable selfTest!"+e.toString());
	}
}

}
