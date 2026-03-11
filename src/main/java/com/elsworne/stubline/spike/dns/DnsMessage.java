package com.elsworne.stubline.spike.dns;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Minimal DNS wire-format encoder and A-answer decoder. */
public final class DnsMessage {
  public record Answer(String name, int ttl, String address) {}

  public record Response(int id, int rcode, boolean truncated, List<Answer> answers) {}

  private DnsMessage() {}

  public static byte[] query(String name, int id) throws IOException {
    if (id < 0 || id > 65535) throw new IllegalArgumentException("Invalid ID");
    var bytes = new ByteArrayOutputStream();
    try (var out = new DataOutputStream(bytes)) {
      out.writeShort(id);
      out.writeShort(0x0100);
      out.writeShort(1);
      out.writeShort(0);
      out.writeShort(0);
      out.writeShort(0);
      String domain = name.endsWith(".") ? name.substring(0, name.length() - 1) : name;
      if (domain.isBlank() || domain.length() > 253)
        throw new IllegalArgumentException("Invalid domain");
      for (var label : domain.split("\\.", -1)) {
        byte[] encoded = label.getBytes(StandardCharsets.US_ASCII);
        if (encoded.length == 0 || encoded.length > 63 || !label.matches("[A-Za-z0-9-]+"))
          throw new IllegalArgumentException("Invalid label");
        out.writeByte(encoded.length);
        out.write(encoded);
      }
      out.writeByte(0);
      out.writeShort(1);
      out.writeShort(1);
    }
    return bytes.toByteArray();
  }

  public static Response decode(byte[] data, int length) throws IOException {
    if (length < 12 || length > data.length) throw new IOException("Short DNS response");
    int flags = u16(data, 2), questions = u16(data, 4), answers = u16(data, 6);
    if ((flags & 0x8000) == 0) throw new IOException("Not a DNS response");
    if ((flags & 0x0200) != 0) return new Response(u16(data, 0), flags & 15, true, List.of());
    int[] offset = {12};
    for (int i = 0; i < questions; i++) {
      readName(data, length, offset);
      require(length, offset[0], 4);
      offset[0] += 4;
    }
    var decoded = new ArrayList<Answer>();
    for (int i = 0; i < answers; i++) {
      String name = readName(data, length, offset);
      require(length, offset[0], 10);
      int type = u16(data, offset[0]), kind = u16(data, offset[0] + 2);
      long ttl = ((long) u16(data, offset[0] + 4) << 16) | u16(data, offset[0] + 6);
      int rdLength = u16(data, offset[0] + 8);
      offset[0] += 10;
      require(length, offset[0], rdLength);
      if (type == 1 && kind == 1 && rdLength == 4)
        decoded.add(
            new Answer(
                name,
                (int) Math.min(ttl, Integer.MAX_VALUE),
                InetAddress.getByAddress(
                        java.util.Arrays.copyOfRange(data, offset[0], offset[0] + 4))
                    .getHostAddress()));
      offset[0] += rdLength;
    }
    return new Response(u16(data, 0), flags & 15, (flags & 0x0200) != 0, List.copyOf(decoded));
  }

  private static String readName(byte[] data, int length, int[] offset) throws IOException {
    var labels = new ArrayList<String>();
    int pos = offset[0], jumps = 0;
    boolean jumped = false;
    while (true) {
      require(length, pos, 1);
      int size = data[pos] & 255;
      if ((size & 0xc0) == 0xc0) {
        require(length, pos, 2);
        int target = ((size & 0x3f) << 8) | (data[pos + 1] & 255);
        if (!jumped) offset[0] = pos + 2;
        pos = target;
        jumped = true;
        if (++jumps > length) throw new IOException("DNS compression loop");
      } else if ((size & 0xc0) != 0) throw new IOException("Invalid DNS label");
      else if (size == 0) {
        if (!jumped) offset[0] = pos + 1;
        return String.join(".", labels);
      } else {
        require(length, pos + 1, size);
        labels.add(new String(data, pos + 1, size, StandardCharsets.US_ASCII));
        pos += size + 1;
        if (!jumped) offset[0] = pos;
      }
    }
  }

  private static int u16(byte[] data, int at) {
    return ((data[at] & 255) << 8) | (data[at + 1] & 255);
  }

  private static void require(int length, int at, int count) throws IOException {
    if (at < 0 || count < 0 || at + count > length) throw new IOException("Truncated DNS message");
  }
}
