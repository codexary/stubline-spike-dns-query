English | [简体中文](FINDINGS.zh-CN.md)

# Findings

## What we observed

The program builds a DNS A query, sends it over UDP and makes up to three attempts after timeouts. Fixtures and tests cover A answers, CNAMEs, compression pointers, NXDOMAIN and the TC bit. A truncated reply is reported but this spike does not retry it over TCP. The local UDP sink test exercises the timeout path.

## Backend decision

A timeout is an absence of a timely reply, not proof that the resolver or endpoint is down. A heartbeat or discovery check must validate the reply and its source and use bounded retries; TCP fallback for truncated DNS replies is outside this spike. See [backend ADR 0001 — UDP heartbeat suspicion](https://github.com/codexary/stubline-backend/blob/main/docs/adr/0001-udp-heartbeat-suspicion.md).
