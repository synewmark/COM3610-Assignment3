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
import java.util.List;

public class ISOHandler {
	private static final int RootFat = 2;
	private static final int BytePerSector = 512;
	private final Fat32File rootFatFile;

	private int fat32Start;
	private int clusterStart;
	private short SectorPerCluster;
	private int entireSectorSize;

	private int currFat;

	private final StringBuilder returnMessage = new StringBuilder(100);

	File currDir = new File(File.separator);

	RandomAccessFile fat32;

	public ISOHandler(String iso_path) throws IOException {
		if (!iso_path.endsWith(".img")) {
			System.out.println("Error: File must be an .img file");
			System.exit(0); // ! CHANGE THIS!
		}
		try {
			byte[] bytes = Files.readAllBytes(Paths.get(iso_path));
		} catch (IOException e) {
			System.out.println("Error: File not found");
			System.exit(1); // ! CHANGE THIS!
		}

		byte[] bytes = Files.readAllBytes(Paths.get(iso_path));
		ByteBuffer BootSector = ByteBuffer.allocate(64);
		BootSector.put(bytes, 0, 64);
		BootSector.order(ByteOrder.LITTLE_ENDIAN);
//		short BytePerSector = BootSector.getShort(0x0B);
		SectorPerCluster = BootSector.get(0x0D);
		short ReservedSectorCount = BootSector.get(0x0E);
		short FATCount = BootSector.getShort(0x10);
		int SectorsPerFAT = BootSector.getInt(0x24);
		entireSectorSize = SectorPerCluster * BytePerSector;
		fat32 = new RandomAccessFile(new File("./fat32.img"), "r");
		clusterStart = (ReservedSectorCount + (FATCount * SectorsPerFAT)) * BytePerSector;
		fat32Start = ReservedSectorCount * BytePerSector;
//		currentDirectory = fat32Start * BytePerSector;
		fat32.seek(clusterStart);
		currFat = RootFat;
		rootFatFile = getRootFatFile();
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
	public String cd(File file) {
		Fat32File fat32 = getFileFat(file);
		if (fat32 == null) {
			return returnAndClearBuffer();
		}
		if (!fat32.directory) {
			returnMessage.setLength(0);
			return (fat32.filename + " is not a directory");
		}
		currFat = fat32.cluster;
		currDir = new File(returnMessage.toString());
		returnMessage.setLength(0);
		return "";

	}

	/*
	 * Description: For the directory at the relative or absolute path specified in
	 * DIR_NAME, lists the contents of DIR_NAME, including “.” and “..”, and
	 * including hidden files (in other words, it should behave like the real “ls
	 * -a”). Display an error message if DIR_NAME is not a directory.
	 */
	public String ls(String director_path) {
		// find the cluster the directory starts at
		Fat32File dir = getFileFat(new File(director_path));
		if (dir == null) {
			return returnAndClearBuffer();
		}
		for (Fat32File df : getContentsOfDir(dir.cluster)) {
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
	public String size(String file_path) {
		Fat32File fatFile = getFileFat(new File(file_path));
		if (fatFile == null) {
			return returnAndClearBuffer();
		}
		return Integer.toBinaryString(fatFile.size);
	}

	/*
	 * Description: For the file at the relative or absolute path specified in
	 * FILE_NAME, reads from the file starting OFFSET bytes from the beginning of
	 * the file and prints NUM_BYTES bytes of the file’s contents, interpreted as
	 * ASCII text (for each byte, if the byte is less than decimal 127, print the
	 * corresponding ascii character. Else, print " 0xNN ", where NN is the hex
	 * value of the byte).
	 */
	public void read(String filename, long offset, long num_bytes) throws IOException {
		Fat32File fat32File = getFileFat(new File(filename));
		System.out.println(fat32File);
		if (fat32File == null) {
			System.out.println(returnMessage.toString());
			return;
		}
		if (fat32File.directory) {
			System.out.println(fat32File.filename + " is a directory.");
			return;
		}
		int clusterNumber = fat32File.cluster;

		while (offset >= entireSectorSize) {
			clusterNumber = readFromFat(clusterNumber);
			offset -= entireSectorSize;
			if ((clusterNumber & 0x0FFFFFF8) == 0x0FFFFFF8) {
				System.out.println("Offset greater than file length");
				return;
			}
		}
		byte[] buffer = new byte[entireSectorSize];
		while ((clusterNumber & 0x0FFFFFF8) != 0x0FFFFFF8) {
			readFromCluster(clusterNumber, buffer);
			// safe cast because at this point offset must be less than an int because it
			// can't be larger than entireSectorSize
			for (int i = (int) offset; i < buffer.length && num_bytes > 0; i++, num_bytes--) {
				int curr = Byte.toUnsignedInt(buffer[i]);
				if (curr < 128) {
					System.out.print((char) curr);
				} else {
					System.out.print("0x" + Integer.toHexString(curr));
				}
				offset = 0;
			}
			clusterNumber = readFromFat(clusterNumber);
		}

		return;
	}

	public Fat32File getFileFat(File file) {
		if (file.toString().equals(File.separator)) {
			returnMessage.append(File.separatorChar);
			return rootFatFile;
		}
		int currPos = isAbsolute(file) ? RootFat : currFat;
		File movingDir = isAbsolute(file) ? new File(File.separator) : currDir;
		Fat32File returnDirectory = null;
		findnextpath: for (Path path : file.toPath()) {
			String pathName = path.toString();
			if (pathName.equals(".")) {
				continue;
			}

			if (returnDirectory != null && !returnDirectory.directory) {
				returnMessage.append(returnDirectory.filename + " is not a directory");
				return null;
			}
			for (Fat32File df : getContentsOfDir(currPos)) {
				if (pathName.equals("..") && currPos == RootFat) {
					returnMessage.append("Could not cd .. from root");
					return null;
				}
				if (df.filename.equalsIgnoreCase(pathName)) {
					currPos = df.cluster;
					returnDirectory = df;
					if (pathName.equals("..")) {
						movingDir = movingDir.getParentFile();
					} else {
						movingDir = new File(movingDir, df.filename);
					}
					continue findnextpath;
				}
			}
			// if continue fails, i.e. a matching directory could not be found
			returnMessage.append("Could not find path: " + pathName + " in path: " + file);
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
					continue;
				}
				if (buffer[i] == 'A' && ((buffer[i + 11]) & 0x0F) == 0x0F) {
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
		readFromCluster(RootFat, buffer);
		for (int i = 0; i < 11; i++) {
			buffer[i] = 0;
		}
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

	static class Fat32File {
		final String filename;
		final int cluster;
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
	}
}