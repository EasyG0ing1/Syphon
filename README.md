## Syphon

Syphon is a program that allows you to paste in a URL of any folder on a web server and as long as that server has directory listing enabled, then the entire folder tree including the files within the folders from that point downward will be mirrored on your local hard drive.

You also have the option of loading the folder structure into a tree view where you can select which files you wish to download.

Simply putting in a URL then pressing GO will cause the program to traverse that folder and it will recursively scour all of the folders and subfolders and as it finds files, it will add them to a que where it starts downloading as soon as the first file hits the queue.

The queue submits the jobs to a Java Thread manager which has a max base pool of 20 threads. However, you can change the number of threads from 1 to 20.

![MainScreen](./Images/MainScreen.png)

![TreeScreen](./Images/TreeScreen.png)

When you set a folder for downloading, I recommend creating a folder called something like `Websites` as the program will first create a folder that is the name of the web server you're downloading from (www.server.com) then it will create the tree mirror underneath that folder.

The program remembers both the last URL you downloaded as well as your local download path for convenience.

The tabs at the bottom capture the standard and error output from the program and it puts various messages into the related tab categories. (this could use some improvement).

The total progress bar at the top accumulates the size of each file as they are added to the job batch. It can take a little time for file size query to happen since it happens over an http connection. In addition to that, web servers have a limit, apparently, on how large they report a file to be. If the file is over something like 2Gigs, it will report a filesize of 0. So the overall progress bar might not ever be completely accurate, and it will take some time for it to finish receiving all of the file sizes.

Once the job que count at the top of the screen stops counting, then that top progress bar will be as accurate as it's going to be.

When you see files downloading where the progress bar just bounces from side to side, that would be a file that the server reported back as being of 0 size. The file will still download, but theres no way, obviously to show the progress since we don't know what the final size of the file will be.

___
I threw this project together over a weekend, and there are certainly areas that could use improvement, so feel free to create an issue or a pull request if you have any thoughts, concerns or wish to contribute.
