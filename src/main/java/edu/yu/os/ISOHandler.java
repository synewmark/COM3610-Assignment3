package edu.yu.os;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ISOHandler {
	private static final int RootFat = 2;
	public static final int BytePerSector = 512;
	private final Fat32File rootFatFile;
	private Fat32File currFatFile;

	private int fat32Start;
	private int clusterStart;
	public static short SectorPerCluster;
	private int entireSectorSize;
	public final short reservedSectorCount;
	public final short fatCount;
	public final int fatSize;

	private int currFat;

	private final StringBuilder returnMessage = new StringBuilder(100);

	File currDir = new File(File.separator);

	RandomAccessFile fat32;

	public ISOHandler(String iso_path) throws IOException {
		byte[] bytes = Files.readAllBytes(Paths.get(iso_path));
		ByteBuffer BootSector = ByteBuffer.allocate(64);
		BootSector.put(bytes, 0, 64);
		BootSector.order(ByteOrder.LITTLE_ENDIAN);
		SectorPerCluster = BootSector.get(0x0D);
		reservedSectorCount = BootSector.get(0x0E);
		fatCount = BootSector.getShort(0x10);
		fatSize = BootSector.getInt(0x24);
		entireSectorSize = SectorPerCluster * BytePerSector;
		fat32 = new RandomAccessFile(new File(iso_path), "r");
		clusterStart = (reservedSectorCount + (fatCount * fatSize)) * BytePerSector;
		fat32Start = reservedSectorCount * BytePerSector;
		fat32.seek(clusterStart);
		rootFatFile = getRootFatFile();
		currFatFile = rootFatFile;

	}

	public String getCurrDir() {
		return currDir.toString();
	}

	/*
	 * Description: For the directory at the relative or absolute path specified in
	 * DIR_NAME, changes the current directory to DIR_NAME. The prompt is updated to
	 * show the new current directory. Return an error if DIR_NAME does not exist or
	 * is not a directory.
	 */
	// returns empty string if successful, otherwise returns err msg

	public String cd(String directory) {
		Fat32File fat32 = getFileFat(new File(directory));
		if (fat32 == null) {
			return returnAndClearBuffer();
		}
		if (!fat32.directory) {
			returnMessage.setLength(0);
			return (fat32.filename + " is not a directory");
		}
		currFatFile = fat32;
		currDir = new File(returnMessage.toString());
		returnMessage.setLength(0);
		return "";

	}

	/*
	 * Description: For the directory at the relative or absolute path specified in
	 * DIR_NAME, lists the contents of DIR_NAME, including "." and "..", and
	 * including hidden files (in other words, it should behave like the real "ls
	 * -a"). Display an error message if DIR_NAME is not a directory.
	 */
	public String ls(String director_path) {
		// find the cluster the directory starts at
		Fat32File dir = director_path.isEmpty() ? getFileFat(currDir) : getFileFat(new File(director_path));
		if (dir == null) {
			return returnAndClearBuffer();
		}
		returnMessage.setLength(0);
		var dirList = getContentsOfDir(dir.cluster);
		Collections.sort(dirList);
		for (Fat32File df : dirList) {
			returnMessage.append(df.filename + " ");
		}
		String returnMsg = returnMessage.toString();
		returnMessage.setLength(0);
		return returnMsg;
	}

	/*
	 * Description: For the file at the relative or absolute path specified in
	 * FILE_NAME, prints the size of file. Return an error if FILE_NAME does not
	 * exist or is not a file.
	 */
	// returns size if successful, otherwise returns err msg

	public String size(String file_path) {
		Fat32File fatFile = getFileFat(new File(file_path));
		if (fatFile == null) {
			return returnAndClearBuffer();
		}
		returnMessage.setLength(0);
		if (fatFile.directory) {

		}
		return "Size of: " + file_path.toUpperCase() + " is " + Integer.toString(fatFile.size) + " bytes";
	}

	/*
	 * Description: For the file at the relative or absolute path specified in
	 * FILE_NAME, reads from the file starting OFFSET bytes from the beginning of
	 * the file and prints NUM_BYTES bytes of the file’s contents, interpreted as
	 * ASCII text (for each byte, if the byte is less than decimal 127, print the
	 * corresponding ascii character. Else, print " 0xNN ", where NN is the hex
	 * value of the byte).
	 */
	// returns contents if successful, otherwise returns err msg
	public String read(String filename, long offset, long num_bytes) {
		if (offset < 0) {
			return "Error: OFFSET must be a positive value";
		}
		if (num_bytes <= 0) {
			return "Error: NUM_BYTES must be a greater than zero";
		}
		Fat32File fat32File = getFileFat(new File(filename));
		returnMessage.setLength(0);

		if (fat32File == null || fat32File.directory || fat32File.volumeid) {
			return "ERROR: " + filename.toString().toUpperCase() + " is not a file";
		}
		if (fat32File.size < (offset + num_bytes)) {
			return ("ERROR: Attempt to read data outside of file bounds");
		}
		int clusterNumber = fat32File.cluster;

		while (offset >= entireSectorSize) {
			clusterNumber = readFromFat(clusterNumber);
			offset -= entireSectorSize;
		}
		byte[] buffer = new byte[entireSectorSize];
		while ((clusterNumber & 0x0FFFFFF8) != 0x0FFFFFF8) {
			readFromCluster(clusterNumber, buffer);
			// safe cast because at this point offset must be less than an int because it
			// can't be larger than entireSectorSize
			for (int i = (int) offset; i < buffer.length && num_bytes > 0; i++, num_bytes--) {
				int curr = Byte.toUnsignedInt(buffer[i]);
				if (curr < 128) {
					returnMessage.append((char) curr);
				} else {
					returnMessage.append("0x" + Integer.toHexString(curr));
				}
				offset = 0;
			}
			clusterNumber = readFromFat(clusterNumber);
		}

		return returnAndClearBuffer();
	}

	public Fat32File getFileFat(File file) {
		if (file.toString().equals(File.separator)) {
			return rootFatFile;
		}
		if (file.toString().isBlank()) {
			return currFatFile;
		}
		int currPos = isAbsolute(file) ? rootFatFile.cluster : currFatFile.cluster;
		File movingDir = isAbsolute(file) ? new File("") : currDir;
		Fat32File returnDirectory = currPos == rootFatFile.cluster ? rootFatFile : null;
		findnextpath: for (Path path : file.toPath()) {
			String pathName = path.toString();
			if (pathName.equals(".")) {
				continue;
			}

			if (returnDirectory != null && !returnDirectory.directory) {
				returnMessage.append("Error: " + file.toString().toUpperCase() + " is not a directory");
				return null;
			}
			for (Fat32File df : getContentsOfDir(currPos)) {
				if (pathName.equals("..") && currPos == rootFatFile.cluster) {
					returnMessage.append("Error: can not cd .. from root");
					return null;
				}
				if (df.filename.equalsIgnoreCase(pathName)) {
					currPos = df.cluster;
					returnDirectory = df;
					if (pathName.equals("..")) {
						if (df.cluster == 0) {
							df.cluster = 2;
						}
						movingDir = movingDir.getParentFile();
					} else {
						movingDir = new File(movingDir, df.filename);
					}
					continue findnextpath;
				}
			}
			// if continue fails, i.e. a matching directory could not be found
			returnMessage.append("Error: " + file.toString().toUpperCase() + " is not a directory");
			return null;
		}
		returnMessage.append(movingDir.toString());
		return returnDirectory;

	}

	private int readFromFat(int fatNumber) {
		try {
			fat32.seek(fat32Start + (fatNumber) * 4);
			return Integer.reverseBytes(fat32.readInt());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return -1;
		}
	}

	private byte[] readFromCluster(int clusterNumber, byte[] buffer) {
		int singleSectorSize = BytePerSector * SectorPerCluster;
		if (buffer == null) {
			buffer = new byte[singleSectorSize];
		}
		try {
			fat32.seek(clusterStart + ((clusterNumber - 2) * (long) singleSectorSize));
			fat32.read(buffer, 0, buffer.length);
			return buffer;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}

	}

	private List<Fat32File> getContentsOfDir(int dir) {
		byte[] buffer = new byte[entireSectorSize];
		List<Fat32File> returnArray = new ArrayList<Fat32File>();
		// for each cluster
		while ((dir & 0x0FFFFFF8) != 0x0FFFFFF8) {
			// for each file in that cluster
			readFromCluster(dir, buffer);
			for (int i = 0; i < entireSectorSize; i += 32) {
				if (dir == 2 && i == 0) {
					for (int j = 0; j < 11; j++) {
						buffer[j] = ' ';
					}
					buffer[0] = '.';
					buffer[1] = '.';
					Fat32File df = new Fat32File(buffer, i);
					returnArray.add(df);
					buffer[1] = ' ';
					buffer[26] = 2;
					df = new Fat32File(buffer, i);
					returnArray.add(df);
					continue;
				}
				if (((buffer[i + 11]) & 0x0F) == 0x0F) {
					continue;
				}
				if (buffer[i] == (byte) 0xE5) {
					continue;
				}
				if (buffer[i] == 0) {
					return returnArray;
				}

				Fat32File df = new Fat32File(buffer, i);
				returnArray.add(df);
			}
			dir = readFromFat(dir);
		}
		return returnArray;
	}

	private Fat32File getRootFatFile() {
		byte[] buffer = new byte[32];
		readFromCluster(2, buffer);
		for (int i = 0; i < 11; i++) {
			buffer[i] = ' ';
		}
		buffer[11] = 1 << 4;
		buffer[20] = 0;
		buffer[21] = 0;
		buffer[27] = 0;
		buffer[26] = 2;
		return new Fat32File(buffer, 0);
	}

	public File getWorkingDirectory() {
		return currDir;
	}

	private boolean isAbsolute(File file) {
		return file.toString().startsWith(File.separator);
	}

	private String returnAndClearBuffer() {
		String msg = returnMessage.toString();
		returnMessage.setLength(0);
		return msg;
	}

	/*
	 * Description: prints out information about the following fields in both hex
	 * and base 10. Be careful to use the proper endian-ness: o BPB_BytesPerSec o
	 * BPB_SecPerClus o BPB_RsvdSecCnt o BPB_NumFATS o BPB_FATSz32
	 */
	public String info() {
		StringBuilder sb = new StringBuilder();
		sb.append("BPB_BytesPerSec: is 0x" + Integer.toHexString(BytePerSector) + " " + Integer.toString(BytePerSector)
				+ "\n");
		sb.append("BPB_SecPerClus: is 0x" + Integer.toHexString(SectorPerCluster) + " "
				+ Integer.toString(SectorPerCluster) + "\n");
		sb.append("BPB_RsvdSecCnt: is 0x" + Integer.toHexString(reservedSectorCount) + " "
				+ Integer.toString(reservedSectorCount) + "\n");
		sb.append("BPB_NumFATS: is 0x" + Integer.toHexString(fatCount) + " " + Integer.toString(fatCount) + "\n");
		sb.append("BPB_FATSz32: is 0x" + Integer.toHexString(fatSize) + " " + Integer.toString(fatSize) + "\n");
		return sb.toString();
	}

	/*
	 * Description: For the file or directory at the relative or absolute path
	 * specified in FILE_NAME or DIR_NAME, prints the size of the file or directory,
	 * the attributes of the file or directory, and the first cluster number of the
	 * file or directory. Return an error if FILE_NAME/DIR_NAME does not exist (see
	 * example below). (Note: The size of a directory will always be zero.)
	 */
	public String stat(String file_path) {
		Fat32File fat32 = getFileFat(new File(file_path));
		returnMessage.setLength(0);
		if (fat32 == null) {
			return "Error: file/directory does not exist";
		}
		returnMessage.append("Size is ");
		returnMessage.append(fat32.size);
		returnMessage.append("\nAttributes");
		if (fat32.readonly) {
			returnMessage.append(" ATTR_READ_ONLY");
		}
		if (fat32.hidden) {
			returnMessage.append(" ATTR_HIDDEN");
		}
		if (fat32.system) {
			returnMessage.append(" ATTR_SYSTEM");
		}
		if (fat32.volumeid) {
			returnMessage.append(" ATTR_VOLUME_ID");
		}
		if (fat32.directory) {
			returnMessage.append(" ATTR_DIRECTORY");
		}
		if (fat32.archive) {
			returnMessage.append(" ATTR_ARCHIVE");
		}
		returnMessage.append("\nNext cluster number is ");
		returnMessage.append(String.format("0x%04x", fat32.cluster));
		return returnAndClearBuffer();
	}

	static class Fat32File implements Comparable<Fat32File> {
		final String filename;
		int cluster;
		final int size;

		final boolean readonly;
		final boolean hidden;
		final boolean system;
		final boolean volumeid;
		final boolean directory;
		final boolean archive;

		private Fat32File(byte[] rawBytes, int offset) {
			String name = new String(rawBytes, offset, 8);
			String extension = new String(rawBytes, offset + 8, 3);
			name = name.strip();
			extension = extension.strip();

			if (!extension.isBlank()) {
				extension = '.' + extension;
			}
			filename = name + extension;

			cluster = ((0xFF & rawBytes[offset + 21]) << 24) | ((0xFF & rawBytes[offset + 20]) << 16)
					| ((0xFF & rawBytes[offset + 27]) << 8) | (0xFF & rawBytes[offset + 26]);

			size = ((0xFF & rawBytes[offset + 31]) << 24) | ((0xFF & rawBytes[offset + 30]) << 16)
					| ((0xFF & rawBytes[offset + 29]) << 8) | (0xFF & rawBytes[offset + 28]);

			byte attribute = rawBytes[offset + 11];
			readonly = ((attribute & 1) == 1);
			attribute >>= 1;

			hidden = ((attribute & 1) == 1);
			attribute >>= 1;

			system = ((attribute & 1) == 1);
			attribute >>= 1;

			volumeid = ((attribute & 1) == 1);
			attribute >>= 1;

			directory = ((attribute & 1) == 1);
			attribute >>= 1;

			archive = ((attribute & 1) == 1);
			attribute >>= 1;

		}

		@Override
		public int compareTo(Fat32File o) {
			return this.filename.compareTo(o.filename);
		}
	}
}