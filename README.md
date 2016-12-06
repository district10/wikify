---
title: Wikify - Build wiki from my notes
keywords:
  - wikify
  - pandoc
  - java
---

build jar file:

```bash
mvn package # target/wikify.jar
```

run jar file

```bash
java -jar target/wikify.jar
# you can specify input/output dir by "-i <DIR>"/"--input <DIR>", "-o <DIR>"/"--output <DIR>".
```
使用举例：

为我的 [笔记](https://github.com/district10/notes) 生成搜索数据，用于页面搜索：

```bash
java -jar wikify.jar -bs '#main-body' -ns '#main-body > ul > li > dl' -i publish/ -o publish/
```

## 参考

-   [jQuery-based Local Search Engine for Hexo | HaHack](http://hahack.com/codes/local-search-engine-for-hexo/#%E5%AE%89%E8%A3%85%E5%92%8C%E9%85%8D%E7%BD%AE-hexo-generator-search)
-   [PaicHyperionDev/hexo-generator-search: A plugin to generate the site's search data](https://github.com/PaicHyperionDev/hexo-generator-search)
-   <http://wzpan.github.io/hexo-theme-freemind/search.xml>
