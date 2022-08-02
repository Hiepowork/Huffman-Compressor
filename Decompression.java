
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Decompression implements IHuffConstants{

	private int[] frequencies;
	private IHuffViewer viewer;
	
	//use viewer to show updates
	//pre: none
	public Decompression(IHuffViewer viewer) {
		this.viewer = viewer;
	}

	//Returns number of bits written to decompressed file
	//param: in - input stream to read from
	//param: out - output stream to write to
	//pre: none
	public int uncompress(InputStream in, OutputStream out) throws IOException{
		frequencies = new int[ALPH_SIZE];
		BitInputStream input = new BitInputStream(in);
		int readMagic = input.readBits(BITS_PER_INT);
		//Doesn't start with right magic number, don't read anymore
		if (readMagic != MAGIC_NUMBER) {
			viewer.showError("Error reading compressed file: does"
					+ " not start with huff magic number.");
			input.close();
			return -1;
		}
		//Read constant
		int storeConstant = input.readBits(BITS_PER_INT);
		HuffmanTree hTree;
		//Reading STF and SCF varies
		if (storeConstant == STORE_COUNTS) {
			//Build new queue, make new huffmantree from that queue
			PriorityQueue<TreeNode> queue = recreateTreeFromCounts(input);
			hTree = new HuffmanTree(queue);
		}
		else {
			//Recreate tree by reading stf
			//Read size in bits of tree
			input.readBits(BITS_PER_INT);
			hTree = new HuffmanTree(input);
		}
		BitOutputStream output = new BitOutputStream(out);
		//Read compressed data and return number of bits written
		return hTree.decodeData(input, output, viewer);
	}
	
	//Helper method to build queue from counts
	//param: input - BitInputStream to read from
	//pre: none
	private PriorityQueue<TreeNode> recreateTreeFromCounts(BitInputStream input) throws IOException{
		PriorityQueue<TreeNode> queue = new PriorityQueue<>();
		for (int i = 0; i < ALPH_SIZE; i++) {
			frequencies[i] = input.readBits(BITS_PER_INT);
			//Add nonzero frequencies to queue
			if (frequencies[i] > 0) {
				queue.enqueue(new TreeNode(i, frequencies[i]));
			}
		}
		//add pseudo_eof last, freq of 1
		queue.enqueue(new TreeNode(PSEUDO_EOF, 1));
		return queue;
	}

}
