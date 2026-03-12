package com.elsworne.stubline.spike.dns;

import static org.junit.jupiter.api.Assertions.*;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;

class DnsMessageTest {
  @Test
  void encodesHeaderQuestionAndLabels() throws Exception {
    byte[] query = DnsMessage.query("example.com", 0x1234);
    assertEquals(0x12, query[0] & 255);
    assertEquals(0x34, query[1] & 255);
    assertEquals(1, query[2] & 255);
    assertEquals(1, query[5] & 255);
    assertEquals(7, query[12] & 255);
    assertEquals('e', query[13]);
    assertEquals(3, query[20] & 255);
    assertEquals(0, query[24] & 255);
    assertEquals(1, query[26] & 255);
    assertEquals(1, query[28] & 255);
  }

  @Test
  void decodesCapturedAAnswersWithCompressionPointers() throws Exception {
    byte[] bytes = Files.readAllBytes(Path.of("src/test/resources/example-com-a.bin"));
    var response = DnsMessage.decode(bytes, bytes.length);
    assertEquals(0, response.rcode());
    assertFalse(response.truncated());
    assertEquals(2, response.answers().size());
    assertEquals("example.com", response.answers().getFirst().name());
    assertTrue(response.answers().stream().anyMatch(a -> a.address().equals("172.66.147.243")));
    assertTrue(response.answers().stream().anyMatch(a -> a.address().equals("104.20.23.154")));
  }

  @Test
  void decodesCapturedNxdomainAndTruncationBit() throws Exception {
    byte[] bytes = Files.readAllBytes(Path.of("src/test/resources/nxdomain.bin"));
    assertEquals(3, DnsMessage.decode(bytes, bytes.length).rcode());
    byte[] answer = Files.readAllBytes(Path.of("src/test/resources/example-com-a.bin"));
    answer[2] |= 0x02;
    assertTrue(DnsMessage.decode(answer, 12).truncated());
  }

  @Test
  void decodesRecordedCnameAndAAnswer() throws Exception {
    byte[] bytes = Files.readAllBytes(Path.of("src/test/resources/github-cname.bin"));
    var response = DnsMessage.decode(bytes, bytes.length);
    assertEquals("www.github.com", response.answers().get(0).name());
    assertEquals("CNAME", response.answers().get(0).type());
    assertEquals("github.com", response.answers().get(0).value());
    assertEquals("A", response.answers().get(1).type());
  }

  @Test
  void timesOutAndRetriesThreeTimes() throws Exception {
    try (var sink = new DatagramSocket(0);
        var executor = Executors.newSingleThreadExecutor()) {
      sink.setSoTimeout(1000);
      var count =
          executor.submit(
              () -> {
                int received = 0;
                while (received < 3) {
                  var packet = new DatagramPacket(new byte[512], 512);
                  sink.receive(packet);
                  received++;
                }
                return received;
              });
      assertThrows(
          SocketTimeoutException.class,
          () ->
              DnsMain.lookup(
                  "example.com",
                  new InetSocketAddress("127.0.0.1", sink.getLocalPort()),
                  30,
                  3,
                  null));
      assertEquals(3, count.get());
    }
  }
}
