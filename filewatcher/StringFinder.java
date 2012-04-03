

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class StringFinder {

	public static final String SERV1_PATH =  "C:/wasx_dev/SERV1/ws/code/" ;


  @SuppressWarnings("unchecked")
public static void main(String[] args) throws Exception {
    String[] searchIncludes = {"throw new"};
    String[] searchExcludes = {"throws","nls.getString","()","FacesMessages.getMsg"};
    String[] fileExts = {".java"};
    final String component = "jsf";
    String path = SERV1_PATH + component;


    int totalCount = 0;
	// List of directories to parse
	DirectoryStream<Path> stream = Files.newDirectoryStream(FileSystems.getDefault().getPath(SERV1_PATH), "*"+ component + "*");
	for (Path entry: stream) {
		System.out.println("Now Processing "+ entry);
		String outputFile = entry.toString() + "/"+ "StringFinderOutput.txt";
		totalCount +=searchForStringsInFiles(true, entry.toString(), fileExts, searchIncludes, searchExcludes, true,outputFile, null, true, component);
    }
	System.out.println(component + " TOTAL "+ totalCount);
  }




  public static int searchForStringsInFiles(boolean crawlDirs, String basePath, String[] fileExts, String[] includes, String[] excludes, boolean sameLine,
      String outputFile, String terminator, boolean ignoreComments, String component) throws Exception {
    File dir = new File(basePath);
    File[] files = dir.listFiles();
    BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));
    long start = System.currentTimeMillis();
    int[] info = searchRecur(crawlDirs, writer, files, fileExts, includes, excludes, sameLine, terminator, ignoreComments);
    int numFiles = info[0];
    int numFound = info[1];

    long duration = (System.currentTimeMillis() - start)/1000;
    String endMsg = component + " StringFinder > FINISHED! (" + numFound + " instances found in " + numFiles + " files scanned in " + duration + " seconds)";
    writer.write(endMsg);
    writer.close();
    System.out.println(endMsg);
    return numFound;
  }

  private static int[] searchRecur(boolean crawlDirs, BufferedWriter writer, File[] files, String[] fileExts, String[] includes,
      String[] excludes, boolean sameLine, String terminator, boolean ignoreComments) throws Exception {
    String nl = System.getProperty("line.separator");
    int filesScanned = 0;
    int numFound = 0;
    for(File file : files) {
      if(file.isDirectory()) {
        if(crawlDirs) {
       // <--- REMOVE CODE WHEN DONE WITH VECODE CHECK
          if(!file.getName().toUpperCase().contains("IFIXTOOL") && !file.getName().toUpperCase().contains(".TEST")) {
         // REMOVE CODE WHEN DONE WITH VECODE CHECK --->
            int[] recurInfo = searchRecur(crawlDirs, writer, file.listFiles(), fileExts, includes, excludes, sameLine, terminator, ignoreComments);
            filesScanned += recurInfo[0];
            numFound += recurInfo[1];
          }
        }
      } else if (isMatchingFileExt(fileExts, file)){
        filesScanned++;
        List<String> lines = new ArrayList<String>();
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String inLine;
        System.out.print(file.getAbsolutePath() + " : Reading file...");
        while((inLine = reader.readLine()) != null) {
          lines.add(inLine);
        }
        reader.close();
        System.out.println("done. " + lines.size() + " lines read");
        int lineNum = 1;
        int hits = 0;
        boolean ignore = false;
        int segmentStartLine = 1;


        for(int i=0; i<lines.size(); i++) {
          String line = lines.get(i);

          ignore=false;
          if(ignoreComments)
            if(line.trim().startsWith("*") || line.trim().startsWith("/*") || line.trim().startsWith("//")) {
              ignore = true;
              lineNum++;
            }

          if(!ignore){
            if(!sameLine) {

              for(String include : includes) {
                if(line.toUpperCase().contains(include.toUpperCase())) {
                  for(String exclude : excludes) {
                    if(line.toUpperCase().contains(exclude.toUpperCase())) {
                      ignore = true;
                      break;
                    }
                  }
                  if(!ignore) {
                 // <--- REMOVE CODE WHEN DONE WITH VECODE CHECK
                    String codeSegment = line;
                    segmentStartLine = lineNum;
                    if(terminator != null && !terminator.isEmpty()) {
                      while(!codeSegment.contains(terminator)) {
                        i++;
                        lineNum++;

                        try {
                        codeSegment += nl + lines.get(i);
                        } catch (IndexOutOfBoundsException e) {
                          break;
                        }
                      }
                    }
                    int startIndex = codeSegment.indexOf("(");
                    if(startIndex > -1) {
                      int stopIndex = codeSegment.indexOf(")", startIndex);
                      String sub;
                      if(stopIndex > -1)
                        sub = codeSegment.substring(startIndex+1, stopIndex);
                      else
                        sub = codeSegment;
                      if(sub.isEmpty())
                        ignore = true;
                    }
                    // REMOVE CODE WHEN DONE WITH VECODE CHECK --->

                    if(!ignore) {
                      hits++;
                      if (hits == 1) writer.write("file=" + file.getAbsolutePath() + nl);
                      String lineFound = String.format("[%8s]", segmentStartLine) + ": " + codeSegment;
                      writer.write(lineFound + nl);
                      break;
                    }
                  }
                }
              }
            } else {
              boolean allFound = true;
              for(String include : includes) {
                if(!line.toUpperCase().contains(include.toUpperCase())) {
                  allFound = false;
                  break;
                }
              }
              if(allFound) {
                for(String exclude : excludes) {
                  if(line.toUpperCase().contains(exclude.toUpperCase())) {
                    ignore = true;
                    break;
                  }
                }
                if(!ignore) {
                  hits++;
                  if (hits == 1) writer.write("file=" + file.getAbsolutePath() + nl);
                  String lineFound = String.format("[%8s]", lineNum) + ": " + line;
                  writer.write(lineFound + nl);
                  break;
                }
              }
            }
            lineNum++;
          }
        }
        System.out.println(file.getAbsolutePath() + " : hits=" + hits + nl);
        //writer.write(nl);
        numFound += hits;

        }
      }

    return new int[]{filesScanned,numFound};
  }

  public static boolean isMatchingFileExt(String[] exts, File file) throws Exception{
    for(String ext : exts) {
      if(ext.equals("*"))
        return true;

      int index = file.getAbsolutePath().lastIndexOf('.');

      if(index == -1)
        return false;

      if(file.getAbsolutePath().substring(index).toUpperCase().equals(ext.toUpperCase()))
        return true;
    }

    return false;
  }
}

