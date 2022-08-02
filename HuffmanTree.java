
import java.io.IOException;

public class HuffmanTree implements IHuffConstants{

	private TreeNode root;
	private int sizeBits;
	
	//Creates huffmantree by reading bitinputstream
	//param: input - BitInputStream used to create tree
	//pre: none
	public HuffmanTree(BitInputStream input) throws IOException{
		//determine root of tree
		root = readTreeHelper(input);
		//get size bits
		sizeBits = sizeBitsHelper();
	}
	
	//Read and decode standard tree format
	//param: input - BitInputStream to read from
	//pre: none
	private TreeNode readTreeHelper(BitInputStream input) throws IOException{
		//Read 1 bit
		int readBit = input.readBits(1);
		//no more bits left
		if (readBit == -1) {
			throw new IOException("Not enough bits to read. Header incorrect");
		}
		else if (readBit == 0) {
			//create internal node, get children through recurisve calls
			TreeNode internalNode = new TreeNode(-1, 1);
			internalNode.setLeft(readTreeHelper(input));
			internalNode.setRight(readTreeHelper(input));
			return internalNode;
		}
		else {
			//1 means create a leaf, get value by reading 9 bits
			int value = input.readBits(BITS_PER_WORD + 1);
			TreeNode leaf = new TreeNode(value, -1);
			return leaf;
		}
	}

	//Creates huffmantree from priority queue of treenodes
	//param: q - priority queue used to build tree
	//pre: q.size() > 0
	public HuffmanTree(PriorityQueue<TreeNode> q) {
		if (q.size() == 0) {
			throw new IllegalArgumentException("q must empty.");
		}
		//Do this until q's size is 1
		while (q.size() > 1) {
			//Add new node back to queue with left child as the first
			//dequeued node and right child as second dequeued node
			TreeNode result = new TreeNode(q.dequeue(), -1, q.dequeue());
			q.enqueue(result);
		}
		//root equals the last node dequeued
		root = q.dequeue();
		sizeBits = sizeBitsHelper();
	}

	//Return string[] of paths to get to values
	//pre: none
	public String[] getPaths() {
		String[] paths = new String[ALPH_SIZE + 1];
		pathsHelper(paths, "", root);
		return paths;
	}
	
	//paths helper
	//param: paths - array to add to. 
	//param: path - the current string built up so far
	//param: n - the current treenode
	//pre: none
	private void pathsHelper(String[] paths, String path, TreeNode n) {
		if (n.isLeaf()) {
			//you are at a value, set corresponding value in array to string
			paths[n.getValue()] = path;
		}
		else {
			//go left, add 0 to string
			pathsHelper(paths, path + "0", n.getLeft());
			//go right, add 1 to string
			pathsHelper(paths, path + "1", n.getRight());
		}
	}

	//Encode the tree
	//param: output - BitOutputStream to write to
	//pre: none
	public void encodeTree(BitOutputStream output) {
		encodeTreeHelper(output, root);
	}

	//Encode tree helper
	//pre: none
	private void encodeTreeHelper(BitOutputStream output, TreeNode n) {
		if (n != null) {
			//if leaf, write 1, then its value as 9 bits
			if (n.isLeaf()) {
				output.writeBits(1, 1);
				output.writeBits(BITS_PER_WORD + 1, n.getValue());
			}
			else {
				//internal nodes, just write 0
				output.writeBits(1, 0);
				//call recursively on left and right children
				encodeTreeHelper(output, n.getLeft());
				encodeTreeHelper(output, n.getRight());
			}
		}
	}
	
	//Return num of bits in tree
	//pre: none
	public int getSizeBits() {
		return sizeBits;
	}
	
	//Helper for num of bits in tree
	//pre: none
	private int sizeBitsHelper() {
		int numLeaves = numLeaves(root);
		int totalNodes = (2 * numLeaves) - 1;
		//1 bit for each node, 9 bits for each leaf
		return totalNodes + (numLeaves * (BITS_PER_WORD + 1));
	}

	//Helper to get numLeaves
	//param: n - current treenode
	//pre: none
	private int numLeaves(TreeNode n) {
		if (n != null) {
			//add 1 if leaf present
			if (n.isLeaf()) {
				return 1;
			}
			//else, add recursive calls to left and to right
			return numLeaves(n.getLeft()) + numLeaves(n.getRight());
		}
		//n is null, doesn't exist, nothing to add
		return 0;
	}
	
	//Decode actual compressed data
	//return number of bits written
	//param: input - BitInputStream to read from
	//param: output - BitOutputStream to write to
	//pre: none
	public int decodeData(BitInputStream input, BitOutputStream output, IHuffViewer viewer) throws IOException{
		TreeNode current = root;
		int numBitsWritten = 0;
		boolean stillReading = true;
		while (stillReading) {
			int readBit = input.readBits(1);
			//unexpected end, no pseudo_eof
			if (readBit == -1) {
				input.close();
				output.close();
				throw new IOException("Error reading compressed file. No PSEUDO_EOF");
			}
			else {
				//if 0, go left, if 1, go right
				if (readBit == 0) {
					current = current.getLeft();
				}
				else {
					current = current.getRight();
				}
				if (current.isLeaf()) {
					//you are done
					if (current.getValue() == PSEUDO_EOF) {
						stillReading = false;
					}
					else {
						//write value as 8 bits
						output.writeBits(BITS_PER_WORD, current.getValue());
						numBitsWritten += BITS_PER_WORD;
						//reset back to root
						current = root;
					}
				}
			}
		}
		output.close();
		return numBitsWritten;
	}
}
