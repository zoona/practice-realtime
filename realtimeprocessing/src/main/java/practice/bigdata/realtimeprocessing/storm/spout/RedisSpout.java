package practice.bigdata.realtimeprocessing.storm.spout;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import practice.bigdata.realtimeprocessing.model.CoffeeOrder;
import redis.clients.jedis.Jedis;
import backtype.storm.spout.SpoutOutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichSpout;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Values;
import backtype.storm.utils.Utils;

public class RedisSpout extends BaseRichSpout {

  private static final long serialVersionUID = 1L;
  Logger logger = LoggerFactory.getLogger(this.getClass());
  private SpoutOutputCollector collector;
  // redis info
  private String redisHost;
  private int redisPort;
  private String redisKey = "CoffeeOrderQueue";
  private Jedis jedis;

  public RedisSpout(String redisHost, int redisPort) {
    this.redisHost = redisHost;
    this.redisPort = redisPort;
  }
  
  @Override
  public void declareOutputFields(OutputFieldsDeclarer declarer) {
    declarer.declareStream("redis_input_stream", new Fields("customerAgeGrade",
        "paymentMethod", "orders"));

  }

  @Override
  public void open(Map conf, TopologyContext context,
      SpoutOutputCollector collector) {
    this.collector = collector;
    jedis = new Jedis(redisHost, redisPort);
    jedis.connect();

  }

  @Override
  public void nextTuple() {
    String valueString = jedis.lpop(redisKey);
    if (valueString == null) {
      Utils.sleep(5);
    } else {
      CoffeeOrder coffeeOrder = new CoffeeOrder(valueString);
      collector.emit("redis_input_stream",
          new Values(coffeeOrder.getCustomerAgeGrade(),
              coffeeOrder.getPaymentMethod(), coffeeOrder.getOrders()));
    }
  }

}
