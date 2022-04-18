
import java.io.*;
import java.util.*;



public class fat32_reader {

    public static void main(String[] args) throws IOException {
        File file = new File("fat32.img");
        Reader reader = new FileReader(file);
        //String image = args[0];
        //System.out.println(Integer.toHexString(reader.read()));
        char[] data = new char[512];
        reader.read(data);
        for (int i = 0; i < data.length; i++){
            System.out.print(data[i]);
            //System.out.print(Integer.toHexString(reader.read()) + " ");
        }



    }
}
