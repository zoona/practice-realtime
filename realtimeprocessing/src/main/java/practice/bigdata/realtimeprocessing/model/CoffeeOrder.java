package practice.bigdata.realtimeprocessing.model;

import java.lang.reflect.Type;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

public class CoffeeOrder {
  String customerAgeGrade;
  String paymentMethod;
  List<String> orders;

  public CoffeeOrder(String jsonString) {
    JsonParser parser = new JsonParser();
    JsonObject json = (JsonObject)parser.parse(jsonString);
    this.customerAgeGrade = json.get("customerAgeGrade").getAsString();
    this.paymentMethod = json.get("paymentMethod").getAsString();
    Type collectionType = new TypeToken<List<String>>() {}.getType();
    this.orders = new Gson().fromJson(json.get("orders"), collectionType);
  }

  public String getCustomerAgeGrade() {
    return customerAgeGrade;
  }

  public void setCustomerAgeGrade(String customerAgeGrade) {
    this.customerAgeGrade = customerAgeGrade;
  }

  public String getPaymentMethod() {
    return paymentMethod;
  }

  public void setPaymentMethod(String paymentMethod) {
    this.paymentMethod = paymentMethod;
  }

  public List<String> getOrders() {
    return orders;
  }

  public void setOrders(List<String> orders) {
    this.orders = orders;
  }
}
