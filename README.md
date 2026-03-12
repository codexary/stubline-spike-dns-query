English | [简体中文](README.zh-CN.md)

# DNS query spike

This standalone project builds a DNS A query packet by hand (header, question, label-length name encoding), sends it over UDP, retries after a timeout, and decodes A answers including compression pointers. It defaults to `1.1.1.1:53`; the resolver host and port can be changed. A response with the TC bit is reported as truncated and requires TCP retry, which this UDP-focused spike does not implement.

Install Java 21. The Maven wrapper uses Maven 3.9.16. From the repository root:

```sh
./mvnw -q verify
./mvnw -q exec:java -Dexec.mainClass=com.elsworne.stubline.spike.dns.DnsMain -Dexec.args='example.com 1.1.1.1 53 1000'
```

The first command exits 0 with no normal output. The second prints `rcode=0 truncated=false` followed by one or more `example.com A <address> TTL=<seconds>` lines. Addresses and TTLs change with the resolver response. The fourth argument is timeout milliseconds; the program makes up to three attempts. An optional fifth argument is a file path for saving the raw response bytes. The checked-in `example-com-a.bin` and `nxdomain.bin` fixtures were captured through this program from 1.1.1.1. Tests decode those bytes, set TC in a copied response, and use a local UDP sink to verify three timeouts and retries.

A successful response also prints CNAME aliases, such as `www.github.com CNAME github.com`. An unanswered resolver prints one timeout line to stderr and exits 1.

## License

This repository is licensed under the [MIT License](LICENSE).

## About this repository

This repository contains a focused Stubline experiment. The README documents its commands and observed results.

The code and documents in this repository were written with the help of AI tools; every recorded output comes from actually running them.
