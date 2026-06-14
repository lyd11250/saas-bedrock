# party 模块设计说明（相关方主数据）

> 本文件是 **party 业务模块的就近设计文档**（随模块维护，不并入基座主文档）。
> 接入基座的通用范式见仓库根 [业务模块接入规范.md](../../../../../../../../../../业务模块接入规范.md)；本文件只记录 party 自身的设计决策与字段语义。
> party 是首个 `biz` 模块，兼作上层业务接入样板。

---

## 1. 定位

管理租户内的**相关方主数据**：相关方 = 与本租户业务有关的主体，可能是**自然人**，也可能是**组织**（企业、政府机构、事业单位、社会组织等）。
其他业务模块（合同、项目等）将来统一以 `party_id` 引用相关方，不关心其具体是人还是组织。

---

## 2. 建模：Fowler Party 模式（统一主表 + 子表）

相关方按「自然人 vs 组织」二分，采用**主表存共同身份、子表存类型专属字段、子表与主表共享主键**的结构：

```
party (相关方主表)                         party_type: PERSON | ORGANIZATION
  id, tenant_id, party_type, name,         name = 统一显示名（姓名 / 单位名称）
  status, remark, 审计, deleted
   │  共享主键（子表 id = party.id）
   ├─ party_person       (PERSON 子表)     gender, id_card, contact
   └─ party_organization (ORGANIZATION 子表)
          org_type, tax_no, registered_capital, established_date,
          legal_person, reg_address, business_scope
```

### 为什么这样选

| 决策                                        | 理由                                                                                                                                     |
| ------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------- |
| 统一主表 + 子表（非单表多态，非独立双表）   | 给每个相关方一个**稳定的 `party_id`** 供跨模块引用；公共字段（名称/状态）只存一份；子表只放类型专属字段，不稀疏。                        |
| 自然人 vs 组织二分（而非「人员/企业」二分） | 企业只是组织的一种；政府机构/事业单位/社会组织等既非自然人也非企业，统一归「组织」，由 `org_type` 区分，**扩展新机构类型无需加表加页**。 |
| 共享主键（子表 id = 主表 id）               | 1:1 关系最简实现；其他模块引用 `party_id` 即可直达主表与子表。                                                                           |

### 共享主键的实现要点

子表实体（`PartyPerson`/`PartyOrganization`）**不重声明 `id`**，沿用 `BaseEntity` 的 `@TableId(ASSIGN_ID)`。创建流程：先 `insert` 主表 `Party` 拿到雪花 id → 子表 `setId(party.getId())` 后 `insert`。`ASSIGN_ID` 在 id 已赋值时沿用、不再生成。删除时主表、子表各 `deleteById`（软删除）。

---

## 3. 实体关系（ER）

```
            ┌──────────────────────────┐
            │           party          │  (tenant_id 隔离)
            │  id (PK, 雪花)           │
            │  party_type, name,       │
            │  status, remark          │
            └────────────┬─────────────┘
                         │ 1:1 共享主键
          ┌──────────────┴───────────────┐
          ▼                               ▼
┌───────────────────┐         ┌────────────────────────────┐
│   party_person    │         │     party_organization     │
│  id (PK=party.id) │         │  id (PK=party.id)          │
│  gender, id_card, │         │  org_type, tax_no,         │
│  contact          │         │  registered_capital,       │
└───────────────────┘         │  established_date,         │
                              │  legal_person, reg_address,│
                              │  business_scope            │
                              └────────────────────────────┘
```

三表均带 `tenant_id`，由多租户插件自动隔离（不入 `IGNORE_TABLES`）。

---

## 4. 字段与约束

### party（主表）

| 字段         | 含义 / 约束                             |
| ------------ | --------------------------------------- |
| `party_type` | `PERSON` 人员 / `ORGANIZATION` 组织     |
| `name`       | 统一显示名（人员姓名 / 单位名称），非空 |
| `status`     | 1 启用 / 0 停用                         |
| `remark`     | 备注                                    |

### party_person（人员）

| 字段      | 含义 / 约束                                                                                                         |
| --------- | ------------------------------------------------------------------------------------------------------------------- |
| `gender`  | 0 未知 / 1 男 / 2 女                                                                                                |
| `id_card` | 身份证号；**可空，填了才租户内唯一**（偏过滤唯一索引 `WHERE deleted=0 AND id_card IS NOT NULL`）；DTO 校验 15/18 位 |
| `contact` | 联系方式                                                                                                            |

### party_organization（组织/单位）

| 字段                 | 含义 / 约束                                                                  |
| -------------------- | ---------------------------------------------------------------------------- |
| `org_type`           | 组织类型（企业/政府机构/事业单位/社会组织/其他），自由文本，前端按历史值补全 |
| `tax_no`             | 统一社会信用代码；**可空，填了才租户内唯一**（同上偏过滤索引）               |
| `registered_capital` | 注册资本，自由文本（如「500 万元人民币」），仅企业适用                       |
| `established_date`   | 成立日期（`DATE`）                                                           |
| `legal_person`       | 法定代表人 / 负责人                                                          |
| `reg_address`        | 住所（注册地址；避开 `address` 命名，语义更准）                              |
| `business_scope`     | 经营范围（`TEXT`），仅企业适用                                               |

> 注册资本/经营范围对非企业组织留空即可，不做强校验。

---

## 5. 接口与权限码

URL 前缀 `/api/v1/party/*`；列表按资源路径分接口、**后端固定 `party_type`**（人员接口固定 PERSON，单位接口固定 ORGANIZATION），前端零参数心智。

| 方法   | 路径                         | 权限码                      | 说明                                        |
| ------ | ---------------------------- | --------------------------- | ------------------------------------------- |
| GET    | `/party/persons`             | `party:person:list`         | 人员分页（`page/size/keyword`，按名称模糊） |
| POST   | `/party/persons`             | `party:person:create`       | 新建人员                                    |
| PUT    | `/party/persons/{id}`        | `party:person:update`       | 编辑人员                                    |
| DELETE | `/party/persons/{id}`        | `party:person:delete`       | 删除人员                                    |
| GET    | `/party/organizations`       | `party:organization:list`   | 单位分页                                    |
| GET    | `/party/organizations/types` | `party:organization:list`   | 已入库组织类型（去重），供输入补全          |
| POST   | `/party/organizations`       | `party:organization:create` | 新建单位                                    |
| PUT    | `/party/organizations/{id}`  | `party:organization:update` | 编辑单位                                    |
| DELETE | `/party/organizations/{id}`  | `party:organization:delete` | 删除单位                                    |

列表查询策略：以主表 `party`（固定类型 + 名称模糊 + 创建时间倒序）分页，再 `selectByIds` 批量补子表详情合并为 VO（2 次查询，无 N+1，无自定义 SQL）。

---

## 6. 菜单与套餐

菜单种子在 `V9__party_module.sql` 注册（id 块 200，避开系统菜单 1xx）：

```
相关方管理 (M /party)                                     id 200
├─ 人员管理 (C /party/persons → party/PersonList)         id 201  party:person:list  (+F 2011/2012/2013)
└─ 单位管理 (C /party/organizations → party/OrganizationList) id 202  party:organization:list (+F 2021/2022/2023)
```

纳入「全功能（FULL，id=2）」套餐；「基础版（BASIC）」不含 party，以体现套餐裁剪。
**无独立「相关方」页面**：相关方是数据层统一概念，UI 上「人员 ∪ 单位」已无遗漏覆盖；将来跨模块「选相关方」用 party 选择器组件解决，而非浏览页。

---

## 7. 前端三件套

| 件   | 位置                                                                              |
| ---- | --------------------------------------------------------------------------------- |
| api  | `frontend/src/api/party.ts`                                                       |
| 视图 | `frontend/src/views/party/{PersonList,OrganizationList}.vue`                      |
| 菜单 | 由后端 `sys_menu` 种子动态下发，无需前端登记（动态路由按 `component` 字符串加载） |

要点：组织类型用 `el-autocomplete` 拉 `/party/organizations/types` 历史值补全（聚焦即展示、可自由输入）；成立日期 `el-date-picker`（`format`/`value-format` = `YYYY-MM-DD`）；按钮级权限 `v-permission`；时间显示 `formatDateTime()`。

---

## 8. 文件清单

| 层         | 文件                                                           |
| ---------- | -------------------------------------------------------------- |
| 迁移       | `backend/src/main/resources/db/migration/V9__party_module.sql` |
| 实体       | `entity/{Party,PartyPerson,PartyOrganization}.java`            |
| Mapper     | `mapper/{Party,PartyPerson,PartyOrganization}Mapper.java`      |
| DTO        | `dto/{Person,Organization}DTO.java`                            |
| VO         | `vo/{Person,Organization}VO.java`                              |
| Service    | `service/{Person,Organization}Service.java`                    |
| Controller | `controller/{Person,Organization}Controller.java`              |
| 前端 api   | `frontend/src/api/party.ts`                                    |
| 前端视图   | `frontend/src/views/party/{PersonList,OrganizationList}.vue`   |

---

## 9. 域扩展设计：账户与资质（规划，尚未落地）

> 本节为 party 域的**下一步扩展设计**（迁移目标系统的「企业/人员 开户信息、资质信息」四个页面），随设计推进更新；编码落地后把「规划」字样去掉并补「文件清单」。
> 来源背景：目标系统按「企业信息管理 / 人员信息管理」两组菜单各含 `基本信息 / 开户(账户)信息 / 资质信息` 三页。映射到本模块时，**基本信息已由 `party_organization` / `party_person` 承载**，本节只补「账户」「资质」两类**从属数据**。

### 9.1 设计原则：账户/资质是挂在相关方身份上的从属数据

| 决策                                                                          | 理由                                                                                                                                                                           |
| ----------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| 账户/资质归 **party 模块内**（非独立模块）                                    | 它们与相关方主数据同生命周期、随主体增删，作为 party 的子资源内聚最高（已与你确认）。                                                                                          |
| 「企业开户」与「人员账户」**合一张表** `party_bank_account`                   | 二者结构同构（都是银行账户）；相关方是组织还是人由父表 `party.party_type` 已区分，**不为 UI 的两个页面建两张同构表**。                                                         |
| 「企业资质」与「人员资质」**合一张表** `party_qualification`                  | 同理，都是「证照/证书 + 扫描件」；类型差异同样靠父表 `party_type` 区分。                                                                                                       |
| 资质用 **类型 / 名称 / 等级** 三维刻画，**单表一行一项**                      | 既能记下等级（驾驶证 C1）与下分领域（特种作业的高压/低压电工作业），又不引入证书主表的复杂度；代价是「一证多项」时证书编号/发证机关按行重复一份，对当前规模可接受。详见 §9.3。 |
| 账户/资质对相关方 **1:N**（一个相关方多个账户/多张资质项）                    | 账户：同一主体多个银行账号并存；资质：一人可持多证、一证可拆多项（各占一行）。                                                                                                 |
| 资质扫描件**复用 `sys_file`**，不另造文件表                                   | 沿用基座文件能力（计入存储配额、SPI 翻译 bizType）；资质行存 `file_id` 引用 `sys_file.id`，**应用层维护关系、不加外键**（与 `sys_user.avatar_file_id` 一致范式）。             |
| 资质附件上传/下载**走资质自身接口**，不挂 `system:file:*` 权限                | 仿 `system` 头像范式：鉴权注解挂在 `FileController`（文件管理页）上，`FileService` 本身不鉴权；资质 Controller 直接调 `FileService`，凭**资质自身权限**（`party:qualification:*`）即可传/取附件，与文件管理页权限解耦。详见 §9.3。 |
| 相关方删除时，账户/资质（及其 `sys_file`）**级联软删**（编排式）             | 在 party 主表 service 的 `delete()` 内**显式**调各从属 service 的 `deleteByParty(partyId)`，与现有「子表 + 主表」级联同构；资质附件按行调 `fileService.softDelete(fileId)`。详见 §9.3.1。 |
| 通过 **人员/单位列表页的「账户」「资质」入口（抽屉）** 管理，不新增独立菜单页 | 延续 party「无冗余浏览页」哲学（见 §6）：账户/资质总是在某个相关方上下文里维护，抽屉比独立页更贴语义；仅新增按钮级 perm，菜单树不膨胀。                                        |

### 9.2 相关方账户 `party_bank_account`（1:N，挂 `party_id`）

承载「企业开户信息 / 人员账户信息」。

| 字段           | 含义 / 约束                                                              |
| -------------- | ------------------------------------------------------------------------ |
| `id`           | 雪花主键                                                                 |
| `party_id`     | 所属相关方（= `party.id`；组织或人员皆可）                               |
| `account_name` | 户名（开户名称；可与相关方名不同，如个人卡持卡人）                       |
| `account_no`   | 银行账号；**租户内同一相关方下唯一**（偏过滤唯一索引 `WHERE deleted=0`） |
| `bank_name`    | 开户银行（如「中国工商银行」）自由文本，前端按历史值补全                 |
| `bank_branch`  | 开户支行/网点（如「中国工商银行XX 路支行」）                             |
| `status`       | 1 启用 / 0 停用                                                          |
| `remark`       | 备注                                                                     |

索引：`idx_pba_tenant_party (tenant_id, party_id)`；唯一 `uk_pba_account_no (tenant_id, party_id, account_no) WHERE deleted=0`。表带 `tenant_id`，走多租户插件（不入 `IGNORE_TABLES`）。

### 9.3 相关方资质 `party_qualification`（1:N，挂 `party_id`，扫描件复用 `sys_file`）

承载「企业资质信息 / 人员资质信息」（营业执照、各类许可证、职业资格证书、特种作业证、驾驶证等）。**一行 = 一项资质**。

**用「类型 / 名称 / 等级」三维刻画资质项**：

| 维度     | 字段         | 含义                                    | 三类样例取值                                             |
| -------- | ------------ | --------------------------------------- | -------------------------------------------------------- |
| 资质类型 | `qual_type`  | 证照大类，用于归类与筛选                | 特种作业操作证 / 驾驶证 / 建筑业企业资质证书             |
| 资质名称 | `qual_name`  | 具体资质项 / 操作领域（即「下分领域」） | 高压电工作业、低压电工作业 / 驾驶证 / 建筑业企业资质证书 |
| 资质等级 | `qual_level` | 级别（可空，即原先缺失的「等级」）      | （空） / C1 / 输变电工程专业承包贰级                     |

完整字段：

| 字段                | 含义 / 约束                                                                           |
| ------------------- | ------------------------------------------------------------------------------------- |
| `id`                | 雪花主键                                                                              |
| `party_id`          | 所属相关方（= `party.id`）                                                            |
| `qual_type`         | 资质类型（特种作业操作证 / 驾驶证 / 建筑业企业资质证书…），自由文本，前端按历史值补全 |
| `qual_name`         | 资质名称 / 操作领域（高压电工作业、输变电工程专业承包…），非空                        |
| `qual_level`        | 资质等级（C1、壹级/贰级…），**可空**（如特种作业操作项目无等级）                      |
| `qual_no`           | 证书/证照编号                                                                         |
| `issuing_authority` | 发证机关                                                                              |
| `issue_date`        | 发证日期（`DATE`）                                                                    |
| `expiry_date`       | 有效期至（`DATE`，**可空 = 长期有效**）                                               |
| `file_id`           | 证书扫描件，引用 `sys_file.id`（**可空 = 未上传**；应用层维护，无外键）               |
| `status`            | 1 有效 / 0 失效（也可由 `expiry_date` 派生展示「已过期」）                            |
| `remark`            | 备注                                                                                  |

索引：`idx_pq_tenant_party (tenant_id, party_id)`；可加 `idx_pq_type (tenant_id, qual_type)` 支持按类型筛选。表带 `tenant_id`，走多租户插件。

**文件接入（SPI，基座零改动）**：在 `biz/party/spi/PartyFileBizTypeProvider` 追加两条 bizType（与现有 `party:person:idcard` 并列）：

```java
new FileBizTypeDef("party:organization:qualification", "单位资质证书"),
new FileBizTypeDef("party:person:qualification",       "人员资质证书")
```

**上传/下载走资质自身接口，不挂 `system:file:*`**（仿 `system` 头像范式）。关键在于基座把鉴权放在 Controller 层、`FileService` 本身不鉴权：

- 头像参照：`PUT /api/v1/system/users/me/avatar`、`GET /api/v1/system/users/{id}/avatar`（见 `UserController`）**均未挂 `@SaCheckPermission`**，只靠登录态放行；`UserService` 内部直接调 `FileService.storeFile(file, bizType)` / `loadInline(id)` / `softDelete(id)`。`system:file:upload/download` 这组权限只挂在文件管理页的 `FileController` 上——**不经过它就不受其约束**。
- 资质照搬：资质附件的上传/下载端点（见 §9.4）用**资质自身权限**（如 `party:qualification:update` / `party:qualification:list`），Controller 内部调 `FileService`：
  - 上传：`fileService.storeFile(file, bizType).getId()` → 写入资质行 `file_id`（`bizType` 按相关方类型选 `party:organization:qualification` 或 `party:person:qualification`）。
  - 下载/预览：`fileService.loadInline(qual.getFileId())`。
  - 换证/删除：旧 `file_id` 调 `fileService.softDelete(oldId)`（同头像换图）。

> 因此「有 `party:qualification:*` 却没 `system:file:*` 就传不了附件」的问题**不存在**：资质流程根本不校验 `system:file:*`。代价是资质附件不出现在「文件管理」页列表里（该页按 `system:file:list` 独立鉴权），但可在资质抽屉内查看——与头像不进文件管理页列表一致。

**模块边界**：party 取附件一律经基座 `FileService`（`storeFile`/`loadInline`/`softDelete`），**不注入 `SysFileMapper`**（遵接入规范「不跨模块直连他人 mapper」）。资质 VO 的文件名/预览链接同样由 `FileService` 提供的元数据填充。

### 9.3.1 级联软删（编排式）

相关方被删除时，其账户、资质及资质关联的 `sys_file` **一并软删**，采用**编排式**：在 party 主表 service 的 `delete()` 内**显式**调用各从属 service，与现有 `PersonService.delete()`/`OrganizationService.delete()` 里「先删子表再删主表」的写法同构，不引入事件等新机制。

```
PersonService.delete(id) / OrganizationService.delete(id):
  accountService.deleteByParty(id)         // 软删该相关方全部账户
  qualificationService.deleteByParty(id)   // 软删全部资质，并对每行 file_id 调 fileService.softDelete
  personMapper/organizationMapper.deleteById(id)   // 既有：子表软删
  partyMapper.deleteById(id)                       // 既有：主表软删
```

- `QualificationService.deleteByParty(partyId)`：查该 party 全部资质行，逐行 `fileService.softDelete(fileId)`（`file_id` 非空时）后软删资质行。
- `AccountService.deleteByParty(partyId)`：软删该 party 全部账户行（无附件，无需动 `sys_file`）。
- 三步同处一个 `@Transactional` 方法内，任一步异常整体回滚。
- 账户/资质均在 party 模块内，编排式只改 party 自身 service，不破坏模块边界；将来如新增其他从属表，在 `delete()` 内续加一行 `xxxService.deleteByParty(id)` 即可。

### 9.4 接口与权限码（账户/资质各一组 CRUD，子资源走 party 层级）

URL 前缀仍为 `/api/v1/party/*`；账户/资质以**相关方为上下文**的子资源建模。

| 方法   | 路径                              | 权限码                       | 说明                        |
| ------ | --------------------------------- | ---------------------------- | --------------------------- |
| GET    | `/party/{partyId}/accounts`       | `party:account:list`         | 某相关方账户列表            |
| POST   | `/party/{partyId}/accounts`       | `party:account:create`       | 新增账户                    |
| PUT    | `/party/accounts/{id}`            | `party:account:update`       | 编辑账户                    |
| DELETE | `/party/accounts/{id}`            | `party:account:delete`       | 删除账户                    |
| GET    | `/party/{partyId}/qualifications` | `party:qualification:list`   | 某相关方资质列表            |
| POST   | `/party/{partyId}/qualifications` | `party:qualification:create` | 新增资质（含扫描件 fileId） |
| PUT    | `/party/qualifications/{id}`      | `party:qualification:update` | 编辑资质                    |
| DELETE | `/party/qualifications/{id}`      | `party:qualification:delete` | 删除资质                    |
| POST   | `/party/qualifications/{id}/file` | `party:qualification:update` | 上传/替换资质附件（内部调 `FileService.storeFile`，旧附件软删；**不挂 `system:file:*`**） |
| GET    | `/party/qualifications/{id}/file` | `party:qualification:list`   | 下载/预览资质附件（内部调 `FileService.loadInline`；**不挂 `system:file:*`**） |

> 账户/资质**不按企业/人员拆两套接口**：路径以 `partyId` 定位，相关方是组织还是人由父表决定，前端人员页/单位页复用同一组接口，只是上下文的 `partyId` 类型不同。权限码也共用一组（`party:account:*` / `party:qualification:*`）——若将来需按企业/人员独立裁剪套餐，再拆细。
>
> 资质附件的上传/下载**复用资质自身权限**（写操作归 `:update`、读操作归 `:list`），**不引入 `system:file:*`**——仿头像范式，Controller 直接调不鉴权的 `FileService`（见 §9.3）。附件也可随新增资质一并提交（`POST /party/{partyId}/qualifications` 带文件），或建好资质后再单独传。

### 9.5 菜单与套餐增量

延续 party 菜单 id 块 200，在「人员管理(201)」「单位管理(202)」两页下各挂账户/资质的按钮级 perm（抽屉入口，不新增 C 页面）：

```
人员管理 (201)
├─ party:account:list/create/update/delete        （人员账户抽屉）
└─ party:qualification:list/create/update/delete   （人员资质抽屉）
单位管理 (202)
├─ party:account:*        （单位开户抽屉，复用同组 perm）
└─ party:qualification:*  （单位资质抽屉，复用同组 perm）
```

- perm 共用：账户/资质各 4 个按钮 perm（F 类菜单），id 取 200 块内空段（如 2014~2017 账户、2018~201B 资质，或另起 203x 子块，落地时定）。
- 套餐：与现有 party 菜单一并纳入「全功能（FULL，id=2）」套餐（`sys_package_menu`，沿用 `1000+menu.id` 规约）；「基础版」仍不含。
- 若产品上确需把「开户信息/资质信息」做成**独立菜单页**（贴合目标系统的菜单形态），则改为新增 C 菜单（component 复用同一 `AccountList.vue`/`QualificationList.vue`，路由参数带 `partyType` 区分人员/单位场景）——作为备选，默认走抽屉。

### 9.6 落地清单（待新增文件，照样板对位仿写）

| 层         | 文件                                                                                                                       |
| ---------- | -------------------------------------------------------------------------------------------------------------------------- |
| 迁移       | `db/migration/V12__party_account_qualification.sql`（建两表 + 菜单/套餐种子；版本号在现有 V11 上递增）                     |
| 实体       | `entity/{PartyBankAccount,PartyQualification}.java`（继承 `BaseEntity`）                                                   |
| Mapper     | `mapper/{PartyBankAccount,PartyQualification}Mapper.java`                                                                  |
| DTO        | `dto/{Account,Qualification}DTO.java`                                                                                      |
| VO         | `vo/{Account,Qualification}VO.java`（资质 VO 内联文件名/下载链接，由 `file_id` 关联 `sys_file`）                           |
| Service    | `service/{Account,Qualification}Service.java`                                                                              |
| Controller | `controller/{Account,Qualification}Controller.java`                                                                        |
| SPI        | `spi/PartyFileBizTypeProvider.java`（追加两条资质 bizType）                                                                |
| 前端 api   | `frontend/src/api/party.ts`（追加 account/qualification 函数）                                                             |
| 前端视图   | `frontend/src/views/party/{PersonList,OrganizationList}.vue` 内嵌账户/资质抽屉（或独立 `AccountList`/`QualificationList`） |

---

## 10. 待定 / 后续可扩展

- **相关方选择器组件**：将来业务模块需要「引用一个相关方」时，提供跨类型的 party 选择弹窗（按名称同时搜人员+单位），而非独立浏览页。
- **组织类型字典化**：当前 `org_type` 为自由文本 + 历史值补全；若需固定可选项与统计，可升级为字典维护。
- **更多相关方属性**：如人员的邮箱/证件类型、组织的行业分类等，按业务需要在子表增列（Flyway 同序列递增）。
- **资质到期提醒**：`party_qualification.expiry_date` 已预留；将来可加定时任务/首页待办，对临期/过期资质提醒（届时可考虑是否独立成 `credential` 模块）。
