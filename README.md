
## Authors

- [@Oze Botach](https://github.com/ozeitis/)
- [@Shmuel Newmark](https://github.com/synewmark)

#  Files & Directories List 
- We use Apache Maven to handle our builds. 
- In our root directory we have the "fat32.img" file and the "src" folder, and the "pom.xml" file.
    - The "fat32.img" is in the root directory of the project so the code can easily call it with './FILENAME.EXT'
    - The "src" folder contains the file-path with empty folders "src/main/java/edu/yu".
    - The "pom.xml" file contains the settings and configuration for our Apache Maven build manager.
- The "yu" folder contains the "os" folder which has the project Java files: "ISOHandler.java" and "Shell.java.
    - ISOHandler.java is teh beef of our program as it contains the passed in indexed file with a FAT32 architecture, and all the required methods to access it.
    - Shell.java is the high level part of our programw which contains the while loop that infinately scans for new user inputs, and sends the users commands to the ISOHandler.java, compelting the requested task.

# Compile Instructions
As we use Apache Maven to manage our code, to compile our code one must be in the root folder where the "src" folder exists, and run "mvn compile".

# Running Instructions

# Challenges