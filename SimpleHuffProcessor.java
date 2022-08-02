
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;

public class SimpleHuffProcessor implements IHuffProcessor {

	private IHuffViewer myViewer;
	private Compression fileCompression;
	private Decompression fileDecompression;

	/**
	 * Preprocess data so that compression is possible ---
	 * count characters/create tree/store state so that
	 * a subsequent call to compress will work. The InputStream
	 * is <em>not</em> a BitInputStream, so wrap it int one as needed.
	 * @param in is the stream which could be subsequently compressed
	 * @param headerFormat a constant from IHuffProcessor that determines what kind of
	 * header to use, standard count format, standard tree format, or
	 * possibly some format added in the future.
	 * @return number of bits saved by compression or some other measure
	 * Note, to determine the number of
	 * bits saved, the number of bits written includes
	 * ALL bits that will be written including the
	 * magic number, the header format number, the header to
	 * reproduce the tree, AND the actual data.
	 * @throws IOException if an error occurs while reading from the input file.
	 */
	public int preprocessCompress(InputStream in, int headerFormat) throws IOException {
		fileCompression = new Compression(headerFormat, myViewer);
		BufferedInputStream inBuff = new BufferedInputStream(in);
		int val = fileCompression.preprocess(inBuff);
		return val;
	}

	/**
	 * Compresses input to output, where the same InputStream has
	 * previously been pre-processed via <code>preprocessCompress</code>
	 * storing state used by this call.
	 * <br> pre: <code>preprocessCompress</code> must be called before this method
	 * @param in is the stream being compressed (NOT a BitInputStream)
	 * @param out is bound to a file/stream to which bits are written
	 * for the compressed file (not a BitOutputStream)
	 * @param force if this is true create the output file even if it is larger than the input file.
	 * If this is false do not create the output file if it is larger than the input file.
	 * @return the number of bits written.
	 * @throws IOException if an error occurs while reading from the input file or
	 * writing to the output file.
	 */
	public int compress(InputStream in, OutputStream out, boolean force) throws IOException {
		BufferedInputStream inBuff = new BufferedInputStream(in);
		BufferedOutputStream outBuff = new BufferedOutputStream(out);
		int val = fileCompression.compress(inBuff, outBuff, force);
		return val;
	}
	


	/**
	 * Uncompress a previously compressed stream in, writing the
	 * uncompressed bits/data to out.
	 * @param in is the previously compressed data (not a BitInputStream)
	 * @param out is the uncompressed file/stream
	 * @return the number of bits written to the uncompressed file/stream
	 * @throws IOException if an error occurs while reading from the input file or
	 * writing to the output file.
	 */
	public int uncompress(InputStream in, OutputStream out) throws IOException {
		fileDecompression = new Decompression(myViewer);
		BufferedInputStream inBuff = new BufferedInputStream(in);
		BufferedOutputStream outBuff = new BufferedOutputStream(out);
		return fileDecompression.uncompress(inBuff, outBuff);
	}

	public void setViewer(IHuffViewer viewer) {
		myViewer = viewer;
	}



	private void showString(String s){
		if(myViewer != null)
			myViewer.update(s);
	}
}
