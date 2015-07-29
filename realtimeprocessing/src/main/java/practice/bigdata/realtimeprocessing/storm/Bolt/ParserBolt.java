package practice.bigdata.realtimeprocessing.storm.Bolt;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import practice.bigdata.realtimeprocessing.model.CoffeeOrder;
import practice.bigdata.realtimeprocessing.model.Statistics;
import redis.clients.jedis.Jedis;

import java.text.SimpleDateFormat;
import backtype.storm.tuple.Fields;
import java.util.*;

/**
 * Created by zoona on 7/28/15.
 */
public class ParserBolt extends BaseRichBolt {

    Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final long serialVersionUID = 1L;
    OutputCollector collector;


    public ParserBolt() {

    }

    @Override
    public void prepare(Map stormConf, TopologyContext context,
                        OutputCollector collector) {
        this.collector = collector;
    }

    @Override
    public void execute(Tuple input) {
        // get fields from tuple
        String valueString = input.getString(0);

        CoffeeOrder coffeeOrder = new CoffeeOrder(valueString);
        collector.emit("kafka_input_stream",
                new Values(coffeeOrder.getBranch(), coffeeOrder.getCustomerAgeGrade(),
                        coffeeOrder.getPaymentMethod(), coffeeOrder.getOrders()));
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        declarer.declareStream("kafka_input_stream", new Fields("branch", "customerAgeGrade",
                "paymentMethod", "orders"));
    }
}
