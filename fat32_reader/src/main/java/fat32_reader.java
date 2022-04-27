
import java.io.*;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.*;



public class fat32_reader {

    public static void main(String[] args) throws IOException {
        File file = new File("fat32.img");
        Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_16);
        //String image = args[0];
        //System.out.println(Integer.toHexString(reader.read()));
        char[] data = new char[512];
        byte[] databyte = new byte [1028];
        //reader.read(data);
        int num =0;
        int j = 0;
        for(int i = 0; i< data.length; i++){
           String fullHex = (Integer.toHexString(reader.read()));
         //   byte fullHex = ((Integer)(reader.read())).byteValue();
            System.out.println(fullHex);
            byte firstByte = Byte.parseByte(fullHex.substring(0,2), 16);
            byte secondByte = Byte.parseByte(fullHex.substring(2,4), 16);
            System.out.println(firstByte);
            System.out.println(secondByte);
            /*num = reader.read();
            System.out.println(num);
            databyte[j] = (byte)(num&0xFF00);
            databyte[j+1] = (byte)((num&0x00FF)*0xFF);
            System.out.println(databyte[j]);
            System.out.println(databyte[j+1]);
            j++;*/

        }
//        for (int i = 1; i <= data.length; i++){
//            System.out.print(data[i]);
//            //reader.read();
//            if (i % 4 == 0){
//                System.out.print("|");
//            }
//            if (i % 16 == 0){
//                System.out.print("\n");
//            }
//        }
       // byte[] dataByte = new String(data).getBytes(StandardCharsets.UTF_8);
      /*  for (int i = 0; i < data.length; i++){
            //System.out.print(data[i]);
            System.out.print(dataByte[i] + " ");
            if ((i+1) % 4 == 0){
                System.out.print("|");
            }
            if ((i+1) % 16 == 0){
                System.out.print("\n");
            }
        }*/

      /*  int i = 0;
        for(char c: data){
            databyte[i] = (byte)(c&0xFF00);
            databyte[i+1] = (byte)((c&0x00FF)*0xFF);
            i= i+2;
            System.out.println(databyte[i]);
        }*/
        ByteOrder byteOrder = ByteOrder.nativeOrder();

        System.out.println(byteOrder);



    }
}
