
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Compression implements IHuffConstants {

	private int headerFormat;
	private int[] frequencies;
	private String[] paths;
	private HuffmanTree hTree;
	private int bitsCompressed;
	private int bitsOriginal;
	private IHuffViewer viewer;

	//Creates Compression object
	//param: headerFormat - tells how to code tree
	//pram: viewer - used to show updates
	public Compression(int headerFormat, IHuffViewer viewer) {
		this.headerFormat = headerFormat;
		this.viewer = viewer;
	}

	//Preprocesses a file for compression
	//param: in - input stream to read from
	//returns bitsoriginal - bitscompressed (bits saved)
	//pre: none
	public int preprocess(InputStream in) throws IOException{
		int bitsOriginal = 0;
		frequencies = new int[ALPH_SIZE];
		BitInputStream input = new BitInputStream(in);
		int byteIn;
		//if -1, not enough bits to read 8 more
		while ((byteIn = input.readBits(BITS_PER_WORD)) != -1) {
			//original file goes up by 8
			bitsOriginal += BITS_PER_WORD;
			//increment value in array based off read int
			frequencies[byteIn]++;
		}
		input.close();
		this.bitsOriginal = bitsOriginal;
		//Create priority queue with nonzero frequencies
		PriorityQueue<TreeNode> queue = new PriorityQueue<>();
		addNodesToQueue(queue);
		//Create huffmantree
		hTree = new HuffmanTree(queue);
		//Get string[] showing paths
		paths = hTree.getPaths();
		//Calculate the bits that compressed file will have
		bitsCompressed = getBitsCompression();
		return bitsOriginal - bitsCompressed;
	}

	//Adds nonzero frequencies and pseudo_eof as nodes to queue
	//param: queue - priority queue to add to
	//pre: none
	private void addNodesToQueue(PriorityQueue<TreeNode> queue) {
		for (int i = 0; i < frequencies.length; i++) {
			//Only deal with nonzero frequencies, add new node
			//with value of i and frequency specified by array
			if (frequencies[i] > 0) {
				queue.enqueue(new TreeNode(i, frequencies[i]));
			}
		}
		//Add pseudo_eof separately, always freq of 1
		queue.enqueue(new TreeNode(PSEUDO_EOF, 1));
	}
	
	//Calculate bits the compressed file will have
	//pre: none
	private int getBitsCompression() {
		//32 bits written for magic num and constant
		int bitsCompression = BITS_PER_INT * 2;
		if (headerFormat == STORE_COUNTS) {
			//32 bits written for 0-255
			bitsCompression += (BITS_PER_INT * ALPH_SIZE);
		}
		else {
			//32 bits representing num bits in tree
			bitsCompression += BITS_PER_INT;
			//write num bits in trees
			bitsCompression += hTree.getSizeBits();
		}
		for (int i = 0; i < ALPH_SIZE; i++) {
			//if frequency was 0, no path was created, doesn't exist
			if (frequencies[i] > 0) {
				//bits written for path's length each time it occurs
				bitsCompression += paths[i].length() * frequencies[i];
			}
		}
		//get bits of PSEUDO_EOF path
		bitsCompression += paths[PSEUDO_EOF].length();
		return bitsCompression;
	}

	//Compress file, preprocess must have been called before
	//param: in - inputstream to read from
	//param: out - outputstream to write to
	//param: force - if true & compressed file larger, compress anyways, else don't
	//pre: none
	//Returns number of bits written to compressed file
	public int compress(InputStream in, OutputStream out, boolean force) throws IOException{
		//if force is false and compressed file larger than original, don't compress
		if (!force && bitsCompressed > bitsOriginal) {
			viewer.showError("Compressed file has " + (bitsCompressed - bitsOriginal) + 
					" more bits than uncompressed file. Select force compression "
					+ "option to compress.");
			return -1;
		}
		else {
			BitOutputStream output = new BitOutputStream(out);
			//write magic number as 32 bit
			output.writeBits(BITS_PER_INT, MAGIC_NUMBER);
			//if header is store_counts, code tree using SCF
			if (headerFormat == STORE_COUNTS) {
				standardCountFormat(output);
			}
			//if header is store_tree, code tree using STF
			else if (headerFormat == STORE_TREE) {
				standardTreeFormat(output);
			}
			BitInputStream input = new BitInputStream(in);
			int nextInt;
			//Read input stream again, for each  bit chunk, write
			//the corresponding string path from the tree
			while ((nextInt = input.readBits(BITS_PER_WORD)) != -1) {
				String path = paths[nextInt];
				writeBitsOfString(output, path);
			}
			input.close();
			String pseudoPath = paths[PSEUDO_EOF];
			//lastly, write the encoding of pseudo_eof
			writeBitsOfString(output, pseudoPath);
			output.close();
			//return calculated size of compressed file from earlier
			return bitsCompressed;
		}
	}

	//Helper to write bits of a string
	//param: output - BitOutputStream used to write bits
	//param: path - string to write as bits
	//pre: none
	private void writeBitsOfString(BitOutputStream output, String path) {
		for (int i = 0; i < path.length(); i++) {
			//if char is '0', write 0, single bit
			if (path.charAt(i) == '0' ) {
				output.writeBits(1, 0);
			}
			//if '1', write 1, single bit
			else {
				output.writeBits(1, 1);
			}
		}
	}

	//Store tree using standard count format
	//pre: none
	private void standardCountFormat(BitOutputStream output) {
		//write store_count constant as 32 bit
		output.writeBits(BITS_PER_INT, STORE_COUNTS);
		for (int i = 0; i < ALPH_SIZE; i++) {
			//write frequencies of values 0-255
			output.writeBits(BITS_PER_INT, frequencies[i]);
		}
	}

	//Store tree using standard tree format
	//pre: none
	private void standardTreeFormat(BitOutputStream output) {
		//write store_tree constant as 32 bit
		output.writeBits(BITS_PER_INT, STORE_TREE);
		//get num bits in tree. 9 bits for n leaves, in full tree with n
		//leaves, total of 2n - 1 nodes, so 1 bit for each node
		//write numtreebits as 32 bit
		output.writeBits(BITS_PER_INT, hTree.getSizeBits());
		//write out the tree using huffman tree class
		hTree.encodeTree(output);
	}
}
