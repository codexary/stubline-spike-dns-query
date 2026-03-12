[English](FINDINGS.md) | 简体中文

# 实验结论

## 观察

程序手工构造 DNS A 查询，通过 UDP 发送，超时后最多尝试三次。仓库中的响应样本和测试覆盖 A 记录、CNAME、压缩指针、NXDOMAIN 与 TC 位。遇到截断回复时，程序只报告结果，没有实现 TCP 重试。本地 UDP 接收端测试覆盖了超时路径。

## 对后端的影响

超时只说明没有及时收到回复，不能证明解析器或目标服务已宕机。心跳与服务发现需要核对回复及其来源，并限制重试次数；截断 DNS 回复的 TCP 后续处理不在本实验范围内。参见[后端 ADR 0001：UDP heartbeat suspicion](https://github.com/codexary/stubline-backend/blob/main/docs/adr/0001-udp-heartbeat-suspicion.md)。
