
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
To compile our code one must be in the root folder where the "src" folder exists, and run "javac src/main/java/edu/yu/os/*.java".

# Running Instructions
To run our code one must first execute "cd src/main/java/" and then run "java edu.yu.os.Shell <FAT32 FILE_PATH>".

# Challenges
- Initially had issues leanring the proccess and math logic on how to access and go through file systems. In particular one major difficulty was "cd ..". In particular, the fact that the cluster # for ".." from a directory just below root is 0 and not 2 was a major source of headaches.