package com.elsworne.stubline.spike.dns;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;

/** Queries a DNS resolver over UDP with a bounded timeout and retries. */
public final class DnsMain {
  private DnsMain() {}

  public static void main(String[] args) throws Exception {
    String domain = args.length > 0 ? args[0] : "example.com";
    String host = args.length > 1 ? args[1] : "1.1.1.1";
    int port = args.length > 2 ? Integer.parseInt(args[2]) : 53;
    int timeout = args.length > 3 ? Integer.parseInt(args[3]) : 1000;
    Path capture = args.length > 4 ? Path.of(args[4]) : null;
    int attempts = 3;
    DnsMessage.Response response;
    try {
      response = lookup(domain, new InetSocketAddress(host, port), timeout, attempts, capture);
    } catch (SocketTimeoutException e) {
      System.err.printf(
          "Resolver %s:%d did not answer after %d attempts (%d ms per attempt)%n",
          host, port, attempts, timeout);
      System.exit(1);
      return;
    }
    System.out.println("rcode=" + response.rcode() + " truncated=" + response.truncated());
    response
        .answers()
        .forEach(
            answer ->
                System.out.println(
                    answer.name()
                        + " "
                        + answer.type()
                        + " "
                        + answer.value()
                        + " TTL="
                        + answer.ttl()));
  }

  public static DnsMessage.Response lookup(
      String domain, InetSocketAddress resolver, int timeoutMillis, int attempts, Path capture)
      throws Exception {
    if (timeoutMillis <= 0 || attempts <= 0)
      throw new IllegalArgumentException("Invalid retry settings");
    int id = new SecureRandom().nextInt(65536);
    byte[] query = DnsMessage.query(domain, id);
    try (var socket = new DatagramSocket()) {
      socket.setSoTimeout(timeoutMillis);
      for (int attempt = 1; attempt <= attempts; attempt++) {
        socket.send(new DatagramPacket(query, query.length, resolver));
        try {
          long expires =
              System.nanoTime() + java.util.concurrent.TimeUnit.MILLISECONDS.toNanos(timeoutMillis);
          while (true) {
            long remaining = expires - System.nanoTime();
            if (remaining <= 0) throw new SocketTimeoutException("No matching DNS response");
            socket.setSoTimeout(
                (int) Math.max(1, java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(remaining)));
            byte[] bytes = new byte[4096];
            var reply = new DatagramPacket(bytes, bytes.length);
            socket.receive(reply);
            if (!reply.getSocketAddress().equals(resolver)) continue;
            var decoded = DnsMessage.decode(bytes, reply.getLength());
            if (decoded.id() != id) continue;
            if (capture != null)
              Files.write(capture, java.util.Arrays.copyOf(bytes, reply.getLength()));
            if (decoded.truncated())
              throw new IllegalStateException("DNS answer truncated; retry over TCP is required");
            return decoded;
          }
        } catch (SocketTimeoutException e) {
          if (attempt == attempts) throw e;
        }
      }
    }
    throw new SocketTimeoutException("No matching DNS response");
  }
}
