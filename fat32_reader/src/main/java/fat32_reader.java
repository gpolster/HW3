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
    static List<Integer> currentClusters = new LinkedList<Integer>();
    public static void main(String[] args) throws IOException {
        File file = new File("newfat32.img");
        Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_16);

        byte[] dataByte = new byte [512];


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

        byte[] fat = new byte[BytesPerSec*FATSz32];//512*1009
        int amountSkipped = 0;
        for(int i = 0; i<BytesPerSec*RsvdSecCnt-512; i+=2){
            reader.read();
            amountSkipped = i;
        }
        System.out.println("amount skipped "+ amountSkipped);
        //  System.out.println("Amount skipped "+reader.skip((long)(BytesPerSec*RsvdSecCnt-512)));

        //we are at the start of the fat
        for(int i = 0; i< fat.length; i+=2){


            int   twoBytes = reader.read();

            //     System.out.print(twoBytes);
            int firstInt = ((twoBytes&0x0000FF00)>>8);
            int secondInt = ((twoBytes&0x000000FF));
            byte firstByte =  (byte)firstInt;
            byte secondByte =  (byte)secondInt;
            fat[i] = firstByte;
            fat[i+1] = secondByte;


            //    System.out.print(fat[i]+" ");
            //    System.out.print(fat[i+1]+" ");

        }
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



        RandomAccessFile fat32 = new RandomAccessFile("newfat32.img", "r");
        fat32.skipBytes(BytesPerSec*(RsvdSecCnt));//skipping up to the first fat

        fat32.skipBytes(BytesPerSec*FATSz32*(NumFATS));//skipping to start of data region
        // fat32.skipBytes(BytesPerSec*2); //skip to start of root

        for(int i = 0; i< 11; i++){
            //   System.out.print((char)fat32.readUnsignedByte());//short name
        }
        try{
            cd("DIR        ", fat32);
        }
        catch(IOException e){
            System.out.println("saf");
        }
        fat32.skipBytes(10);
        System.out.println();
        fat32.skipBytes(18);
        System.out.print(fat32.readUnsignedByte()+" ");
        System.out.print(fat32.readUnsignedByte()+" ");
        System.out.print(fat32.readUnsignedByte()+" ");
        System.out.print(fat32.readUnsignedByte()+" ");


    }
    private static void cd(String dirName, RandomAccessFile fat32)throws IOException{
        for(Integer cluster: currentClusters){
            fat32.skipBytes((cluster-2)*512);//skip to the cluster we want to read
            for(int i = 0; i<BytesPerSec/32; i++){//look at all 16 entries
                String shortName = "";
                int dirFlag =  0b00010000;
                for(int j = 0; j<11; j++){
                    shortName = shortName+((char)fat32.readUnsignedByte());
                }
                System.out.println(shortName);
                // System.out.println();
                System.out.println("first "+dirFlag);
                byte flagByte = (fat32.readByte());
                System.out.println("fb "+ flagByte);
                dirFlag = flagByte&dirFlag;
                System.out.println("sec " +dirFlag);
                if(shortName.equals(dirName)&&dirFlag==0b00010000){
                    //change into this directory.
                    System.out.println("Yayyyyy");
                }
                System.out.println();
                fat32.skipBytes(20);
            }
        }

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