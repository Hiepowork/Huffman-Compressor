
import java.util.Iterator;
import java.util.LinkedList;

public class PriorityQueue<E extends Comparable<? super E>>{

	private LinkedList<E> list;
	
	//linked list as storage container
	public PriorityQueue() {
		list = new LinkedList<>();
	}
	
	//Return size of linked list
	//pre: none
	public int size() {
		return list.size();
	}
	
	//Add val to appropriate spot based on priority
	//param: val - the thing to add to queue
	//pre: val != null
	public void enqueue(E val) {
		if (val == null) {
			throw new IllegalArgumentException("Val can't be null");
		}
		Iterator<E> it = list.iterator();
		int index = 0;
		boolean spotNotFound = true;
		while (it.hasNext() && spotNotFound) {
			E current = it.next();
			//if val is smaller, it has passed
			//through anything it might've tied with
			if (val.compareTo(current) < 0) {
				spotNotFound = false;
			}
			//val is greater than some items after it
			//not the desired outcome, keep searching
			else {
				index++;
			}
		}
		//add value to this index in linked list
		list.add(index, val);
	}
	
	//Removes element from the front
	//pre: list.size() > 0
	public E dequeue() {
		if (list.size() == 0) {
			throw new IllegalStateException("list must not be empty.");
		}
		//remove item from front, return this value
		E front = list.getFirst();
		list.remove(front);
		return front;
	}
}
