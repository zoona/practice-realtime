package practice.bigdata.realtimeprocessing.storm;

import backtype.storm.generated.AuthorizationException;
import practice.bigdata.realtimeprocessing.storm.Bolt.CounterBolt;
import practice.bigdata.realtimeprocessing.storm.Bolt.SaveBolt;
import practice.bigdata.realtimeprocessing.storm.spout.RedisSpout;
import backtype.storm.Config;
import backtype.storm.LocalCluster;
import backtype.storm.StormSubmitter;
import backtype.storm.generated.AlreadyAliveException;
import backtype.storm.generated.InvalidTopologyException;
import backtype.storm.topology.TopologyBuilder;
import backtype.storm.tuple.Fields;

public class CoffeeTopology {
  private static String redisHost = "Tom";
  private static int redisPort = 6379;
  private static String topologyID = "CoffeeTopology";

  public static void main(String[] args) {
    // build topology
    TopologyBuilder builder = new TopologyBuilder();
    builder.setSpout("redisSpout", new RedisSpout(redisHost, redisPort));
    builder.setBolt("counterBolt", new CounterBolt(redisHost, redisPort), 20)
        .fieldsGrouping("redisSpout", "redis_input_stream",
            new Fields("branch"));
    builder.setBolt("saveBolt", new SaveBolt(redisHost, redisPort), 5)
        .shuffleGrouping("counterBolt", "counter_stream");
    // config
    Config conf = new Config();
    conf.setDebug(true);
    if (args.length > 1) {
      topologyID = args[1];
    }
    // submit
    //SubmitToLocal(conf, builder);
    SubmitToCluster(conf, builder);
  }

  public static void SubmitToLocal(Config conf, TopologyBuilder builder) {
    LocalCluster cluster = new LocalCluster();
    cluster.submitTopology(topologyID, conf, builder.createTopology());
  }

  public static void SubmitToCluster(Config conf, TopologyBuilder builder) {
    try {
      StormSubmitter.submitTopology(topologyID, conf, builder.createTopology());
    } catch (AlreadyAliveException e) {
      e.printStackTrace();
    } catch (InvalidTopologyException e) {
      e.printStackTrace();
    } catch (AuthorizationException e) {
      e.printStackTrace();
    }
  }
}
