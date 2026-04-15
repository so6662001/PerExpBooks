# 钱酷报销 - 测试指南

## 1. 测试策略

### 1.1 测试分层

| 层级 | 类型 | 覆盖范围 | 工具 |
|------|------|----------|------|
| L1 | 单元测试 | Service 层业务逻辑 | JUnit 5 + Mockito |
| L2 | 集成测试 | Controller → Service → Mapper 链路 | SpringBootTest + H2 |
| L3 | 前端测试 | 组件单元测试 | Vitest + Vue Test Utils |
| L4 | E2E 测试 | 关键业务流程端到端 | Playwright |

### 1.2 单元测试（L1）

- **目标**：验证 Service 层每个方法的业务逻辑正确性
- **策略**：使用 Mockito mock 所有外部依赖（Mapper、Redis、RabbitMQ 等）
- **覆盖率要求**：核心 Service ≥ 80%
- **运行方式**：`cd backend && mvn test`

### 1.3 集成测试（L2）

- **目标**：验证 Controller → Service → Mapper 完整链路
- **策略**：使用 `@SpringBootTest` + H2 内存数据库
- **数据管理**：`@Transactional + @Rollback` 自动回滚
- **运行方式**：`cd backend && mvn verify`

### 1.4 前端测试（L3）

- **目标**：验证 Vue 组件渲染和交互逻辑
- **策略**：使用 Vitest + `@vue/test-utils` 挂载组件
- **覆盖范围**：核心页面组件、表单校验、状态管理
- **运行方式**：`cd frontend && pnpm test`

### 1.5 E2E 测试（L4）

- **目标**：验证关键业务流程的端到端正确性
- **策略**：使用 Playwright 模拟用户操作
- **关键流程**：
  - 注册 → 登录 → 上传发票 → 创建费用 → 生成报销单 → 下载PDF
  - 注册 → 开通会员 → 使用会员功能
  - 邀请注册 → 好友付费 → 返佣到账
- **运行方式**：`cd frontend && pnpm test:e2e`

---

## 2. 测试用例矩阵

### 2.1 用户模块（TC-USER）

| 用例ID | 描述 | 前置条件 | 操作步骤 | 预期结果 | 优先级 |
|--------|------|----------|----------|----------|--------|
| TC-USER-001 | 手机号+验证码注册新用户 | 手机号未注册，Redis中有对应验证码 | 1. 调用 sendSmsCode 获取验证码<br>2. 调用 smsLogin 传入手机号+验证码 | 1. 创建新用户记录<br>2. 自动生成邀请码<br>3. 设置30天试用期<br>4. 返回JWT token<br>5. isNew=true | P0 |
| TC-USER-002 | 已注册用户手机号登录 | 手机号已注册，Redis中有对应验证码 | 1. 调用 smsLogin 传入已注册手机号+正确验证码 | 1. 不创建新用户<br>2. 返回JWT token<br>3. isNew=false | P0 |
| TC-USER-003 | 验证码过期后登录失败 | Redis中验证码已过期（5分钟TTL） | 1. 调用 smsLogin 传入手机号+过期验证码 | 抛出 BizException，提示"验证码错误或已过期" | P0 |
| TC-USER-004 | 验证码错误登录失败 | Redis中有验证码，但用户传入错误验证码 | 1. 调用 smsLogin 传入错误验证码 | 抛出 BizException，提示"验证码错误或已过期" | P0 |
| TC-USER-005 | 携带邀请码注册（试用延长至45天） | 邀请码对应的邀请人存在 | 1. 调用 smsLogin 传入手机号+验证码+邀请码 | 1. 新用户 trialEndTime = now + 45天<br>2. 设置 inviterId | P1 |
| TC-USER-006 | 注册时自动签署协议 | 系统有生效的用户协议和隐私政策 | 1. 新用户注册 | 1. 自动创建 UserAgreementSign 记录<br>2. 用户的 agreementVersionId 不为空 | P1 |
| TC-USER-007 | 注册时自动生成邀请码 | 无 | 1. 新用户注册 | 用户 inviteCode 为6位字母数字组合，不重复 | P1 |
| TC-USER-008 | 注册时自动发放新人券 | 无 | 1. 新用户注册 | 调用 couponService.issueNewUserCoupon | P2 |
| TC-USER-009 | 获取用户资料 | 用户已注册 | 1. 调用 getProfile(userId) | 返回 UserVO 包含所有字段 | P1 |
| TC-USER-010 | 更新用户资料 | 用户已注册 | 1. 调用 updateProfile 传入新昵称/公司等 | 1. 对应字段更新成功<br>2. updatedAt 更新 | P1 |
| TC-USER-011 | 账号注销 | 用户已注册 | 1. 调用 deleteAccount(userId) | 1. status=1<br>2. phone/openid/unionid 清空<br>3. Redis 缓存清除 | P1 |

### 2.2 费用模块（TC-EXP）

| 用例ID | 描述 | 前置条件 | 操作步骤 | 预期结果 | 优先级 |
|--------|------|----------|----------|----------|--------|
| TC-EXP-001 | 上传PDF发票并解析成功 | 用户已登录，有上传额度 | 1. 上传有效PDF发票文件 | 1. 文件上传到OSS<br>2. 解析出发票号码/金额等字段<br>3. 返回 InvoiceUploadVO | P0 |
| TC-EXP-002 | 上传非支持格式文件被拒绝 | 用户已登录 | 1. 上传 .doc 文件 | 抛出 BizException，提示"不支持的文件格式" | P0 |
| TC-EXP-003 | 上传超过10MB文件被拒绝 | 用户已登录 | 1. 上传大于10MB的文件 | 抛出 BizException，提示"文件大小不能超过10MB" | P1 |
| TC-EXP-004 | 免费用户上传第6张发票被拒绝 | 免费用户，monthlyInvoiceUsed=5 | 1. 上传发票 | 抛出 BizException(QUOTA_EXCEEDED)，提示额度已用完 | P0 |
| TC-EXP-005 | 手动创建费用记录 | 用户已登录 | 1. 调用 createExpense 传入有效数据 | 1. 创建 Expense 记录<br>2. reimburseStatus=0<br>3. 生成 dataSign | P0 |
| TC-EXP-006 | 金额为0或负数被拒绝 | 用户已登录 | 1. 创建费用时金额≤0 | DTO校验拒绝（@DecimalMin("0.01")） | P1 |
| TC-EXP-007 | 更新费用记录 | 费用记录存在，reimburseStatus=0 | 1. 调用 updateExpense 更新金额等字段 | 1. 字段更新成功<br>2. dataSign 重新生成 | P1 |
| TC-EXP-008 | 已报销的费用不可编辑 | 费用 reimburseStatus>0 | 1. 调用 updateExpense | 抛出 BizException，提示"已提交报销的费用不允许编辑" | P0 |
| TC-EXP-009 | 已报销的费用不可删除 | 费用 reimburseStatus>0 | 1. 调用 deleteExpense | 抛出 BizException，提示"已提交报销的费用不允许删除" | P0 |
| TC-EXP-010 | 费用列表分页查询 | 用户有多条费用记录 | 1. 调用 listExpenses 传入分页参数 | 返回分页结果 IPage<ExpenseVO> | P1 |
| TC-EXP-011 | 待报销费用列表 | 用户有待报销费用 | 1. 调用 listPending(userId) | 返回 reimburseStatus=0 的费用列表 | P1 |
| TC-EXP-012 | 创建出差自动生成补贴费用 | 用户已登录 | 1. 调用 createTrip 设定出差日期和每日补贴 | 1. 创建 BusinessTrip<br>2. 自动创建 type=2 的补贴 Expense<br>3. 补贴金额 = 天数 × 每日补贴 | P0 |
| TC-EXP-013 | 修改出差同步更新补贴费用 | 出差记录存在 | 1. 调用 updateTrip 更改日期/补贴 | 关联的补贴 Expense 金额和描述同步更新 | P1 |
| TC-EXP-014 | 删除出差同步删除补贴费用 | 出差记录存在 | 1. 调用 deleteTrip | 1. 出差 status=1<br>2. 关联补贴 Expense status=1 | P1 |
| TC-EXP-015 | 出差结束日期不能早于开始日期 | 无 | 1. 创建出差 endDate < startDate | 抛出 BizException，提示"出差结束日期不能早于开始日期" | P1 |

### 2.3 报销模块（TC-REM）

| 用例ID | 描述 | 前置条件 | 操作步骤 | 预期结果 | 优先级 |
|--------|------|----------|----------|----------|--------|
| TC-REM-001 | 选择费用项生成报销单 | 有待报销费用（reimburseStatus=0） | 1. 调用 generate 传入费用ID列表 | 1. 创建 Reimbursement<br>2. 费用 reimburseStatus→1<br>3. 生成报销单号<br>4. 生成PDF | P0 |
| TC-REM-002 | 选择已报销的费用项被拒绝 | 费用 reimburseStatus≠0 | 1. 调用 generate 传入已报销费用ID | 抛出 BizException，提示"费用已关联其他报销单" | P0 |
| TC-REM-003 | 选择其他用户的费用项被拒绝 | 费用属于其他用户 | 1. 调用 generate 传入他人费用ID | 抛出 BizException，提示"部分费用记录不存在或不属于当前用户" | P0 |
| TC-REM-004 | 免费用户每月第3张报销单被拒绝 | 免费用户，monthlyReimburseUsed=2 | 1. 调用 generate | 抛出 BizException(QUOTA_EXCEEDED) | P0 |
| TC-REM-005 | 确认收款同步更新费用状态 | 报销单存在 | 1. 调用 confirmReceived | 1. 报销单 reimburseStatus→2<br>2. 关联费用 reimburseStatus→2 | P1 |
| TC-REM-006 | 取消报销单恢复费用状态 | 报销单 reimburseStatus≠2 | 1. 调用 cancelReimbursement | 1. 报销单 status=1<br>2. 费用 reimburseStatus→0，reimbursementId→null | P1 |
| TC-REM-007 | 编辑报销单重新生成PDF | 报销单存在 | 1. 调用 update 修改标题/费用 | 1. 重新生成PDF<br>2. 更新 pdfUrl/mergedPdfUrl/zipUrl | P2 |
| TC-REM-008 | 免费用户PDF有水印 | 免费用户 | 1. 导出PDF | 封面PDF包含水印标记 | P2 |
| TC-REM-009 | 免费用户不能下载ZIP | 免费用户(memberStatus=0) | 1. 调用 getZipUrl | 抛出 BizException(MEMBER_REQUIRED) | P1 |
| TC-REM-010 | 报销单号不重复 | 无 | 1. 多次调用 generateReimburseNo | 每次生成的报销单号不同，格式 RByyyyMMdd#### | P1 |

### 2.4 会员模块（TC-MEM）

| 用例ID | 描述 | 前置条件 | 操作步骤 | 预期结果 | 优先级 |
|--------|------|----------|----------|----------|--------|
| TC-MEM-001 | 创建月度会员订单 | 用户已登录 | 1. 调用 createOrder(planType=1) | 1. 创建 MemberOrder<br>2. payAmount=12<br>3. payStatus=0 | P0 |
| TC-MEM-002 | 创建年度会员订单 | 用户已登录 | 1. 调用 createOrder(planType=2) | 1. payAmount=99<br>2. 生成订单号 | P0 |
| TC-MEM-003 | 团队版5人起购校验 | 用户已登录 | 1. 调用 createOrder(planType=3, teamMemberCount=3) | 抛出 BizException，提示"团队版最少5人起购" | P0 |
| TC-MEM-004 | 优惠券下单时不扣减 | 用户有可用优惠券 | 1. 调用 createOrder 带 couponId | 1. 优惠券状态不变<br>2. 订单记录 couponId<br>3. discountAmount > 0 | P1 |
| TC-MEM-005 | 支付成功激活会员 | 订单存在，payStatus=0 | 1. 调用 handlePaySuccess | 1. payStatus→1<br>2. 用户 memberStatus→1<br>3. memberExpireTime 延期 | P0 |
| TC-MEM-006 | 支付成功触发返佣 | 用户有邀请人 | 1. 支付成功 | 发送MQ消息到 pay_success 队列（或同步调用 commissionService） | P1 |
| TC-MEM-007 | 退款撤销会员和返佣 | 订单已支付 | 1. 调用 applyRefund | 1. payStatus→2<br>2. 用户 memberStatus→0<br>3. 调用 commissionService.revokeCommission | P0 |
| TC-MEM-008 | 重复支付幂等处理 | 订单已支付(payStatus=1) | 1. 再次调用 handlePaySuccess | 不重复处理，直接返回 | P0 |
| TC-MEM-009 | 会员到期自动降级 | 用户 memberExpireTime < now | 1. MemberExpireTask 执行 | 用户 memberStatus→0 | P2 |

### 2.5 推广模块（TC-PRO）

| 用例ID | 描述 | 前置条件 | 操作步骤 | 预期结果 | 优先级 |
|--------|------|----------|----------|----------|--------|
| TC-PRO-001 | 邀请好友注册给邀请人加5积分 | 邀请人存在 | 1. 好友携带邀请码注册 | 邀请人积分+5，记录action=invite_register | P0 |
| TC-PRO-002 | 好友付费给邀请人返佣¥19 | 一级邀请关系存在，新手等级 | 1. 好友支付成功 | 1. 创建 Commission(level=1, amount=19)<br>2. 邀请人冻结余额+19 | P0 |
| TC-PRO-003 | 二级返佣¥5 | 二级邀请关系存在 | 1. 好友支付成功 | 创建 Commission(level=2, amount=5) | P1 |
| TC-PRO-004 | 月度返佣封顶¥5000 | 当月返佣总额接近5000 | 1. 触发新返佣 | 超出5000部分不生成返佣记录 | P1 |
| TC-PRO-005 | 推广等级升级（3人→银牌） | paidInviteCount=3 | 1. 调用 checkAndUpgrade | level→2，levelName→银牌推广员 | P1 |
| TC-PRO-006 | 积分兑换7天会员 | 积分≥50 | 1. 调用 redeemPoints(type=1) | 1. 积分-50<br>2. 会员到期时间+7天 | P1 |
| TC-PRO-007 | 提现余额不足被拒绝 | availableBalance < 提现金额 | 1. 调用 applyWithdraw | 抛出 BizException，提示"可提现余额不足" | P0 |
| TC-PRO-008 | 提现每月最多2次 | 当月已提现2次 | 1. 再次申请提现 | 抛出 BizException，提示"每月最多提现2次" | P1 |
| TC-PRO-009 | 并发积分操作安全 | 用户正在执行积分操作 | 1. 同时发起两次积分操作 | Redis分布式锁保证只有一次成功，另一次返回429 | P1 |

### 2.6 安全模块（TC-SEC）

| 用例ID | 描述 | 前置条件 | 操作步骤 | 预期结果 | 优先级 |
|--------|------|----------|----------|----------|--------|
| TC-SEC-001 | 无token请求被拦截 | 不携带 Authorization header | 1. 请求需要登录的接口 | AuthInterceptor 拦截，返回401 | P0 |
| TC-SEC-002 | 过期token被拒绝 | 使用过期的JWT token | 1. 请求接口 | JwtUtil.parseToken 抛出 ExpiredJwtException | P0 |
| TC-SEC-003 | 用户A不能访问用户B的数据 | 两个用户分别有自己的数据 | 1. 用户A请求用户B的费用/报销单 | 返回404（通过 userId 过滤确保隔离） | P0 |
| TC-SEC-004 | 接口限流(60次/分钟) | 无 | 1. 短时间内发送超过60次请求 | RateLimitInterceptor 返回429 | P1 |
| TC-SEC-005 | 管理员接口非管理员被拒绝 | 普通用户 | 1. 请求管理员接口 | AdminInterceptor 拦截，返回403 | P0 |
| TC-SEC-006 | 费用data_sign篡改被检测 | 费用记录有 dataSign | 1. 修改费用金额但不更新 dataSign<br>2. 调用 DataSignService.verify | verify 返回 false | P1 |

---

## 3. 运行测试

### 3.1 后端单元测试

```bash
cd backend
mvn test
```

### 3.2 后端集成测试

```bash
cd backend
mvn verify
```

### 3.3 运行指定测试类

```bash
cd backend
mvn test -Dtest=UserServiceTest
mvn test -Dtest=ExpenseServiceTest
```

### 3.4 生成测试覆盖率报告

```bash
cd backend
mvn test jacoco:report
# 报告位置: target/site/jacoco/index.html
```

---

## 4. CI/CD 配置

### 4.1 GitHub Actions Workflow

项目配置了 `.github/workflows/ci.yml`，在每次 push/PR 到 main 分支时自动执行：

1. **backend-test**：编译后端并运行全部单元测试
2. **frontend-build**：安装前端依赖并构建 mobile/desktop 版本
3. **docker-build**：（仅 main 分支）构建 Docker 镜像

### 4.2 本地 pre-commit 检查（推荐）

```bash
# 后端测试
cd backend && mvn test

# 前端构建检查
cd frontend && pnpm install && pnpm build:mobile
```

---

## 5. 测试文件结构

```
backend/src/test/java/com/qiankubx/
├── BaseServiceTest.java              # 集成测试基类
├── module/
│   ├── user/
│   │   └── service/
│   │       └── UserServiceTest.java
│   ├── expense/
│   │   └── service/
│   │       ├── ExpenseServiceTest.java
│   │       ├── TripServiceTest.java
│   │       └── InvoiceParseEngineTest.java
│   ├── reimbursement/
│   │   └── service/
│   │       └── ReimbursementServiceTest.java
│   ├── member/
│   │   └── service/
│   │       └── MemberServiceTest.java
│   └── promotion/
│       └── service/
│           ├── CommissionServiceTest.java
│           ├── PointsServiceTest.java
│           └── WithdrawalServiceTest.java
└── common/
    ├── security/
    │   └── DataSignServiceTest.java
    └── util/
        └── JwtUtilTest.java
```

---

## 6. 编写测试规范

### 6.1 命名规范

- 测试类：`{被测类名}Test`
- 测试方法：`{方法名}_{场景}_{预期结果}`
- 示例：`smsLogin_wrongCode_shouldThrowException`

### 6.2 测试结构（AAA 模式）

```java
@Test
void methodName_scenario_expectedResult() {
    // Arrange - 准备测试数据和 mock
    // Act - 执行被测方法
    // Assert - 验证结果
}
```

### 6.3 Mock 使用原则

- 使用 `@ExtendWith(MockitoExtension.class)` 而非 `@SpringBootTest`（单元测试）
- 使用 `@Mock` 标注外部依赖
- 使用 `@InjectMocks` 注入被测对象
- 使用 `when(...).thenReturn(...)` 设定返回值
- 使用 `verify(...)` 验证方法调用
