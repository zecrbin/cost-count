package com.costcount.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.costcount.dto.account.AccountBalanceDto;
import com.costcount.dto.account.AccountQueryDTO;
import com.costcount.dto.account.AccountSaveDTO;
import com.costcount.dto.transaction.InitialTransactionSaveDTO;
import com.costcount.entity.Account;
import com.costcount.entity.AccountDailyBalance;
import com.costcount.entity.AccountProvider;
import com.costcount.entity.AccountType;
import com.costcount.entity.Transaction;
import com.costcount.exception.BizException;
import com.costcount.mapper.AccountMapper;
import com.costcount.mapper.AccountTypeMapper;
import com.costcount.service.AccountDailyBalanceService;
import com.costcount.service.AccountIconStorageService;
import com.costcount.service.AccountService;
import com.costcount.service.TransactionService;
import com.costcount.vo.account.AccountVO;
import com.github.yulichang.base.MPJBaseServiceImpl;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import jakarta.annotation.Resource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.costcount.common.CommonConstant.isCreditType;
import static com.costcount.common.TransactionConstant.INITIAL;

/**
 * 账户聚合服务，负责账户资料、初始流水及动态图标的协同维护。
 */
@Service
@Slf4j
public class AccountServiceImpl
        extends MPJBaseServiceImpl<AccountMapper, Account>
        implements AccountService {

    @Resource
    private TransactionService transactionService;

    @Resource
    private AccountDailyBalanceService accountDailyBalanceService;

    @Resource
    private AccountIconStorageService accountIconStorageService;

    @Resource
    private AccountTypeMapper accountTypeMapper;

    @Override
    public List<AccountVO> listAccounts(AccountQueryDTO query) {
        AccountQueryDTO param = query == null ? new AccountQueryDTO() : query;
        String accName = StringUtils.trimToNull(param.getAccName());
        String accTailNum = StringUtils.trimToNull(param.getAccTailNum());

        MPJLambdaWrapper<Account> wrapper = accountVOWrapper();
        wrapper.eq(param.getTypeId() != null, Account::getTypeId, param.getTypeId())
                .like(accName != null, Account::getAccName, accName)
                .like(accTailNum != null, Account::getAccTailNum, accTailNum)
                .eq(param.getStatus() != null, Account::getStatus, param.getStatus())
                .orderByAsc(Account::getSort)
                .orderByDesc(Account::getCreatedTime);

        return selectJoinList(AccountVO.class, wrapper);
    }

    @Override
    public AccountVO getAccount(Long id) {
        MPJLambdaWrapper<Account> wrapper = accountVOWrapper();
        wrapper.eq(Account::getId, id);
        return selectJoinOne(AccountVO.class, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String saveAccount(AccountSaveDTO accountSaveDTO) {

        String accTailNum = StringUtils.trimToNull(accountSaveDTO.getAccTailNum());
        checkDuplicateTailNum(accountSaveDTO.getTypeId(), accTailNum, null);

        AccountTypeInfoVO typeInfoVO = getTypeInfo(accountSaveDTO.getTypeId());
        checkCreditLimit(typeInfoVO, accountSaveDTO);

        BigDecimal initialBalance = accountSaveDTO.getInitialBalance() == null
                ? BigDecimal.ZERO
                : accountSaveDTO.getInitialBalance();
        // 发生时间缺省为当前时间；留空会让初始流水没有业务日期，快照重建时会被整条跳过
        LocalDateTime initialTransactionTime = accountSaveDTO.getInitialTransactionTime() == null
                ? LocalDateTime.now()
                : accountSaveDTO.getInitialTransactionTime();
        // 晚于当前的初始流水会排在之后所有新增流水的后面，导致新增流水全部被判为补充
        if (initialTransactionTime.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("初始资金发生时间不能晚于当前时间");
        }

        // 新增账户
        Account account = new Account();
        BeanUtils.copyProperties(accountSaveDTO, account);
        // 主键一律由雪花算法生成，不接受入参携带的 ID
        account.setId(null);
        account.setAccTailNum(accTailNum);
        account.setAccName(buildAccName(typeInfoVO, accTailNum));
        account.setBalance(initialBalance);

        save(account);

        Long accountId = account.getId();

        // 图标文件以账户 ID 命名，只能在主键生成后绘制
        refreshIcon(account, typeInfoVO);
        if (account.getIcon() != null) {
            updateById(account);
        }

        // 补初始化流水
        InitialTransactionSaveDTO transactionSaveDTO = new InitialTransactionSaveDTO();
        transactionSaveDTO.setAccountId(accountId);
        transactionSaveDTO.setAmount(initialBalance);
        transactionSaveDTO.setTransactionTime(initialTransactionTime);
        transactionService.saveInitialTransaction(transactionSaveDTO);

        return String.valueOf(accountId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String updateAccount(AccountSaveDTO accountSaveDTO) {

        Long id = accountSaveDTO.getId();
        if (id == null) {
            throw new IllegalArgumentException("账户 ID 不能为空");
        }
        Account account = baseMapper.selectByIdsForUpdate(List.of(id)).stream().findFirst().orElse(null);
        if (account == null) {
            throw new IllegalArgumentException("账户不存在");
        }

        String accTailNum = StringUtils.trimToNull(accountSaveDTO.getAccTailNum());
        checkDuplicateTailNum(accountSaveDTO.getTypeId(), accTailNum, id);

        AccountTypeInfoVO typeInfoVO = getTypeInfo(accountSaveDTO.getTypeId());
        checkCreditLimit(typeInfoVO, accountSaveDTO);

        // 资产账户余额是可用资金，信用账户余额是待还金额，两者含义相反；
        // 切换性质会让已有流水的余额正负号全部失真
        AccountType currentType = account.getTypeId() == null ? null : accountTypeMapper.selectById(account.getTypeId());
        if (currentType != null && isCreditType(currentType.getTypeCode()) != isCreditType(typeInfoVO.getTypeCode())) {
            throw new IllegalArgumentException("账户不能在资产类和信用类之间切换");
        }

        // 初始资金和余额不在这里修改：前者仅新增时生效，后者走余额修正
        account.setTypeId(accountSaveDTO.getTypeId());
        account.setAccTailNum(accTailNum);
        account.setAccName(buildAccName(typeInfoVO, accTailNum));
        account.setCreditLimit(accountSaveDTO.getCreditLimit());
        account.setIdealCreditLimit(accountSaveDTO.getIdealCreditLimit());
        account.setSort(accountSaveDTO.getSort());
        account.setStatus(accountSaveDTO.getStatus());
        account.setRemark(accountSaveDTO.getRemark());
        refreshIcon(account, typeInfoVO);

        updateAccountDetails(account);

        return String.valueOf(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAccounts(List<Long> accountIds) {

        if (accountIds == null || accountIds.isEmpty() || accountIds.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("账户 ID 不能为空");
        }
        List<Long> ids = accountIds.stream().distinct().toList();
        List<Account> accounts = baseMapper.selectByIdsForUpdate(ids.stream().sorted().toList());

        // 初始流水随账户一起删；其余流水承载余额历史，转账还牵连对方账户，有这类流水时只能停用
        boolean hasTransactions = transactionService.lambdaQuery()
                .ne(Transaction::getTransactionType, INITIAL)
                .and(w -> w.in(Transaction::getAccountId, ids)
                        .or().in(Transaction::getTargetAccountId, ids))
                .exists();
        if (hasTransactions) {
            throw new BizException(409, "账户已有流水，无法删除，可改为停用");
        }

        transactionService.remove(new LambdaQueryWrapper<Transaction>()
                .eq(Transaction::getTransactionType, INITIAL)
                .in(Transaction::getAccountId, ids));
        accountDailyBalanceService.remove(new LambdaQueryWrapper<AccountDailyBalance>()
                .in(AccountDailyBalance::getAccountId, ids));
        removeByIds(ids);

        accounts.forEach(account -> scheduleIconChange(account.getIcon(), null));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String updateBalance(AccountBalanceDto accountBalanceDto) {

        Long accountId = accountBalanceDto.getAccountId();

        // 余额变更一律通过调整流水落库，由流水服务加锁、取差额并联动余额与日快照，
        // 避免账户余额脱离"流水是事实来源"的口径
        transactionService.saveBalanceAdjustment(accountId, accountBalanceDto.getCurrentBalance());

        return String.valueOf(accountId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refreshAccountsOfTypes(Collection<Long> typeIds) {
        if (typeIds == null || typeIds.isEmpty()) {
            return;
        }
        Map<Long, AccountTypeInfoVO> typeInfos = new HashMap<>();
        List<Long> accountIds = lambdaQuery().in(Account::getTypeId, typeIds).list().stream()
                .map(Account::getId).sorted().toList();
        if (accountIds.isEmpty()) {
            return;
        }
        for (Account account : baseMapper.selectByIdsForUpdate(accountIds)) {
            if (!typeIds.contains(account.getTypeId())) {
                continue;
            }
            AccountTypeInfoVO typeInfoVO = typeInfos.computeIfAbsent(account.getTypeId(), this::getTypeInfo);
            account.setAccName(buildAccName(typeInfoVO, account.getAccTailNum()));
            refreshIcon(account, typeInfoVO);
            Account update = new Account();
            update.setId(account.getId());
            update.setAccName(account.getAccName());
            update.setAccTailNum(account.getAccTailNum());
            update.setCreditLimit(account.getCreditLimit());
            update.setIdealCreditLimit(account.getIdealCreditLimit());
            update.setIcon(account.getIcon());
            updateById(update);
        }
    }

    private void updateAccountDetails(Account account) {
        Account update = new Account();
        update.setId(account.getId());
        update.setTypeId(account.getTypeId());
        update.setAccTailNum(account.getAccTailNum());
        update.setAccName(account.getAccName());
        update.setCreditLimit(account.getCreditLimit());
        update.setIdealCreditLimit(account.getIdealCreditLimit());
        update.setSort(account.getSort());
        update.setStatus(account.getStatus());
        update.setRemark(account.getRemark());
        update.setIcon(account.getIcon());
        updateById(update);
    }

    /** 账户展示信息的查询骨架：连带账户类型和提供方。 */
    private MPJLambdaWrapper<Account> accountVOWrapper() {
        MPJLambdaWrapper<Account> wrapper = new MPJLambdaWrapper<>();
        wrapper.select(
                        Account::getId,
                        Account::getTypeId,
                        Account::getAccName,
                        Account::getAccTailNum,
                        Account::getBalance,
                        Account::getCreditLimit,
                        Account::getIdealCreditLimit,
                        Account::getIcon,
                        Account::getSort,
                        Account::getStatus,
                        Account::getRemark
                )
                .select(AccountType::getTypeCode, AccountType::getTypeName)
                .select(AccountProvider::getProviderName)
                .selectAs(AccountProvider::getIcon, AccountVO::getProviderIcon)
                .leftJoin(AccountType.class, AccountType::getId, Account::getTypeId)
                .leftJoin(AccountProvider.class, AccountProvider::getId, AccountType::getProviderId);
        return wrapper;
    }

    /**
     * 同类账户尾号不能重复。
     *
     * <p>没有尾号就没有可比对的尾号，整条校验跳过。这里不能写成 eq(column, null)：
     * 那会生成恒不成立的 {@code acc_tail_num = NULL}，看着在校验，实际什么都没查。</p>
     */
    private void checkDuplicateTailNum(Long typeId, String accTailNum, Long excludeId) {
        if (accTailNum == null) {
            return;
        }
        boolean duplicated = lambdaQuery()
                .eq(Account::getTypeId, typeId)
                .eq(Account::getAccTailNum, accTailNum)
                .ne(excludeId != null, Account::getId, excludeId)
                .exists();
        if (duplicated) {
            throw new IllegalArgumentException("同类账户尾号已存在");
        }
    }

    /** 查询账户类型及其提供方信息，类型不存在时报错。 */
    private AccountTypeInfoVO getTypeInfo(Long typeId) {
        MPJLambdaWrapper<AccountType> wrapper = new MPJLambdaWrapper<>();

        // 别名必须是 getter 方法引用：MPJ 用 PropertyNamer 解析别名，record 访问器会被判成非法属性名
        wrapper.selectAs(AccountProvider::getProviderName, AccountTypeInfoVO::getProviderName)
                .selectAs(AccountProvider::getIcon, AccountTypeInfoVO::getProviderIcon)
                .selectAs(AccountType::getTypeName, AccountTypeInfoVO::getTypeName)
                .selectAs(AccountType::getTypeCode, AccountTypeInfoVO::getTypeCode)
                .leftJoin(AccountProvider.class, AccountProvider::getId, AccountType::getProviderId)
                .eq(AccountType::getId, typeId);

        AccountTypeInfoVO typeInfoVO = accountTypeMapper.selectJoinOne(AccountTypeInfoVO.class, wrapper);
        if (typeInfoVO == null) {
            throw new IllegalArgumentException("账户类型不存在");
        }
        return typeInfoVO;
    }

    /** 信用额度只有信用类账户才有意义，非信用账户带着额度入库后没有任何链路会用到它。 */
    private void checkCreditLimit(AccountTypeInfoVO typeInfoVO, AccountSaveDTO accountSaveDTO) {
        if (!isCreditType(typeInfoVO.getTypeCode())
                && (accountSaveDTO.getCreditLimit() != null || accountSaveDTO.getIdealCreditLimit() != null)) {
            throw new IllegalArgumentException("非信用类账户不能设置信用额度");
        }
    }

    /**
     * 账户名由提供方名、类型名、尾号直接拼接；trimToEmpty 把缺失的段落变成空串，不会出现字面量 "null"。
     *
     * <p>花呗、余额宝这类提供方和类型同名，类型名已带提供方名时不再重复拼接，避免出现"花呗花呗"。</p>
     */
    private String buildAccName(AccountTypeInfoVO typeInfoVO, String accTailNum) {
        String providerName = StringUtils.trimToEmpty(typeInfoVO.getProviderName());
        String typeName = StringUtils.trimToEmpty(typeInfoVO.getTypeName());
        String prefix = typeName.startsWith(providerName) ? "" : providerName;
        return prefix + typeName + StringUtils.trimToEmpty(accTailNum);
    }

    /**
     * 以提供方图标叠加尾号重新生成账户图标。
     *
     * <p>没有尾号时不生成专属图标并清掉旧图标，展示时直接使用提供方图标。</p>
     */
    private void refreshIcon(Account account, AccountTypeInfoVO typeInfoVO) {
        String previousIcon = account.getIcon();
        if (account.getAccTailNum() == null) {
            account.setIcon(null);
        } else {
            account.setIcon(accountIconStorageService.storeAccountIcon(account.getId(),
                    typeInfoVO.getProviderIcon(), typeInfoVO.getProviderName(), account.getAccTailNum()));
        }
        scheduleIconChange(previousIcon, account.getIcon());
    }

    private void scheduleIconChange(String previousIcon, String newIcon) {
        if (Objects.equals(previousIcon, newIcon)) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                deleteIconQuietly(previousIcon);
            }

            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    deleteIconQuietly(newIcon);
                }
            }
        });
    }

    private void deleteIconQuietly(String icon) {
        try {
            accountIconStorageService.deleteAccountIcon(icon);
        } catch (RuntimeException exception) {
            log.error("账户图标清理失败: {}", icon, exception);
        }
    }

    /** 账户类型及其提供方的名称、编码和图标信息。 */
    @Data
    public static class AccountTypeInfoVO {

        /** 账户提供方名称。 */
        private String providerName;

        /** 账户提供方图标，相对于 /icons/default/。 */
        private String providerIcon;

        /** 账户类型名称。 */
        private String typeName;

        /** 账户类型编码，例如 DEBIT、CREDIT。 */
        private String typeCode;
    }
}
