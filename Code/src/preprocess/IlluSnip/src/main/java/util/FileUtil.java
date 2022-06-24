package util;

import java.io.*;
import java.util.HashSet;
import java.util.Set;

public class FileUtil {

    public static boolean write(String path, String text) {

        try {
            FileWriter fw = null;
//如果文件存在，则追加内容；如果文件不存在，则创建文件
            File f = new File(path);
            fw = new FileWriter(f, true);

            PrintWriter pw = new PrintWriter(fw);
            pw.println(text);
            pw.flush();

            fw.flush();
            pw.close();
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public static Set<Integer> hasDoneDatasets(String path){
        Set<Integer> results = new HashSet<>();

            try {
                BufferedReader reader = new BufferedReader(new FileReader(path));
                String dataset = null;
                while((dataset=reader.readLine())!=null){
                    results.add(Integer.parseInt(dataset.substring(dataset.indexOf('\t')+1)));
//                System.out.println(dataset);
//                    System.out.println();
                }


            } catch (IOException e) {
                e.printStackTrace();
            }
        return results;
    }

}
