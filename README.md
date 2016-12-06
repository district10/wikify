---
title: Wikify - Build wiki from my notes
keywords:
  - wikify
  - pandoc
  - java
---

## 使用方法

```
make
```

## 使用举例

为我的 [笔记](https://github.com/district10/notes) 生成搜索数据，用于页面搜索：

```bash
mkdir -p publish/_wikify
java -jar wikify.jar -ps '#main-body' -ns '#main-body > ul > li > dl' -i publish/ -o publish/ -ccs 50 -ncs 50 -pcs 20
```

生成的 xml 文件可以用于检索。我写了一个前端检索工具：[district10/searchy: Front-end of wikify](https://github.com/district10/searchy)。

在线 DEMO：[Searchy](http://whudoc.qiniudn.com/2016/wikify_20161206/index.html)。

## 参考

-   [jQuery-based Local Search Engine for Hexo | HaHack](http://hahack.com/codes/local-search-engine-for-hexo/#%E5%AE%89%E8%A3%85%E5%92%8C%E9%85%8D%E7%BD%AE-hexo-generator-search)
-   [PaicHyperionDev/hexo-generator-search: A plugin to generate the site's search data](https://github.com/PaicHyperionDev/hexo-generator-search)
-   <http://wzpan.github.io/hexo-theme-freemind/search.xml>
