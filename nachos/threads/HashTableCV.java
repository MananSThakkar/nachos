package nachos.threads;
import nachos.machine.Machine;


@SuppressWarnings("serial")
class HashTableException extends Exception {
	String err;
	HashTableException(String s) {
		err = s;
	}
}


class KVpairHashTable {
	private int k;
	private int v;
	private KVpairHashTable next; // if two keys clash with the same hash, then they will be chained together in a list.

	KVpairHashTable(int key, int value) {
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
	KVpairHashTable getNext() {
		return next;
	}
	void setKey(int key) {
		k = key;
	}
	void setValue(int value) {
		v = value;
	}
	void setNext(KVpairHashTable next) {
		this.next = next;
	}
}

public class HashTableCV {
	private KVpairHashTable[] hTable;
	private static int[] SizeofBuckets;
	private Condition2[] lock;

	
	public HashTableCV(int buckets){
		SizeofBuckets = new int[buckets];
		hTable = new KVpairHashTable[buckets];
		lock = new Condition2[buckets];
		for (int i = 0; i < buckets; i++) {
			lock[i] = new Condition2(new Lock());
            hTable[i] = new KVpairHashTable(-1, 0);
            SizeofBuckets[i] = 0;
            }
	}
	
	private KVpairHashTable getTail(KVpairHashTable KVHT, int key) throws Exception {
		KVpairHashTable temp = KVHT;
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

		if (hTable[hash].getKey() == -1) {

			throw new HashTableException("Does not exist");
		} else if (hTable[hash].getKey() == key) {
	
			return hTable[hash].getValue();
		} else {
			KVpairHashTable temp = hTable[hash];
			while (temp.getNext() != null) {
				temp = temp.getNext();
				if (temp.getKey() == key) {
					lock[hash].sleep();
					return temp.getValue();
				}
			}

			throw new HashTableException("Not Found");
		}
}
	
	public void put(int key, int v) throws Exception {	
		int hash = key % lock.length;
	
		boolean intStatus = Machine.interrupt().disable();
		if (hTable[hash].getKey()!= -1) {
			if (hTable[hash].getKey() == key) {
				lock[hash].sleep();
				Machine.interrupt().restore(intStatus);
				throw new HashTableException("Duplicate element");	
			}
			KVpairHashTable tail = getTail(hTable[hash], key);
			tail.setNext(new KVpairHashTable(key, v));
			SizeofBuckets[hash]++;

			Machine.interrupt().restore(intStatus);
		} else {
			hTable[hash] = new KVpairHashTable(key, v);
			SizeofBuckets[hash]++;
			Machine.interrupt().restore(intStatus);
		}
	}

	public void out(int key) throws Exception {
		int hash = key % lock.length;
		
		if (hTable[hash].getKey() == -1) {
			lock[hash].sleep();
			throw new HashTableException("Does not exist");
		} else if (hTable[hash].getKey() == key) {
			if (hTable[hash].getNext() != null) {
				hTable[hash] = hTable[hash].getNext();
			} else {
				SizeofBuckets[hash]--;
				hTable[hash].setKey(-1);
				
			}
		} else {
			KVpairHashTable temp = hTable[hash];
			while(temp.getNext() != null) {
				KVpairHashTable pred = temp;
				temp = temp.getNext();
				if (temp.getKey() == key) {
					if (temp.getNext() != null) {
						pred.setNext(temp.getNext());
						temp = temp.getNext();
						lock[hash].sleep();
						SizeofBuckets[hash]--;
						return;
					} else {
						pred.setNext(null);
						lock[hash].sleep();
						SizeofBuckets[hash]--;
					}
				}
			}
			lock[hash].sleep();
			throw new HashTableException("Not Found");
		}
	}
	
	public int getSizeofBuckets(int key) {

		return key % lock.length;
	}

	enum OperationTypeCV{
		INSERT,
		REMOVE,
		QUERY
	}

	class ThreadOperationCV{
	int key;
	OperationTypeCV op;
	int result;
	ThreadOperationCV(){
		
	}
	public int getKey() {
		return key;
	}
	public void setKey(int key) {
		this.key = key;
	}
	public OperationTypeCV getOp() {
		return op;
	}
	public void setOperationTypeCV(OperationTypeCV op) {
		this.op = op;
	}
	public int getResult() {
		return result;
	}
	public void setResult(int result) {
		this.result = result;
	}

	};
	
	
public void batch(int op, ThreadOperationCV[] ops) throws Exception {
		int i = 0;
		while(i < op) {
			switch(ops[i].op) {
			case INSERT: this.put(ops[i].key, ops[i].result); continue;
			case REMOVE: this.out(ops[i].key); continue;
			case QUERY: ops[i].setResult(this.get(ops[i].key)); continue;
			}
			i++;
		}
}

public static void selfTest(){
	System.out.println("HashTable seftTest begins...");
	
	try {
		HashTableCV table = new HashTableCV(10);
		table.put(1, 30);
		table.put(2, 32);
		table.put(3, 34);
		System.out.println(table.get(3));
		System.out.println(table.get(2));
		System.out.println(table.get(1));
		System.out.println(table.getSizeofBuckets(3));
		table.out(3);
		System.out.println(table.get(3));  // will give error
	}catch (Exception e) {
	System.out.println("Error in HashTable selfTest!"+e.toString());
	}
}
}
