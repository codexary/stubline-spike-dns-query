[English](README.md) | 简体中文

# DNS query 小项目

这个独立项目手工构建 DNS A 查询包（header、question 和带长度的域名 label 编码），通过 UDP 发送，超时重试，并解码包含 compression pointer 的 A 答案。默认 resolver 为 `1.1.1.1:53`，可更改主机和端口。TC 位表示答案被截断，需要通过 TCP 重试；这个聚焦 UDP 的小项目只报告该情况。

安装 Java 21。Maven wrapper 使用 Maven 3.9.16。在仓库根目录运行：

```sh
./mvnw -q verify
./mvnw -q exec:java -Dexec.mainClass=com.elsworne.stubline.spike.dns.DnsMain -Dexec.args='example.com 1.1.1.1 53 1000'
```

第一条命令以 0 退出，没有正常输出。第二条输出 `rcode=0 truncated=false`，然后输出一行或多行 `example.com A <address> TTL=<seconds>`。地址和 TTL 随 resolver 的响应而变。第四个参数是毫秒级 timeout；程序最多尝试三次。可选的第五个参数是保存原始响应字节的文件路径。仓库中的 `example-com-a.bin` 和 `nxdomain.bin` 通过本程序从 1.1.1.1 采集。测试解码这些字节，在复制的响应中设置 TC 位，并使用本地 UDP 接收端验证三次超时和重试。

成功响应也输出 CNAME 别名，例如 `www.github.com CNAME github.com`。解析器无响应时，程序向 stderr 输出一行超时信息并以状态 1 退出。

## 许可证

本仓库采用 [MIT License](LICENSE)。

## 关于本仓库

本仓库包含一项针对 Stubline 的独立实验。README 说明运行命令与观察结果。

本仓库的代码与文档借助 AI 工具编写；记录的输出都来自实际运行。

## 结论

实验结果及其对后端决策的影响见[FINDINGS.zh-CN.md](FINDINGS.zh-CN.md)。
