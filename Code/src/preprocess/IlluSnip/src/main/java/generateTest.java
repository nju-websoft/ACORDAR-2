import PCSG.PATHS;
import beans.OPTTriple;
import beans.ResultBean;
//import example.illusnipTest;
import util.*;
import beans.OPTTriple;
import beans.ResultBean;

import java.sql.*;
import java.util.*;

public class generateTest {
    private final static int TIMEOUT = 360000;
    private final static int MAX_SIZE = 171; //20;
    Connection dashConn = JdbcUtil.getConnection(GlobalVariances.LOCAL);

    private void getResultBase(String timeFilePath){
        Set<Integer> dones = new HashSet<>();
        List<String> lists = ReadFile.readString(timeFilePath);
        for(String s : lists){
            int dataset_id = Integer.parseInt(s.split("\t")[0]);
            dones.add(dataset_id);
        }
        try{
            Connection conn = JdbcUtil.getConnection(GlobalVariances.REMOTE);
            Connection dashConn = JdbcUtil.getConnection(GlobalVariances.LOCAL);
            dashConn.setAutoCommit(false);
            String pidStr = "select dataset_id from pid group by dataset_id having count(dataset_id)>1";
            Statement pidStmt = conn.createStatement();
            ResultSet pidRst = pidStmt.executeQuery(pidStr);

            int cnt=0;
            while(pidRst.next()){
                try{
                    int dataset_id = pidRst.getInt("dataset_id");
                    if(dones.contains(dataset_id)) continue;

                    OPTRank finder = new OPTRank(dataset_id, MAX_SIZE); // ======== MAX_SIZE ========
                    List<ResultBean> runningInfos = new ArrayList<>();
                    long runTime = timoutService(finder, dataset_id, runningInfos, TIMEOUT);//finder
                    boolean timeout = (runTime == Long.MAX_VALUE);
                    if (timeout) {
                        System.out.println("Time out: " + dataset_id);
                        Set<Integer> ids = new HashSet<>();
                        StringBuilder triplestr = new StringBuilder();
                        ArrayList<OPTTriple> result = finder.result;
                        if (result.isEmpty()) {
                            result = finder.currentSnippet;
                        }

                        String snippetstr = result.toString().replace("[","").replace("]","");
                        ResultBean bean = new ResultBean(dataset_id, snippetstr, TIMEOUT);
                        bean.setDataset(dataset_id);
                        saveResult(bean, timeFilePath);
                        continue;
                    }

                    ResultBean middleRuntimeBean = runningInfos.get(0);
                    middleRuntimeBean.setDataset(dataset_id);
                    saveResult(middleRuntimeBean, timeFilePath);


                }catch (Exception e){
                    e.printStackTrace();
                }
            }
            System.out.println(cnt);

        }catch (SQLException e){
            e.printStackTrace();
        }

    }

    private void saveResult(ResultBean bean, String timeFilePath) {
        try {
            String sql = String.format("INSERT INTO IlluSnip_3(dataset_id,snippet) values(%d,?)",bean.getDataset());

            PreparedStatement pst = dashConn.prepareStatement(sql);
            pst.setString(1, bean.getSnippet());
            pst.executeUpdate();
            pst.close();

            FileUtil.write(timeFilePath,bean.getDataset()+"\t"+ bean.runningTime + "\t" + bean.snippet);

        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static long  timoutService(OPTRank finder, int datasetId, List<ResultBean> runningInfos, long timeout){
        long time = Long.MAX_VALUE;
        CustomedThread subThread = new CustomedThread(finder, datasetId, runningInfos);
        subThread.start();
        try {
            subThread.join(timeout);

            if(!subThread.isAlive())
                time = subThread.lastTime;

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        subThread.interrupt();
        return time;
    }

    private static class CustomedThread extends Thread{
        public long lastTime = Long.MAX_VALUE;
        OPTRank finder;
        int datasetId;
//        Set<OPTTriple> result;
        ArrayList<OPTTriple> result;
        List<ResultBean> runningInfos;
        public CustomedThread(OPTRank finder, int datasetId, List<ResultBean> runningInfos) {
            super();
            this.finder = finder;
            this.datasetId = datasetId;
            this.runningInfos = runningInfos;
        }
        @Override
        public void run(){
            long start = System.currentTimeMillis();
            try {
                finder.findSnippet();
                result = finder.result;
            } catch (Exception e) {
                e.printStackTrace();
            }
            lastTime = System.currentTimeMillis() - start;

            String snippetstr = result.toString().replace("[","").replace("]","");
            ResultBean bean = new ResultBean(datasetId, snippetstr, lastTime);
            runningInfos.add(bean);
        }
    }

    public static void main(String[] args){
        generateTest test = new generateTest();
        test.getResultBase("C:\\Users\\17223\\Desktop\\websoft\\code\\IlluSnip\\src\\main\\resources\\input_file.txt");
    }
}
