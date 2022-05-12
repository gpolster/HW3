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
    public static void main(String[] args) throws IOException {
        File file = new File("newlfat32.img");
        Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_16);

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
        RandomAccessFile fat32 = new RandomAccessFile("newlfat32.img", "r");
        fat32.skipBytes(BytesPerSec*RsvdSecCnt);
        fat32.read(fat);
        System.out.println(fat);
        System.out.println(fat[817*4]+" "+ fat[817*4+1]+" "+fat[817*4+2]+" "+fat[817*4+3]);
        System.out.println(endianConverter(fat, 3268, 4));

        LinkedList<Integer> rootClusters = new LinkedList<Integer>();
        System.out.println("RC"+RootClus);
        rootClusters.add(RootClus);

        int currentRootCluster = endianConverter(fat, RootClus*4, 4); //Rootclus = 2
        System.out.println("rootcluster "+ currentRootCluster);
        System.out.println(currentRootCluster);
        while(currentRootCluster<0x0FFFFFF8){
            rootClusters.add(currentRootCluster);
            currentRootCluster = endianConverter(fat, (currentRootCluster)*4,4);
            System.out.println(currentRootCluster);
        }
        currentClusters.addAll(rootClusters);




        fat32.skipBytes(BytesPerSec*(RsvdSecCnt));//skipping up to the first fat

        fat32.skipBytes(BytesPerSec*FATSz32*(NumFATS));//skipping to start of data region
        // fat32.skipBytes(BytesPerSec*2); //skip to start of root

        try{
            cd("DIr", fat32);
            cd("A", fat32);
            cd("SpeC ", fat32);
            cd("..", fat32);
            cd("..", fat32);
            cd("A", fat32);

        }
        catch(IOException e){
            System.out.println("sad");
        }
        fat32.skipBytes(10);
        System.out.println();
        fat32.skipBytes(18);
        System.out.print(fat32.readUnsignedByte()+" ");
        System.out.print(fat32.readUnsignedByte()+" ");
        System.out.print(fat32.readUnsignedByte()+" ");
        System.out.print(fat32.readUnsignedByte()+" ");


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
    private static void cd(String dirName, RandomAccessFile fat32)throws IOException{
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
                if(dirName.equals("..")){
                    if(listOfPastClusters.size()>1){
                        listOfPastClusters.remove();
                        getClusters(listOfPastClusters.getLast());
                    }
                    else{
                        System.out.println("Error: Can not leave root");
                    }
                    breakAgain = true;
                    break;
                }
                if(dirName.equals("")){
                    listOfPastClusters = new LinkedList<Integer>();
                    listOfPastClusters.add(2);
                    getClusters(2);
                    breakAgain = true;
                    break;
                }
                if(shortName.equals(dirName)&&dirFlag==0b00010000){
                    //change into this directory.
                    //  currentAbsolutePath.add(dirName);
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