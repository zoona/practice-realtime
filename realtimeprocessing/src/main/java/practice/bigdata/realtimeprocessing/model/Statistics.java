package practice.bigdata.realtimeprocessing.model;

import java.util.HashMap;
import java.util.Map;

public class Statistics {
  Map<String, Long> customerAgeGrades;
  Map<String, Long> paymentMethods;
  Map<String, Long> orders;

  public Statistics() {
    customerAgeGrades = new HashMap<String, Long>();
    paymentMethods = new HashMap<String, Long>();
    orders = new HashMap<String, Long>();
  }
  
  public Map<String, Long> getCustomerAgeGrades() {
    return customerAgeGrades;
  }

  public void setCustomerAgeGrades(Map<String, Long> customerAgeGrades) {
    this.customerAgeGrades = customerAgeGrades;
  }

  public Map<String, Long> getPaymentMethods() {
    return paymentMethods;
  }

  public void setPaymentMethods(Map<String, Long> paymentMethods) {
    this.paymentMethods = paymentMethods;
  }

  public Map<String, Long> getOrders() {
    return orders;
  }

  public void setOrders(Map<String, Long> orders) {
    this.orders = orders;
  }
}
