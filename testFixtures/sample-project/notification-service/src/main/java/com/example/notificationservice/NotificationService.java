package com.example.notificationservice;

/** notification-service entry point - prints the value the run-environments plugin would inject. */
public final class NotificationService {
  public static void main(String[] args) {
    System.out.println("[notification-service] RUN_ENVIRONMENT_GREETING=" + greeting());
  }

  private static String greeting() {
    String value = System.getenv("RUN_ENVIRONMENT_GREETING");
    return value != null ? value : "<not set>";
  }
}
