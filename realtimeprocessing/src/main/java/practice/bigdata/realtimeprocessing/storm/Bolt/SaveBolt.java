package practice.bigdata.realtimeprocessing.storm.Bolt;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import practice.bigdata.realtimeprocessing.model.Statistics;
import redis.clients.jedis.Jedis;
import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Tuple;

public class SaveBolt extends BaseRichBolt {

  private static final long serialVersionUID = 1L;
  Logger logger = LoggerFactory.getLogger(this.getClass());
  OutputCollector collector;
  Jedis jedis;

  String redisHost;
  int redisPort;

  public SaveBolt(String redisHost, int redisPort) {
    this.redisHost = redisHost;
    this.redisPort = redisPort;
  }
  
  @Override
  public void cleanup() {
    super.cleanup();
    jedis.disconnect();
  }

  @Override
  public void prepare(Map stormConf, TopologyContext context,
      OutputCollector collector) {
    this.collector = collector;
    jedis = new Jedis(redisHost, redisPort);
    jedis.connect();
  }

  @Override
  public void execute(Tuple input) {
    String branch = input.getStringByField("branch");
    String dateString = input.getStringByField("dateString");
    Statistics statistics = (Statistics) input.getValueByField("statistics");
 
    String key = branch + ":" + dateString + ":CustomerAgeGrades";
    increaseCounts(key, statistics.getCustomerAgeGrades());
  }

  @Override
  public void declareOutputFields(OutputFieldsDeclarer declarer) {
    // TODO Auto-generated method stub

  }

  private void increaseCounts(String hashKey, Map<String, Long> map) {
    Set<String> keys = map.keySet();
    for(String key : keys) {
      jedis.hincrBy(hashKey, key, map.get(key));
    }
  }

}
