package com.ynixt.sharedfinances.entity

import com.ynixt.sharedfinances.enums.TransactionType
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate

@Entity
class Transaction(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) val id: Long? = null,

    @Enumerated(EnumType.STRING) var type: TransactionType,

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "group_id") var group: Group? = null,

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id") var user: User? = null,

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "bank_account_id") var bankAccount: BankAccount? = null,

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "credit_card_id") var creditCard: CreditCard? = null,

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "credit_card_bill_date_id") var creditCardBillDate: CreditCardBillDate? = null,

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "other_side_id") var otherSide: Transaction? = null,

    var date: LocalDate,
    var value: BigDecimal,
    var installmentId: String? = null,
    var description: String?,
    var installment: Int? = null,
    var totalInstallments: Int? = null
) : AuditedEntity() {
    @ManyToMany
    @JoinTable(
        name = "transaction_has_categories",
        joinColumns = [JoinColumn(name = "transaction_id")],
        inverseJoinColumns = [JoinColumn(name = "category_id")]
    )
    var categories: MutableSet<TransactionCategory>? = null

    @Column(name = "group_id", updatable = false, insertable = false)
    var groupId: Long? = null

    @Column(name = "user_id", updatable = false, insertable = false)
    var userId: Long? = null

    @Column(name = "bank_account_id", updatable = false, insertable = false)
    var bankAccountId: Long? = null

    @Column(name = "credit_card_id", updatable = false, insertable = false)
    var creditCardId: Long? = null

    @Column(name = "credit_card_bill_date_id", updatable = false, insertable = false)
    var creditCardBillDateId: Long? = null

    @Column(name = "other_side_id", updatable = false, insertable = false)
    var otherSideId: Long? = null

    fun copy(
        id: Long? = this.id,
        type: TransactionType = this.type,
        categories: MutableSet<TransactionCategory>? = this.categories,
        group: Group? = this.group,
        user: User? = this.user,
        bankAccount: BankAccount? = this.bankAccount,
        creditCard: CreditCard? = this.creditCard,
        creditCardBillDate: CreditCardBillDate? = this.creditCardBillDate,
        otherSide: Transaction? = this.otherSide,
        date: LocalDate = this.date,
        value: BigDecimal = this.value,
        description: String? = this.description,
        installment: Int? = this.installment,
        installmentId: String? = this.installmentId,
        totalInstallments: Int? = this.totalInstallments,
        groupId: Long? = this.groupId,
        userId: Long? = this.userId,
        bankAccountId: Long? = this.bankAccountId,
        creditCardId: Long? = this.creditCardId,
        otherSideId: Long? = this.otherSideId
    ): Transaction {
        return Transaction(
            id = id,
            type = type,
            group = group,
            user = user,
            bankAccount = bankAccount,
            creditCard = creditCard,
            creditCardBillDate = creditCardBillDate,
            otherSide = otherSide,
            date = date,
            value = value,
            description = description,
            installment = installment,
            installmentId = installmentId,
            totalInstallments = totalInstallments,
        ).apply {
            this.categories = categories
            this.groupId = groupId
            this.userId = userId
            this.bankAccountId = bankAccountId
            this.creditCardId = creditCardId
            this.otherSideId = otherSideId
        }
    }
}
