package edu.yu.os;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

public class Shell {
	static Scanner s = new Scanner(System.in);

	public static void main(String[] args) {
		if (!new File(args[0]).exists()) {
			System.err.println("File does not exist. Exiting...");
			return;
		}
		ISOHandler fat32;
		try {
			fat32 = new ISOHandler(args[0]);
		} catch (IOException e) {
			System.err.println("Could not open file");
			e.printStackTrace();
			return;
		}
		// scan infinitely for commands
		while (true) {
			System.out.print(fat32.getWorkingDirectory() + "] ");
			String command = s.nextLine();
			String[] commandArray = command.split("\\s+");
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
				if (commandArray.length < 2) {
					System.out.println("ERROR: stat requires 2 arguements");
					continue;
				}
				System.out.println(fat32.stat(commandArray[1]));
				break;
			case "size":
				if (commandArray.length < 2) {
					System.out.println("ERROR: size requires 2 arguements");
					continue;
				}
				System.out.println(fat32.size(commandArray[1]));
				break;
			case "cd":
				if (commandArray.length < 2) {
					System.out.println("ERROR: cd requires 2 arguements");
					continue;
				}
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