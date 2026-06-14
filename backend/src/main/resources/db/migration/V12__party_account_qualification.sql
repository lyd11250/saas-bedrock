-- =====================================================================
-- V12: party 模块扩展 —— 相关方账户 party_bank_account + 资质 party_qualification
-- 模型：二者均为 party 的从属数据，挂 party_id（1:N），随相关方主体增删（编排式级联软删）。
--   账户：企业开户信息 ∪ 人员账户信息合一表，组织/人员由父表 party.party_type 区分。
--   资质：企业资质 ∪ 人员资质合一表，「类型/名称/等级」三维刻画，一行一项；
--         扫描件复用 sys_file（file_id 引用，应用层维护、不加外键），不另造文件表。
-- 隔离：两表均带 tenant_id，走多租户插件自动隔离（不入 IGNORE_TABLES）。
-- 菜单：账户/资质各 4 个按钮级 perm（type=F），挂在「相关方管理」目录(200)下，
--       经人员/单位列表页的抽屉管理，不新增导航 C 页。纳入「全功能(FULL,id=2)」套餐。
-- =====================================================================

-- ---------------------------------------------------------------------
-- 相关方账户（1:N，挂 party_id；企业开户信息 / 人员账户信息）
-- ---------------------------------------------------------------------
CREATE TABLE party_bank_account (
    id           BIGINT       PRIMARY KEY,
    tenant_id    BIGINT       NOT NULL,
    party_id     BIGINT       NOT NULL,            -- 所属相关方（= party.id，组织或人员）
    account_name VARCHAR(128) NOT NULL,            -- 户名（开户名称）
    account_no   VARCHAR(64)  NOT NULL,            -- 银行账号
    bank_name    VARCHAR(128),                     -- 开户银行（前端按历史值补全）
    bank_branch  VARCHAR(128),                     -- 开户支行/网点
    status       SMALLINT     NOT NULL DEFAULT 1,  -- 1 启用 0 停用
    remark       VARCHAR(255),
    created_by   BIGINT,
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by   BIGINT,
    updated_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted      SMALLINT     NOT NULL DEFAULT 0
);
CREATE INDEX idx_pba_tenant_party ON party_bank_account (tenant_id, party_id);
CREATE UNIQUE INDEX uk_pba_account_no ON party_bank_account (tenant_id, party_id, account_no)
    WHERE deleted = 0;
COMMENT ON TABLE  party_bank_account IS '相关方账户（party 从属，挂 party_id；企业开户/人员账户合一表）';
COMMENT ON COLUMN party_bank_account.party_id IS '所属相关方 id（= party.id）';
COMMENT ON COLUMN party_bank_account.account_no IS '银行账号（租户内同一相关方下唯一）';

-- ---------------------------------------------------------------------
-- 相关方资质（1:N，挂 party_id；一行一项，扫描件复用 sys_file）
-- ---------------------------------------------------------------------
CREATE TABLE party_qualification (
    id                BIGINT       PRIMARY KEY,
    tenant_id         BIGINT       NOT NULL,
    party_id          BIGINT       NOT NULL,            -- 所属相关方（= party.id）
    qual_type         VARCHAR(64)  NOT NULL,            -- 资质类型（特种作业操作证/驾驶证/建筑业企业资质证书…）
    qual_name         VARCHAR(128) NOT NULL,            -- 资质名称/操作领域（高压电工作业/输变电工程专业承包…）
    qual_level        VARCHAR(64),                      -- 资质等级（C1、壹级/贰级…，可空）
    qual_no           VARCHAR(64),                      -- 证书/证照编号
    issuing_authority VARCHAR(128),                     -- 发证机关
    issue_date        DATE,                             -- 发证日期
    expiry_date       DATE,                             -- 有效期至（可空 = 长期有效）
    file_id           BIGINT,                           -- 证书扫描件（引用 sys_file.id，可空 = 未上传）
    status            SMALLINT     NOT NULL DEFAULT 1,  -- 1 有效 0 失效
    remark            VARCHAR(255),
    created_by        BIGINT,
    created_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by        BIGINT,
    updated_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted           SMALLINT     NOT NULL DEFAULT 0
);
CREATE INDEX idx_pq_tenant_party ON party_qualification (tenant_id, party_id);
CREATE INDEX idx_pq_type         ON party_qualification (tenant_id, qual_type);
COMMENT ON TABLE  party_qualification IS '相关方资质（party 从属，挂 party_id；企业/人员资质合一表，一行一项）';
COMMENT ON COLUMN party_qualification.party_id IS '所属相关方 id（= party.id）';
COMMENT ON COLUMN party_qualification.qual_type IS '资质类型（证照大类，用于归类筛选）';
COMMENT ON COLUMN party_qualification.qual_name IS '资质名称/操作领域（具体资质项，即下分领域）';
COMMENT ON COLUMN party_qualification.qual_level IS '资质等级（可空，如特种作业操作项目无等级）';
COMMENT ON COLUMN party_qualification.file_id IS '证书扫描件 id（引用 sys_file；空=未上传）';

-- =====================================================================
-- 种子：账户/资质按钮级 perm（type=F，挂「相关方管理」目录 200 下）
-- 200 块已占：200,201,2011-2013,202,2021-2023；此处用 203x/204x 子块。
-- 账户为敏感信息，party:account:list 独立于 party:person:list，可单独裁剪。
-- =====================================================================
INSERT INTO sys_menu (id, parent_id, type, name, path, component, icon, perm, sort, visible) VALUES
  (2031, 200, 'F', '查看账户', NULL, NULL, NULL, 'party:account:list',         31, 1),
  (2032, 200, 'F', '新增账户', NULL, NULL, NULL, 'party:account:create',       32, 1),
  (2033, 200, 'F', '编辑账户', NULL, NULL, NULL, 'party:account:update',       33, 1),
  (2034, 200, 'F', '删除账户', NULL, NULL, NULL, 'party:account:delete',       34, 1),
  (2041, 200, 'F', '查看资质', NULL, NULL, NULL, 'party:qualification:list',   41, 1),
  (2042, 200, 'F', '新增资质', NULL, NULL, NULL, 'party:qualification:create', 42, 1),
  (2043, 200, 'F', '编辑资质', NULL, NULL, NULL, 'party:qualification:update', 43, 1),
  (2044, 200, 'F', '删除资质', NULL, NULL, NULL, 'party:qualification:delete', 44, 1);

-- 纳入「全功能」套餐（id=2）；package_menu.id 沿用 1000+menu.id 规约
INSERT INTO sys_package_menu (id, package_id, menu_id)
SELECT 1000 + m.id, 2, m.id FROM sys_menu m
WHERE m.id IN (2031,2032,2033,2034, 2041,2042,2043,2044);
