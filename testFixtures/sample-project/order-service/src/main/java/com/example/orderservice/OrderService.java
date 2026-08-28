package com.example.orderservice;

/** order-service entry point - prints the value the run-environments plugin would inject. */
public final class OrderService {
  public static void main(String[] args) {
    System.out.println("[order-service] RUN_ENVIRONMENT_GREETING=" + greeting());
  }

  private static String greeting() {
    String value = System.getenv("RUN_ENVIRONMENT_GREETING");
    return value != null ? value : "<not set>";
  }
}
