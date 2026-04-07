# 设计：解决文章重复抓取与保存耦合问题

## 问题描述

当前 `getLatestTextBySourceKey` 方法存在两个问题：

1. **重复插入**：每次抓取后都会调用 `saveText` 插入数据库，不检查文章是否已存在
2. **过度耦合**：获取文章（getText）与保存文章（saveText）逻辑耦合在一起

第三方API返回结果中包含 `a_name` 字段作为文章标题，而 `t_text` 表已经预留了 `title` 字段，可以利用标题做唯一性判断。

## 解决方案

### 架构变化

1. **新建 DTO `FetchedText`** - 承载抓取结果（title + content）
2. **修改 `SaiWenTextFetcher`** - 同时提取标题和内容，返回完整对象
3. **在 `TextMapper` 新增查询方法** - 按 `sourceId + title` 查找已有文章
4. **重构 `TextFetchService`** - 保存前检查唯一性，不存在才插入
5. **更新 `TextService`** - 适配新接口

### 唯一性保证

使用组合 `(sourceId, title)` 做为业务唯一键：
- 同一个文章来源不会有相同标题
- 若标题已存在则返回已有记录，避免重复插入

### API返回结构

根据用户提供的样例，第三方API返回结构：
- `error`: 错误码，0 表示成功
- `msg`: {
  - `"0"`: 文章内容正文
  - `"a_name"`: 文章标题
  - ...其他字段
}

## 模块修改清单

| 模块 | 修改类型 | 说明 |
|------|----------|------|
| `com.typetype.text.dto.FetchedText` | 新建 | DTO，存放 title + content |
| `com.typetype.text.service.SaiWenTextFetcher` | 修改 | 返回类型改为 `FetchedText`，提取标题 |
| `com.typetype.text.mapper.TextMapper` | 修改 | 新增 `findBySourceIdAndTitle` |
| `com.typetype.text.service.TextFetchService` | 修改 | 适配新返回类型，增加去重检查 |
| `com.typetype.text.service.TextService` | 修改 | 适配新接口 |

## 预期结果

- 相同标题的文章不会重复插入数据库
- 抓取逻辑和保存逻辑职责分离
- 解决了耦合问题，代码更清晰
