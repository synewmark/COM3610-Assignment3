package edu.yu.os;

import java.io.IOException;
import java.util.Scanner;

public class Shell {
	static Scanner s = new Scanner(System.in);

	public static void main(String[] args) throws IOException {
		// ISOHandler iso = new ISOHandler(args[0]);
		ISOHandler fat32 = new ISOHandler(args[0]);
//		fat32.ls("/");
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
//		System.out.println(fat32.size("const.txt"));
//		System.out.println(fat32.ls("\\"));

//		System.out.println(fat32.cd("\\dir\\a\\spec"));

//		System.out.println(fat32.getCurrDir());
//		System.out.println(fat32.ls("..\\..\\..\\"));

//		System.out.println(fat32.cd("..\\..\\..\\"));
//		System.out.println(fat32.getCurrDir());
//		System.out.println(fat32.size("const.txt"));
//		System.out.println(fat32.read("\\const.txt", 45118, 1));

		// scan infinitely for commands
		while (true) {
			System.out.print(fat32.getWorkingDirectory() + "] ");
			String command = s.nextLine();
			String[] commandArray = command.split(" ");
			switch (commandArray[0]) {
			case "stop":
				stop();
				break;
			case "info":
				System.out.println(fat32.info());
				break;
			case "ls":
				if (commandArray.length > 1) {
					System.out.println(fat32.ls(commandArray[1]));
				} else {
					System.out.println(fat32.ls(""));
				}
				break;
			case "stat":
				System.out.println(fat32.stat(commandArray[1]));
				break;
			case "size":
				System.out.println(fat32.size(commandArray[1]));
				break;
			case "cd":
				String returnValue = fat32.cd(commandArray[1]);
				if (!returnValue.isEmpty()) {
					System.out.println(returnValue);
				}
				break;
			case "read":
				if (commandArray.length < 4) {
					System.out.println("ERROR insufficent parameters. Please include a file name, offset, and length");
					continue;
				}
				System.out.println(fat32.read(commandArray[1], Integer.parseInt(commandArray[2]),
						Integer.parseInt(commandArray[3])));
				break;
			default:
				System.out.println("Invalid command");
				break;
			}
		}

	}

	// Description: exits your shell-like utility
	private static void stop() {
		System.exit(0);
	}
}