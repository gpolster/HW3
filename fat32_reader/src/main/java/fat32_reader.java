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
    public static void main(String[] args) throws IOException {
        File file = new File("fat32.img");
        Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_16);

        byte[] dataByte = new byte [512];


        for(int i = 0; i< dataByte.length; i+=2){
            int twoBytes = reader.read();
            //  System.out.println(twoBytes);
            int firstInt = ((twoBytes&0x0000FF00)/0xFF);
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
        for(int i = 0; i<BytesPerSec*RsvdSecCnt-512; i+=2){
            reader.read();
        }
        //  System.out.println("Amount skipped "+reader.skip((long)(BytesPerSec*RsvdSecCnt-512)));

        //we are at the start of the fat
        for(int i = 0; i< fat.length; i+=2){
            int twoBytes = reader.read();
            //     System.out.print(twoBytes);
            int firstInt = ((twoBytes&0x0000FF00)/0xFF);
            int secondInt = ((twoBytes&0x000000FF));
            byte firstByte =  (byte)firstInt;
            byte secondByte =  (byte)secondInt;
            fat[i] = firstByte;
            fat[i+1] = secondByte;
        }
        for(int i = 0; i < fat.length; i++){
            //   System.out.println(fat[i]);
        }
        LinkedList<Integer> rootClusters = new LinkedList<Integer>();
        rootClusters.add(RootClus);
        int currentRootCluster = endianConverter(fat, RootClus*4, 4);
        while(currentRootCluster<0x0FFFFFF8){
            rootClusters.add(currentRootCluster);
            System.out.println(currentRootCluster);
            currentRootCluster = endianConverter(fat, currentRootCluster*4,4);
        }



        for(int i = 0; i< dataByte.length; i++){
            //     System.out.println(dataByte[i]);
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