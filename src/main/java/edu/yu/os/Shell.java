package edu.yu.os;

import java.io.File;
import java.io.IOException;

public class Shell {
	public static void main(String[] args) throws IOException {
		// ISOHandler iso = new ISOHandler(args[0]);
		ISOHandler fat32 = new ISOHandler(args[0]);
		fat32.ls("/");
//		Fat32File fatFile = fat32.getFileFat(new File("dir/a/spec/"));
//		System.out.println("File name: " + fatFile.filename);
//		System.out.println("File size: " + fatFile.size);
//
//		System.out.println();
//		fat32.read("\\fsinfo.txt", 0, 1000);
//		System.out.println();
//		fat32.read("\\dir\\a\\spec\\fatspec.pdf", 0, 1000);
//		System.out.println();
//		fat32.read("\\const.txt", 0, 1000);
//		System.out.println();

		System.out.println(fat32.ls("\\"));

		System.out.println(fat32.cd(new File("\\dir\\a\\spec")));

		System.out.println(fat32.getCurrDir());

		System.out.println(fat32.cd(new File("..\\..\\")));
		System.out.println(fat32.getCurrDir());
		System.out.println(fat32.size("\\const.txt"));
		System.out.println(fat32.read("\\const.txt", 45118, 1));

	}

	// Description: exits your shell-like utility
	private static void stop() {
		System.exit(0);
	}

	/*
	 * Description: prints out information about the following fields in both hex
	 * and base 10. Be careful to use the proper endian-ness: o BPB_BytesPerSec o
	 * BPB_SecPerClus o BPB_RsvdSecCnt o BPB_NumFATS o BPB_FATSz32
	 */
	private static String info() {
		return "";
	}

	/*
	 * Description: For the file or directory at the relative or absolute path
	 * specified in FILE_NAME or DIR_NAME, prints the size of the file or directory,
	 * the attributes of the file or directory, and the first cluster number of the
	 * file or directory. Return an error if FILE_NAME/DIR_NAME does not exist (see
	 * example below). (Note: The size of a directory will always be zero.)
	 */
	private static void stat(String file_path) {
		System.exit(0);
	}
}