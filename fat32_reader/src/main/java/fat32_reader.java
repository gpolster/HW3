import java.io.*;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.*;



public class fat32_reader {
    static int BytesPerSec;
    static int SecPerClus;
    static int RsvdSecCnt;
    static int NumFATS;
    static int FATSz32;
    static int RootClus;
    static byte[] fat;
    static LinkedList<Integer> listOfPastClusters;
    static List<Integer> currentClusters = new LinkedList<Integer>();
    static LinkedList<String> path = new LinkedList<>();

    public static void main(String[] args) throws IOException {
        File file = new File(args[0]);
        Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_16);
        path.add("");
        byte[] dataByte = new byte [512];
        listOfPastClusters = new LinkedList<Integer>();
        listOfPastClusters.add(2);//root is at cluster 2
        for(int i = 0; i< dataByte.length; i+=2){
            int twoBytes = reader.read();
            //  System.out.println(twoBytes);
            int firstInt = ((twoBytes&0x0000FF00)>>8);
            int secondInt = ((twoBytes&0x000000FF));
            byte firstByte =  (byte)firstInt;
            byte secondByte =  (byte)secondInt;
            dataByte[i] = firstByte;
            dataByte[i+1] = secondByte;
        }
        BytesPerSec = endianConverter(dataByte, 11, 2);
        SecPerClus = endianConverter(dataByte, 13, 1);
        RsvdSecCnt = endianConverter(dataByte, 14, 2);
        NumFATS = endianConverter(dataByte, 16, 1);
        FATSz32 = endianConverter(dataByte, 36, 4);
        RootClus = endianConverter(dataByte, 0x2c, 4);//shoulfd be 2

        fat = new byte[BytesPerSec*FATSz32];//512*1009
        RandomAccessFile fat32 = new RandomAccessFile(args[0], "r");
        fat32.skipBytes(BytesPerSec*RsvdSecCnt);
        fat32.read(fat);
        LinkedList<Integer> rootClusters = new LinkedList<Integer>();
        rootClusters.add(RootClus);
        int currentRootCluster = endianConverter(fat, RootClus*4, 4); //Rootclus = 2
        while(currentRootCluster<0x0FFFFFF8){
            rootClusters.add(currentRootCluster);
            currentRootCluster = endianConverter(fat, (currentRootCluster)*4,4);
            System.out.println(currentRootCluster);
        }
        currentClusters.addAll(rootClusters);
        fat32.skipBytes(BytesPerSec*(RsvdSecCnt));//skipping up to the first fat
        fat32.skipBytes(BytesPerSec*FATSz32*(NumFATS));//skipping to start of data region
        // fat32.skipBytes(BytesPerSec*2); //skip to start of root
        Scanner scanner = new Scanner(System.in);
        System.out.print("/] ");
        while(scanner.hasNextLine()){

            switch (scanner.next()){
                case("stop"):
                    System.exit(0);
                    break;
                case ("info"):
                    info();
                    break;
                case ("cd"):
                    cd(scanner.next(),fat32);
                    break;
                case ("ls"):
                    ls(scanner.next(),fat32);
                    break;
                case("stat"):
                    stat(scanner.next(),fat32);
                    break;
                case("size"):
                    size(scanner.next(),fat32);
                    break;
                case("read"):
                    read(scanner.next(),scanner.next(),scanner.next(),fat32);
                    break;
            }
            System.out.println("\n");
            System.out.print("/] " + getDir(path.getLast()) + " ");
        }



    }

    private static void ls(String dirName, RandomAccessFile fat32)throws IOException{
        if(dirName.contains("/")) cd(dirName, fat32, true);
        else {
            cd(dirName, fat32);
        }
        for(Integer cluster: currentClusters){
            fat32.seek((long)(BytesPerSec*RsvdSecCnt)+(long)(BytesPerSec*FATSz32*(NumFATS)));//Go to start of data region
            fat32.skipBytes((cluster-2)*512);//skip to the cluster we want to read
            for(int i = 0; i<BytesPerSec/32; i++){//look at all 16 entries
                String shortName = "";
                for(int j = 0; j<11; j++){
                    shortName = shortName+((char)fat32.readUnsignedByte());
                }
                int totalVal = 0;
                for(int g = 0; g< 11; g++){
                    totalVal = totalVal+((int)shortName.charAt(g));
                }
                if(totalVal!=0){
                    System.out.print(shortName+"    ");

                }
                fat32.skipBytes(21);//switchedby a bit
            }
        }
    }

    private static void stat(String name, RandomAccessFile fat32) throws IOException {
        String ogName = name;
        int lastSlash = 0;
        boolean thereIsError = true;
        if(name.contains("/")){
            lastSlash = name.lastIndexOf("/");
            cd(name.substring(0, lastSlash), fat32, true);
            name = name.substring(lastSlash+1);
        }
        boolean breakAgain = false;
        name = getFile(name);
        byte flagByte = 0;
        int nextCluster = 0;
        //    fileName = getShortName(fileName);
        for(Integer cluster: currentClusters){
            fat32.seek((long)(BytesPerSec*RsvdSecCnt)+(long)(BytesPerSec*FATSz32*(NumFATS)));//Go to start of data region
            fat32.skipBytes((cluster-2)*512);//skip to the cluster we want to read
            for(int i = 0; i<BytesPerSec/32; i++){//look at all 16 entries
                String shortName = "";
                for(int j = 0; j<11; j++){
                    shortName = shortName+((char)fat32.readUnsignedByte());
                }
                int dirFlag =  0b00010000;
                flagByte = (fat32.readByte());//we are reading this byte
                dirFlag = flagByte&dirFlag;

                if(shortName.toUpperCase().equals(name)&&dirFlag!=0b00010000){
                    thereIsError = false;

                    fat32.skipBytes(8);
                    byte[] clusterBytesLE = new byte[4];
                    clusterBytesLE[0] = fat32.readByte();
                    clusterBytesLE[1] = fat32.readByte();
                    byte temp0 = clusterBytesLE[0];
                    byte temp1 = clusterBytesLE[1];
                    fat32.skipBytes(4);
                    clusterBytesLE[2] = fat32.readByte();
                    clusterBytesLE[3] = fat32.readByte();
                    clusterBytesLE[0]=clusterBytesLE[2];
                    clusterBytesLE[1]=clusterBytesLE[3];
                    clusterBytesLE[2]=temp0;
                    clusterBytesLE[3]=temp1;

                    nextCluster =  endianConverter(clusterBytesLE, 0, 4);

                    //  fat32.skipBytes(16);//switched by a bit
                    byte[] size = new byte[4];
                    fat32.read(size);
                    System.out.println("Size is "+endianConverter(size, 0, 4));
                    breakAgain = true;
                    break;
                } else if(shortName.toUpperCase().equals(name)&&dirFlag == 0b00010000){
                    thereIsError = false;
                    fat32.skipBytes(8);
                    byte[] clusterBytesLE = new byte[4];
                    clusterBytesLE[0] = fat32.readByte();
                    clusterBytesLE[1] = fat32.readByte();
                    byte temp0 = clusterBytesLE[0];
                    byte temp1 = clusterBytesLE[1];
                    fat32.skipBytes(4);
                    clusterBytesLE[2] = fat32.readByte();
                    clusterBytesLE[3] = fat32.readByte();
                    clusterBytesLE[0]=clusterBytesLE[2];
                    clusterBytesLE[1]=clusterBytesLE[3];
                    clusterBytesLE[2]=temp0;
                    clusterBytesLE[3]=temp1;

                    nextCluster =  endianConverter(clusterBytesLE, 0, 4);
                    System.out.println("Size is 0");
                    breakAgain = true;
                    break;
                }

                fat32.skipBytes(20);//switched by a bit
            }
            if(breakAgain)
                break;
        }
        if(thereIsError){
            System.out.println("Error: file/directory does not exist");
            return;
        }
        System.out.println("Attributes " + getAttributes(flagByte));
        System.out.println("Next cluster number is 0x" + addZeros(Integer.toHexString(nextCluster)));
    }
    private static String addZeros(String s){
        StringBuilder stringBuilder = new StringBuilder(s);
        while (stringBuilder.length() != 4){
            stringBuilder.append("0");
        }
        return stringBuilder.reverse().toString();
    }
    private static String getAttributes(byte b){
        int mask =  0b1000000;
        StringBuilder s = new StringBuilder();
        for(int i = 6; i >= 0; i--){
            if(mask == (mask&b)){
                s.append(" ");
                s.append(getAtr(i));
            }
            b= (byte) (b<<1);
        }
        return s.toString();

    }
    private static String getAtr(int i){
        switch (i){
            case(5):
                return "ATTR_ARCHIVE";
            case(4):
                return "ATTR_DIRECTORY";
            case(3):
                return "ATTR_VOLUME ID";
            case(2):
                return "ATTR_SYSTEM";
            case(1):
                return "ATTR_HIDDEN";
            case(0):
                return "ATTR_READ-ONLY";
        }
        return i + "";
    }
    private static String getFile(String s){
        char [] letters = s.toCharArray();
        StringBuilder stringBuilder = new StringBuilder();
        int i = 0;
        while(i<letters.length&&'.' !=(letters[i])){
            stringBuilder.append(letters[i]);
            i++;
        }
        if(i== letters.length){
            return getShortName(s);
        }

        for (int j = i; j < 8; j++){
            stringBuilder.append(" ");
        }
        i++;
        stringBuilder.append(letters[i]);
        stringBuilder.append(letters[i+1]);
        stringBuilder.append(letters[i+2]);
        return stringBuilder.toString().toUpperCase();
    }
    private static int size(String fileName, RandomAccessFile fat32)throws IOException{
        String ogName = fileName;
        int lastSlash = 0;
        boolean thereIsError = true;
        if(fileName.contains("/")){
            lastSlash = fileName.lastIndexOf("/");
            cd(fileName.substring(0, lastSlash), fat32);
            fileName = fileName.substring(lastSlash+1);
        }
        fileName = getFile(fileName);
        System.out.println("FN:"+fileName);
        byte[] size= new byte[4];
        //    fileName = getShortName(fileName);
        for(Integer cluster: currentClusters){
            fat32.seek((long)(BytesPerSec*RsvdSecCnt)+(long)(BytesPerSec*FATSz32*(NumFATS)));//Go to start of data region
            fat32.skipBytes((cluster-2)*512);//skip to the cluster we want to read
            boolean breakAgain = false;

            for(int i = 0; i<BytesPerSec/32; i++){//look at all 16 entries
                String shortName = "";
                for(int j = 0; j<11; j++){
                    shortName = shortName+((char)fat32.readUnsignedByte());
                }
                int dirFlag =  0b00010000;
                System.out.println("SN: "+shortName);
                byte flagByte = (fat32.readByte());//we are reading this byte
                dirFlag = flagByte&dirFlag;

                if(shortName.toUpperCase().equals(fileName)&&dirFlag!=0b00010000){
                    System.out.println("found it");
                    thereIsError = false;
                    fat32.skipBytes(16);//switched by a bit
                    // size = new byte[4];
                    fat32.read(size);
                    System.out.println("Size of "+ogName+" is "+endianConverter(size, 0, 4)+" Bytes");

                    breakAgain = true;
                    break;
                }
                if(breakAgain) break;
                fat32.skipBytes(20);//switched by a bit
            }
        }
        if(thereIsError){
            System.out.println("Error: "+ogName+" is not a file");
        }
        return endianConverter(size, 0, 4);
    }

    private static void read(String fileName, String offset, String numBytes, RandomAccessFile fat32) throws IOException{
        String ogName = fileName;
        int lastSlash = 0;
        boolean thereIsError = true;
        if(fileName.contains("/")){
            lastSlash = fileName.lastIndexOf("/");
            //System.out.println("going: "+fileName.substring(0, lastSlash-1));
            cd(fileName.substring(0, lastSlash), fat32);
            fileName = fileName.substring(lastSlash+1);
        }
        fileName = getFile(fileName);

        //System.out.println("FN:"+fileName);
        if(isFile(fileName)){
            for(Integer cluster: currentClusters){
                fat32.seek((long)(BytesPerSec*RsvdSecCnt)+(long)(BytesPerSec*FATSz32*(NumFATS)));//Go to start of data region
                fat32.skipBytes((cluster-2)*512);//skip to the cluster we want to read
                boolean breakAgain = false;
                for(int i = 0; i<BytesPerSec/32; i++){//look at all 16 entries
                    String shortName = "";
                    for(int j = 0; j<11; j++){
                        shortName = shortName+((char)fat32.readUnsignedByte());
                    }
                    int dirFlag =  0b00010000;
                    System.out.println("SN: "+shortName);
                    byte flagByte = (fat32.readByte());//we are reading this byte
                    dirFlag = flagByte&dirFlag;
                    if(shortName.toUpperCase().equals(fileName)&&dirFlag!=0b00010000){
                        System.out.println("found it: ");
                        try{
                            System.out.println("found the file");
                            fat32.skipBytes(8);
                            byte[] clusterBytesLE = new byte[4];
                            clusterBytesLE[0] = fat32.readByte();
                            clusterBytesLE[1] = fat32.readByte();
                            byte temp0 = clusterBytesLE[0];
                            byte temp1 = clusterBytesLE[1];
                            fat32.skipBytes(4);
                            clusterBytesLE[2] = fat32.readByte();
                            clusterBytesLE[3] = fat32.readByte();
                            clusterBytesLE[0]=clusterBytesLE[2];
                            clusterBytesLE[1]=clusterBytesLE[3];
                            clusterBytesLE[2]=temp0;
                            clusterBytesLE[3]=temp1;
                            int startPoint = endianConverter(clusterBytesLE, 0, 4);
                            System.out.println("StartPoint "+ startPoint);
                            read(startPoint, Integer.parseInt(offset), Integer.parseInt(numBytes), size(shortName, fat32), fat32);
                        }
                        catch(NumberFormatException e){
                            System.out.println("Must be a number");
                        }

                        breakAgain = true;
                        break;
                    }
                    if(breakAgain) break;
                    fat32.skipBytes(20);//switched by a bit
                }
            }

        }
    }
    private static void read(int startPoint, int offset, int numBytes, int size, RandomAccessFile fat32)throws IOException{
        //  int startPoint = getNextEntry(fat32);

        LinkedList<Integer> fatTrav = new LinkedList<Integer>();

        System.out.println("SP " +startPoint);

        int currentCluster = startPoint;
        // fatTrav.add(startPoint);
        while(currentCluster<0x0FFFFFF8){
            fatTrav.add(currentCluster);
            currentCluster = endianConverter(fat, (currentCluster)*4,4);
            //    System.out.println("Next cluster "+currentCluster);
        }

        byte[] content = new byte[size+1];

        byte c = 'a';
        int index = 0;
        for(Integer cluster: fatTrav){
            fat32.seek((long)(BytesPerSec*RsvdSecCnt)+(long)(BytesPerSec*FATSz32*(NumFATS)));//Go to start of data region
            fat32.skipBytes((cluster-2)*512);//skip to the cluster we want to read
            for(int i = 0; i<BytesPerSec&&(index<size); i++){
                c = fat32.readByte();

                //  System.out.print((char)c);
                content[index] = c;
                index++;
                //System.out.println("adding");


            }

        }
        for(int i = offset; i<numBytes+offset&&i<size; i++){

            if(content[i]<127&&content[i]>-1){
                System.out.print((char)content[i]);
            }
            else{
                System.out.print(String.format("%02X ", content[i]));//to hex
            }
        }

    }
    private static boolean isFile(String file){
        if(file.toUpperCase().contains("PDF")||file.toUpperCase().contains("TXT")) return true;
        return false;
    }

    private static int getNextEntry(RandomAccessFile fat32) throws IOException {
        System.out.println("Yayyyyy");
        fat32.skipBytes(8);
        byte[] clusterBytesLE = new byte[4];
        clusterBytesLE[0] = fat32.readByte();
        clusterBytesLE[1] = fat32.readByte();
        byte temp0 = clusterBytesLE[0];
        byte temp1 = clusterBytesLE[1];
        fat32.skipBytes(4);
        clusterBytesLE[2] = fat32.readByte();
        clusterBytesLE[3] = fat32.readByte();
        clusterBytesLE[0]=clusterBytesLE[2];
        clusterBytesLE[1]=clusterBytesLE[3];
        clusterBytesLE[2]=temp0;
        clusterBytesLE[3]=temp1;
        for (byte b : clusterBytesLE) {
            System.out.println(b);
        }
        return endianConverter(clusterBytesLE, 0, 4);
    }
    private static LinkedList<Integer> getClusters(int startingPoint){
        LinkedList<Integer> clusters = new LinkedList<Integer>();
        System.out.println("SP " +startingPoint);
        //   clusters.add(startingPoint);

        //   int startingPointOffset = endianConverter(fat, startingPoint*4, 4); //Rootclus = 2
        int currentCluster = startingPoint;

        while(currentCluster<0x0FFFFFF8){
            clusters.add(currentCluster);
            currentCluster = endianConverter(fat, (currentCluster)*4,4);
            System.out.println("Next cluster "+currentCluster);
        }
        currentClusters = clusters;
        return clusters;
    }
    private static void cd(String absPath, RandomAccessFile fat32, boolean path) throws IOException{
        String[] dirs = absPath.split("/");
        for(int i = 0; i<dirs.length; i++){
            System.out.println("next: "+dirs[i]);
            cd(dirs[i],fat32);
        }
    }
    private static void cd(String dirName, RandomAccessFile fat32)throws IOException{
        if(dirName.contains("/")) cd(dirName, fat32, true);
        dirName = getShortName(dirName);
        boolean breakAgain = false;
        for(Integer cluster: currentClusters){
            fat32.seek((long)(BytesPerSec*RsvdSecCnt)+(long)(BytesPerSec*FATSz32*(NumFATS)));//Go to start of data region
            fat32.skipBytes((cluster-2)*512);//skip to the cluster we want to read
            for(int i = 0; i<BytesPerSec/32; i++){//look at all 16 entries
                String shortName = "";
                int dirFlag =  0b00010000;
                for(int j = 0; j<11; j++){
                    shortName = shortName+((char)fat32.readUnsignedByte());
                }
                System.out.println(shortName);

                System.out.println("first "+dirFlag);
                byte flagByte = (fat32.readByte());
                System.out.println("fb "+ flagByte);
                dirFlag = flagByte&dirFlag;
                System.out.println("sec " +dirFlag);
//                if(dirName.equals(getShortName(".."))){
//                    System.out.println("removing last = "+ path.removeLast());
//                    if(listOfPastClusters.size()>1){
//
//                        listOfPastClusters.remove();
//                        getClusters(listOfPastClusters.getLast());
//                    }
//                    else{
//                        System.out.println("Error: Can not leave root");
//                    }
//                    breakAgain = true;
//                    break;
//                }
//                else if(dirName.equals("")){
//                    path = new LinkedList<>();
//                    listOfPastClusters = new LinkedList<Integer>();
//                    listOfPastClusters.add(2);
//                    getClusters(2);
//                    breakAgain = true;
//                    break;
//                }
                if(shortName.equals(dirName)&&dirFlag==0b00010000){
                    System.out.println("baaaaaaaaad");
                    //change into this directory.
                    //  currentAbsolutePath.add(dirName);
                    path.addLast(dirName);
                    int nextFatEntry = getNextEntry(fat32);
                    listOfPastClusters.add(nextFatEntry);
                    getClusters(nextFatEntry);//this changes the currentClusters
                    breakAgain = true;
                    break;
                    // currentClusters = //some traversal of the fat

                }
                System.out.println();
                if(breakAgain) break;
                fat32.skipBytes(20);
            }
        }

    }
    private static String getShortName(String s){
        StringBuilder sBuilder = new StringBuilder(s.toUpperCase());
        sBuilder.append(" ".repeat(Math.max(0, 11 - sBuilder.length())));
        s = sBuilder.toString();
        return s;
    }
    private static String getDir(String s){
        if(s.equals(""))return "";
        char [] letters = s.toCharArray();
        StringBuilder stringBuilder = new StringBuilder();
        int i = 0;
        while(' ' !=(letters[i])){
            stringBuilder.append(letters[i]);
            i++;
        }
        return stringBuilder.toString();
    }
    private static int endianConverter(byte[] bytes, int offSet, int amountOfBytes){
        int val = 0;
        int lastByte = offSet+amountOfBytes-1;
        // val = val & bytes[lastByte];
        // System.out.println("start for");
        for(int i = lastByte; i>=offSet; i--){
            val = val << 8;
            val = val | (bytes[i]&0x000000FF);
        }
        return val;
    }
    private static void info(){
        System.out.println("bps "+ BytesPerSec);
        System.out.println("sec "+ SecPerClus);
        System.out.println("rsvd "+ RsvdSecCnt);
        System.out.println("num "+ NumFATS);
        System.out.println("fat "+ FATSz32);
    }
}